package com.endit.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.cmn.DTO;
import com.endit.cmn.LocaleTextHelper;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.ContentVO;
import com.endit.domain.GenreVO;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentService;
import com.endit.service.GenreService;

/** H-01 메인화면. 정렬 축이 다른 포스터 선반을 여러 줄로 쌓아 보여준다. */
@Controller
public class HomeViewController {

	// 정렬 축은 searchMap을 통로로 쓴다. 서비스가 이 값을 화이트리스트로 검증한다.
	// ContentServiceImpl의 같은 상수가 private이라 호출부에서 다시 정의한다
	private static final String SEARCH_KEY_SORT = "sort";
	private static final String SORT_POPULAR = "popular";
	// 개봉일 기준이다. 적재 시각(registered)을 쓰면 sync 한 번으로 채운 DB에서는
	// 적재 순서가 곧 TMDB 인기순이라 인기 선반을 뒤집어 놓은 것이 되어 선반 뜻이 겹친다
	private static final String SORT_LATEST = "latest";
	// 큐레이션 4줄이 쓰는 축. popular은 WHERE를 안 타서 필터와 같이 못 쓴다
	private static final String SORT_BOX_OFFICE = "boxoffice";

	// 개봉 여부로 가르는 통로. latest는 release_year DESC라 안 걸면 개봉 예정작이 맨 앞에 온다
	private static final String SEARCH_KEY_RELEASED = "released";
	private static final String RELEASED_ONLY = "Y";

	// 큐레이션 필터 통로. 값은 매퍼 contentWhere가 받는다
	private static final String SEARCH_KEY_GENRE_ID = "genreId";
	private static final String SEARCH_KEY_PERSON_ID = "personId";
	private static final String SEARCH_KEY_PERSON_ROLE = "personRole";
	private static final String SEARCH_KEY_DECADE = "decade";

	// POL-033 4종 중 큐레이션이 쓰는 둘. ContentCreditServiceImpl의 같은 상수가 private이라 다시 정의한다
	private static final String ROLE_DIRECTOR = "DIRECTOR";
	private static final String ROLE_ACTOR = "ACTOR";

	// 연대 선반 후보. 매 요청 이 중 하나를 뽑는다
	private static final List<String> DECADES =
			List.of("1970", "1980", "1990", "2000", "2010", "2020");

	// 화면에 나가는 카피는 전부 messages*.properties에 있다. 여기에는 그 키만 둔다 -
	// 한국어 문구를 상수로 들고 있으면 번역할 통로가 없어서다(F-01).
	// 정렬 축은 popular(TMDB 실시간 순위)인데 화면 이름만 "박스오피스"다.
	// 서비스의 boxoffice 축(적재순)과는 다른 것이니 상수 이름에 끌려가지 않는다
	private static final String MSG_SHELF_BOXOFFICE = "home.shelf.boxoffice";
	private static final String MSG_SHELF_LATEST = "home.shelf.latest";

	// 대상이 매 요청 정해지므로 제목도 그때 만든다.
	// 장르·연대는 카피를 찾아 "{카피} {대상} 추천작!"으로 조립하고, 카피 키가 없으면 fallback으로 떨어진다.
	// 감독·배우는 인물 이름이 곧 대상이라 "추천작" 꼬리를 붙이지 않는다(2026-09-03 회의)
	private static final String MSG_SHELF_CURATION = "home.shelf.curation";
	private static final String MSG_SHELF_CURATION_FALLBACK = "home.shelf.curation.fallback";
	private static final String MSG_SHELF_DIRECTOR = "home.shelf.director";
	private static final String MSG_SHELF_ACTOR = "home.shelf.actor";

