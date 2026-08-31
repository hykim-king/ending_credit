package com.endit.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.endit.cmn.DTO;
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

/**
 * <pre>
 * Class Name  : ContentServiceImpl
 * Description : 콘텐츠 조회 및 TMDB 콘텐츠 적재 기능을 처리하는 Service 구현체
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    컬렉션 작품 선택용 제목 검색·페이징 조회 추가
 * ------------------------------------------------------------
 * </pre>
 */
@Service
public class ContentServiceImpl implements ContentService {

	private static final String ROLE_ACTOR = "ACTOR";
	private static final String ROLE_DIRECTOR = "DIRECTOR";
	// 한 영화에서 저장할 배우 수
	private static final int MAX_CAST_COUNT = 10;
	// 신규 저장 없이 skip만 나는 페이지가 이만큼 연속되면 조기 종료
	private static final int SKIP_PAGE_LIMIT = 5;
	// 배경 이미지와 포스터를 각각 최대 몇 장까지 저장할지
	private static final int MAX_GALLERY_PER_TYPE = 10;
	// 국제코드 미국인 영화만 DB에 넣기
	private static final String TARGET_PRODUCTION_COUNTRY = "US";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ContentMapper contentMapper;
	private final PersonMapper personMapper;
	private final ContentCreditMapper contentCreditMapper;
	private final ContentImageMapper contentImageMapper;
	private final GenreMapper genreMapper;
	private final ContentGenreMapper contentGenreMapper;

	
	// 라이브러리 진입점.
	private final TmdbApi tmdbApi;
	private final TmdbProperties tmdbProperties;

