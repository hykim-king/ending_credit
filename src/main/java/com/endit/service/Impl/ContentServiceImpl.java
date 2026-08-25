package com.endit.service.Impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.endit.config.TmdbProperties;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.ContentGenreVO;
import com.endit.domain.ContentImageVO;
import com.endit.domain.ContentVO;
import com.endit.domain.GenreVO;
import com.endit.domain.PersonVO;
import com.endit.mapper.ContentCreditMapper;
import com.endit.mapper.ContentGenreMapper;
import com.endit.mapper.ContentImageMapper;
import com.endit.mapper.ContentMapper;
import com.endit.mapper.GenreMapper;
import com.endit.mapper.PersonMapper;
import com.endit.service.ContentService;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.core.Genre;
import info.movito.themoviedbapi.model.core.Movie;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.core.ProductionCountry;
import info.movito.themoviedbapi.model.core.image.Artwork;
import info.movito.themoviedbapi.model.movies.Cast;
import info.movito.themoviedbapi.model.movies.Credits;
import info.movito.themoviedbapi.model.movies.Crew;
import info.movito.themoviedbapi.model.movies.Images;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
// TMDB에서 영화, 인물, 이미지, 장르를 받아 Oracle에 저장하는 서비스
public class ContentServiceImpl implements ContentService {

	private static final String ROLE_ACTOR = "ACTOR";
	private static final String ROLE_DIRECTOR = "DIRECTOR";
	// 한 영화에서 저장할 배우 수
	private static final int MAX_CAST_COUNT = 10;
	// 배경 이미지와 포스터를 각각 최대 몇 장까지 저장할지
	private static final int MAX_GALLERY_PER_TYPE = 10;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ContentMapper contentMapper;
	private final PersonMapper personMapper;
	private final ContentCreditMapper contentCreditMapper;
	private final ContentImageMapper contentImageMapper;
	private final GenreMapper genreMapper;
	private final ContentGenreMapper contentGenreMapper;

	
	// 라이브러리 진입점. movies, genre, images 호출할 때 이 객체 씀. configuration은 안 침
	private final TmdbApi tmdbApi;
	private final TmdbProperties tmdbProperties;

	// 인기 영화 목록을 limit건까지 훑고, 우리 db에 없는 영화만 상세 조회해서 넣음.
	// 이미 있는 영화는 skip. skip해도 processedCount는 올라감. 반환값은 진짜 새로 넣은 건수.
	// 장르 코드표를 영화보다 먼저 넣고, 전체 import는 트랜잭션 한 방임
	@Override
	@Transactional
	public int importPopular(int limit) {
		// API 키가 존재하는지 확인  
		validateApiKey();
		// 영화보다 장르 코드표-> 장르를 먼저 넣는다
		syncGenreMaster();

		if (limit <= 0) {
			limit = 100;
		}

		int processedCount = 0;
		int insertedCount = 0;
		// TMDB 인기 목록은 한 페이지에 20건이다
		int page = 1;

		try {
			// 최대 조회가 되기 전까지, skip 해도 카운트는 오름 
			while (processedCount < limit) {
				// HTTP GET /movie/popular 로 인기 영화 목록 조회 하기 
				MovieResultsPage results = tmdbApi.getMovieLists().getPopular(
						tmdbProperties.getLanguage(), page, null);
				// 결과 값이 비어 있다면 break 
				if (results.getResults() == null || results.getResults().isEmpty()) {
					break;
				}
				// 가져온 영화 목록 순회
				for (Movie movie : results.getResults()) {
					if (processedCount >= limit) {
						break;
					}
					processedCount++;
					// 이미 기존에 있는 영화(영화 ID) 라면, 상세 조회 하지 않고 넘어가기 
					if (existsTmdbContent(movie.getId())) {
						log.debug("TMDB 영화 skip. 기존 데이터: externalId={}", movie.getId());
						continue;
					}
					// 기존에 없는 영화를 상세조회하고, 콘텐츠(CONTENT) 테이블 및 하위 테이블, 기타 테이블에 데이터를 집어옇는 메서드 
					saveFromTmdb(movie.getId());
					insertedCount++;
				}
				
				if (results.getTotalPages() == null || page >= results.getTotalPages()) {
					break;
				}
				page++;
			}
			log.info("TMDB 인기 영화 import: processed={}, inserted={}", processedCount, insertedCount);
			return insertedCount;
		} catch (TmdbException e) {
			throw new IllegalStateException("TMDB 인기 영화 조회 실패", e);
		}
	}

	// 외부 영화 id (TMDB에서 제공하는 영화 ID값)가 이미 우리 테이블에 있는지 확인하는 메서드 
	private boolean existsTmdbContent(int tmdbMovieId) {
		return contentMapper.findContentIdByExternal(String.valueOf(tmdbMovieId)) != null;
	}

