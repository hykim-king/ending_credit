package com.endit.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.ContentVO;
import com.endit.domain.PageResponse;
import com.endit.service.ContentService;

/**
 * 검색 화면이 비동기로 읽는 데이터를 제공하는 Controller
 */
@RestController
@RequestMapping("/api/search")
public class SearchApiController {

	private static final Logger log = LoggerFactory.getLogger(SearchApiController.class);

	// 정렬 축 - searchWord를 검색 조건이 쓰고 있어 searchMap의 이 키로 받는다
	private static final String SEARCH_KEY_SORT = "sort";
	// 순위의 기본 출처. 화면이 source를 생략해도 박스오피스를 준다
	private static final String SOURCE_BOX_OFFICE = "boxoffice";

	private static final int FIRST_PAGE_NO = 1;
	private static final String FIRST_PAGE_NO_TEXT = "1";
	// S-01 박스오피스 순위 노출 건수 - 설계서가 5위까지만 보여준다
	private static final int RANKING_SIZE = 5;

	// S-03 영화 결과 - 콘텐츠 검색 축(국문/원제 동시 부분일치)과 기본 정렬
	private static final String SEARCH_BY_TITLE = "50";
	private static final String SORT_RELEVANCE = "relevance";
	private static final String MOVIE_PAGE_SIZE_TEXT = "12";
	// 화면과 같은 상한이다. 넘는 검색어는 조회하지 않는다
	private static final int MAX_QUERY_LENGTH = 100;

	private final ContentService contentService;

	public SearchApiController(ContentService contentService) {
		this.contentService = contentService;
	}

	/** S-01 박스오피스 순위 - no(rnum)가 곧 순위다 */
	@GetMapping("/rankings")
	public List<ContentVO> retrieveRankings(
			@RequestParam(defaultValue = SOURCE_BOX_OFFICE) String source) {

		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(RANKING_SIZE);
		// 모르는 출처는 서비스의 정렬 검증이 걸러 준다
		param.getSearchMap().put(SEARCH_KEY_SORT, source);

		return contentService.retrieve(param);
	}

	/*
	 * S-03 영화 결과 - 화면이 totalPages로 페이저를 그린다.
	 *
	 * 정의서가 같은 URL을 두 번 적었다 - API-003(S-03 영화 전체 검색, query/sort/page/size)과
	 * API-025(D-04 컬렉션 작품 검색, query/page/size)가 둘 다 GET /api/search/movies다.
	 * D-04 쪽을 ContentQueryController가 먼저 구현해 두었고, 같은 경로에 둘을 걸면
	 * Spring이 Ambiguous mapping으로 뜨지 않는다.
	 * 그래서 S-03은 테이블 이름을 딴 /contents로 물러나 있는다.
	 * 두 화면의 계약(sort 유무, items/list)을 하나로 합치는 건 D-04 담당과 협의할 몫이고,
	 * 합쳐지면 이 메서드를 지우고 화면을 그쪽 엔드포인트로 돌리면 된다.
	 */
	@GetMapping("/contents")
	public PageResponse<ContentVO> retrieveMovies(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(defaultValue = SORT_RELEVANCE) String sort,
			@RequestParam(defaultValue = FIRST_PAGE_NO_TEXT) int page,
			@RequestParam(defaultValue = MOVIE_PAGE_SIZE_TEXT) int size) {

		String searchWord = query.trim();

		// 검색어가 없으면 매퍼의 조건이 통째로 빠져 전 건이 나온다
		if (searchWord.isEmpty()) {
			throw new IllegalArgumentException("검색어가 필요합니다.");
		}

		if (searchWord.length() > MAX_QUERY_LENGTH) {
			throw new IllegalArgumentException("검색어는 " + MAX_QUERY_LENGTH + "자 이내여야 합니다.");
		}

		DTO param = new DTO();
		param.setPageNo(page);
		param.setPageSize(size);
		param.setSearchDiv(SEARCH_BY_TITLE);
		param.setSearchWord(searchWord);
		// 모르는 정렬 값은 서비스의 화이트리스트가 막는다
		param.getSearchMap().put(SEARCH_KEY_SORT, sort);

		List<ContentVO> movies = contentService.retrieve(param);

		// 서비스가 normalizePaging으로 손본 값을 그대로 실어야 화면의 페이저가 맞는다
		return new PageResponse<>(
				movies,
				param.getPageNo(),
				param.getPageSize(),
				param.getTotalCnt()
		);
	}

	/** 지원하지 않는 정렬 축은 서비스가 IllegalArgumentException으로 알린다 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleIllegalArgumentException(IllegalArgumentException exception) {
		MessageVO body = new MessageVO(
				String.valueOf(HttpStatus.BAD_REQUEST.value()),
				exception.getMessage(),
				"No Detail Message."
		);

		return ResponseEntity.badRequest().body(body);
	}

	/** 예상하지 못한 예외의 내부 상세정보는 외부에 노출하지 않는다 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<MessageVO> handleException(Exception exception) {
		log.warn("검색 API 처리에 실패했습니다.", exception);

		MessageVO body = new MessageVO(
				String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
				"서비스 처리 중 오류가 발생했습니다.",
				"No Detail Message."
		);

		return ResponseEntity.internalServerError().body(body);
	}

}
