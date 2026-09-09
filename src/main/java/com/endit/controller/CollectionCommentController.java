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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.cmn.LoginMember;
import com.endit.cmn.MessageVO;
import com.endit.domain.CommentLikeVO;
import com.endit.domain.ReportCommentVO;
import com.endit.domain.UserCommentVO;
import com.endit.security.LoginMemberHelper;
import com.endit.service.CollectionService;
import com.endit.service.CommentLikeService;
import com.endit.service.ReportCommentService;
import com.endit.service.UserCommentService;

/**
 * <pre>
 * Class Name  : CollectionCommentController
 * Description : 컬렉션 상세 화면의 댓글 요약·전체 목록 조회와 등록을 처리하는 REST Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 9. 05. jinyoung    최초 생성
 * 2026. 9. 07. jinyoung    댓글 수정·삭제·좋아요·신고 API 추가
 * ------------------------------------------------------------
 * </pre>
 */
@RestController
@RequestMapping("/api/collections/{collectionId}/comments")
public class CollectionCommentController {

	private static final String SEARCH_COLLECTION = "30"; 	// 컬렉션별 코멘트 검색 구분
	private static final String SORT_LIKES = "likes"; 		// 좋아요 많은 순 정렬
	private static final int DEFAULT_PAGE_SIZE = 10; 		// 기본 댓글 조회 건수
	private static final int MAX_PAGE_SIZE = 50; 			// 페이지당 최대 댓글 수
	private static final int MAX_COMMENT_LENGTH = 1000; 	// 댓글 최대 글자 수

	private final CollectionService collectionService;
	private final CommentLikeService commentLikeService;
	private final ReportCommentService reportCommentService;
	private final UserCommentService userCommentService;

	/**
	 * 화면 처리에 필요한 의존성 주입
	 *
	 * @param collectionService    컬렉션 Service
	 * @param commentLikeService   코멘트 좋아요 Service
	 * @param reportCommentService 코멘트 신고 Service
	 * @param userCommentService   코멘트 Service
	 */
	public CollectionCommentController(
			CollectionService collectionService, CommentLikeService commentLikeService,
			ReportCommentService reportCommentService, UserCommentService userCommentService) {
		this.collectionService = collectionService;
		this.commentLikeService = commentLikeService;
		this.reportCommentService = reportCommentService;
		this.userCommentService = userCommentService;
	}

	/**
	 * 좋아요가 많은 순, 동률이면 최신순으로 컬렉션 댓글을 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @param pageNo       페이지 번호
	 * @param pageSize     페이지당 건수
	 * @return 조회 목록과 페이징 정보
	 */
	@GetMapping
	public ResponseEntity<Map<String, Object>> retrieve(
			@PathVariable int collectionId,
			@RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		OptionalLong currentMemberId = findCurrentMemberId();
		collectionService.get(collectionId, currentMemberId);

		DTO param = createCommentSearch(collectionId, pageNo, pageSize, currentMemberId);
		List<UserCommentVO> items = userCommentService.doRetrieve(param);
		int totalCount = items.isEmpty() ? userCommentService.totalCntBySearch(param) : items.get(0).getTotalCnt();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("totalCount", totalCount);
		response.put("pageNo", param.getPageNo());
		response.put("pageSize", param.getPageSize());
		response.put("hasNext", param.getPageNo() * param.getPageSize() < totalCount);

		return ResponseEntity.ok(response);
	}

	/**
	 * 로그인 회원의 컬렉션 댓글을 등록
	 *
	 * @param collectionId 컬렉션 번호
	 * @param request      요청 본문
	 * @return 코멘트 정보
	 */
	@PostMapping
	public ResponseEntity<UserCommentVO> create(
			@PathVariable int collectionId, @RequestBody CommentCreateRequest request) {

		long memberId = LoginMemberHelper.getMemberId();
		collectionService.get(collectionId, OptionalLong.of(memberId));

		UserCommentVO comment = new UserCommentVO();
		comment.setMemberId(memberId);
		comment.setCollectionId(Long.valueOf(collectionId));
		comment.setCommentDetail(normalizeCommentDetail(request));
		comment.setSpoiler(normalizeSpoiler(request));

		if (userCommentService.doSave(comment) != 1) {
			throw new IllegalStateException("댓글 등록에 실패했습니다.");
		}

		UserCommentVO created = userCommentService.doSelectOne(comment);
		URI location = URI.create("/api/collections/" + collectionId + "/comments/" + comment.getCommentId());

		return ResponseEntity.created(location).body(created);
	}