	// 대상별 카피·표시명 키의 앞부분. 뒤에 장르는 external_genre_id가, 연대는 시작 연도가 붙는다.
	// 장르를 GENRE.name이 아니라 id로 잡는 것은 이름이 한국어뿐이라 영어를 담을 곳이 없기 때문이다
	private static final String MSG_PREFIX_GENRE_COPY = "home.copy.genre.";
	private static final String MSG_PREFIX_GENRE_NAME = "genre.name.";
	private static final String MSG_PREFIX_DECADE_COPY = "home.copy.decade.";
	private static final String MSG_PREFIX_DECADE_LABEL = "home.decade.label.";

	// 한 화면에 보이는 칸 수. 박스오피스만 크게 깐다
	private static final int BOXOFFICE_PER_VIEW = 5;
	private static final int CURATION_PER_VIEW = 7;

	// 캐러셀로 넘길 페이지 수. 조회 건수는 칸 수 x 페이지 수다(5x4=20, 7x3=21)
	private static final int BOXOFFICE_PAGES = 4;
	private static final int CURATION_PAGES = 3;

	// 순위 배지와 국가 표기는 박스오피스 선반만 단다.
	// 배지를 그쪽만 다는 이유는 ContentVO.no에 순위 숫자가 실려 오는 축이 popular뿐이기 때문이다 -
	// 순위가 비어 서비스가 boxoffice로 폴백해도 no는 적재순 행번호라 배지가 깨지지는 않는다
	// 고정 2줄도 제목이 로케일마다 달라 static 상수로 둘 수 없다. 매 요청 만든다

	// @RequestParam의 defaultValue는 문자열만 받는다
	private static final String FIRST_PAGE_NO = "1";

	private static final String HOME_VIEW = "home/index";

	private final Logger log = LoggerFactory.getLogger(getClass());

	// 화제 인물 캐시. 순위 500편에 대한 참여 집계는 IN 목록이 그만큼 길고 바인드 변수가 안 잡혀
	// 메인을 열 때마다 오라클이 새로 파싱한다. 순위 목록이 그대로면 결과도 그대로다.
	// getTopPerson이 null을 줄 수 있어 Optional로 감싼다 - ConcurrentHashMap이 null을 못 담는다
	private final Map<String, Optional<ContentCreditVO>> topPersons = new ConcurrentHashMap<>();
	// 캐시가 어느 순위 목록에서 나온 것인지. syncRank가 목록을 통째로 갈아 끼우므로 참조 비교로 충분하다
	private volatile List<Integer> topPersonRank = List.of();

	private final ContentService contentService;
	private final GenreService genreService;
	private final ContentCreditService contentCreditService;
	private final MessageSource messageSource;
	private final LocaleTextHelper localeText;

	public HomeViewController(
			ContentService contentService,
			GenreService genreService,
			ContentCreditService contentCreditService,
			MessageSource messageSource,
			LocaleTextHelper localeText) {
		this.contentService = contentService;
		this.genreService = genreService;
		this.contentCreditService = contentCreditService;
		this.messageSource = messageSource;
		this.localeText = localeText;
	}