	//장르 코드표를 받아 GENRE에 넣음. API 는 영화 목록 조회와 다름. 영화 목록 조회 하기 전에 GENRE 정보를 먼저 받아와 삽입. 이건
	//있으면 업데이트하고 없으면 새로 삽입함. 
	private void syncGenreMaster() {
		try {
			// HTTP GET /genre/movie/list 
			List<Genre> genres = tmdbApi.getGenre().getMovieList(tmdbProperties.getLanguage());
			// 장르 지대로 받아왔나 비어있나 검사 
			if (genres == null || genres.isEmpty()) {
				return;
			}

			int saved = 0;
			// 장르 순회하며 비어 있으면 pass 
			for (Genre genre : genres) {
				if (genre == null) {
					continue;
				}
				// 말그대로 업서트. 있으면 업데이트 없으면 인서트 ㅋㅋ 업 + 서트 ㅋㅋ
				upsertGenre(genre);
				saved++;
			}
			log.info("TMDB 장르 마스터 동기화: count={}", saved);
		} catch (TmdbException e) {
			throw new IllegalStateException("TMDB 장르 목록 조회 실패", e);
		}
	}

	// 신규 영화만 상세 조회한 뒤 CONTENT와 하위 테이블에 넣는다
	private void saveFromTmdb(int tmdbMovieId) {
		try {
			// HTTP GET /movie/영화id + append_to_response=credits 로 영화 상세보기에 CREDIT 정보를 함께 호출함 
			// 이유는 API 호출 횟수를 줄이기 위해서 
			// 여기서 보면 영화 상세 (.getdetails) 랑 MovieAppendToResponse.CREDITS 로 크레딧 정보 받아오고 있음.
			MovieDb movie = tmdbApi.getMovies().getDetails(
					tmdbMovieId, tmdbProperties.getLanguage(), MovieAppendToResponse.CREDITS);

			ContentVO content = toContentVO(movie);
			
			int contentId = saveContent(content);
			// 받은 데이터를 content 하위 테이블 "크레딧" 에 넣는 로직  
			syncCredits(contentId, movie.getCredits());
			// syncImages 안에 호출해서 이미지 집어넣는거 다 포함 되어 있음. 
			syncImages(contentId, tmdbMovieId);
			// 장르는 credits가 아님. 상세 JSON에 이미 들어있는 genres로 CONTENT_GENRE만 연결
			syncContentGenres(contentId, movie);
		} catch (TmdbException e) {
			throw new IllegalStateException("TMDB 영화 조회 실패: " + tmdbMovieId, e);
		}
	}

	// 컨텐츠 저장 
	private int saveContent(ContentVO content) {
		contentMapper.doSave(content);
		log.info("TMDB 영화 등록: externalId={}, titleKo={}", content.getExternalId(), content.getTitleKo());
		return content.getContentId();
	}

	// 배우 상위 10명과 감독을 PERSON, CONTENT_CREDIT에 저장함.. 크레딧에 배우, 감독이 n 명 오는데, 이거 다 넣기 싫어서 
	// 배우 상위 10명만 넣기로 혼자 판단. 이때 감독은 10명에 포함 안됨. 
	private void syncCredits(int contentId, Credits credits) {
		try {
			if (credits == null) {
				log.warn("TMDB 크레딧 없음: contentId={}", contentId);
				return;
			}

			List<Cast> castList = pickTopCast(credits.getCast());
			for (Cast cast : castList) {
				int personId = saveOrUpdatePerson(
						cast.getId(),
						cast.getName(),
						cast.getOriginalName(),
						cast.getProfilePath());
				saveCredit(contentId, personId, ROLE_ACTOR, cast.getCharacter(),
						cast.getOrder() != null ? cast.getOrder() : 0);
			}

			List<Crew> directors = pickDirectors(credits.getCrew());
			int directorOrder = 0;
			for (Crew crew : directors) {
				int personId = saveOrUpdatePerson(
						crew.getId(),
						crew.getName(),
						crew.getOriginalName(),
						crew.getProfilePath());
				// 감독은 배역명이 없어 character를 넣지 않는다
				saveCredit(contentId, personId, ROLE_DIRECTOR, null, directorOrder++);
			}

			log.info("TMDB 크레딧 동기화: contentId={}, cast={}, directors={}",
					contentId, castList.size(), directors.size());
		} catch (Exception e) {
			log.warn("TMDB 크레딧 동기화 실패. 영화는 유지한다: contentId={}", contentId, e);
		}
	}

