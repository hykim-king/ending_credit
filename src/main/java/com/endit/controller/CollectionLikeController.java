package com.endit.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.auth.CurrentMemberProvider;
import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.CollectionLikeItemVO;
import com.endit.domain.CollectionLikeVO;
import com.endit.service.CollectionLikeService;

/**
 * <pre>
 * Class Name  : CollectionLikeController
 * Description : 컬렉션 좋아요의 등록, 취소 및 회원별 좋아요 컬렉션 목록 조회 요청을 처리하는 REST Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. gunwoo      최초 생성
 * 2026. 8. 28. jinyoung    조회 규격 및 예외 처리 보완
 * 2026. 8. 29. jinyoung    인증·공개 범위·본인 제한 및 좋아요 상태 조회 적용
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 27.
 */
@RestController
public class CollectionLikeController {

	private static final String TYPE_COLLECTION = "collection";

	private final CollectionLikeService collectionLikeService;
	private final CurrentMemberProvider currentMemberProvider;

	/**
	 * CollectionLikeService를 주입받아 Controller 생성
	 *
	 * @param collectionLikeService 컬렉션 좋아요 Service
	 * @param currentMemberProvider 현재 로그인 회원 Provider
	 */
	public CollectionLikeController(
			CollectionLikeService collectionLikeService,
			CurrentMemberProvider currentMemberProvider) {

		this.collectionLikeService = collectionLikeService;
		this.currentMemberProvider = currentMemberProvider;
	}

	/**
	 * 컬렉션 좋아요 등록 (D-01 좋아요 토글)
	 *
	 * 정책(POL-010) · 이미 좋아요를 누른 상태에서 재요청해도 오류 없이
	 * 현재 좋아요 정보를 그대로 응답하는 멱등 처리를 한다.
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 등록된(또는 이미 등록되어 있던) 컬렉션 좋아요 정보
	 */
	@PostMapping("/api/collections/{collectionId}/likes")
	public ResponseEntity<CollectionLikeVO> like(
			@PathVariable int collectionId) {

		long memberId = currentMemberProvider.requireMemberId();
		CollectionLikeVO like = collectionLikeService.create(
				memberId, collectionId);

		return ResponseEntity.status(HttpStatus.CREATED).body(like);
	}

	/**
	 * 컬렉션 좋아요 취소 (D-01 좋아요 토글)
	 *
	 * 정책(POL-011) · 이미 좋아요가 없는 상태에서 취소 요청이 와도 오류 없이
	 * 204 응답으로 처리하는 멱등 처리를 한다.
	 */
	@DeleteMapping("/api/collections/{collectionId}/likes")
	public ResponseEntity<Void> unlike(
			@PathVariable int collectionId) {

		long memberId = currentMemberProvider.requireMemberId();
		collectionLikeService.delete(memberId, collectionId);

		return ResponseEntity.noContent().build();
	}

	/** 현재 인증 회원의 컬렉션 좋아요 여부 조회 */
	@GetMapping("/api/collections/{collectionId}/likes")
	public ResponseEntity<Map<String, Boolean>> likeStatus(
			@PathVariable int collectionId) {

		long memberId = currentMemberProvider.requireMemberId();
		boolean liked = collectionLikeService.isLiked(memberId, collectionId);

		return ResponseEntity.ok(Map.of("liked", liked));
	}

	/**특정 회원이 좋아요를 누른 컬렉션 목록 조회 (U-07 좋아요 목록 · 컬렉션 탭)*/
	@GetMapping("/api/users/{memberId}/likes")
	public ResponseEntity<Map<String, Object>> retrieveByMember(
			@PathVariable int memberId,
			@RequestParam(defaultValue = TYPE_COLLECTION) String type,
			@RequestParam(name = "page", defaultValue = "1") int pageNo,
			@RequestParam(name = "size", defaultValue = "12") int pageSize) {

		if (!TYPE_COLLECTION.equals(type)) {
			throw new IllegalArgumentException(
					"이 API는 type=" + TYPE_COLLECTION + " 요청만 처리합니다.");
		}

		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);

		List<CollectionLikeItemVO> items = collectionLikeService.retrieveByMember(
				memberId,
				param,
				currentMemberProvider.findCurrentMemberId());

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return ResponseEntity.ok(response);
	}

	/**잘못된 요청값 예외를 HTTP 400 응답으로 변환*/
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(
			IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"컬렉션 좋아요 요청값을 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(message);
	}

	/**존재하지 않는 회원 또는 컬렉션 등의 데이터 무결성 예외를 HTTP 400 응답으로 변환*/
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<MessageVO> handleDataIntegrityViolation(
			DataIntegrityViolationException exception) {

		MessageVO message = new MessageVO(
				"400",
				"존재하는 회원과 컬렉션 번호를 입력해 주세요.",
				"컬렉션 좋아요 데이터의 참조 관계를 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(message);
	}

	/** 존재하지 않거나 접근할 수 없는 컬렉션을 HTTP 404 응답으로 변환 */
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<MessageVO> handleNotFound(
			NoSuchElementException exception) {

		MessageVO message = new MessageVO(
				"404",
				exception.getMessage(),
				"요청한 컬렉션을 찾을 수 없습니다.");

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(message);
	}

	/**저장 및 상태 변경 실패를 HTTP 409 응답으로 변환*/
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<MessageVO> handleConflict(
			IllegalStateException exception) {

		MessageVO message = new MessageVO(
				"409",
				exception.getMessage(),
				"컬렉션 좋아요의 현재 상태를 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(message);
	}

}
