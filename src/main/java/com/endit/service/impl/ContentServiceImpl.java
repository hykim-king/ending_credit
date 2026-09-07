package com.endit.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.endit.domain.EnglishContentVO;
import com.endit.domain.GenreVO;
import com.endit.domain.PersonVO;
import com.endit.mapper.ContentCreditMapper;
import com.endit.mapper.ContentGenreMapper;
import com.endit.mapper.ContentImageMapper;
import com.endit.mapper.ContentMapper;
import com.endit.mapper.GenreMapper;
import com.endit.mapper.PersonMapper;
import com.endit.service.ContentImageService;
import com.endit.service.ContentService;
import com.endit.service.GenreService;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.core.Genre;
import info.movito.themoviedbapi.model.core.Movie;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.core.ProductionCountry;
import info.movito.themoviedbapi.model.core.image.Artwork;
import info.movito.themoviedbapi.model.movies.Cast;
import info.movito.themoviedbapi.model.movies.Credits;
import info.movito.themoviedbapi.model.movies.Crew;
import info.movito.themoviedbapi.model.movies.Data;
import info.movito.themoviedbapi.model.movies.Images;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.model.movies.ReleaseDate;
import info.movito.themoviedbapi.model.movies.ReleaseInfo;
import info.movito.themoviedbapi.model.movies.Translation;
import info.movito.themoviedbapi.model.people.PersonDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse;
import info.movito.themoviedbapi.tools.appendtoresponse.PersonAppendToResponse;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
import info.movito.themoviedbapi.tools.sortby.DiscoverMovieSortBy;

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
 * 2026. 8. 31. jinyoung    영화 상세용 sync·get 및 이미지 서비스 구조와 통합
 * ------------------------------------------------------------
 * </pre>
 */
@Service
public class ContentServiceImpl implements ContentService {

	private static final String ROLE_ACTOR = "ACTOR";
	private static final String ROLE_DIRECTOR = "DIRECTOR";
	// TMDB crew의 job 값. 우리 ROLE_DIRECTOR와 표기가 달라 따로 둔다
	private static final String DIRECTOR_JOB = "Director";
	// 한 영화에서 저장할 배우 수
	private static final int MAX_CAST = 10;
	// 배경 이미지와 포스터를 각각 최대 몇 장까지 저장할지
	private static final int MAX_GALLERY_PER_TYPE = 10;
	// TRANSLATIONS 응답에서 영문 번역을 찾을 때 쓰는 iso_639_1 코드
	private static final String LANG_EN = "en";
	// 인물 TRANSLATIONS의 data 안에 있는 이름 키. 라이브러리가 매핑을 안 해 맵에서 직접 꺼낸다
	private static final String TRANSLATION_NAME_KEY = "name";

	// contentWhere가 지원하는 검색 축. 이 넷 밖의 값이 오면 WHERE 절이 통째로 빠져 전체 조회가 되므로 서비스에서 막는다
	private static final String SEARCH_BY_TITLE_KO = "10";
	private static final String SEARCH_BY_TITLE_ORG = "20";
	private static final String SEARCH_BY_COUNTRY = "30";
	private static final String SEARCH_BY_EXTERNAL_ID = "40";
	private static final String SEARCH_BY_TITLE = "50";

	// 정렬 축 - searchWord를 검색 조건이 쓰고 있어 searchMap의 이 키로 받는다
	private static final String SEARCH_KEY_SORT = "sort";

	// 매퍼 contentWhere가 받는 필터 통로. 인기순 경로가 어떤 필터를 감당할 수 있는지 판정하는 데 쓴다.
	// released는 목록을 만들 때 이미 개봉작만 담으므로(syncGenreRank의 releaseDateLte) 여기 세지 않는다
	private static final String SEARCH_KEY_GENRE_ID = "genreId";
	private static final String SEARCH_KEY_PERSON_ID = "personId";
	private static final String SEARCH_KEY_DECADE = "decade";
	private static final String SEARCH_KEY_EXTERNAL_ID = "externalId";
	// 최신순 - 개봉일 기준이다. 적재 시점이 필요하면 SORT_REGISTERED를 쓴다
	private static final String SORT_LATEST = "latest";
	private static final String SORT_BOX_OFFICE = "boxoffice";
	// 관련도 - 검색어가 있어야 뜻이 있어 매퍼가 검색어 없는 호출은 기본 정렬로 흘린다
	private static final String SORT_RELEVANCE = "relevance";
	// 등록순 - TMDB 적재 시각 기준
	private static final String SORT_REGISTERED = "registered";
	// 인기순 - 매퍼가 아니라 서비스가 순위 목록으로 정렬한다. 순위가 비면 SORT_BOX_OFFICE로 흘린다
	private static final String SORT_POPULAR = "popular";

	private static final Set<String> ALLOWED_SORT =
			Set.of(SORT_LATEST, SORT_BOX_OFFICE, SORT_RELEVANCE, SORT_REGISTERED, SORT_POPULAR);

	private static final int FIRST_RANK_PAGE = 1;
	// 순위 목록에 채울 목표 건수. 화면이 쓰는 건 앞의 20건뿐이고 나머지는 교차검증용 표본이다.
	// ContentCreditServiceImpl.MAX_TOP_PERSON_POOL과 같은 값이라 getTopPerson이 목록을 자르지 않는다
	private static final int TARGET_RANK_SIZE = 500;
	// 목표를 못 채워도 여기서 멈춘다. 한 페이지 20건이므로 최대 800건까지 훑는다
	private static final int MAX_RANK_PAGE = 40;

	// 장르별 순위는 홈 선반 한 줄(21건)이면 되지만, 교차검증 표본으로 쓰려고 여유를 뒀다
	private static final int TARGET_GENRE_RANK_SIZE = 50;
	// 장르 수만큼 곱해지는 상한이다. 장르 19개면 호출이 최대 95회이므로 함부로 올리지 않는다
	private static final int MAX_GENRE_RANK_PAGE = 5;

	// LIKE 이스케이프 - 매퍼의 ESCAPE '\'와 짝이다. 역슬래시를 먼저 바꿔야 이중 이스케이프가 안 난다
	private static final String LIKE_ESCAPE_CHAR = "\\";

