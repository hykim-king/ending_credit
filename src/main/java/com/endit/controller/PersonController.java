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
import com.endit.service.PersonService;

/**
 * 인물 상세 / 목록 API
 * 화면설계: P-01
 * 담당: 이기준
 *
 * GET /api/people/{personId}                              인물 상세 (+ 참여 작품)
 * GET /api/people?searchWord=&searchDiv=&pageNo=&pageSize= 인물 검색·목록
 */
@RestController
@RequestMapping("/api/people")
public class PersonController {

	private static final Logger log = LoggerFactory.getLogger(PersonController.class);

	private final PersonService personService;

	public PersonController(PersonService personService) {
		this.personService = personService;
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
