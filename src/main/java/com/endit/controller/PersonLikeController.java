package com.endit.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.PersonLikeVO;
import com.endit.security.LoginMemberHelper;
import com.endit.service.PersonLikeService;

/**
 * <pre>
 * Class Name  : PersonLikeController
 * Description : 인물 좋아요 등록·해제와 회원별 좋아요 목록 요청을 처리하는 REST Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * 2026. 9. 05. jinyoung    인물 좋아요 변경 요청을 Spring Security 로그인 회원 기준으로 통일
 * 2026. 9. 05. jinyoung    로그인 회원 조회를 팀 공용 LoginMemberHelper로 통일
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@RestController
@RequestMapping("/api")
public class PersonLikeController {

	private final PersonLikeService personLikeService;

	/**
	 * PersonLikeService를 주입받아 Controller 생성
	 *
	 * @param personLikeService 인물 좋아요 Service
	 */
	public PersonLikeController(PersonLikeService personLikeService) {

		this.personLikeService = personLikeService;
	}

	/**
	 * 회원이 좋아요한 인물 목록 조회
	 *
	 * type=person 요청만 이 Controller에서 처리한다.
	 * type=collection은 컬렉션 좋아요 Controller에서 처리할 수 있도록 분리한다.
	 *
	 * @param memberId 조회할 회원 번호
	 * @param pageNo 페이지 번호
	 * @param pageSize 페이지당 건수
	 * @param sort 정렬 조건
	 * @return 인물 좋아요 목록과 페이징 정보
	 */
	@GetMapping(
			value = "/users/{memberId}/likes",
			params = "type=person")
	public ResponseEntity<Map<String, Object>> retrieveLikes(
			@PathVariable int memberId,
			@RequestParam(name = "page", defaultValue = "1") int pageNo,
			@RequestParam(name = "size", defaultValue = "12") int pageSize,
			@RequestParam(defaultValue = "latest") String sort) {

		DTO param = createPagingParam(pageNo, pageSize);
		List<PersonLikeVO> items =
				personLikeService.retrieveLikes(memberId, param, sort);

		return ResponseEntity.ok(createListResponse(items, param));
	}

	/** 로그인 회원이 좋아요한 인물 목록 조회 */
	@GetMapping(value = "/members/likes", params = "type=person")
	public ResponseEntity<Map<String, Object>> retrieveMyLikes(
			@RequestParam(name = "page", defaultValue = "1") int pageNo,
			@RequestParam(name = "size", defaultValue = "12") int pageSize,
			@RequestParam(defaultValue = "latest") String sort) {

		int memberId = requireMemberId();
		DTO param = createPagingParam(pageNo, pageSize);
		List<PersonLikeVO> items =
				personLikeService.retrieveLikes(memberId, param, sort);

		return ResponseEntity.ok(createListResponse(items, param));
	}

	/**
	 * 로그인 회원의 인물 좋아요 등록
	 *
	 * @param personId 인물 번호
	 * @return 등록되었거나 이미 존재하는 인물 좋아요 정보
	 */
	@PostMapping("/people/{personId}/likes")
	public ResponseEntity<PersonLikeVO> addLike(
			@PathVariable int personId) {

		int memberId = requireMemberId();
		PersonLikeVO saved =
				personLikeService.addLike(memberId, personId);

		return ResponseEntity.ok(saved);
	}

	/**
	 * 로그인 회원의 인물 좋아요 해제
	 *
	 * @param personId 인물 번호
	 * @return 본문이 없는 응답
	 */
	@DeleteMapping("/people/{personId}/likes")
	public ResponseEntity<Void> deleteLike(
			@PathVariable int personId) {

		int memberId = requireMemberId();
		personLikeService.deleteLike(memberId, personId);

		return ResponseEntity.noContent().build();
	}

	/** Spring Security 로그인 회원 번호 조회 */
	private int requireMemberId() {
		return Math.toIntExact(LoginMemberHelper.getMemberId());
	}

	/**
	 * 요청받은 페이지 번호와 크기로 조회 조건 생성
	 */
	private DTO createPagingParam(
			int pageNo,
			int pageSize) {

		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);

		return param;
	}

	/**
	 * 목록과 페이징 정보를 공통 응답 구조로 생성
	 */
	private Map<String, Object> createListResponse(
			List<PersonLikeVO> items,
			DTO param) {

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return response;
	}

	/**
	 * 잘못된 회원 번호, 인물 번호 및 조회 조건을 HTTP 400으로 변환
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(
			IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"인물 좋아요 요청값을 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(message);
	}

	/**
	 * 존재하지 않는 회원 또는 인물 등의 데이터 무결성 예외를 HTTP 400으로 변환
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<MessageVO> handleDataIntegrityViolation(
			DataIntegrityViolationException exception) {

		MessageVO message = new MessageVO(
				"400",
				"존재하는 회원과 인물 번호를 입력해 주세요.",
				"인물 좋아요 데이터의 참조 관계를 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(message);
	}

	/**
	 * 저장 및 상태 변경 실패를 HTTP 409로 변환
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<MessageVO> handleConflict(
			IllegalStateException exception) {

		MessageVO message = new MessageVO(
				"409",
				exception.getMessage(),
				"인물 좋아요의 현재 상태를 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(message);
	}
}
