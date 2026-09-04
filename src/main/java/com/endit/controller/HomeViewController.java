package com.endit.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;
import com.endit.service.ContentService;

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
	private static final String SORT_UPCOMING = "upcoming";

	// 개봉 여부로 가르는 통로. latest는 release_year DESC라 안 걸면 개봉 예정작이 맨 앞에 온다
	private static final String SEARCH_KEY_RELEASED = "released";
	private static final String RELEASED_ONLY = "Y";
	private static final String UPCOMING_ONLY = "N";

	// 정렬 축은 popular(TMDB 실시간 순위)인데 화면 이름만 "박스오피스"다.
	// 서비스의 boxoffice 축(적재순)과는 다른 것이니 상수 이름에 끌려가지 않는다
	private static final String TITLE_BOXOFFICE = "박스오피스";
	private static final String TITLE_LATEST = "최신 개봉작";
	private static final String TITLE_UPCOMING = "개봉 예정작";

	// 한 화면에 보이는 칸 수. 박스오피스는 크게, 개봉일 기준 두 선반은 작게 깐다
	private static final int BOXOFFICE_PER_VIEW = 5;
	private static final int RELEASE_PER_VIEW = 6;

	// 캐러셀로 넘길 페이지 수. 조회 건수는 칸 수 x 페이지 수다(5x4=20, 6x3=18)
	private static final int BOXOFFICE_PAGES = 4;
	private static final int RELEASE_PAGES = 3;

	// 순위 배지는 박스오피스 선반만 단다 - ContentVO.no에 순위 숫자가 실려 오는 축이 popular뿐이다.
	// 순위가 비어 서비스가 boxoffice로 폴백해도 no는 적재순 행번호라 배지가 깨지지는 않는다.
	// released가 null인 선반은 개봉 여부로 거르지 않는다
	private static final ShelfSpec SHELF_BOXOFFICE = new ShelfSpec(
			TITLE_BOXOFFICE, SORT_POPULAR, null, true, BOXOFFICE_PER_VIEW, BOXOFFICE_PAGES);
	private static final ShelfSpec SHELF_LATEST = new ShelfSpec(
			TITLE_LATEST, SORT_LATEST, RELEASED_ONLY, false, RELEASE_PER_VIEW, RELEASE_PAGES);
	// 지금 DB에 7건뿐이라 상한(18)에 한참 못 미친다 - 수집이 /movie/popular라 개봉 예정작이 거의 안 걸린다
	private static final ShelfSpec SHELF_UPCOMING = new ShelfSpec(
			TITLE_UPCOMING, SORT_UPCOMING, UPCOMING_ONLY, false, RELEASE_PER_VIEW, RELEASE_PAGES);

	// @RequestParam의 defaultValue는 문자열만 받는다
	private static final String FIRST_PAGE_NO = "1";

	private static final String HOME_VIEW = "home/index";

	private final ContentService contentService;

	public HomeViewController(ContentService contentService) {
		this.contentService = contentService;
	}

	/**
	 * <pre>
	 * Method Name : home
	 * Description : 메인화면(H-01). 선반 하나가 정렬 축 하나이며, 비어 있는 선반은 넘기지 않는다.
	 *               선반은 박스오피스(TMDB 실시간 순위)·최신 개봉작(개봉일 내림차순)
	 *               ·개봉 예정작(개봉일 오름차순) 3줄이다. 뒤의 둘은 SYSDATE를 기준으로
	 *               서로 배타적이며, 개봉일 미상은 양쪽 어디에도 안 나온다.
	 *               칸 수·건수·순위 배지 여부는 선반마다 다르다(ShelfSpec) - 박스오피스는 5칸 20건에
	 *               순위 배지를 달고, 나머지 둘은 6칸 18건에 달지 않는다.
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
		addShelf(shelves, SHELF_LATEST, pageNo);
		addShelf(shelves, SHELF_UPCOMING, pageNo);

		model.addAttribute("shelves", shelves);

		return HOME_VIEW;
	}

	// 선반 하나를 조회해 목록에 담는다. 빈 선반은 담지 않는다
	private void addShelf(List<Shelf> shelves, ShelfSpec spec, int pageNo) {
		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(spec.getPageSize());
		// 정렬을 안 넘기면 매퍼 기본값(created_dt DESC, content_id ASC)이라 선반 뜻이 안 선다.
		// 인기순은 서비스가 TMDB 순위 목록으로 정렬한다. 순위가 비어 있으면 적재순으로 대체돼 나온다
		param.getSearchMap().put(SEARCH_KEY_SORT, spec.getSort());

		// 안 넣으면 매퍼의 조건이 통째로 빠져 전체가 나온다. 끄는 값을 따로 두지 않는 이유다
		if (spec.getReleased() != null) {
			param.getSearchMap().put(SEARCH_KEY_RELEASED, spec.getReleased());
		}

		// 포스터 URL은 여기서 풀 URL로 완성돼 나온다
		List<ContentVO> movies = contentService.retrieve(param);

		if (movies.isEmpty()) {
			return;
		}

		shelves.add(new Shelf(spec, movies));
	}

	// 선반 한 줄의 명세. 값이 여섯이라 addShelf 파라미터로 늘어놓지 않고 묶어서 상수로 둔다
	private static final class ShelfSpec {

		private final String title;
		private final String sort;
		// "Y"=개봉작만, "N"=개봉 예정작만, null=거르지 않음
		private final String released;
		private final boolean ranked;
		private final int perView;
		private final int pages;

		private ShelfSpec(String title, String sort, String released,
				boolean ranked, int perView, int pages) {
			this.title = title;
			this.sort = sort;
			this.released = released;
			this.ranked = ranked;
			this.perView = perView;
			this.pages = pages;
		}

		private String getTitle() {
			return title;
		}

		private String getSort() {
			return sort;
		}

		private String getReleased() {
			return released;
		}

		private boolean isRanked() {
			return ranked;
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

		// 한 화면 칸 수. 템플릿이 CSS 변수 --per-view로 내려 칸 너비와 캐러셀 이동량을 함께 정한다
		public int getPerView() {
			return spec.getPerView();
		}

		public List<ContentVO> getMovies() {
			return movies;
		}

	}

}
