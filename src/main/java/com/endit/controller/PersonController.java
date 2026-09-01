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

/**
 * 인물 상세 / 목록 API
 * 화면설계: P-01
 * 담당: 이기준
 *
 * GET /api/people/{personId}                              인물 상세 (+ 참여 작품)
 * GET /api/people/{personId}/filmography?page=&size=       인물 참여 작품 (P-01 더보기)
 * GET /api/people?searchWord=&searchDiv=&pageNo=&pageSize= 인물 검색·목록
 */
@RestController
@RequestMapping("/api/people")
public class PersonController {

	private static final Logger log = LoggerFactory.getLogger(PersonController.class);

	// P-01 더보기가 페이지 크기를 안 넘겼을 때의 기본값. 화면은 data-page-size로 첫 페이지와 같은 값을 보낸다
	private static final String DEFAULT_PAGE_SIZE_TEXT = "12";
	private static final String FIRST_PAGE_NO_TEXT = "1";

	private final PersonService personService;
	private final ContentCreditService contentCreditService;

	public PersonController(PersonService personService, ContentCreditService contentCreditService) {
		this.personService = personService;
		this.contentCreditService = contentCreditService;
	}

	/** 인물 상세 + 참여 작품 */
	@GetMapping("/{personId}")
	public ResponseEntity<Map<String, Object>> get(@PathVariable int personId) {
		log.debug("get personId={}", personId);

		PersonVO person = personService.get(personId);
		if (person == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		List<ContentCreditVO> filmography = personService.getFilmography(personId);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("person", person);
		body.put("filmography", filmography);
		return ResponseEntity.ok(body);
	}

	/**
	 * 인물 참여 작품 (P-01 더보기)
	 *
	 * PersonService.getFilmography는 pageSize 50 고정에 DTO를 버려서 총건수가 나오지 않는다.
	 * 더보기는 남은 건수를 알아야 하므로 ContentCreditService를 직접 부른다.
	 * (ContentCreditMapper로 내려가면 이미지 URL 변환을 건너뛴다 - P-01 이미지가 404 났던 원인)
	 */
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
		// 화면이 남은 건수를 재려면 totalCnt가 필요하다. param에 담겨 돌아온다
		response.put("page", param);

		return ResponseEntity.ok(response);
	}

	/** 인물 목록·검색 */
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