	// limit을 안 주거나 0 이하로 준 호출의 기본 수집 건수
	private static final int DEFAULT_SYNC_LIMIT = 100;

	// 성인물 차단 - TMDB의 adult 플래그는 포르노 전용이라 항상 false다. 국가별 등급으로만 판정된다
	private static final String CERTIFICATION_COUNTRY_KR = "KR";
	// 이 나이 이상의 숫자 등급을 성인으로 본다(19·18). 12·15는 통과
	private static final int ADULT_AGE_LIMIT = 18;
	private static final int MAX_AGE_DIGITS = 3;
	// "19+"처럼 기호가 붙은 변종이 실제로 온다. 숫자 부분만 떼어 나이로 읽는다
	private static final Pattern AGE_IN_CERTIFICATION = Pattern.compile("\\d+");
	private static final String CERTIFICATION_ADULT_ONLY = "청소년관람불가";
	private static final String CERTIFICATION_RESTRICTED = "제한상영";

	// 영문 줄거리를 받을 때 쓰는 언어. tmdbProperties.getLanguage()는 ko-KR이라 그대로 쓸 수 없다
	private static final String LANGUAGE_EN = "en-US";

	// 한국 등급이 없는 작품이 20%쯤 되어 미국 등급으로 보완한다
	private static final String CERTIFICATION_COUNTRY_US = "US";

	// 나이 숫자로는 못 잡는 미국 성인 등급. R·X·AO는 숫자가 없고, NC-17은 숫자만 뽑으면 17이라 미성년으로 읽힌다.
	// R을 넣는 것은 "아이 원트 유어 섹스"(KR 없음·US R)처럼 R에서 새어 나온 사례가 실제로 있었기 때문이다
	private static final Set<String> ADULT_CERTIFICATIONS = Set.of("R", "NC-17", "X", "AO");

	private static final int FIRST_PAGE_NO = 1;
	private static final int DEFAULT_PAGE_SIZE = 12;
	private static final int MAX_PAGE_SIZE = 100;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ContentMapper contentMapper;
	private final PersonMapper personMapper;
	private final ContentCreditMapper contentCreditMapper;
	private final ContentImageMapper contentImageMapper;
	private final GenreMapper genreMapper;
	private final ContentGenreMapper contentGenreMapper;
	private final ContentImageService contentImageService;
	// 장르 마스터 읽기는 GenreService 계약을 그대로 쓴다. 쓰기(syncGenreMaster)만 genreMapper로 직접 간다
	private final GenreService genreService;


	// 라이브러리 진입점.
	private final TmdbApi tmdbApi;
	private final TmdbProperties tmdbProperties;

	// 스케줄러 스레드가 쓰고 웹 요청 스레드가 읽는다. 통째로 교체하므로 잠금 없이도 중간 상태가 보이지 않는다
	private volatile List<Integer> rankedIds = List.of();

	// 장르별 순위. rankedIds와 같은 이유로 통째로 교체한다. 키는 GENRE.genre_id다
	private volatile Map<Integer, List<Integer>> genreRankedIds = Map.of();

	// 영문 표시값 캐시. 위 둘과 달리 통째로 갈지 않고 조회할 때마다 한 칸씩 채우므로 ConcurrentHashMap이다.
	// 줄거리·포스터·배경이 상세 응답 하나에서 함께 오므로 묶어서 담는다 - 따로 담으면 같은 호출을 두 번 하게 된다
	private final Map<Integer, EnglishContentVO> englishContents = new ConcurrentHashMap<>();

	public ContentServiceImpl(
			ContentMapper contentMapper,
			PersonMapper personMapper,
			ContentCreditMapper contentCreditMapper,
			ContentImageMapper contentImageMapper,
			GenreMapper genreMapper,
			ContentGenreMapper contentGenreMapper,
			ContentImageService contentImageService,
			GenreService genreService,
			TmdbApi tmdbApi,
			TmdbProperties tmdbProperties) {
		this.contentMapper = contentMapper;
		this.personMapper = personMapper;
		this.contentCreditMapper = contentCreditMapper;
		this.contentImageMapper = contentImageMapper;
		this.genreMapper = genreMapper;
		this.contentGenreMapper = contentGenreMapper;
		this.contentImageService = contentImageService;
		this.genreService = genreService;
		this.tmdbApi = tmdbApi;
		this.tmdbProperties = tmdbProperties;
	}

