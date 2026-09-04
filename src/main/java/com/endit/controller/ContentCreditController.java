package com.endit.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.ContentCreditVO;
import com.endit.service.ContentCreditService;

/**
 * 영화 출연·제작 크레딧 API (API-007)
 * 화면: C-02 본문 미리보기의 전체보기 / C-03 · MOD-05 출연·제작 전체
 *
 * GET /api/movies/{contentId}/credits?role=&pageNo=&pageSize=
 */
@RestController
@RequestMapping("/api/movies")
public class ContentCreditController {

	private static final Logger log = LoggerFactory.getLogger(ContentCreditController.class);

	private static final String DEFAULT_PAGE_NO = "1";
	private static final String DEFAULT_PAGE_SIZE = "12";

	// 역할 필터는 DTO 필드가 아니라 searchMap의 이 키로 간다(ContentCreditServiceImpl과 같은 약속)
	private static final String SEARCH_KEY_ROLE = "role";

	private final ContentCreditService contentCreditService;

	public ContentCreditController(ContentCreditService contentCreditService) {
		this.contentCreditService = contentCreditService;
	}

	/**
	 *
	 * <pre>
	 * Method Name : retrieve
	 * Description : 콘텐츠 하나의 출연·제작 목록. role을 비우면 전체이고, 넣으면 그 역할만 남는다.
	 *               정렬은 매퍼가 감독 우선(DIRECTOR → ACTOR → WRITER → PRODUCER)으로 하므로
	 *               페이지를 나눠도 감독이 첫 페이지에 남는다.
	 *               POL-033의 역할 4종 밖이면 400.
	 *               쿼리 파라미터가 정의서의 page·size가 아니라 pageNo·pageSize인 것은
	 *               기존 API 컨트롤러(API-006 등)와 DTO 필드명에 맞춘 의도된 차이다.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param role DIRECTOR / ACTOR / WRITER / PRODUCER, 비우면 전체
	 * @param pageNo
	 * @param pageSize
	 * @return items(List<ContentCreditVO>) + page(전체 건수 포함)
	 */
	@GetMapping("/{contentId}/credits")
	public ResponseEntity<Map<String, Object>> retrieve(
			@PathVariable int contentId,
			@RequestParam(defaultValue = "") String role,
			@RequestParam(defaultValue = DEFAULT_PAGE_NO) int pageNo,
			@RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {

		log.debug("retrieve contentId={}, role={}, pageNo={}, pageSize={}", contentId, role, pageNo, pageSize);

		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);

		// 빈 값을 넣으면 매퍼의 <if>가 걸리지 않지만, 키 자체를 두지 않아 조회 조건을 깨끗하게 남긴다
		if (StringUtils.hasText(role)) {
			param.getSearchMap().put(SEARCH_KEY_ROLE, role);
		}

		List<ContentCreditVO> items = contentCreditService.retrieve(contentId, param);

		// 목록(items)과 페이징 정보(page)를 한 JSON 응답으로 전달한다.
		// LinkedHashMap은 디버깅할 때 응답 키 순서를 일정하게 유지하기 위해 사용한다.
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return ResponseEntity.ok(response);
	}

	// 잘못된 요청값 예외를 HTTP 400 응답으로 변환 - 역할 화이트리스트도 여기로 떨어진다
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"출연·제작 요청값을 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(message);
	}

}
