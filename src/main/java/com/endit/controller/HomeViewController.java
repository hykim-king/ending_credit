package com.endit.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.cmn.DTO;
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

	// 정렬 축은 popular(TMDB 실시간 순위)인데 화면 이름만 "박스오피스"다.
	// 서비스의 boxoffice 축(적재순)과는 다른 것이니 상수 이름에 끌려가지 않는다
	private static final String TITLE_BOXOFFICE = "박스오피스";
	private static final String TITLE_LATEST = "최신 개봉작";

	// 대상이 매 요청 정해지므로 제목도 그때 만든다. "TV 영화" 장르에서도 안 깨지는 형태다
	private static final String TITLE_GENRE_FORMAT = "%s 인기작";
	private static final String TITLE_DECADE_FORMAT = "%s년대 인기작";
	private static final String TITLE_DIRECTOR_FORMAT = "화제의 감독 %s";
	private static final String TITLE_ACTOR_FORMAT = "화제의 배우 %s";

	// 한 화면에 보이는 칸 수. 박스오피스만 크게 깐다
	private static final int BOXOFFICE_PER_VIEW = 5;
	private static final int CURATION_PER_VIEW = 7;

	// 캐러셀로 넘길 페이지 수. 조회 건수는 칸 수 x 페이지 수다(5x4=20, 7x3=21)
	private static final int BOXOFFICE_PAGES = 4;
	private static final int CURATION_PAGES = 3;

	// 순위 배지와 국가 표기는 박스오피스 선반만 단다.
	// 배지를 그쪽만 다는 이유는 ContentVO.no에 순위 숫자가 실려 오는 축이 popular뿐이기 때문이다 -
	// 순위가 비어 서비스가 boxoffice로 폴백해도 no는 적재순 행번호라 배지가 깨지지는 않는다
	private static final ShelfSpec SHELF_BOXOFFICE = new ShelfSpec(
			TITLE_BOXOFFICE, SORT_POPULAR, Map.of(), true, true, BOXOFFICE_PER_VIEW, BOXOFFICE_PAGES);
	private static final ShelfSpec SHELF_LATEST = new ShelfSpec(
			TITLE_LATEST, SORT_LATEST, Map.of(SEARCH_KEY_RELEASED, RELEASED_ONLY),
			false, false, CURATION_PER_VIEW, CURATION_PAGES);

	// @RequestParam의 defaultValue는 문자열만 받는다
	private static final String FIRST_PAGE_NO = "1";

	private static final String HOME_VIEW = "home/index";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ContentService contentService;
	private final GenreService genreService;
	private final ContentCreditService contentCreditService;

	public HomeViewController(
			ContentService contentService,
			GenreService genreService,
			ContentCreditService contentCreditService) {
		this.contentService = contentService;
		this.genreService = genreService;
		this.contentCreditService = contentCreditService;
	}

	/**
	 * <pre>
	 * Method Name : home
	 * Description : 메인화면(H-01). 선반 6줄이며 비거나 실패한 선반은 통째로 빠진다.
	 *               순서는 박스오피스 - 장르 - 감독 - 배우 - 연대 - 최신 개봉작이다.
	 *               가운데 넷은 대상이 매 요청 정해진다 - 장르와 연대는 랜덤,
	 *               감독과 배우는 박스오피스 순위 100편 안에서 참여가 가장 많은 인물이다.
	 *               박스오피스 외 다섯 줄이 popular이 아니라 boxoffice/latest로 정렬하는 이유는
	 *               popular 경로가 WHERE를 타지 않아 필터를 걸 수 없기 때문이다.
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
		addShelf(shelves, SHELF_BOXOFFICE, pageNo);
		addShelf(shelves, getGenreSpec(), pageNo);
		addShelf(shelves, getPersonSpec(ROLE_DIRECTOR, TITLE_DIRECTOR_FORMAT), pageNo);
		addShelf(shelves, getPersonSpec(ROLE_ACTOR, TITLE_ACTOR_FORMAT), pageNo);
		addShelf(shelves, getDecadeSpec(), pageNo);
		addShelf(shelves, SHELF_LATEST, pageNo);

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

	// 장르 선반 - 보유 장르 중 매 요청 하나를 뽑는다. 못 뽑으면 null
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

		GenreVO picked = genres.get(ThreadLocalRandom.current().nextInt(genres.size()));

		return toCurationSpec(
				String.format(TITLE_GENRE_FORMAT, picked.getName()),
				Map.of(SEARCH_KEY_RELEASED, RELEASED_ONLY,
						SEARCH_KEY_GENRE_ID, String.valueOf(picked.getGenreId())));
	}

	// 감독·배우 선반 - 박스오피스 순위 100편 안에서 참여가 가장 많은 인물. 순위가 비면 null
	private ShelfSpec getPersonSpec(String role, String titleFormat) {
		ContentCreditVO top;

		try {
			top = contentCreditService.getTopPerson(role, contentService.retrieveRank());
		} catch (RuntimeException e) {
			log.warn("화제 인물 선별 실패로 선반을 건너뜁니다. role={}", role, e);
			return null;
		}

		if (top == null) {
			log.warn("순위 안에 해당 역할 크레딧이 없어 선반을 건너뜁니다. role={}", role);
			return null;
		}

		return toCurationSpec(
				String.format(titleFormat, top.getNameKo()),
				Map.of(SEARCH_KEY_RELEASED, RELEASED_ONLY,
						SEARCH_KEY_PERSON_ID, String.valueOf(top.getPersonId()),
						SEARCH_KEY_PERSON_ROLE, role));
	}

	// 연대 선반 - DECADES 중 매 요청 하나. DB를 안 타므로 실패 경로가 없다
	private ShelfSpec getDecadeSpec() {
		String picked = DECADES.get(ThreadLocalRandom.current().nextInt(DECADES.size()));

		return toCurationSpec(
				String.format(TITLE_DECADE_FORMAT, picked),
				Map.of(SEARCH_KEY_RELEASED, RELEASED_ONLY, SEARCH_KEY_DECADE, picked));
	}

	// 큐레이션 4줄은 제목과 필터만 다르고 정렬·크기·배지가 같다
	private ShelfSpec toCurationSpec(String title, Map<String, String> filters) {
		return new ShelfSpec(title, SORT_BOX_OFFICE, filters,
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