	/**
	 * 로그인 회원이 작성한 컬렉션 댓글의 내용과 스포일러 여부를 수정
	 *
	 * @param collectionId 컬렉션 번호
	 * @param commentId    댓글 번호
	 * @param request      요청 본문
	 * @return 수정된 댓글 정보
	 */
	@PutMapping("/{commentId}")
	public ResponseEntity<UserCommentVO> update(
			@PathVariable int collectionId,
			@PathVariable long commentId,
			@RequestBody CommentUpdateRequest request) {

		long memberId = LoginMemberHelper.getMemberId();
		collectionService.get(collectionId, OptionalLong.of(memberId));
		requireCommentOwner(collectionId, commentId, memberId);

		UserCommentVO comment = new UserCommentVO();
		comment.setCommentId(commentId);
		// 매퍼가 작성자 본인만 수정하도록 member_id를 함께 본다(위 requireCommentOwner와 같은 값)
		comment.setMemberId(memberId);
		comment.setCommentDetail(normalizeCommentDetail(request));
		comment.setSpoiler(normalizeSpoiler(request));

		if (userCommentService.doUpdate(comment) != 1) {
			throw new IllegalStateException("댓글 수정에 실패했습니다.");
		}

		return ResponseEntity.ok(userCommentService.doSelectOne(comment));
	}

	/** 로그인 회원이 작성한 컬렉션 댓글을 삭제 */
	@DeleteMapping("/{commentId}")
	public ResponseEntity<Void> delete(
			@PathVariable int collectionId, @PathVariable long commentId) {

		long memberId = LoginMemberHelper.getMemberId();
		collectionService.get(collectionId, OptionalLong.of(memberId));
		UserCommentVO comment = requireCommentOwner(collectionId, commentId, memberId);

		if (userCommentService.doDelete(comment) != 1) {
			throw new IllegalStateException("댓글 삭제에 실패했습니다.");
		}

		return ResponseEntity.noContent().build();
	}

	/** 로그인 회원의 컬렉션 댓글 좋아요 상태를 토글 */
	@PostMapping("/{commentId}/likes")
	public ResponseEntity<Map<String, Object>> toggleLike(
			@PathVariable int collectionId, @PathVariable long commentId) {

		long memberId = LoginMemberHelper.getMemberId();
		collectionService.get(collectionId, OptionalLong.of(memberId));
		requireCollectionComment(collectionId, commentId);

		CommentLikeVO like = new CommentLikeVO();
		like.setMemberId(memberId);
		like.setCommentId(commentId);
		int state = commentLikeService.upToggleLike(like);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("liked", state == CommentLikeService.LIKE_ON);
		response.put("likeCount", commentLikeService.getLikeCnt(commentId));

		return ResponseEntity.ok(response);
	}

	/** 로그인 회원이 다른 회원의 컬렉션 댓글을 신고 */
	@PostMapping("/{commentId}/reports")
	public ResponseEntity<Void> report(
			@PathVariable int collectionId,
			@PathVariable long commentId,
			@RequestBody CommentReportRequest request) {

		long memberId = LoginMemberHelper.getMemberId();
		collectionService.get(collectionId, OptionalLong.of(memberId));
		UserCommentVO comment = requireCollectionComment(collectionId, commentId);
		if (comment.getMemberId() == memberId) {
			throw new SecurityException("본인이 작성한 댓글은 신고할 수 없습니다.");
		}

		ReportCommentVO report = new ReportCommentVO();
		report.setReportMemberId(memberId);
		report.setCommentId(commentId);
		report.setReason(normalizeReportReason(request));
		report.setDetail(normalizeReportDetail(request, report.getReason()));

		if (reportCommentService.doSave(report) != 1) {
			throw new IllegalStateException("댓글 신고에 실패했습니다.");
		}

		return ResponseEntity.noContent().build();
	}

