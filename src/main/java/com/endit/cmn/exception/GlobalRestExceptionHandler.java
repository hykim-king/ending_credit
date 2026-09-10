/**
 * Fetch(AJAX) 예외 처리
 * 컨트롤러에서 던져진 예외를 한 곳에서 받아 MessageVO(JSON)로 응답한다.
 * id="0"(실패), message=사용자 안내, detailMessage=스택트레이스.
 * ※ 학원 원본(sb13)은 NotFound를 NO_CONTENT(204)로 응답하나 주석 의도(404)대로 교정했다
 * (204는 fetch에서 응답 본문이 무시될 수 있음).
 * ※ @Order(1): 화면용 advice와 순서를 명시해 동작을 결정적으로 고정(AJAX 우선).
 * ※ assignableTypes로 4조 컨트롤러만 대상으로 한정한다 —
 * 전역으로 두면 타 조 화면 컨트롤러의 예외까지 가로채 JSON으로 응답하려다
 * (응답이 text/html로 정해진 뒤라) 변환 실패로 백지 500이 된다.
 * 실측 2026-08-31: 2조 공지 화면(/notices) 템플릿 부재 예외를 이 advice가 가로챔.
 * ※ 401(인증 필요)·403(권한 없음) 처리는 3조 이진영 추가(2026-08-29).
 * ※ 4조: 브라우저 주소창 이동(Sec-Fetch-Dest=document)은 JSON이 아니라 오류 화면으로
 *   리다이렉트한다. 이 앱은 브라우저와 fetch가 모두 Accept 와일드카드를 보내
 *   콘텐츠 협상(produces)으로는 둘을 못 가린다(실측 2026-09-10). Sec-Fetch-Dest로 판별한다.
 */
package com.endit.cmn.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.endit.auth.AuthenticationRequiredException;
import com.endit.auth.ForbiddenOperationException;
import com.endit.cmn.MessageVO;

import jakarta.servlet.http.HttpServletRequest;
import com.endit.controller.AdminMemberController;
import com.endit.controller.CommentController;
import com.endit.controller.CommentLikeController;
import com.endit.controller.ReportCommentController;

@Order(1)
@RestControllerAdvice(assignableTypes = { CommentController.class, CommentLikeController.class,
		ReportCommentController.class, AdminMemberController.class })
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
	public ResponseEntity<?> handlerReportNotFoundException(ReportNotFoundException e,
			HttpServletRequest request) {
		log.debug("=============================");
		log.debug("handlerReportNotFoundException: {}", e.getMessage());
		log.debug("=============================");

		if (isBrowserNavigation(request)) {
			return redirectToErrorView(request, "/error-page/business", e.getMessage());
		}

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
	public ResponseEntity<?> handlerException(Exception e, HttpServletRequest request) {
		log.debug("=============================");
		log.debug("handlerException: {}", e.getMessage());
		log.debug("=============================");

		if (isBrowserNavigation(request)) {
			return redirectToErrorView(request, "/error-page/system", "서비스 처리 중 오류가 발생했습니다.");
		}

		MessageVO messageVO = new MessageVO();
		messageVO.setId("0");
		messageVO.setMessage("서비스 처리중 오류가 발생했습니다.");
		messageVO.setDetailMessage(ExceptionUtils.getStackTrace(e));

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(messageVO);
	}


	/**
	 * 브라우저 주소창 이동/링크 클릭인가(= 오류 화면이 필요한가).
	 * Sec-Fetch-Dest=document면 확실한 화면 요청. 이 헤더가 없는 구형 브라우저는
	 * Accept가 text/html을 원하고 X-Requested-With(AJAX 표시)가 없으면 화면으로 본다.
	 */
	private boolean isBrowserNavigation(HttpServletRequest request) {
		String dest = request.getHeader("Sec-Fetch-Dest");
		if (null != dest) {
			return "document".equals(dest);
		}
		String accept = request.getHeader("Accept");
		boolean wantsHtml = null != accept && accept.contains("text/html");
		boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
		return wantsHtml && false == isAjax;
	}

	/**
	 * 오류 메시지를 세션에 잠깐 담고 오류 화면(ErrorViewController)으로 302 보낸다.
	 * ResponseEntity 리다이렉트라 @RestControllerAdvice에서도 그대로 나간다.
	 */
	private ResponseEntity<Void> redirectToErrorView(HttpServletRequest request, String path,
			String message) {
		request.getSession().setAttribute("enditErrorMessage", message);
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(path)).build();
	}
}