	// 영화 갤러리 이미지를 CONTENT_IMAGE에 저장한다
	private void syncImages(int contentId, int tmdbMovieId) {
		try {
			// HTTP GET /movie/영화id/images
			Images images = tmdbApi.getMovies().getImages(
					tmdbMovieId, tmdbProperties.getLanguage(), "null", "ko", "en");
			// 이미지 종류는 빽드롭과 포스터가 있음. 근데 지금 우리 db 는 따로 구분 안하고 있음. 문제는 영화 상세보기에 이미 있는
			// 영화 포스터와 빽드롭이 여기 중복으로 들어올 수 있을수도??
			int backdropCount = saveArtworks(contentId, images.getBackdrops());
			int posterCount = saveArtworks(contentId, images.getPosters());

			log.info("TMDB 이미지 동기화: contentId={}, backdrop={}, poster={}",
					contentId, backdropCount, posterCount);
		} catch (Exception e) {
			log.warn("TMDB 이미지 동기화 실패. 영화는 유지한다: movieId={}", tmdbMovieId, e);
		}
	}

	// 투표 점수가 높은 이미지부터 최대 10장을 저장한다
	private int saveArtworks(int contentId, List<Artwork> artworks) {
		if (artworks == null || artworks.isEmpty()) {
			return 0;
		}

		List<Artwork> sorted = new ArrayList<>(artworks);
		// voteAverage는 이 이미지에 대한 사용자 점수다. 영화 평점이 아니다
		sorted.sort(Comparator.comparing(Artwork::getVoteAverage, Comparator.nullsLast(Double::compareTo)).reversed());
		if (sorted.size() > MAX_GALLERY_PER_TYPE) {
			sorted = sorted.subList(0, MAX_GALLERY_PER_TYPE);
		}

		int saved = 0;
		for (Artwork artwork : sorted) {
			String path = toImagePath(artwork.getFilePath());
			if (!StringUtils.hasText(path)) {
				continue;
			}
			ContentImageVO image = new ContentImageVO();
			image.setContentId(contentId);
			image.setImageUrl(path);
			contentImageMapper.doSave(image);
			saved++;
		}
		return saved;
	}

	// 영화와 장르를 CONTENT_GENRE로 연결한다
	private void syncContentGenres(int contentId, MovieDb movie) {
		try {
			List<Genre> genres = movie.getGenres();
			if (genres == null || genres.isEmpty()) {
				log.info("TMDB 장르 없음: contentId={}", contentId);
				return;
			}

			int saved = 0;
			for (Genre genre : genres) {
				if (genre == null) {
					continue;
				}
				int genreId = resolveGenreId(genre);
				ContentGenreVO contentGenre = new ContentGenreVO();
				contentGenre.setContentId(contentId);
				contentGenre.setGenreId(genreId);
				contentGenreMapper.doSave(contentGenre);
				saved++;
			}
			log.info("TMDB 장르 연결 동기화: contentId={}, genres={}", contentId, saved);
		} catch (Exception e) {
			log.warn("TMDB 장르 연결 동기화 실패. 영화는 유지한다: contentId={}", contentId, e);
		}
	}

	// 외부 (TMDB에서 주는  장르 id) id로 우리 genre_id를 찾고, 없으면 새로 
	private int resolveGenreId(Genre genre) {
		String externalGenreId = String.valueOf(genre.getId());
		Integer genreId = genreMapper.findGenreIdByExternal(externalGenreId);
		if (genreId != null) {
			return genreId;
		}
		return upsertGenre(genre);
	}

	// 장르가 없으면 insert, 있으면 이름만 update한다
	private int upsertGenre(Genre genre) {
		String externalGenreId = String.valueOf(genre.getId());
		Integer genreId = genreMapper.findGenreIdByExternal(externalGenreId);

		GenreVO vo = new GenreVO();
		vo.setExternalGenreId(externalGenreId);
		vo.setName(StringUtils.hasText(genre.getName()) ? genre.getName() : externalGenreId);

		if (genreId == null) {
			genreMapper.doSave(vo);
			return vo.getGenreId();
		}

		vo.setGenreId(genreId);
		genreMapper.doUpdate(vo);
		return genreId;
	}

	// 출연 순서가 빠른 배우부터 상위 10명만 고른다
	private List<Cast> pickTopCast(List<Cast> castList) {
		if (castList == null || castList.isEmpty()) {
			return List.of();
		}
		List<Cast> sorted = new ArrayList<>(castList);
		// order가 작을수록 주연에 가깝다
		sorted.sort(Comparator.comparing(Cast::getOrder, Comparator.nullsLast(Integer::compareTo)));
		if (sorted.size() > MAX_CAST_COUNT) {
			return sorted.subList(0, MAX_CAST_COUNT);
		}
		return sorted;
	}

