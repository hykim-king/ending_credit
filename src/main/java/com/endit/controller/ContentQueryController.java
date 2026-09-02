package com.endit.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;
import com.endit.service.ContentService;

/**
 * <pre>
 * Class Name  : ContentQueryController
 * Description : 컬렉션 작품 선택용 영화 제목 검색 및 페이징 조회를 처리하는 REST Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    컬렉션 작품 선택용 읽기 API 최초 생성
 * 2026. 8. 31. jinyoung    /api/search/movies와 query/page/size 최종 계약 적용
 * ------------------------------------------------------------
 * </pre>
 */
@RestController
@RequestMapping("/api/search/movies")
public class ContentQueryController {

	private final ContentService contentService;

	public ContentQueryController(ContentService contentService) {
		this.contentService = contentService;
	}

	/** 저장된 콘텐츠를 한글 제목 또는 원제로 검색한다. */
	@GetMapping
	public ResponseEntity<Map<String, Object>> retrieve(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(name = "page", defaultValue = "1") int pageNo,
			@RequestParam(name = "size", defaultValue = "10") int pageSize) {

		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);
		param.setSearchDiv("50");
		param.setSearchWord(query);

		List<ContentVO> items = contentService.retrieve(param);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return ResponseEntity.ok(response);
	}
}