	/**
	 * <pre>
	 * Method Name : home
	 * Description : 메인화면(H-01). 선반 6줄이며 비거나 실패한 선반은 통째로 빠진다.
	 *               순서는 박스오피스 - 장르 - 감독 - 배우 - 연대 - 최신 개봉작이다.
	 *               가운데 넷은 대상이 매 요청 정해진다 - 연대는 랜덤,
	 *               장르는 인기순위가 한 화면(7건) 이상 잡힌 장르 중 랜덤,
	 *               감독과 배우는 박스오피스 순위 500편 안에서 참여가 가장 많은 인물이다.
	 *               정렬은 박스오피스와 장르가 popular, 최신 개봉작이 latest, 나머지가 boxoffice다 -
	 *               popular 경로는 WHERE를 타지 않아 장르 말고는 필터를 걸 수 없다(장르만 순위 목록이 따로 있다).
	 *               장르 순위가 아직 없으면(동기화 전·실패) 전체 장르에서 뽑고 boxoffice로 물러난다.
	 *               칸 수·순위 배지·국가 표기는 선반마다 다르다(ShelfSpec) -
	 *               박스오피스만 5칸 20건에 배지와 국가를 달고, 나머지는 7칸 21건에 제목·연도만 쓴다.
	 *               pageNo는 선반 전체에 같이 걸린다.
	 * </pre>
	 * @param pageNo
	 * @param model
	 * @return String (home/index)
	 */
	@GetMapping("/")
	public String home(
			@RequestParam(name = "pageNo", defaultValue = FIRST_PAGE_NO) int pageNo,
			Model model) {

		List<Shelf> shelves = new ArrayList<>();
		// 한 섹션이 비어도 나머지는 그려야 한다(정의서 H-04 "빈 섹션 숨김")
		addShelf(shelves, getBoxOfficeSpec(), pageNo);
		addShelf(shelves, getGenreSpec(), pageNo);
		addShelf(shelves, getPersonSpec(ROLE_DIRECTOR, MSG_SHELF_DIRECTOR), pageNo);
		addShelf(shelves, getPersonSpec(ROLE_ACTOR, MSG_SHELF_ACTOR), pageNo);
		addShelf(shelves, getDecadeSpec(), pageNo);
		addShelf(shelves, getLatestSpec(), pageNo);

		model.addAttribute("shelves", shelves);

		return HOME_VIEW;
	}

	// 선반 하나를 조회해 목록에 담는다. 비거나 대상을 못 정했거나 조회가 실패한 선반은 담지 않는다
	private void addShelf(List<Shelf> shelves, ShelfSpec spec, int pageNo) {
		// 큐레이션 빌더가 대상을 못 정하면 null을 준다. 그 줄만 빠진다
		if (spec == null) {
			return;
		}

		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(spec.getPageSize());
		// 정렬을 안 넘기면 매퍼 기본값(created_dt DESC, content_id ASC)이라 선반 뜻이 안 선다.
		// 인기순은 서비스가 TMDB 순위 목록으로 정렬한다. 순위가 비어 있으면 적재순으로 대체돼 나온다
		param.getSearchMap().put(SEARCH_KEY_SORT, spec.getSort());
		param.getSearchMap().putAll(spec.getFilters());

		List<ContentVO> movies;

		// 선반 하나가 던져도 홈 전체가 죽지 않게 한다(정의서 H-04). 재시도 버튼은 H-04 본 작업 몫이다
		try {
			// 포스터 URL은 여기서 풀 URL로 완성돼 나온다
			movies = contentService.retrieve(param);
		} catch (RuntimeException e) {
			log.warn("선반 조회 실패로 건너뜁니다. title={}", spec.getTitle(), e);
			return;
		}

		if (movies.isEmpty()) {
			return;
		}

		shelves.add(new Shelf(spec, movies));
	}

	// 장르 선반 - 순위 목록이 한 화면을 채울 만큼 있는 장르 중 매 요청 하나를 뽑는다. 못 뽑으면 null.
	// released를 같이 넘기는 것은 폴백(boxoffice) 경로용이다 - 인기순 경로는 목록 자체가 개봉작만이라 걸 필요가 없다
	private ShelfSpec getGenreSpec() {
		List<GenreVO> genres;

		try {
			genres = genreService.retrieveAll();
		} catch (RuntimeException e) {
			log.warn("장르 목록 조회 실패로 장르 선반을 건너뜁니다.", e);
			return null;
		}

		if (genres == null || genres.isEmpty()) {
			return null;
		}

		// 보유 편수가 적은 장르가 뽑히면 선반이 빈약해지거나 통째로 사라진다. 채울 수 있는 장르만 후보로 둔다
		List<GenreVO> rankedGenres = toRankedGenres(genres);
		// 순위가 아직 없으면(동기화 전·실패) 예전처럼 전체에서 뽑고 정렬도 적재순으로 물러난다
		boolean ranked = !rankedGenres.isEmpty();
		List<GenreVO> candidates = ranked ? rankedGenres : genres;

		GenreVO picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

		return toCurationSpec(
				toCurationTitle(MSG_PREFIX_GENRE_COPY + picked.getExternalGenreId(),
						toLabel(MSG_PREFIX_GENRE_NAME + picked.getExternalGenreId(), picked.getName())),
				ranked ? SORT_POPULAR : SORT_BOX_OFFICE,
				Map.of(SEARCH_KEY_RELEASED, RELEASED_ONLY,
						SEARCH_KEY_GENRE_ID, String.valueOf(picked.getGenreId())));
	}

