package com.endit.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.PersonVO;
import com.endit.service.ContentCreditService;
import com.endit.service.PersonService;

import jakarta.servlet.http.HttpSession;

// AD-05 인물 목록 / AD-06 인물 등록·수정 - 인물·크레딧 REST API (API-053·054)
@RestController
@RequestMapping("/api/admin")
public class AdminPersonApiController {

	private static final Logger log = LoggerFactory.getLogger(AdminPersonApiController.class);

	private static final String FIRST_PAGE_NO_TEXT = "1";
	private static final String DEFAULT_PAGE_SIZE_TEXT = "20";

	private final PersonService personService;
	private final ContentCreditService contentCreditService;

	public AdminPersonApiController(PersonService personService, ContentCreditService contentCreditService) {
		this.personService = personService;
		this.contentCreditService = contentCreditService;
	}

	// AD-05 인물 검색·목록 - searchDiv 10 국문명 / 20 원문명 / 30 외부ID
	@GetMapping("/people")
	public ResponseEntity<Map<String, Object>> retrieve(
			@RequestParam(defaultValue = "") String searchWord,
			@RequestParam(defaultValue = "") String searchDiv,
			@RequestParam(name = "page", defaultValue = FIRST_PAGE_NO_TEXT) int pageNo,
			@RequestParam(name = "size", defaultValue = DEFAULT_PAGE_SIZE_TEXT) int pageSize,
			HttpSession session) {


		DTO param = new DTO();
		param.setSearchWord(searchWord);
		param.setSearchDiv(searchDiv);
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);

		List<PersonVO> items = personService.retrieve(param);

		return ResponseEntity.ok(toListResponse(items, param));
	}

	// AD-06 인물 등록
	@PostMapping("/people")
	public ResponseEntity<PersonVO> create(@RequestBody PersonVO param, HttpSession session) {

		PersonVO saved = personService.create(param);
		log.debug("create personId={}", saved.getPersonId());

		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	// AD-06 인물 수정 - 비워 둔 필드는 서비스가 기존 값으로 메운다
	@PatchMapping("/people/{personId}")
	public ResponseEntity<PersonVO> update(
			@PathVariable int personId,
			@RequestBody PersonVO param,
			HttpSession session) {


		return ResponseEntity.ok(personService.update(personId, param));
	}

	// AD-06 크레딧 등록 - 이미 등록된 작품만 고른다
	@PostMapping("/credits")
	public ResponseEntity<ContentCreditVO> createCredit(
			@RequestBody ContentCreditVO param,
			HttpSession session) {


		if (param == null || param.getContentId() <= 0) {
			throw new IllegalArgumentException("연결할 작품을 선택해 주세요.");
		}

		if (param.getPersonId() <= 0) {
			throw new IllegalArgumentException("인물 번호가 필요합니다.");
		}

		ContentCreditVO saved = contentCreditService.create(param.getContentId(), param);

		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	// AD-06 크레딧 수정 - 역할·배역·표시순서 정정
	@PatchMapping("/credits/{creditId}")
	public ResponseEntity<ContentCreditVO> updateCredit(
			@PathVariable int creditId,
			@RequestBody ContentCreditVO param,
			HttpSession session) {


		return ResponseEntity.ok(contentCreditService.update(creditId, param));
	}

	// AD-06 크레딧 삭제
	@DeleteMapping("/credits/{creditId}")
	public ResponseEntity<Void> deleteCredit(@PathVariable int creditId, HttpSession session) {

		contentCreditService.delete(creditId);

		return ResponseEntity.noContent().build();
	}

	// 목록 + 페이징 공통 응답 구조
	private Map<String, Object> toListResponse(List<PersonVO> items, DTO param) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return response;
	}

	// 필드 오류(POL-034 이름 조건, POL-033 역할 값) → 400
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(IllegalArgumentException exception) {
		return toResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), "입력값을 확인해 주세요.");
	}

	// 없는 인물·크레딧 → 404
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<MessageVO> handleNotFound(NoSuchElementException exception) {
		return toResponse(HttpStatus.NOT_FOUND, exception.getMessage(), "대상을 찾을 수 없습니다.");
	}

	// 외부ID 중복(UK_PERSON_EXTERNAL)·저장 실패 → 409
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<MessageVO> handleConflict(IllegalStateException exception) {
		return toResponse(HttpStatus.CONFLICT, exception.getMessage(), "저장할 수 없는 상태입니다.");
	}

	// 참조 관계·제약 위반 → 400
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<MessageVO> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		log.warn("인물 저장의 제약 조건을 위반했습니다.", exception);

		// 인물의 NOT NULL 위반과 크레딧의 참조 위반이 함께 걸리는 자리라 한쪽만 가리키지 않는다
		return toResponse(HttpStatus.BAD_REQUEST,
				"필수 항목을 채우고, 존재하는 작품과 인물 번호인지 확인해 주세요.",
				"데이터의 제약 조건을 확인해 주세요.");
	}

	private ResponseEntity<MessageVO> toResponse(HttpStatus status, String message, String detail) {
		MessageVO body = new MessageVO(String.valueOf(status.value()), message, detail);

		return ResponseEntity.status(status).body(body);
	}

}
