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

/**
 * H-01 메인화면 착수 전, 포스터 캐러셀을 확인하는 임시 Controller.
 * 확인이 끝나면 삭제하고 정식 HomeViewController(루트 매핑)로 옮긴다.
 *
 * 경로를 /test/main으로 분리한 이유 - 루트(/)는 정식 홈이 가져갈 자리라 임시 코드가 선점하지 않는다.
 * 이 클래스와 templates/test/는 .gitignore로 로컬 전용이다(known-issues.md 규약).
 */
@Controller
public class TestMainPageController {

	// 정렬 축은 searchMap을 통로로 쓴다. 서비스가 이 값을 화이트리스트로 검증한다.
	// ContentServiceImpl의 같은 상수가 private이라 호출부에서 다시 정의한다
	private static final String SEARCH_KEY_SORT = "sort";
	private static final String SORT_BOX_OFFICE = "boxoffice";
	private static final String SORT_LATEST = "latest";

	private static final String TITLE_BOX_OFFICE = "박스오피스 순";
	private static final String TITLE_LATEST = "최신 등록작";

	// 캐러셀 4번 넘길 분량(5칸 x 4)
	private static final int PAGE_SIZE = 20;

	// @RequestParam의 defaultValue는 문자열만 받는다
	private static final String FIRST_PAGE_NO = "1";

	private static final String MAIN_PAGE_VIEW = "test/main_page";

	private final ContentService contentService;

	public TestMainPageController(ContentService contentService) {
		this.contentService = contentService;
	}

	/** 포스터 캐러셀 확인용 화면 */
	@GetMapping("/test/main")
	public String main(
			@RequestParam(name = "pageNo", defaultValue = FIRST_PAGE_NO) int pageNo,
			Model model) {

		List<Shelf> shelves = new ArrayList<>();
		// 한 섹션이 비어도 나머지는 그려야 한다(정의서 H-04 "빈 섹션 숨김")
		addShelf(shelves, TITLE_BOX_OFFICE, SORT_BOX_OFFICE, pageNo);
		addShelf(shelves, TITLE_LATEST, SORT_LATEST, pageNo);

		model.addAttribute("shelves", shelves);
		// 전체 건수는 정렬과 무관하게 같으므로 첫 선반 것을 그대로 쓴다.
		// 0이면 화면 문제가 아니라 적재 문제다
		model.addAttribute("totalCnt", shelves.isEmpty() ? 0 : shelves.get(0).getTotalCnt());

		return MAIN_PAGE_VIEW;
	}

	// 선반 하나를 조회해 목록에 담는다. 빈 선반은 담지 않는다
	private void addShelf(List<Shelf> shelves, String title, String sort, int pageNo) {
		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(PAGE_SIZE);
		// 기본 정렬(created_dt DESC)은 created_dt가 초 단위 SYSDATE라 덩어리째 뒤집힌다.
		// 박스오피스 정렬은 content_id ASC라 적재순 = 적재 시점 TMDB 인기순이 그대로 나온다
		param.getSearchMap().put(SEARCH_KEY_SORT, sort);

		// retrieve가 param에 totalCnt를 채워 주고, 포스터 URL도 여기서 풀 URL로 완성돼 나온다
		List<ContentVO> movies = contentService.retrieve(param);

		if (movies.isEmpty()) {
			return;
		}

		shelves.add(new Shelf(title, movies, param.getTotalCnt()));
	}

	/** 화면에 선반 한 줄을 넘기기 위한 그릇. 도메인 VO가 아니라 이 화면 전용이다 */
	public static class Shelf {

		private final String title;
		private final List<ContentVO> movies;
		private final int totalCnt;

		public Shelf(String title, List<ContentVO> movies, int totalCnt) {
			this.title = title;
			this.movies = movies;
			this.totalCnt = totalCnt;
		}

		public String getTitle() {
			return title;
		}

		public List<ContentVO> getMovies() {
			return movies;
		}

		public int getTotalCnt() {
			return totalCnt;
		}

	}

}