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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.MemberContentVO;
import com.endit.domain.RatingRequest;
import com.endit.service.MemberContentService;

/**
 * <pre>
 * Class Name  : MemberContentController
 * Description : 회원별 콘텐츠 평가와 보고싶어요 요청을 처리하는 REST Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@RestController
@RequestMapping("/api")
public class MemberContentController {

	/*
	 * 로그인 기능 병합 전까지 사용하는 임시 회원 식별 헤더다.
	 * 최종 통합 시 RequestHeader를 제거하고 LoginMemberHelper에서 로그인 회원 번호를 조회하도록 변경한다.
	 */
	private static final String TEMP_MEMBER_ID_HEADER = "X-Member-Id";

	private final MemberContentService memberContentService;

	/**
	 * MemberContentService를 주입받아 Controller 생성
	 *
	 * @param memberContentService 회원 콘텐츠 Service
	 */
	public MemberContentController(
			MemberContentService memberContentService) {

		this.memberContentService = memberContentService;
	}

	/**
	 * 회원이 평가한 콘텐츠 목록 조회
	 *
	 * @param memberId 조회할 회원 번호
	 * @param pageNo 페이지 번호
	 * @param pageSize 페이지당 건수
	 * @param sort 정렬 조건
	 * @return 평가 콘텐츠 목록과 페이징 정보
	 */
	@GetMapping("/users/{memberId}/ratings")
	public ResponseEntity<Map<String, Object>> retrieveRatings(
			@PathVariable int memberId,
			@RequestParam(name = "page", defaultValue = "1") int pageNo,
			@RequestParam(name = "size", defaultValue = "12") int pageSize,
			@RequestParam(defaultValue = "latest") String sort) {

		DTO param = createPagingParam(pageNo, pageSize);
		List<MemberContentVO> items =
				memberContentService.retrieveRatings(memberId, param, sort);

		return ResponseEntity.ok(createListResponse(items, param));
	}

	/**
	 * 회원이 보고싶어요로 등록한 콘텐츠 목록 조회
	 *
	 * @param memberId 조회할 회원 번호
	 * @param pageNo 페이지 번호
	 * @param pageSize 페이지당 건수
	 * @param sort 정렬 조건
	 * @return 보고싶어요 콘텐츠 목록과 페이징 정보
	 */
	@GetMapping("/users/{memberId}/watchlist")
	public ResponseEntity<Map<String, Object>> retrieveWatchlist(
			@PathVariable int memberId,
			@RequestParam(name = "page", defaultValue = "1") int pageNo,
			@RequestParam(name = "size", defaultValue = "12") int pageSize,
			@RequestParam(defaultValue = "latest") String sort) {

		DTO param = createPagingParam(pageNo, pageSize);
		List<MemberContentVO> items =
				memberContentService.retrieveWatchlist(memberId, param, sort);

		return ResponseEntity.ok(createListResponse(items, param));
	}

	/**
	 * 로그인 회원의 콘텐츠 별점 등록 또는 변경
	 *
	 * @param memberId 임시 로그인 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @param request 별점 요청값
	 * @return 저장된 회원 콘텐츠 기록
	 */
	@PutMapping("/movies/{contentId}/rating")
	public ResponseEntity<MemberContentVO> saveRating(
			@RequestHeader(TEMP_MEMBER_ID_HEADER) int memberId,
			@PathVariable int contentId,
			@RequestBody RatingRequest request) {

		MemberContentVO saved = memberContentService.saveRating(
				memberId,
				contentId,
				request.getRatingScore());

		return ResponseEntity.ok(saved);
	}

	/**
	 * 로그인 회원의 콘텐츠 별점 해제
	 *
	 * @param memberId 임시 로그인 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @return 본문이 없는 응답
	 */
	@DeleteMapping("/movies/{contentId}/rating")
	public ResponseEntity<Void> deleteRating(
			@RequestHeader(TEMP_MEMBER_ID_HEADER) int memberId,
			@PathVariable int contentId) {

		memberContentService.deleteRating(memberId, contentId);

		return ResponseEntity.noContent().build();
	}

	/**
	 * 로그인 회원의 보고싶어요 등록
	 *
	 * @param memberId 임시 로그인 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @return 저장된 회원 콘텐츠 기록
	 */
	@PostMapping("/watchlist/{contentId}")
	public ResponseEntity<MemberContentVO> addWatchlist(
			@RequestHeader(TEMP_MEMBER_ID_HEADER) int memberId,
			@PathVariable int contentId) {

		MemberContentVO saved =
				memberContentService.addWatchlist(memberId, contentId);

		return ResponseEntity.ok(saved);
	}

	/**
	 * 로그인 회원의 보고싶어요 해제
	 *
	 * @param memberId 임시 로그인 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @return 본문이 없는 응답
	 */
	@DeleteMapping("/watchlist/{contentId}")
	public ResponseEntity<Void> deleteWatchlist(
			@RequestHeader(TEMP_MEMBER_ID_HEADER) int memberId,
			@PathVariable int contentId) {

		memberContentService.deleteWatchlist(memberId, contentId);

		return ResponseEntity.noContent().build();
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
			List<MemberContentVO> items,
			DTO param) {

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return response;
	}

	/**
	 * 잘못된 회원 번호, 콘텐츠 번호, 별점 및 조회 조건을 HTTP 400으로 변환
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(
			IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"회원 콘텐츠 요청값을 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(message);
	}

	/**
	 * 존재하지 않는 회원 또는 콘텐츠 등의 데이터 무결성 예외를 HTTP 400으로 변환
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<MessageVO> handleDataIntegrityViolation(
			DataIntegrityViolationException exception) {

		MessageVO message = new MessageVO(
				"400",
				"존재하는 회원과 콘텐츠 번호를 입력해 주세요.",
				"회원 콘텐츠 데이터의 참조 관계를 확인해 주세요.");

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
				"회원 콘텐츠의 현재 상태를 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(message);
	}
}