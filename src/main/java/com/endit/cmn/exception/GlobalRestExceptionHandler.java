/**
 * <pre>
 * Class Name : GlobalRestExceptionHandler
 * Description : Fetch(AJAX) 예외 처리
 *               컨트롤러에서 던져진 예외를 한 곳에서 받아 MessageVO(JSON)로 응답한다.
 *               id="0"(실패), message=사용자 안내, detailMessage=스택트레이스.
 *               ※ 학원 원본(sb13)은 NotFound를 NO_CONTENT(204)로 응답하나
 *                 주석 의도(404)대로 교정했다.
 *               ※ @Order(1)로 advice 처리 순서를 명시한다.
 *               ※ assignableTypes로 지정된 REST 컨트롤러만 대상으로 한정한다.
 *                 다른 화면 컨트롤러의 예외를 가로채 JSON으로 응답하는 문제를 방지한다.
 *
 * Modification History
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 18.  홍선기   최초 생성
 * 2026. 8. 29.  이진영   인증 필요(401) 및 권한 없음(403) 응답 처리 추가
 * 2026. 8. 31.  이진영   필수 요청 헤더·파라미터 누락 시 400 응답 처리 추가
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 18.
 */
package com.endit.cmn.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.endit.auth.AuthenticationRequiredException;
import com.endit.auth.ForbiddenOperationException;
import com.endit.cmn.MessageVO;
import com.endit.controller.CommentController;
import com.endit.controller.CommentLikeController;
import com.endit.controller.ReportCommentController;

@Order(1)
@RestControllerAdvice(assignableTypes = { CommentController.class, CommentLikeController.class,
		ReportCommentController.class })
public class GlobalRestExceptionHandler {

	final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 로그인 필요 — 401
	 *
	 * @param e 인증 회원 없음 예외
	 * @return ResponseEntity<MessageVO>
	 */
	@ExceptionHandler(AuthenticationRequiredException.class)
	public ResponseEntity<MessageVO> handlerAuthenticationRequiredException(
			AuthenticationRequiredException e) {

		log.debug("handlerAuthenticationRequiredException: {}", e.getMessage());

		MessageVO messageVO = new MessageVO();
		messageVO.setId("401");
		messageVO.setMessage(e.getMessage());
		messageVO.setDetailMessage("인증된 회원 정보를 확인할 수 없습니다.");

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(messageVO);
	}

	/** 인증된 회원에게 요청 작업 권한이 없음 — 403 */
	@ExceptionHandler(ForbiddenOperationException.class)
	public ResponseEntity<MessageVO> handlerForbiddenOperationException(
			ForbiddenOperationException e) {

		log.debug("handlerForbiddenOperationException: {}", e.getMessage());

		MessageVO messageVO = new MessageVO();
		messageVO.setId("403");
		messageVO.setMessage(e.getMessage());
		messageVO.setDetailMessage("요청한 작업을 수행할 권한이 없습니다.");

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(messageVO);
	}

	/**
	 * 신고 없음 — 404
	 *
	 * @param e
	 * @return ResponseEntity<MessageVO>
	 */
	@ExceptionHandler(ReportNotFoundException.class)
	public ResponseEntity<MessageVO> handlerReportNotFoundException(ReportNotFoundException e) {
		log.debug("=============================");
		log.debug("handlerReportNotFoundException: {}", e.getMessage());
		log.debug("=============================");

		MessageVO messageVO = new MessageVO();
		messageVO.setId("0");
		messageVO.setMessage(e.getMessage());
		messageVO.setDetailMessage(ExceptionUtils.getStackTrace(e));

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(messageVO);
	}

	/**
	 * 입력값 오류(검색어 숫자 검증 등) — 400
	 *
	 * @param e
	 * @return ResponseEntity<MessageVO>
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handlerIllegalArgumentException(IllegalArgumentException e) {
		log.debug("=============================");
		log.debug("handlerIllegalArgumentException: {}", e.getMessage());
		log.debug("=============================");

		MessageVO messageVO = new MessageVO();
		messageVO.setId("0");
		messageVO.setMessage(e.getMessage());
		messageVO.setDetailMessage(ExceptionUtils.getStackTrace(e));

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(messageVO);
	}

	/**
	 * 필수 요청 헤더·파라미터 누락 — 400
	 *
	 * @param e 필수 요청값 누락 예외
	 * @return ResponseEntity<MessageVO>
	 */
	@ExceptionHandler(MissingRequestValueException.class)
	public ResponseEntity<MessageVO> handlerMissingRequestValueException(
			MissingRequestValueException e) {

		log.debug(
				"handlerMissingRequestValueException: {}",
				e.getMessage());

		MessageVO messageVO = new MessageVO();
		messageVO.setId("400");
		messageVO.setMessage("필수 요청값이 누락되었습니다.");
		messageVO.setDetailMessage(ExceptionUtils.getStackTrace(e));

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(messageVO);
	}

	/**
	 * 그 외 모든 예외 — 500 (사용자에게는 고정 메시지, 상세는 detailMessage에만)
	 *
	 * @param e
	 * @return ResponseEntity<MessageVO>
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<MessageVO> handlerException(Exception e) {
		log.debug("=============================");
		log.debug("handlerException: {}", e.getMessage());
		log.debug("=============================");

		MessageVO messageVO = new MessageVO();
		messageVO.setId("0");
		messageVO.setMessage("서비스 처리중 오류가 발생했습니다.");
		messageVO.setDetailMessage(ExceptionUtils.getStackTrace(e));

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(messageVO);
	}

}