	// 인기 영화 목록을 훑어서, 우리 db에 없는 영화를 limit건 새로 저장할 때까지 계속 조회함.
	@Override
	@Transactional
	public int sync(int limit) {
		// API 키가 존재하는지 확인
		validateApiKey();
		// 영화보다 장르 코드표-> 장르를 먼저 넣는다
		syncGenreMaster();

		if (limit <= 0) {
			limit = DEFAULT_SYNC_LIMIT;
		}

		int processedCount = 0;
		int insertedCount = 0;
		// 스킵 사유를 나눠 센다. 등급은 DB에 안 남으므로 몇 편이 왜 빠졌는지는 이 로그로만 확인된다
		int skippedCount = 0;
		int adultSkippedCount = 0;
		// TMDB 인기 목록은 한 페이지에 20건이다
		int page = 1;

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
				// 가져온 영화 목록 순회
				for (Movie movie : results.getResults()) {
					if (insertedCount >= limit) {
						break;
					}
					processedCount++;
					// 이미 기존에 있는 영화(영화 ID) 라면, 상세 조회 하지 않고 넘어가기
					if (existsTmdbContent(movie.getId())) {
						skippedCount++;
						log.debug("TMDB 영화 skip. 기존 데이터: externalId={}", movie.getId());
						continue;
					}

					// 한국 성인등급이면 저장하지 않는다. 저장 안 한 건은 limit에 세지 않으므로 다음 페이지로 보충된다
					if (saveFromTmdb(movie.getId())) {
						insertedCount++;
					} else {
						adultSkippedCount++;
					}
				}

				if (results.getTotalPages() == null || page >= results.getTotalPages()) {
					break;
				}
				page++;
			}
			log.info("TMDB 인기 영화 import: processed={}, inserted={}, 기존보유 skip={}, 성인등급 skip={}",
					processedCount, insertedCount, skippedCount, adultSkippedCount);
			return insertedCount;
		} catch (TmdbException e) {
			throw new IllegalStateException("TMDB 인기 영화 조회 실패", e);
		}
	}

	// @Transactional을 붙이지 않는다. TMDB 호출이 수 초 걸려 커넥션을 그동안 붙잡게 되고, 읽기만 해서 얻을 것도 없다
	@Override
	public int syncRank() {
		validateApiKey();

		List<Integer> matched = new ArrayList<>();
		// 페이지를 넘기는 사이 popularity가 밀리면 같은 영화가 두 번 온다
		Set<Integer> seenTmdbIds = new HashSet<>();
		// 목표가 500건이라 findContentIdByExternal을 건건이 부르면 기동 시 쿼리가 수천 건이 된다.
		// 장르별 동기화와 같은 방식으로 대응표를 먼저 한 번에 읽는다
		Map<String, ContentVO> contentByExternal = toContentByExternal();
		int skipped = 0;
		int page = FIRST_RANK_PAGE;

		try {
			// 훑은 건수가 아니라 매칭된 건수를 목표로 삼는다. 보유하지 않은 영화가 많으면 그만큼 더 판다
			while (matched.size() < TARGET_RANK_SIZE && page <= MAX_RANK_PAGE) {
				MovieResultsPage results = tmdbApi.getMovieLists().getPopular(
						tmdbProperties.getLanguage(), page, null);
				if (results.getResults() == null || results.getResults().isEmpty()) {
					break;
				}

				for (Movie movie : results.getResults()) {
					if (matched.size() >= TARGET_RANK_SIZE) {
						break;
					}
					if (!seenTmdbIds.add(movie.getId())) {
						continue;
					}
					// 전체 순위는 개봉 여부를 안 가린다 - 이 목록을 쓰는 박스오피스 선반이 released를 안 걸기 때문이다
					ContentVO content = contentByExternal.get(String.valueOf(movie.getId()));
					if (content == null) {
						skipped++;
						continue;
					}
					matched.add(content.getContentId());
				}

				if (results.getTotalPages() == null || page >= results.getTotalPages()) {
					break;
				}
				page++;
			}
		} catch (TmdbException e) {
			// 던지면 스케줄이 죽어 다음 주기까지 순위가 멈춘다. 모은 데까지만 쓰고 넘어간다
			log.warn("TMDB 인기순위 조회 실패. 수집분까지만 사용: matched={}", matched.size(), e);
		}

		// 어제 순위가 빈 화면보다 낫다
		if (matched.isEmpty()) {
			log.warn("TMDB 인기순위 매칭 0건. 기존 순위 유지: previous={}", rankedIds.size());
			return 0;
		}

		rankedIds = List.copyOf(matched);
		// skipped가 크면 적재가 밀렸다는 신호다
		log.info("TMDB 인기순위 동기화: matched={}, skipped={}, pages={}", matched.size(), skipped, page);
		return matched.size();
	}

	@Override
	public List<Integer> retrieveRank() {
		return rankedIds;
	}

	// syncRank와 같은 이유로 @Transactional을 붙이지 않는다. 장르 수만큼 TMDB를 부르므로 더 오래 걸린다
	@Override
	public int syncGenreRank() {
		validateApiKey();

		List<GenreVO> genres = genreService.retrieveAll();

		if (genres.isEmpty()) {
			log.warn("보유 장르가 없어 장르별 인기순위를 건너뜁니다.");
			return 0;
		}

		// 장르마다 findContentIdByExternal을 건건이 부르면 기동 시 쿼리가 1000건을 넘는다. 대응표를 먼저 한 번에 읽는다
		Map<String, ContentVO> contentByExternal = toContentByExternal();

		if (contentByExternal.isEmpty()) {
			log.warn("적재된 콘텐츠가 없어 장르별 인기순위를 건너뜁니다.");
			return 0;
		}

		Map<Integer, List<Integer>> ranked = new HashMap<>();
		// 장르별 건수를 남기지 않으면 "이 장르 선반이 왜 안 뜨지"를 코드를 읽어야 알 수 있다.
		// 편수 부족으로 후보에서 빠진 것인지 랜덤으로 안 뽑힌 것인지 이 줄로 갈린다
		StringBuilder counts = new StringBuilder();

		for (GenreVO genre : genres) {
			List<Integer> matched = toGenreRank(genre, contentByExternal);

			if (counts.length() > 0) {
				counts.append(", ");
			}
			counts.append(genre.getName()).append(' ').append(matched.size());

			if (!matched.isEmpty()) {
				ranked.put(genre.getGenreId(), List.copyOf(matched));
			}
		}

		// 어제 순위가 빈 화면보다 낫다 - syncRank와 같은 판단이다
		if (ranked.isEmpty()) {
			log.warn("장르별 인기순위 매칭 0건. 기존 목록 유지: previous={}", genreRankedIds.size());
			return 0;
		}

		genreRankedIds = Map.copyOf(ranked);
		log.info("TMDB 장르별 인기순위 동기화: 장르={}/{}, 건수=[{}]",
				ranked.size(), genres.size(), counts);
		return ranked.size();
	}

	@Override
	public List<Integer> retrieveRank(int genreId) {
		return genreRankedIds.getOrDefault(genreId, List.of());
	}

	// 장르 하나의 순위 목록. TMDB가 막히거나 external_genre_id가 숫자가 아니면 빈 목록이고, 그 장르만 빠진다
	private List<Integer> toGenreRank(GenreVO genre, Map<String, ContentVO> contentByExternal) {
		int externalGenreId;

		try {
			externalGenreId = Integer.parseInt(genre.getExternalGenreId());
		} catch (NumberFormatException e) {
			// AD-03에서 수기 등록한 장르는 TMDB id가 아닐 수 있다. Discover를 부를 수 없으니 건너뛴다
			log.warn("TMDB 장르 id가 숫자가 아니라 건너뜁니다: genreId={}, externalGenreId={}",
					genre.getGenreId(), genre.getExternalGenreId());
			return List.of();
		}

		List<Integer> matched = new ArrayList<>();
		// 페이지를 넘기는 사이 popularity가 밀리면 같은 영화가 두 번 온다
		Set<Integer> seenTmdbIds = new HashSet<>();
		int page = FIRST_RANK_PAGE;

		try {
			while (matched.size() < TARGET_GENRE_RANK_SIZE && page <= MAX_GENRE_RANK_PAGE) {
				// releaseDateLte는 미개봉작을 미리 줄여 페이지를 덜 낭비하려는 것이고, 개봉 판정의 근거가 아니다 -
				// TMDB는 영화제·제한상영 기록도 개봉으로 치므로 최종 판정은 아래 isReleased가 우리 값으로 한다
				DiscoverMovieParamBuilder params = new DiscoverMovieParamBuilder()
						.withGenres(List.of(externalGenreId), false)
						.sortBy(DiscoverMovieSortBy.POPULARITY_DESC)
						.releaseDateLte(LocalDate.now().toString())
						.language(tmdbProperties.getLanguage())
						.page(page);

				MovieResultsPage results = tmdbApi.getDiscover().getMovie(params);

				if (results.getResults() == null || results.getResults().isEmpty()) {
					break;
				}

				for (Movie movie : results.getResults()) {
					if (matched.size() >= TARGET_GENRE_RANK_SIZE) {
						break;
					}
					if (!seenTmdbIds.add(movie.getId())) {
						continue;
					}
					// 보유하지 않은 영화는 건너뛴다 - 여기서 새로 적재하지 않는다.
					// 개봉 여부는 TMDB가 아니라 우리 release_year로 본다. TMDB의 release_date.lte는
					// 영화제·제한상영 기록까지 개봉으로 쳐서, 정식 개봉 전인 작품이 통과해 버린다
					ContentVO content = contentByExternal.get(String.valueOf(movie.getId()));
					if (content != null && isReleased(content)) {
						matched.add(content.getContentId());
					}
				}

				if (results.getTotalPages() == null || page >= results.getTotalPages()) {
					break;
				}
				page++;
			}
		} catch (TmdbException e) {
			// 한 장르가 막혀도 나머지 장르는 채워야 한다. 모은 데까지만 쓴다
			log.warn("TMDB 장르 인기순위 조회 실패. 수집분까지만 사용: genre={}, matched={}",
					genre.getName(), matched.size(), e);
		}

		return matched;
	}

	// external_id -> 콘텐츠 대응표. 쿼리 1회로 통째로 읽어 메모리에서 맞춘다.
	// contentId만이 아니라 VO를 담는 이유는 개봉 여부 판정에 release_year가 필요해서다
	private Map<String, ContentVO> toContentByExternal() {
		Map<String, ContentVO> byExternal = new HashMap<>();

		for (ContentVO content : contentMapper.doSelectExternalIdMap()) {
			byExternal.put(content.getExternalId(), content);
		}

		return byExternal;
	}

	// contentWhere의 released와 같은 기준 - 개봉일을 모르면 개봉작이 아니다.
	// releaseYear는 'YYYY-MM-DD'라 사전순 비교가 곧 날짜 비교다
	private boolean isReleased(ContentVO content) {
		String releaseYear = content.getReleaseYear();

		return StringUtils.hasText(releaseYear)
				&& releaseYear.compareTo(LocalDate.now().toString()) <= 0;
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
				resolveGenreId(genre);
				saved++;
			}
			log.info("TMDB 장르 마스터 동기화: count={}", saved);
		} catch (TmdbException e) {
			throw new IllegalStateException("TMDB 장르 목록 조회 실패", e);
		}
	}

	// 신규 영화만 상세 조회한 뒤 CONTENT와 하위 테이블에 넣는다. 한국 성인등급이면 저장하지 않고 false
	private boolean saveFromTmdb(int tmdbMovieId) {
		try {
			// HTTP GET /movie/영화id + append_to_response=credits,translations,release_dates
			// 이유는 API 호출 횟수를 줄이기 위해서. 항목을 얹는 것뿐이라 호출 수는 늘지 않는다.
			// TRANSLATIONS는 언어별 제목·줄거리를 함께 받아 제목 폴백에 쓰고, RELEASE_DATES는 국가별 등급을 받는다
			MovieDb movie = tmdbApi.getMovies().getDetails(
					tmdbMovieId, tmdbProperties.getLanguage(),
					MovieAppendToResponse.CREDITS, MovieAppendToResponse.TRANSLATIONS,
					MovieAppendToResponse.RELEASE_DATES);

			// 비회원도 보는 화면이라 수집 단계에서 막는다. 등급을 담을 컬럼이 없어 표시 단계에서는 거를 수 없다
			if (isAdultMovie(movie)) {
				log.info("성인등급 skip: externalId={}, title={}, KR등급={}, US등급={}",
						tmdbMovieId, movie.getTitle(),
						toCertification(movie, CERTIFICATION_COUNTRY_KR),
						toCertification(movie, CERTIFICATION_COUNTRY_US));
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

	// 성인물 판정 - 한국 등급이 있으면 그것만 보고, 없을 때만 미국 등급으로 판단한다.
	// 둘을 OR로 묶으면 미국 R이 한국 판단을 덮어쓴다 - "오디세이"는 KR 15인데 US R이라 함께 빠졌다.
	// 미국 R은 관람 금지가 아니라 "17세 미만 보호자 동반"이고 폭력·욕설만으로도 붙는다.
	// 그래도 한국 등급이 없을 때는(전체의 20%쯤) 미국이 유일한 근거라 R까지 성인으로 본다
	private boolean isAdultMovie(MovieDb movie) {
		String korean = toCertification(movie, CERTIFICATION_COUNTRY_KR);

		if (StringUtils.hasText(korean)) {
			return isAdultCertification(korean);
		}

		return isAdultCertification(toCertification(movie, CERTIFICATION_COUNTRY_US));
	}

	// RELEASE_DATES 응답에서 그 나라 등급을 꺼낸다. 안 얹었거나 그 나라 개봉 정보가 없으면 null
	private String toCertification(MovieDb movie, String countryCode) {
		if (movie.getReleaseDates() == null || movie.getReleaseDates().getResults() == null) {
			return null;
		}

		for (ReleaseInfo info : movie.getReleaseDates().getResults()) {
			if (!countryCode.equals(info.getIso31661()) || info.getReleaseDates() == null) {
				continue;
			}

			// 같은 나라에 개봉 유형(극장·디지털 등)별로 여러 벌이 오고 등급이 빈 벌도 섞인다
			for (ReleaseDate releaseDate : info.getReleaseDates()) {
				if (StringUtils.hasText(releaseDate.getCertification())) {
					return releaseDate.getCertification();
				}
			}
		}

		return null;
	}

	// 한국 성인등급인지. 등급이 없으면 통과시키는 것이 정책이다(미상까지 막으면 보유량이 절반 아래로 떨어진다)
	private boolean isAdultCertification(String certification) {
		if (!StringUtils.hasText(certification)) {
			return false;
		}

		// 실측값이 "19"·"18"·"19+"·"청소년 관람불가"로 섞여 있어 등호 비교로는 샌다(2026-09-04 전수 500편)
		String normalized = certification.replaceAll("\\s", "").toUpperCase();

		// 숫자보다 먼저 본다. NC-17은 숫자만 뽑으면 17이라 미성년 등급으로 읽히고, X·AO는 숫자가 아예 없다
		if (ADULT_CERTIFICATIONS.contains(normalized)) {
			return true;
		}

		// 통째로 숫자인지 보면 "19+"가 새어 나간다. 붙어 있는 기호를 넘기고 숫자 부분만 뽑는다.
		// 자릿수 상한은 값이 터무니없을 때 parseInt가 던지는 것을 막는다
		Matcher ageMatcher = AGE_IN_CERTIFICATION.matcher(normalized);
		if (ageMatcher.find() && ageMatcher.group().length() <= MAX_AGE_DIGITS) {
			// 숫자 등급은 상한만 보면 앞으로 새 값이 생겨도 걸린다. 12·15는 통과한다
			return Integer.parseInt(ageMatcher.group()) >= ADULT_AGE_LIMIT;
		}

		return normalized.contains(CERTIFICATION_ADULT_ONLY)
				|| normalized.contains(CERTIFICATION_RESTRICTED);
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
		if (sorted.size() > MAX_CAST) {
			return sorted.subList(0, MAX_CAST);
		}
		return sorted;
	}

	// 제작진 중 job이 Director인 사람만 고르고, 같은 인물은 한 번만 넣는다
	private List<Crew> pickDirectors(List<Crew> crewList) {
		if (crewList == null || crewList.isEmpty()) {
			return List.of();
		}
		List<Crew> directors = new ArrayList<>();
		Set<Integer> seenIds = new HashSet<>();
		for (Crew crew : crewList) {
			if (!DIRECTOR_JOB.equalsIgnoreCase(crew.getJob()) || !seenIds.add(crew.getId())) {
				continue;
			}
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
	// name은 TMDB 대표 이름, originalName은 원어 이름 - 둘 다 언어와 무관하게 온다.
	// 프로필도 풀 URL 말고 path만 넣음. 이름 둘 다 없으면 NAME_KO는 UNKNOWN으로 때움
	private PersonVO toPersonVO(int tmdbPersonId, String name, String originalName,
			String profilePath) {
		PersonVO person = new PersonVO();
		person.setExternalId(String.valueOf(tmdbPersonId));

		String nameOrg = StringUtils.hasText(originalName) ? originalName : name;
		String nameKo = StringUtils.hasText(name) ? name : nameOrg;

		// 제목은 ko-KR로 부르면 한국어 제목이 오지만 인물 이름은 언어와 무관하게 온다 - 한국어 등록이
		// 없으면 한자·가나 그대로다. 영문 이름은 제목과 같은 자리(translations)에 있으므로 그걸로 채운다
		if (!hasAllowedScript(nameKo) || !hasAllowedScript(nameOrg)) {
			String english = getEnglishName(tmdbPersonId);
			if (StringUtils.hasText(english)) {
				if (!hasAllowedScript(nameKo)) {
					nameKo = english;
				}
				if (!hasAllowedScript(nameOrg)) {
					nameOrg = english;
				}
			}
		}

		// 번역까지 없으면 원어 이름이 그대로 남는다. 이름이 한 자도 없을 때만 UNKNOWN이다
		if (!StringUtils.hasText(nameKo)) {
			nameKo = "UNKNOWN";
		}

		person.setNameKo(nameKo);
		person.setNameOrg(nameOrg);
		person.setProfileImageUrl(toImagePath(profilePath));
		return person;
	}

	// HTTP GET /person/{id} + append_to_response=translations - 영문 번역 이름만 쓴다.
	// 이름을 못 쓰는 인물마다 호출이 한 번 더 나가므로, 이름이 이미 한글·라틴이면 부르지 않는다
	private String getEnglishName(int tmdbPersonId) {
		try {
			PersonDb person = tmdbApi.getPeople().getDetails(
					tmdbPersonId, tmdbProperties.getLanguage(), PersonAppendToResponse.TRANSLATIONS);
			if (person == null || person.getTranslations() == null
					|| person.getTranslations().getTranslations() == null) {
				return null;
			}

			// people 패키지에도 Translation·Data가 있어 movies 쪽 import와 이름이 겹친다. 여기만 전체 이름을 쓴다
			for (info.movito.themoviedbapi.model.people.Translation translation
					: person.getTranslations().getTranslations()) {
				if (!LANG_EN.equalsIgnoreCase(translation.getIso6391()) || translation.getData() == null) {
					continue;
				}
				// Translation.getName()은 언어 이름("English")이지 인물 이름이 아니다. 인물 이름은 data 안에 있는데,
				// 라이브러리 people.Data가 biography만 매핑해서 AbstractJsonMapping의 newItems로 흘러든다.
				// 라이브러리가 name을 정식 매핑하면 여기가 비므로 getData().getName()으로 바꾼다.
				// 2026-09-02 실측: id 895706(今岡信治)에서 newItems={name=Shinji Imaoka, primary=false}
				Object englishName = translation.getData().getNewItems().get(TRANSLATION_NAME_KEY);
				if (englishName == null) {
					// en은 en-US·en-GB 등 여러 벌이 온다. 첫 줄이 비었다고 포기하면 뒤에 있는 이름을 놓친다
					continue;
				}

				// 사용자 입력이라 en 칸에 한자·가나 이름이 그대로 들어 있는 벌이 있다.
				// 호출부는 이 값을 그대로 NAME_KO에 넣으므로, 여기서 거르지 않으면 스크립트 검사를 우회한다
				String candidate = englishName.toString().trim();
				if (!hasAllowedScript(candidate)) {
					continue;
				}

				return candidate;
			}

			return null;
		} catch (Exception e) {
			// 번역을 못 받아도 인물 저장 자체는 막지 않는다. 원어 이름이 그대로 남을 뿐이다
			log.warn("TMDB 인물 영문 이름 조회 실패. 원어 이름을 그대로 쓴다: externalId={}", tmdbPersonId, e);
			return null;
		}
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
		content.setTitleKo(toTitleKo(movie));
		content.setTitleOrg(toTitleOrg(movie));
		content.setOverview(toOverview(movie));
		content.setReleaseYear(movie.getReleaseDate());
		content.setRuntimeMin(movie.getRuntime() != null ? movie.getRuntime() : 0);
		content.setCountry(resolveCountry(movie));
		content.setPosterUrl(toImagePath(movie.getPosterPath()));
		content.setBackdropUrl(toImagePath(movie.getBackdropPath()));
		return content;
	}

	// ko 번역이 없으면 TMDB가 title에 원어 제목을 담아 주므로, hasText가 아니라 스크립트를 본다
	private String toTitleKo(MovieDb movie) {
		if (hasAllowedScript(movie.getTitle())) {
			return movie.getTitle();
		}

		Data english = toTranslation(movie, LANG_EN);
		if (english != null && StringUtils.hasText(english.getTitle())) {
			return english.getTitle();
		}

		// 제목은 비면 목록에서 식별이 안 되므로 원어라도 넣는다
		return movie.getOriginalTitle();
	}

	// 원제 자리에 영문 제목을 넣는다. title_en 컬럼이 생기면 이 값을 그쪽으로 옮긴다
	private String toTitleOrg(MovieDb movie) {
		Data english = toTranslation(movie, LANG_EN);
		if (english != null && StringUtils.hasText(english.getTitle())) {
			return english.getTitle();
		}

		return movie.getOriginalTitle();
	}

	// 줄거리는 한국어만 받는다. 제목과 달리 영문으로 채우지 않고 null로 둔다 -
	// 한글 화면에 영문 줄거리가 박히느니 비워 두고 화면이 "줄거리 없음"을 그리는 게 낫다
	private String toOverview(MovieDb movie) {
		if (hasHangul(movie.getOverview())) {
			return movie.getOverview();
		}

		return null;
	}

	// TRANSLATIONS 응답에서 해당 언어의 번역 묶음을 꺼낸다. append_to_response에 안 얹었으면 null이다
	private Data toTranslation(MovieDb movie, String languageCode) {
		if (movie.getTranslations() == null || movie.getTranslations().getTranslations() == null) {
			return null;
		}

		for (Translation translation : movie.getTranslations().getTranslations()) {
			if (languageCode.equalsIgnoreCase(translation.getIso6391())) {
				return translation.getData();
			}
		}

		return null;
	}

	// 한글/라틴/숫자·기호만 통과시킨다. 키릴·한자·가나가 섞이면 한국어 값으로 인정하지 않는다.
	// 제목은 "F1 더 무비"처럼 라틴이 섞인 한국어 제목과 "Superman" 같은 영문 제목을 함께 받고,
	// 인물 이름은 이 검사에 걸리면 별칭을 다시 받아 온다
	private boolean hasAllowedScript(String text) {
		if (!StringUtils.hasText(text)) {
			return false;
		}

		return text.codePoints().allMatch(this::isAllowedScript);
	}

	private boolean isAllowedScript(int codePoint) {
		Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);

		return script == Character.UnicodeScript.HANGUL
				|| script == Character.UnicodeScript.LATIN
				// 숫자·공백·문장부호는 COMMON, 결합 문자는 INHERITED로 분류된다
				|| script == Character.UnicodeScript.COMMON
				|| script == Character.UnicodeScript.INHERITED;
	}

	// 한글이 한 글자라도 들어 있는지. 줄거리처럼 한국어여야만 의미가 있는 값에 쓴다.
	// 한자를 인용한 한국어 줄거리도 통과시키려고 allMatch가 아니라 anyMatch로 본다
	private boolean hasHangul(String text) {
		if (!StringUtils.hasText(text)) {
			return false;
		}

		return text.codePoints().anyMatch(
				codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HANGUL);
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

	// ── 여기부터는 TMDB import(등록)와 무관한 단순 조회 기능 ──

	// 상세페이지 헤더 조회 - CONTENT 단건(제목/원제/줄거리/개봉연도/러닝타임/국가/포스터/배경) 반환
	@Override
	@Transactional(readOnly = true)
	public ContentVO get(int contentId) {
		if (contentId <= 0) {
			throw new IllegalArgumentException("올바른 콘텐츠 번호가 필요합니다.");
		}

		ContentVO key = new ContentVO();
		key.setContentId(contentId);

		ContentVO content = contentMapper.doSelectOne(key);
		if (content == null) {
			throw new NoSuchElementException("존재하지 않는 콘텐츠입니다. contentId=" + contentId);
		}

		// DB엔 TMDB 원본 경로만 저장돼 있으므로, 화면에 내려줄 때 여기서 풀 URL로 완성한다
		applyFullImageUrl(content);

		return content;
	}

	// 영문 표시값 - DB에 안 쓰고 메모리에만 둔다. @Transactional을 안 붙이는 것은 DB를 안 타서다
	@Override
	public EnglishContentVO getEnglishContent(int contentId, String externalId) {
		if (contentId <= 0 || !StringUtils.hasText(externalId)) {
			return null;
		}

		EnglishContentVO cached = englishContents.get(contentId);

		if (cached != null) {
			return cached;
		}

		EnglishContentVO english = readEnglishContent(externalId);

		if (english == null) {
			// 실패는 캐시하지 않는다. 굳혀 두면 TMDB가 돌아와도 영영 한국어로 남는다
			return null;
		}

		englishContents.put(contentId, english);
		log.debug("영문 표시값 캐시: contentId={}, 줄거리={}자, 포스터={}",
				contentId, english.getOverview().length(), english.getPosterPath());

		return english;
	}

	// 성공하면 값(없는 항목은 빈 문자열), 실패하면 null. 캐시 여부를 호출부가 가릴 수 있게 둘을 나눈다
	private EnglishContentVO readEnglishContent(String externalId) {
		try {
			MovieDb movie = tmdbApi.getMovies().getDetails(
					Integer.parseInt(externalId), LANGUAGE_EN);

			if (movie == null) {
				return new EnglishContentVO("", "", "");
			}

			// 이미지 경로는 TMDB 원본 그대로 담는다. 크기를 붙이는 것은 ContentImageService 몫이다
			return new EnglishContentVO(
					movie.getOverview() == null ? "" : movie.getOverview().trim(),
					movie.getPosterPath() == null ? "" : movie.getPosterPath(),
					movie.getBackdropPath() == null ? "" : movie.getBackdropPath());
		} catch (TmdbException | NumberFormatException e) {
			log.warn("영문 표시값 조회 실패. 한국어로 대체한다: externalId={}", externalId, e);
			return null;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<ContentVO> retrieve(DTO param) {
		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		normalizePaging(param);
		validateSearchDiv(param);
		ensureSearchMap(param);
		validateSort(param);

		// 폴백으로 정렬 축을 바꿨는지. searchMap은 호출부가 넘긴 객체라 아래 finally에서 되돌린다
		boolean popularFellBack = false;

		if (SORT_POPULAR.equals(param.getSearchMap().get(SEARCH_KEY_SORT))) {
			// 장르는 장르별 순위 목록이 따로 있어 인기순으로 갈 수 있다. 그 밖의 필터는 태울 곳이 없다
			String genreId = param.getSearchMap().get(SEARCH_KEY_GENRE_ID);
			List<Integer> ranked = StringUtils.hasText(genreId)
					? retrieveRank(toGenreId(genreId))
					: rankedIds;

			// 인기순 경로는 WHERE 조건을 태울 수 없고, 순위가 비면 보여줄 것도 없다. 셋 다 적재순으로 흘린다
			if (StringUtils.hasText(param.getSearchWord())
					|| hasFilterBeyondGenre(param)
					|| ranked.isEmpty()) {
				log.warn("인기순 정렬 불가로 적재순 대체: searchWord={}, genreId={}, rankSize={}",
						param.getSearchWord(), genreId, ranked.size());
				popularFellBack = true;
				param.getSearchMap().put(SEARCH_KEY_SORT, SORT_BOX_OFFICE);
			} else {
				return retrievePopular(param, ranked);
			}
		}

		// 이스케이프한 검색어는 매퍼에만 넘기고 호출부 DTO는 원래 값으로 되돌린다.
		// 안 그러면 이 DTO를 응답에 싣는 컨트롤러로 역슬래시가 새어 나가고,
		// 같은 DTO로 다시 조회할 때 이중 이스케이프가 된다
		String rawSearchWord = param.getSearchWord();
		param.setSearchWord(toEscapedSearchWord(param));

		List<ContentVO> contents;

		try {
			contents = contentMapper.doRetrieve(param);
		} finally {
			param.setSearchWord(rawSearchWord);
			// 정렬 축도 같은 이유로 원복한다. 덮어쓰기 전 값은 위 if가 popular임을 보장하므로 따로 보관하지 않는다.
			// 안 돌려놓으면 순위가 늦게 채워져도 그 DTO는 계속 적재순이고, 응답에 실리면 요청하지 않은 축이 나간다
			if (popularFellBack) {
				param.getSearchMap().put(SEARCH_KEY_SORT, SORT_POPULAR);
			}
		}

		if (contents == null) {
			param.setTotalCnt(0);
			return Collections.emptyList();
		}

		// doRetrieve가 CROSS JOIN으로 검색조건까지 걸러낸 총건수를 각 행에 실어 준다.
		param.setTotalCnt(contents.isEmpty() ? 0 : contents.get(0).getTotalCnt());

		for (ContentVO content : contents) {
			applyFullImageUrl(content);
		}

		return contents;
	}

	// 외부 ID 중복 검사 - 이미 등록된 TMDB 영화인지 확인한다
	@Override
	@Transactional(readOnly = true)
	public boolean hasExternalId(String externalId) {
		if (!StringUtils.hasText(externalId)) {
			throw new IllegalArgumentException("외부 ID가 필요합니다.");
		}

		return contentMapper.findContentIdByExternal(externalId) != null;
	}

	// 콘텐츠 등록
	@Override
	@Transactional
	public ContentVO create(ContentVO param) {
		if (param == null) {
			throw new IllegalArgumentException("등록할 콘텐츠 정보가 필요합니다.");
		}

		if (!StringUtils.hasText(param.getTitleKo())) {
			throw new IllegalArgumentException("콘텐츠 제목이 필요합니다.");
		}

		if (!StringUtils.hasText(param.getExternalId())) {
			throw new IllegalArgumentException("외부 ID가 필요합니다.");
		}

		if (hasExternalId(param.getExternalId())) {
			throw new IllegalStateException("이미 등록된 외부 ID입니다. externalId=" + param.getExternalId());
		}

		int result = contentMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("콘텐츠 등록에 실패했습니다.");
		}

		// doSave의 selectKey가 채번한 contentId를 param이 그대로 들고 있으므로 재조회에 사용한다
		return get(param.getContentId());
	}

	// 순위 목록에서 페이지 몫만 잘라 그 순서대로 재조립한다. 매퍼는 IN 조회만 하고 정렬하지 않는다
	private List<ContentVO> retrievePopular(DTO param, List<Integer> ranked) {
		// 인기순의 총건수는 전체 콘텐츠 수가 아니라 순위 목록 길이다. 페이저도 이 값으로 그려진다
		param.setTotalCnt(ranked.size());

		int from = (param.getPageNo() - 1) * param.getPageSize();
		if (from >= ranked.size()) {
			return Collections.emptyList();
		}

		List<Integer> pageIds = ranked.subList(from, Math.min(from + param.getPageSize(), ranked.size()));

		Map<Integer, ContentVO> byId = new HashMap<>();
		for (ContentVO content : contentMapper.doRetrieveByIds(pageIds)) {
			byId.put(content.getContentId(), content);
		}

		List<ContentVO> contents = new ArrayList<>();
		for (int i = 0; i < pageIds.size(); i++) {
			ContentVO content = byId.get(pageIds.get(i));
			// 순위를 만든 뒤 지워진 콘텐츠는 건너뛴다
			if (content == null) {
				continue;
			}
			// SearchApiController가 no를 순위 숫자로 읽는다
			content.setNo(from + i + 1);
			content.setTotalCnt(ranked.size());
			applyFullImageUrl(content);
			contents.add(content);
		}

		return contents;
	}

	// TMDB 원본 경로를 화면에 바로 쓸 수 있는 풀 URL로 완성한다.
	// 어떤 크기를 쓸지는 ContentImageServiceImpl이 정하므로 여기서는 용도만 말한다
	private void applyFullImageUrl(ContentVO content) {
		content.setPosterUrl(contentImageService.toPosterUrl(content.getPosterUrl()));
		content.setBackdropUrl(contentImageService.toBackdropUrl(content.getBackdropUrl()));
	}

	// 페이지 번호와 페이지 크기를 허용 범위의 기본값으로 보정
	private void normalizePaging(DTO param) {
		if (param.getPageNo() <= 0) {
			param.setPageNo(FIRST_PAGE_NO);
		}

		if (param.getPageSize() <= 0) {
			param.setPageSize(DEFAULT_PAGE_SIZE);
		} else if (param.getPageSize() > MAX_PAGE_SIZE) {
			param.setPageSize(MAX_PAGE_SIZE);
		}
	}

	// contentWhere가 모르는 searchDiv가 오면 WHERE 절이 통째로 빠져 전체 조회가 되므로 여기서 막는다
	private void validateSearchDiv(DTO param) {
		if (!StringUtils.hasText(param.getSearchWord())) {
			return;
		}

		String searchDiv = param.getSearchDiv();

		if (!SEARCH_BY_TITLE_KO.equals(searchDiv)
				&& !SEARCH_BY_TITLE_ORG.equals(searchDiv)
				&& !SEARCH_BY_COUNTRY.equals(searchDiv)
				&& !SEARCH_BY_EXTERNAL_ID.equals(searchDiv)
				&& !SEARCH_BY_TITLE.equals(searchDiv)) {
			throw new IllegalArgumentException("지원하지 않는 검색 구분입니다. searchDiv=" + searchDiv);
		}
	}

	// 매퍼의 searchMap.externalId 판정이 NPE를 내지 않도록 빈 맵을 보장한다
	private void ensureSearchMap(DTO param) {
		if (param.getSearchMap() == null) {
			param.setSearchMap(new HashMap<>());
		}
	}

	// 모르는 정렬 값이 오면 매퍼가 조용히 기본 정렬로 넘어가므로 여기서 막는다
	private void validateSort(DTO param) {
		String sort = param.getSearchMap().get(SEARCH_KEY_SORT);

		if (!StringUtils.hasText(sort)) {
			return;
		}

		if (!ALLOWED_SORT.contains(sort)) {
			throw new IllegalArgumentException("지원하지 않는 정렬입니다. sort=" + sort);
		}
	}

	// 인기순 경로는 순위 목록을 그대로 읽어 오므로 WHERE를 못 건다. 장르 외의 필터가 섞였는지 본다.
	// 여기서 true면 필터를 조용히 버리는 대신 적재순으로 폴백한다
	private boolean hasFilterBeyondGenre(DTO param) {
		Map<String, String> searchMap = param.getSearchMap();

		return StringUtils.hasText(searchMap.get(SEARCH_KEY_PERSON_ID))
				|| StringUtils.hasText(searchMap.get(SEARCH_KEY_DECADE))
				|| StringUtils.hasText(searchMap.get(SEARCH_KEY_EXTERNAL_ID));
	}

	// 화면이 넘긴 genreId 문자열을 순위 목록 키로 바꾼다. 숫자가 아니면 그 장르 순위가 없는 것과 같게 둔다
	private int toGenreId(String genreId) {
		try {
			return Integer.parseInt(genreId.trim());
		} catch (NumberFormatException e) {
			log.warn("장르 id가 숫자가 아니라 인기순을 건너뜁니다: genreId={}", genreId);
			return 0;
		}
	}

	// 검색어의 %·_는 LIKE 와일드카드라 그냥 두면 "%" 한 글자에 전체가 걸린다.
	// 매퍼가 ESCAPE '\'를 선언해 두었으므로 앞에 역슬래시를 붙여 글자로 되돌린다.
	// DTO를 고치지 않고 값만 돌려주므로 호출부가 넘긴 검색어는 그대로 남는다
	private String toEscapedSearchWord(DTO param) {
		String searchWord = param.getSearchWord();

		// 외부 ID 축은 등호 비교라 이스케이프가 오히려 검색어를 망친다
		if (!StringUtils.hasText(searchWord) || SEARCH_BY_EXTERNAL_ID.equals(param.getSearchDiv())) {
			return searchWord;
		}

		// 역슬래시를 먼저 바꾼다. 나중에 바꾸면 앞서 붙인 이스케이프까지 다시 이스케이프된다
		return searchWord
				.replace(LIKE_ESCAPE_CHAR, LIKE_ESCAPE_CHAR + LIKE_ESCAPE_CHAR)
				.replace("%", LIKE_ESCAPE_CHAR + "%")
				.replace("_", LIKE_ESCAPE_CHAR + "_");
	}

}