	// 제작진 중 job이 Director인 사람만 고르고, 같은 인물은 한 번만 넣는다
	private List<Crew> pickDirectors(List<Crew> crewList) {
		if (crewList == null || crewList.isEmpty()) {
			return List.of();
		}
		List<Crew> directors = new ArrayList<>();
		List<Integer> seenIds = new ArrayList<>();
		for (Crew crew : crewList) {
			if (!"Director".equalsIgnoreCase(crew.getJob())) {
				continue;
			}
			if (seenIds.contains(crew.getId())) {
				continue;
			}
			seenIds.add(crew.getId());
			directors.add(crew);
		}
		return directors;
	}

	// 인물이 있으면 이름과 프로필을 update하고, 없으면 insert한다
	private int saveOrUpdatePerson(int tmdbPersonId, String name, String originalName,
			String profilePath) {
		PersonVO person = toPersonVO(tmdbPersonId, name, originalName, profilePath);
		Integer personId = personMapper.findPersonIdByExternal(person.getExternalId());

		if (personId == null) {
			personMapper.doSave(person);
			return person.getPersonId();
		}

		person.setPersonId(personId);
		personMapper.doUpdate(person);
		return personId;
	}

	// 라이브러리가 준 인물 정보를 우리 person VO로 바꿈.
	// name은 ko-KR로 요청했을 때 한글 이름, originalName은 원어 이름.
	// 프로필도 풀 URL 말고 path만 넣음. 이름 둘 다 없으면 NAME_KO는 UNKNOWN으로 때움
	private PersonVO toPersonVO(int tmdbPersonId, String name, String originalName,
			String profilePath) {
		PersonVO person = new PersonVO();
		person.setExternalId(String.valueOf(tmdbPersonId));

		// name은 요청 언어 이름, originalName은 원어 이름
		String nameOrg = StringUtils.hasText(originalName) ? originalName : name;
		String nameKo = StringUtils.hasText(name) ? name : nameOrg;
		if (!StringUtils.hasText(nameKo)) {
			nameKo = "UNKNOWN";
		}

		person.setNameKo(nameKo);
		person.setNameOrg(nameOrg);
		person.setProfileImageUrl(toImagePath(profilePath));
		return person;
	}
	// 받아온 정보를 우리 credit vo 에 넣는 작업 + db 에 저장 
	private void saveCredit(int contentId, int personId, String role, String character, int displayOrder) {
		ContentCreditVO credit = new ContentCreditVO();
		credit.setContentId(contentId);
		credit.setPersonId(personId);
		credit.setRole(role);
		credit.setCharacter(character);
		credit.setDisplayOrder(displayOrder);
		contentCreditMapper.doSave(credit);
	}

	// 라이브러리가 만든 Moviedb를 우리 컨텐츠 테이블 VO로 바꾸는 로직. 라이브러리가 json 을 받아서 자바 객체로 변환해주는 것 까지는 함
	// 근데 우리 db 에 맞게 VO 에 집어넣어야함. 
	private ContentVO toContentVO(MovieDb movie) {
		ContentVO content = new ContentVO();
		content.setExternalId(String.valueOf(movie.getId()));
		content.setTitleKo(resolveTitleKo(movie));
		content.setTitleOrg(movie.getOriginalTitle());
		content.setOverview(movie.getOverview());
		content.setReleaseYear(movie.getReleaseDate());
		content.setRuntimeMin(movie.getRuntime() != null ? movie.getRuntime() : 0);
		content.setCountry(resolveCountry(movie));
		content.setPosterUrl(toImagePath(movie.getPosterPath()));
		content.setBackdropUrl(toImagePath(movie.getBackdropPath()));
		return content;
	}
	
	// 한글 제목 고르는 메서드. language가 ko-KR이면 title이 한글제목임.
	// 한글제목 없으면 원제로 넣음. TITLE_KO가 비는 것보다 원제라도 있는 게 나음
	private String resolveTitleKo(MovieDb movie) {
		if (StringUtils.hasText(movie.getTitle())) {
			return movie.getTitle();
		}
		return movie.getOriginalTitle();
	}

	// 제작국가가 여러 개면 첫 번째만 대표 국가로 쓴다. 나머지 컷 
	private String resolveCountry(MovieDb movie) {
		if (movie.getProductionCountries() == null || movie.getProductionCountries().isEmpty()) {
			return null;
		}
		ProductionCountry country = movie.getProductionCountries().get(0);
		return country.getName();
	}

	// TMDB 이미지 경로만(예시: /abc.jpg) 저장. CDN 주소와 크기는 보여줄 때 붙인다
	private String toImagePath(String path) {
		if (!StringUtils.hasText(path)) {
			return null;
		}
		return path;
	}
	//api 키 없으면 안되니께 확인하기 
	private void validateApiKey() {
		if (!StringUtils.hasText(tmdbProperties.getApiKey())) {
			throw new IllegalStateException("tmdb.api-key 설정이 필요합니다.");
		}
	}

}
