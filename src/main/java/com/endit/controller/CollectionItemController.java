package com.endit.controller;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalLong;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.cmn.LoginMember;
import com.endit.cmn.MessageVO;
import com.endit.auth.ForbiddenOperationException;
import com.endit.domain.CollectionItemVO;
import com.endit.security.LoginMemberHelper;
import com.endit.service.CollectionItemService;

/**
 * <pre>
 * Class Name  : CollectionItemController
 * Description : 컬렉션 작품의 목록 조회, 추가 및 삭제 요청을 처리하는 REST Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * 2026. 8. 29. jinyoung    부모 컬렉션 공개 범위·소유권 및 인증 처리 추가
 * 2026. 9. 05. jinyoung    로그인 회원 조회를 팀 공용 LoginMemberHelper로 통일
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@RestController
@RequestMapping("/api/collections/{collectionId}/items")
public class CollectionItemController {

	private final CollectionItemService collectionItemService;

	/** CollectionItemService를 주입받아 Controller 생성 */
	public CollectionItemController(CollectionItemService collectionItemService) {

		this.collectionItemService = collectionItemService;
	}

	/** 컬렉션 작품 목록 조회 */
	@GetMapping
	public ResponseEntity<Map<String, Object>> retrieve(
			@PathVariable int collectionId,
			@RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "12") int pageSize) {

		DTO param = new DTO();
		
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);

		List<CollectionItemVO> items =
				collectionItemService.retrieve(
						collectionId,
						param,
						findCurrentMemberId());

		Map<String, Object> response = new LinkedHashMap<>();
		
		response.put("items", items);
		response.put("page", param);

		return ResponseEntity.ok(response);
	}

	/** 컬렉션 작품 단건 조회 */
	@GetMapping("/{contentId}")
	public ResponseEntity<CollectionItemVO> get(
			@PathVariable int collectionId,
			@PathVariable int contentId) {

		return ResponseEntity.ok(
				collectionItemService.get(
						collectionId,
						contentId,
						findCurrentMemberId()));
	}

	/** 컬렉션에 작품 추가 */
	@PostMapping
	public ResponseEntity<CollectionItemVO> create(
			@PathVariable int collectionId,
			@RequestBody CollectionItemVO param) {

		long memberId = LoginMemberHelper.getMemberId();
		CollectionItemVO created = collectionItemService.create(
				memberId, collectionId, param);

		URI location = URI.create(
				"/api/collections/" + collectionId
				+ "/items/" + created.getContentId());

		return ResponseEntity.created(location).body(created);
	}

	/** 컬렉션에서 작품 삭제 */
	@DeleteMapping("/{contentId}")
	public ResponseEntity<Void> delete(
			@PathVariable int collectionId,
			@PathVariable int contentId) {

		long memberId = LoginMemberHelper.getMemberId();
		collectionItemService.delete(memberId, collectionId, contentId);

		return ResponseEntity.noContent().build();
	}

	/** 비회원 조회를 지원하기 위한 현재 로그인 회원 번호 */
	private static OptionalLong findCurrentMemberId() {
		LoginMember loginMember = LoginMemberHelper.getLoginMember();

		return loginMember == null
				? OptionalLong.empty()
				: OptionalLong.of(loginMember.getMemberId());
	}

	/** 잘못된 요청값 예외를 HTTP 400 응답으로 변환 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(
			IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"컬렉션 작품 요청값을 확인해 주세요.");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
	}

	/** 인증 회원이 소유하지 않은 컬렉션 작품 변경을 HTTP 403으로 변환 */
	@ExceptionHandler(ForbiddenOperationException.class)
	public ResponseEntity<MessageVO> handleForbidden(
			ForbiddenOperationException exception) {

		MessageVO message = new MessageVO(
				"403", exception.getMessage(), "컬렉션 작품을 변경할 권한이 없습니다.");

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
	}

	/** 외래 키 등 데이터 무결성 예외를 HTTP 400 응답으로 변환 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<MessageVO> handleDataIntegrityViolation(
			DataIntegrityViolationException exception) {

		MessageVO message = new MessageVO(
				"400",
				"존재하는 컬렉션과 콘텐츠 번호를 입력해 주세요.",
				"컬렉션 작품 데이터의 참조 관계를 확인해 주세요.");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
	}

	/** 존재하지 않는 컬렉션 작품 예외를 HTTP 404 응답으로 변환 */
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<MessageVO> handleNotFound(
			NoSuchElementException exception) {

		MessageVO message = new MessageVO(
				"404",
				exception.getMessage(),
				"요청한 컬렉션 작품을 찾을 수 없습니다.");

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
	}

	/** 중복 작품 등 현재 상태 충돌을 HTTP 409 응답으로 변환 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<MessageVO> handleConflict(
			IllegalStateException exception) {

		MessageVO message = new MessageVO(
				"409",
				exception.getMessage(),
				"컬렉션 작품의 현재 상태를 확인해 주세요.");

		return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
	}

}