	/**
	 * 잘못된 요청값을 HTTP 400으로 변환
	 *
	 * @param exception 잘못된 요청값 예외
	 * @return 오류 상태와 안내 메시지
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(IllegalArgumentException exception) {

		return ResponseEntity.badRequest().body(
				new MessageVO("400", exception.getMessage(), "댓글 요청값을 확인해 주세요."));
	}

	/**
	 * 대상 미존재 예외를 HTTP 404로 변환
	 *
	 * @param exception 대상 미존재 예외
	 * @return 오류 상태와 안내 메시지
	 */
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<MessageVO> handleNotFound(NoSuchElementException exception) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
				new MessageVO("404", exception.getMessage(), "컬렉션을 찾을 수 없습니다."));
	}

	/**
	 * 데이터 무결성 예외를 댓글 중복 안내와 HTTP 409로 변환
	 *
	 * @param exception 데이터 무결성 예외
	 * @return 오류 상태와 안내 메시지
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<MessageVO> handleDuplicateComment(DataIntegrityViolationException exception) {

		return ResponseEntity.status(HttpStatus.CONFLICT).body(
				new MessageVO("409", "이미 이 컬렉션에 댓글을 작성했어요.", "회원은 컬렉션마다 댓글을 하나만 작성할 수 있습니다."));
	}

	/** 작성자 전용 요청을 다른 회원이 보낸 경우 HTTP 403으로 변환 */
	@ExceptionHandler(SecurityException.class)
	public ResponseEntity<MessageVO> handleForbidden(SecurityException exception) {

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
				new MessageVO("403", exception.getMessage(), "댓글 작성자만 처리할 수 있습니다."));
	}

	// 내부 조회 조건·응답 구성

	/**
	 * 컬렉션 댓글 조회용 DTO를 생성
	 *
	 * @param collectionId    컬렉션 번호
	 * @param pageNo          페이지 번호
	 * @param pageSize        페이지당 건수
	 * @param currentMemberId 현재 로그인 회원 번호
	 * @return 컬렉션 검색·정렬·페이징 조건
	 */
	private static DTO createCommentSearch(int collectionId, int pageNo, int pageSize, OptionalLong currentMemberId) {

		DTO param = new DTO();
		param.setPageNo(Math.max(1, pageNo));
		param.setPageSize(Math.min(MAX_PAGE_SIZE, pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize));
		param.setSearchDiv(SEARCH_COLLECTION);
		param.setSearchWord(String.valueOf(collectionId));
		param.getSearchMap().put("sort", SORT_LIKES);

		if (currentMemberId.isPresent()) {
			param.getSearchMap().put("viewerMemberId", String.valueOf(currentMemberId.getAsLong()));
		}

		return param;
	}

	/**
	 * 비회원 조회를 지원하기 위한 현재 로그인 회원 번호 반환
	 *
	 * @return 로그인 회원 번호, 비회원이면 빈 OptionalLong
	 */
	private static OptionalLong findCurrentMemberId() {

		LoginMember loginMember = LoginMemberHelper.getLoginMember();

		return loginMember == null ? OptionalLong.empty() : OptionalLong.of(loginMember.getMemberId());
	}

	/** 컬렉션에 속한 댓글을 조회하고 대상이 다르거나 없으면 예외 처리 */
	private UserCommentVO requireCollectionComment(int collectionId, long commentId) {

		if (commentId <= 0) {
			throw new IllegalArgumentException("올바른 댓글 번호가 필요합니다.");
		}

		UserCommentVO key = new UserCommentVO();
		key.setCommentId(commentId);
		UserCommentVO comment = userCommentService.doSelectOne(key);

		if (comment == null || comment.getCollectionId() == null
				|| comment.getCollectionId().longValue() != collectionId) {
			throw new NoSuchElementException("컬렉션 댓글을 찾을 수 없습니다.");
		}

		return comment;
	}

	/** 컬렉션 댓글의 작성자가 현재 로그인 회원인지 확인 */
	private UserCommentVO requireCommentOwner(int collectionId, long commentId, long memberId) {

		UserCommentVO comment = requireCollectionComment(collectionId, commentId);
		if (comment.getMemberId() != memberId) {
			throw new SecurityException("본인이 작성한 댓글만 수정하거나 삭제할 수 있습니다.");
		}

		return comment;
	}

	/**
	 * 댓글 본문 필수값과 최대 길이를 검증
	 *
	 * @param request 요청 본문
	 * @return 앞뒤 공백을 제거한 코멘트 내용
	 */
	private static String normalizeCommentDetail(CommentRequest request) {

		if (request == null || request.commentDetail() == null
				|| request.commentDetail().isBlank()) {
			throw new IllegalArgumentException("댓글 내용을 입력해 주세요.");
		}

		String normalized = request.commentDetail().trim();
		if (normalized.length() > MAX_COMMENT_LENGTH) {
			throw new IllegalArgumentException("댓글은 최대 " + MAX_COMMENT_LENGTH + "자까지 작성 가능해요.");
		}

		return normalized;
	}

	/**
	 * 스포일러 여부를 Y 또는 N으로 정규화
	 *
	 * @param request 요청 본문
	 * @return 스포일러 여부 (Y/N)
	 */
	private static String normalizeSpoiler(CommentRequest request) {

		return request != null && UserCommentVO.SPOILER_YES.equalsIgnoreCase(
				request.spoiler()) ? UserCommentVO.SPOILER_YES : UserCommentVO.SPOILER_NO;
	}

	/** 신고 사유를 허용된 코드로 정규화 */
	private static String normalizeReportReason(CommentReportRequest request) {

		if (request == null || request.reason() == null) {
			throw new IllegalArgumentException("신고 사유를 선택해 주세요.");
		}

		String reason = request.reason().trim().toUpperCase();
		if (!ReportCommentVO.REASON_SPOILER.equals(reason)
				&& !ReportCommentVO.REASON_INAPPROPRIATE.equals(reason)
				&& !ReportCommentVO.REASON_SPAM.equals(reason)
				&& !ReportCommentVO.REASON_OTHER.equals(reason)) {
			throw new IllegalArgumentException("올바른 신고 사유를 선택해 주세요.");
		}

		return reason;
	}

	/** 신고 상세 내용을 정리하고 기타 사유의 필수값을 검증 */
	private static String normalizeReportDetail(CommentReportRequest request, String reason) {

		String detail = request.detail() == null ? "" : request.detail().trim();
		if (ReportCommentVO.REASON_OTHER.equals(reason) && detail.isEmpty()) {
			throw new IllegalArgumentException("기타 신고 사유를 입력해 주세요.");
		}
		if (detail.length() > MAX_COMMENT_LENGTH) {
			throw new IllegalArgumentException("신고 내용은 최대 " + MAX_COMMENT_LENGTH + "자까지 작성 가능해요.");
		}

		return detail.isEmpty() ? null : detail;
	}

	/** 댓글 작성·수정 요청의 공통 필드 계약 */
	private interface CommentRequest {
		String commentDetail();

		String spoiler();
	}

	/**
	 * 컬렉션 댓글 등록 요청 본문
	 *
	 * @param commentDetail 코멘트 내용
	 * @param spoiler 스포일러 여부 (Y/N)
	 */
	public record CommentCreateRequest(String commentDetail, String spoiler) implements CommentRequest {
	}

	/** 컬렉션 댓글 수정 요청 본문 */
	public record CommentUpdateRequest(String commentDetail, String spoiler) implements CommentRequest {
	}

	/** 컬렉션 댓글 신고 요청 본문 */
	public record CommentReportRequest(String reason, String detail) {
	}
}
