package com.endit.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.PersonVO;
import com.endit.service.ContentCreditService;
import com.endit.service.PersonService;

// P-01 인물 상세 - 인물 조회·참여작·검색 REST API
@RestController
@RequestMapping("/api/people")
public class PersonController {

	private static final Logger log = LoggerFactory.getLogger(PersonController.class);

	// P-01 더보기 기본값 - 애노테이션이 상수식만 받아 문자열이 원본이다
	private static final String DEFAULT_PAGE_SIZE_TEXT = "12";
	private static final String FIRST_PAGE_NO_TEXT = "1";

	// P-01 상세에 싣는 참여작 첫 페이지 - 위 문자열에서 파생시켜 어긋나지 않게 한다
	private static final int FIRST_PAGE_NO = Integer.parseInt(FIRST_PAGE_NO_TEXT);
	private static final int FILMOGRAPHY_PREVIEW_SIZE = Integer.parseInt(DEFAULT_PAGE_SIZE_TEXT);

	private final PersonService personService;
	private final ContentCreditService contentCreditService;

	public PersonController(PersonService personService, ContentCreditService contentCreditService) {
		this.personService = personService;
		this.contentCreditService = contentCreditService;
	}

	// P-01 인물 상세 + 참여작 첫 페이지
	@GetMapping("/{personId}")
	public ResponseEntity<Map<String, Object>> get(@PathVariable int personId) {
		log.debug("get personId={}", personId);

		PersonVO person = personService.get(personId);
		if (person == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(FILMOGRAPHY_PREVIEW_SIZE);

		List<ContentCreditVO> filmography = contentCreditService.retrieveByPerson(personId, param);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("person", person);
		body.put("filmography", filmography);
		body.put("filmographyPage", toPageInfo(param));
		return ResponseEntity.ok(body);
	}

	// P-01 참여작 더보기 - 매퍼로 바로 내려가면 이미지 URL 변환이 빠진다
	@GetMapping("/{personId}/filmography")
	public ResponseEntity<Map<String, Object>> retrieveFilmography(
			@PathVariable int personId,
			@RequestParam(name = "page", defaultValue = FIRST_PAGE_NO_TEXT) int pageNo,
			@RequestParam(name = "size", defaultValue = DEFAULT_PAGE_SIZE_TEXT) int pageSize) {

		log.debug("retrieveFilmography personId={} pageNo={} pageSize={}", personId, pageNo, pageSize);

		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);

		List<ContentCreditVO> items = contentCreditService.retrieveByPerson(personId, param);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", toPageInfo(param));

		return ResponseEntity.ok(response);
	}

	// P-01 페이징 응답 - retrieveByPerson이 DTO에 덮어쓴 검색축(searchDiv '20')이 새어 나가지 않게 추린다
	private Map<String, Object> toPageInfo(DTO param) {
		Map<String, Object> page = new LinkedHashMap<>();
		page.put("pageNo", param.getPageNo());
		page.put("pageSize", param.getPageSize());
		page.put("totalCnt", param.getTotalCnt());

		return page;
	}

	// 인물 목록·검색
	@GetMapping
	public ResponseEntity<Map<String, Object>> retrieve(
			@RequestParam(defaultValue = "") String searchWord,
			@RequestParam(defaultValue = "") String searchDiv,
			@RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "12") int pageSize) {

		log.debug("retrieve searchWord={} searchDiv={} pageNo={} pageSize={}",
				searchWord, searchDiv, pageNo, pageSize);

		DTO param = new DTO();
		param.setSearchWord(searchWord);
		param.setSearchDiv(searchDiv);
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);

		List<PersonVO> items = personService.retrieve(param);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return ResponseEntity.ok(response);
	}
}