	public ContentServiceImpl(ContentMapper contentMapper, PersonMapper personMapper,
			ContentCreditMapper contentCreditMapper, ContentImageMapper contentImageMapper,
			GenreMapper genreMapper, ContentGenreMapper contentGenreMapper,
			TmdbApi tmdbApi, TmdbProperties tmdbProperties) {
		this.contentMapper = contentMapper;
		this.personMapper = personMapper;
		this.contentCreditMapper = contentCreditMapper;
		this.contentImageMapper = contentImageMapper;
		this.genreMapper = genreMapper;
		this.contentGenreMapper = contentGenreMapper;
		this.tmdbApi = tmdbApi;
		this.tmdbProperties = tmdbProperties;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ContentVO> retrieve(DTO param) {
		if (param == null) {
			throw new IllegalArgumentException("콘텐츠 조회 조건은 null일 수 없습니다.");
		}

		if (param.getPageNo() <= 0) {
			param.setPageNo(1);
		}
		if (param.getPageSize() <= 0) {
			param.setPageSize(8);
		} else if (param.getPageSize() > 50) {
			param.setPageSize(50);
		}

		if (StringUtils.hasText(param.getSearchWord())) {
			param.setSearchDiv("50");
			param.setSearchWord(param.getSearchWord().trim());
		} else {
			param.setSearchDiv(null);
			param.setSearchWord(null);
		}

		List<ContentVO> contents = contentMapper.doRetrieve(param);
		param.setTotalCnt(contents.isEmpty() ? 0 : contents.get(0).getTotalCnt());

		return contents;
	}

	// 인기 영화 목록을 훑어서, 우리 db에 없는 영화를 limit건 새로 저장할 때까지 계속 조회함.
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
		// 신규 저장이 하나도 없었던 페이지가 연속으로 몇 번째인지
		int EmptyPage = 0;

		try {
			// 신규 저장 건수(insertedCount)가 limit을 채울 때까지 돈다. skip된 건 다음 페이지로 보충한다
			while (insertedCount < limit) {
				// HTTP GET /movie/popular 로 인기 영화 목록 조회 하기
				MovieResultsPage results = tmdbApi.getMovieLists().getPopular(
						tmdbProperties.getLanguage(), page, null);
				// 결과 값이 비어 있다면 break
				if (results.getResults() == null || results.getResults().isEmpty()) {
					break;
				}
				int insertedBeforePage = insertedCount;
				// 가져온 영화 목록 순회
				for (Movie movie : results.getResults()) {
					if (insertedCount >= limit) {
						break;
					}
					processedCount++;
					// 이미 기존에 있는 영화(영화 ID) 라면, 상세 조회 하지 않고 넘어가기
					if (existsTmdbContent(movie.getId())) {
						log.debug("TMDB 영화 skip. 기존 데이터: externalId={}", movie.getId());
						continue;
					}
					// 제작국가가 TARGET_PRODUCTION_COUNTRY가 아니면 상세조회만 하고 저장은 하지 않는다
					if (saveFromTmdb(movie.getId())) {
						insertedCount++;
					}
				}

				// 이 페이지에서 신규 저장이 하나도 없었다면 연속 카운트 증가, 하나라도 있었다면 리셋
				if (insertedCount == insertedBeforePage) {
					EmptyPage++;
					if (EmptyPage >= SKIP_PAGE_LIMIT) {
						log.warn("TMDB 인기 영화 import 조기 종료: 신규 없는 페이지 {}회 연속. processed={}, inserted={}",
								EmptyPage, processedCount, insertedCount);
						break;
					}
				} else {
					EmptyPage = 0;
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
				String externalGenreId = String.valueOf(genre.getId());
				Integer genreId = genreMapper.findGenreIdByExternal(externalGenreId);
				upsertGenre(genre, genreId);
				saved++;
			}
			log.info("TMDB 장르 마스터 동기화: count={}", saved);
		} catch (TmdbException e) {
			throw new IllegalStateException("TMDB 장르 목록 조회 실패", e);
		}
	}

	// 신규 영화만 상세 조회한 뒤 CONTENT와 하위 테이블에 넣는다. 제작국가가 TARGET_PRODUCTION_COUNTRY가 아니면 저장하지 않고 false를 반환한다
	private boolean saveFromTmdb(int tmdbMovieId) {
		try {
			// HTTP GET /movie/영화id + append_to_response=credits 로 영화 상세보기에 CREDIT 정보를 함께 호출함
			// 이유는 API 호출 횟수를 줄이기 위해서
			// 여기서 보면 영화 상세 (.getdetails) 랑 MovieAppendToResponse.CREDITS 로 크레딧 정보 받아오고 있음.
			MovieDb movie = tmdbApi.getMovies().getDetails(
					tmdbMovieId, tmdbProperties.getLanguage(), MovieAppendToResponse.CREDITS);

			// popular API에는 제작국가 필터가 없어서, 상세조회 응답을 받은 뒤 여기서 걸러낸다
			if (!isProducedIn(movie, TARGET_PRODUCTION_COUNTRY)) {
				log.debug("TMDB 영화 skip. 제작국가 불일치: externalId={}", tmdbMovieId);
				return false;
			}

			ContentVO content = toContentVO(movie);

			int contentId = saveContent(content);
			// 받은 데이터를 content 하위 테이블 "크레딧" 에 넣는 로직
			syncCredits(contentId, movie.getCredits());
			// syncImages 안에 호출해서 이미지 집어넣는거 다 포함 되어 있음.
			syncImages(contentId, tmdbMovieId);
			// 장르는 credits가 아님. 상세 JSON에 이미 들어있는 genres로 CONTENT_GENRE만 연결
			syncContentGenres(contentId, movie);
			return true;
		} catch (TmdbException e) {
			throw new IllegalStateException("TMDB 영화 조회 실패: " + tmdbMovieId, e);
		}
	}

	// 제작국가 목록에 미국 국제 코드가가 있는지 확인
	private boolean isProducedIn(MovieDb movie, String isoCode) {
		if (movie.getProductionCountries() == null) {
			return false;
		}
		return movie.getProductionCountries().stream()
				.anyMatch(country -> isoCode.equalsIgnoreCase(country.getIsoCode()));
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
		return upsertGenre(genre, genreId);
	}

	// 장르가 없으면 insert, 있으면 이름만 update한다.
	// genreId는 호출부가 이미 조회해둔 값(없으면 null)을 그대로 받는다.
	private int upsertGenre(Genre genre, Integer genreId) {
		String externalGenreId = String.valueOf(genre.getId());

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
