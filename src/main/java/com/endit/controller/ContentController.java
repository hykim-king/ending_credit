package com.endit.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.service.ContentService;

@RestController
@RequestMapping("/content")
public class ContentController {

	private static final String DEFAULT_SYNC_LIMIT = "50";

	private final ContentService contentService;

	public ContentController(ContentService contentService) {
		this.contentService = contentService;
	}

	// TMDB 인기 영화를 가져와 DB에 없는 건만 저장-> 동작 방식 컨텐츠 서비스 인터페이스에 주석으로 엄청 설명해놨으요.
	// DB에 쓰는 동작이므로 POST만 받는다. GET으로 열어 두면 주소창·크롤러가 수집을 일으킨다
	@PostMapping("/tmdb/popular")
	public Map<String, Object> sync(@RequestParam(defaultValue = DEFAULT_SYNC_LIMIT) int limit) {
		int insertedCount = contentService.sync(limit);

		Map<String, Object> result = new HashMap<>();
		result.put("insertedCount", insertedCount);
		return result;
	}

}