	// 인기순으로 선반 한 화면을 채울 수 있는 장르만 남긴다
	private List<GenreVO> toRankedGenres(List<GenreVO> genres) {
		List<GenreVO> rankedGenres = new ArrayList<>();

		for (GenreVO genre : genres) {
			if (contentService.retrieveRank(genre.getGenreId()).size() >= CURATION_PER_VIEW) {
				rankedGenres.add(genre);
			}
		}

		return rankedGenres;
	}

	// "{카피} {대상} 추천작!"을 만든다. 번들에 카피 키가 없는 대상은 기본 문구로 떨어진다
	private String toCurationTitle(String copyCode, String label) {
		// 없을 때를 null로 받아야 폴백을 가릴 수 있다. 기본값에 코드를 넣으면 키 문자열이 화면에 나간다
		String copy = messageSource.getMessage(copyCode, null, null, LocaleContextHolder.getLocale());

		if (copy == null) {
			return toMessage(MSG_SHELF_CURATION_FALLBACK, label);
		}

		return toMessage(MSG_SHELF_CURATION, copy, label);
	}

	// 고정 선반 2줄
	private ShelfSpec getBoxOfficeSpec() {
		return new ShelfSpec(toMessage(MSG_SHELF_BOXOFFICE), SORT_POPULAR, Map.of(),
				true, true, BOXOFFICE_PER_VIEW, BOXOFFICE_PAGES);
	}

	private ShelfSpec getLatestSpec() {
		return new ShelfSpec(toMessage(MSG_SHELF_LATEST), SORT_LATEST,
				Map.of(SEARCH_KEY_RELEASED, RELEASED_ONLY),
				false, false, CURATION_PER_VIEW, CURATION_PAGES);
	}

