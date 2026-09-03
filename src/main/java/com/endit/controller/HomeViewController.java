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
	private static final String SORT_LATEST = "latest";

	private static final String TITLE_POPULAR = "인기 순";
	private static final String TITLE_LATEST = "최신 등록작";

	// 캐러셀 4번 넘길 분량(5칸 x 4)
	private static final int PAGE_SIZE = 20;

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
	 *               선반마다 PAGE_SIZE(20)건까지만 싣는다 - 캐러셀이 5칸씩 4번 넘어가는 분량이다.
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
		addShelf(shelves, TITLE_POPULAR, SORT_POPULAR, pageNo);
		addShelf(shelves, TITLE_LATEST, SORT_LATEST, pageNo);

		model.addAttribute("shelves", shelves);

		return HOME_VIEW;
	}

	// 선반 하나를 조회해 목록에 담는다. 빈 선반은 담지 않는다
	private void addShelf(List<Shelf> shelves, String title, String sort, int pageNo) {
		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(PAGE_SIZE);
		// 기본 정렬(created_dt DESC)은 created_dt가 초 단위 SYSDATE라 덩어리째 뒤집힌다.
		// 인기순은 서비스가 TMDB 순위 목록으로 정렬한다. 순위가 비어 있으면 적재순으로 대체돼 나온다
		param.getSearchMap().put(SEARCH_KEY_SORT, sort);

		// 포스터 URL은 여기서 풀 URL로 완성돼 나온다
		List<ContentVO> movies = contentService.retrieve(param);

		if (movies.isEmpty()) {
			return;
		}

		shelves.add(new Shelf(title, movies));
	}

	/** 화면에 선반 한 줄을 넘기기 위한 그릇. 도메인 VO가 아니라 이 화면 전용이다 */
	public static class Shelf {

		private final String title;
		private final List<ContentVO> movies;

		public Shelf(String title, List<ContentVO> movies) {
			this.title = title;
			this.movies = movies;
		}

		public String getTitle() {
			return title;
		}

		public List<ContentVO> getMovies() {
			return movies;
		}

	}

}
