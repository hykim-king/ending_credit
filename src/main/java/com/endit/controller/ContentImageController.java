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

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.ContentImageVO;
import com.endit.service.ContentImageService;

/**
 * 영화 갤러리 이미지 API (API-006)
 * 화면: C-02 본문 갤러리 미리보기 / C-05 갤러리 확대
 *
 * GET /api/movies/{contentId}/images?pageNo=&pageSize=
 */
@RestController
@RequestMapping("/api/movies")
public class ContentImageController {

	private static final Logger log = LoggerFactory.getLogger(ContentImageController.class);

	private static final String DEFAULT_PAGE_NO = "1";
	private static final String DEFAULT_PAGE_SIZE = "12";

	private final ContentImageService contentImageService;

	public ContentImageController(ContentImageService contentImageService) {
		this.contentImageService = contentImageService;
	}

	// 갤러리 이미지 목록 조회 - 썸네일(imageUrl)과 확대용(zoomImageUrl) URL이 모두 완성된 채로 나간다
	@GetMapping("/{contentId}/images")
	public ResponseEntity<Map<String, Object>> retrieve(
			@PathVariable int contentId,
			@RequestParam(defaultValue = DEFAULT_PAGE_NO) int pageNo,
			@RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {

		log.debug("retrieve contentId={}, pageNo={}, pageSize={}", contentId, pageNo, pageSize);

		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);

		List<ContentImageVO> items = contentImageService.retrieve(contentId, param);

		// 목록(items)과 페이징 정보(page)를 한 JSON 응답으로 전달한다.
		// LinkedHashMap은 디버깅할 때 응답 키 순서를 일정하게 유지하기 위해 사용한다.
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return ResponseEntity.ok(response);
	}

	// 잘못된 요청값 예외를 HTTP 400 응답으로 변환
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"갤러리 요청값을 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(message);
	}

}
