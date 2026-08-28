package com.endit.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.domain.ContentVO;
import com.endit.service.ContentService;

@RestController
@RequestMapping("/content")
public class ContentController {
	
	// autowired 방식임. 생성자 주입 아님 
	@Autowired
	private ContentService contentService;

	// TMDB 인기 영화를 가져와 DB에 없는 건만 저장-> 동작 방식 컨텐츠 서비스 인터페이스에 주석으로 엄청 설명해놨으요. 
	@RequestMapping(value = "/tmdb/popular", method = {RequestMethod.GET, RequestMethod.POST})
	public Map<String, Object> importPopular(@RequestParam(defaultValue = "50") int limit) {
		int insertedCount = contentService.importPopular(limit);

		Map<String, Object> result = new HashMap<>();
		result.put("insertedCount", insertedCount);
		return result;
	}

	/** 컬렉션 작품 추가 모달에서 사용할 영화 제목 검색 */
	@GetMapping("/search")
	public List<ContentVO> search(
			@RequestParam(defaultValue = "") String searchWord,
			@RequestParam(defaultValue = "20") int limit) {

		return contentService.search(searchWord, limit);
	}

}