	// 번들에서 문구를 꺼낸다. 키가 없으면 코드를 그대로 돌려주므로 화면이 비지 않고 무엇이 빠졌는지 보인다
	private String toMessage(String code, Object... args) {
		return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale());
	}

	// 대상의 표시명. toMessage와 달리 키가 없을 때 코드가 아니라 넘겨받은 값으로 떨어진다 -
	// 장르는 번들에 19종만 있어 수기 등록 장르나 새 TMDB 장르가 뽑히면 키가 화면에 그대로 나간다
	private String toLabel(String code, String fallback) {
		return messageSource.getMessage(code, null, fallback, LocaleContextHolder.getLocale());
	}

	// 감독·배우 선반 - 박스오피스 순위 500편 안에서 참여가 가장 많은 인물. 순위가 비면 null
	private ShelfSpec getPersonSpec(String role, String titleCode) {
		ContentCreditVO top;

		try {
			top = getTopPerson(role);
		} catch (RuntimeException e) {
			log.warn("화제 인물 선별 실패로 선반을 건너뜁니다. role={}", role, e);
			return null;
		}

		if (top == null) {
			log.warn("순위 안에 해당 역할 크레딧이 없어 선반을 건너뜁니다. role={}", role);
			return null;
		}

		return toCurationSpec(
				toMessage(titleCode, localeText.get(top.getNameKo(), top.getNameOrg())),
				SORT_BOX_OFFICE,
				Map.of(SEARCH_KEY_RELEASED, RELEASED_ONLY,
						SEARCH_KEY_PERSON_ID, String.valueOf(top.getPersonId()),
						SEARCH_KEY_PERSON_ROLE, role));
	}

	// 순위가 바뀌었으면 캐시를 버리고 다시 센다. 두 스레드가 같이 버려도 한 번 더 세는 것으로 끝난다
	private ContentCreditVO getTopPerson(String role) {
		List<Integer> ranked = contentService.retrieveRank();

		if (topPersonRank != ranked) {
			topPersons.clear();
			topPersonRank = ranked;
		}

		return topPersons
				.computeIfAbsent(role, key -> Optional.ofNullable(contentCreditService.getTopPerson(key, ranked)))
				.orElse(null);
	}

	// 연대 선반 - DECADES 중 매 요청 하나. DB를 안 타므로 실패 경로가 없다.
	// 인기순을 못 쓰는 이유는 순위 목록에 WHERE를 못 걸어서다(장르만 목록을 따로 갖고 있다)
	private ShelfSpec getDecadeSpec() {
		String picked = DECADES.get(ThreadLocalRandom.current().nextInt(DECADES.size()));

		return toCurationSpec(
				toCurationTitle(MSG_PREFIX_DECADE_COPY + picked,
						toLabel(MSG_PREFIX_DECADE_LABEL + picked, picked)),
				SORT_BOX_OFFICE,
				Map.of(SEARCH_KEY_RELEASED, RELEASED_ONLY, SEARCH_KEY_DECADE, picked));
	}

	// 큐레이션 4줄은 제목·정렬·필터만 다르고 크기·배지가 같다
	private ShelfSpec toCurationSpec(String title, String sort, Map<String, String> filters) {
		return new ShelfSpec(title, sort, filters,
				false, false, CURATION_PER_VIEW, CURATION_PAGES);
	}

	// 선반 한 줄의 명세. 값이 여섯이라 addShelf 파라미터로 늘어놓지 않고 묶어서 상수로 둔다
	private static final class ShelfSpec {

		private final String title;
		private final String sort;
		// searchMap에 그대로 실린다. 비면 거르지 않는다
		private final Map<String, String> filters;
		private final boolean ranked;
		private final boolean countryShown;
		private final int perView;
		private final int pages;

		private ShelfSpec(String title, String sort, Map<String, String> filters,
				boolean ranked, boolean countryShown, int perView, int pages) {
			this.title = title;
			this.sort = sort;
			this.filters = filters;
			this.ranked = ranked;
			this.countryShown = countryShown;
			this.perView = perView;
			this.pages = pages;
		}

		private String getTitle() {
			return title;
		}

		private String getSort() {
			return sort;
		}

		private Map<String, String> getFilters() {
			return filters;
		}

		private boolean isRanked() {
			return ranked;
		}

		private boolean isCountryShown() {
			return countryShown;
		}

		private int getPerView() {
			return perView;
		}

		// 캐러셀이 끝까지 넘어갔을 때 채워질 분량
		private int getPageSize() {
			return perView * pages;
		}

	}

	/** 화면에 선반 한 줄을 넘기기 위한 그릇. 도메인 VO가 아니라 이 화면 전용이다 */
	public static class Shelf {

		private final ShelfSpec spec;
		private final List<ContentVO> movies;

		public Shelf(ShelfSpec spec, List<ContentVO> movies) {
			this.spec = spec;
			this.movies = movies;
		}

		public String getTitle() {
			return spec.getTitle();
		}

		// 포스터에 순위 배지를 달지. 템플릿이 ${shelf.ranked}로 읽는다
		public boolean isRanked() {
			return spec.isRanked();
		}

		// 카드 메타에 국가까지 쓸지. 끄면 개봉연도만 남는다
		public boolean isCountryShown() {
			return spec.isCountryShown();
		}

		// 한 화면 칸 수. 템플릿이 CSS 변수 --per-view로 내려 칸 너비와 캐러셀 이동량을 함께 정한다
		public int getPerView() {
			return spec.getPerView();
		}

		public List<ContentVO> getMovies() {
			return movies;
		}

	}

}
