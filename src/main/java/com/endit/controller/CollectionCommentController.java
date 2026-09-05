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
import com.endit.domain.CollectionCommentCreateRequest;
import com.endit.domain.UserCommentVO;
import com.endit.security.LoginMemberHelper;
import com.endit.service.CollectionService;
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
 * ------------------------------------------------------------
 * </pre>
 */
@RestController
@RequestMapping("/api/collections/{collectionId}/comments")
public class CollectionCommentController {

	private static final String SEARCH_COLLECTION = "30";
	private static final String SORT_LIKES = "likes";
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_COMMENT_LENGTH = 1000;

	private final CollectionService collectionService;
	private final UserCommentService userCommentService;

	public CollectionCommentController(
			CollectionService collectionService,
			UserCommentService userCommentService) {

		this.collectionService = collectionService;
		this.userCommentService = userCommentService;
	}

	/** 좋아요가 많은 순, 동률이면 최신순으로 컬렉션 댓글을 조회한다. */
	@GetMapping
	public ResponseEntity<Map<String, Object>> retrieve(
			@PathVariable int collectionId,
			@RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		OptionalLong currentMemberId = findCurrentMemberId();
		collectionService.get(collectionId, currentMemberId);

		DTO param = createCommentSearch(collectionId, pageNo, pageSize, currentMemberId);
		List<UserCommentVO> items = userCommentService.doRetrieve(param);
		int totalCount = items.isEmpty()
				? userCommentService.totalCntBySearch(param)
				: items.get(0).getTotalCnt();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("totalCount", totalCount);
		response.put("pageNo", param.getPageNo());
		response.put("pageSize", param.getPageSize());
		response.put("hasNext", param.getPageNo() * param.getPageSize() < totalCount);

		return ResponseEntity.ok(response);
	}

	/** 로그인 회원의 컬렉션 댓글을 등록한다. */
	@PostMapping
	public ResponseEntity<UserCommentVO> create(
			@PathVariable int collectionId,
			@RequestBody CollectionCommentCreateRequest request) {

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
		URI location = URI.create(
				"/api/collections/" + collectionId + "/comments/" + comment.getCommentId());

		return ResponseEntity.created(location).body(created);
	}

	/** 컬렉션 댓글 조회용 DTO를 생성한다. */
	private static DTO createCommentSearch(
			int collectionId,
			int pageNo,
			int pageSize,
			OptionalLong currentMemberId) {

		DTO param = new DTO();
		param.setPageNo(Math.max(1, pageNo));
		param.setPageSize(Math.min(MAX_PAGE_SIZE,
				pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize));
		param.setSearchDiv(SEARCH_COLLECTION);
		param.setSearchWord(String.valueOf(collectionId));
		param.getSearchMap().put("sort", SORT_LIKES);

		if (currentMemberId.isPresent()) {
			param.getSearchMap().put(
					"viewerMemberId", String.valueOf(currentMemberId.getAsLong()));
		}

		return param;
	}

	/** 비회원 조회를 지원하기 위한 현재 로그인 회원 번호 반환 */
	private static OptionalLong findCurrentMemberId() {
		LoginMember loginMember = LoginMemberHelper.getLoginMember();

		return loginMember == null
				? OptionalLong.empty()
				: OptionalLong.of(loginMember.getMemberId());
	}

	/** 댓글 본문 필수값과 최대 길이를 검증한다. */
	private static String normalizeCommentDetail(CollectionCommentCreateRequest request) {
		if (request == null || request.getCommentDetail() == null
				|| request.getCommentDetail().isBlank()) {
			throw new IllegalArgumentException("댓글 내용을 입력해 주세요.");
		}

		String normalized = request.getCommentDetail().trim();
		if (normalized.length() > MAX_COMMENT_LENGTH) {
			throw new IllegalArgumentException(
					"댓글은 최대 " + MAX_COMMENT_LENGTH + "자까지 작성 가능해요.");
		}

		return normalized;
	}

	/** 스포일러 여부를 Y 또는 N으로 정규화한다. */
	private static String normalizeSpoiler(CollectionCommentCreateRequest request) {
		return request != null && UserCommentVO.SPOILER_YES.equalsIgnoreCase(request.getSpoiler())
				? UserCommentVO.SPOILER_YES
				: UserCommentVO.SPOILER_NO;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(
				new MessageVO("400", exception.getMessage(), "댓글 요청값을 확인해 주세요."));
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<MessageVO> handleNotFound(NoSuchElementException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
				new MessageVO("404", exception.getMessage(), "컬렉션을 찾을 수 없습니다."));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<MessageVO> handleDuplicateComment(
			DataIntegrityViolationException exception) {

		return ResponseEntity.status(HttpStatus.CONFLICT).body(
				new MessageVO("409", "이미 이 컬렉션에 댓글을 작성했어요.",
						"회원은 컬렉션마다 댓글을 하나만 작성할 수 있습니다."));
	}
}
