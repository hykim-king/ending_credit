/**
 * 화면(뷰) 예외 처리
 * 화면 요청 중 예외가 나면 message/errorTrace를 담아 오류 화면으로 보낸다.
 * 업무 예외 → error/business_error, 그 외 → error/error (학원 19장 이원화).
 * ※ @Order(2): AJAX용 advice(@Order(1))가 우선한다.
 * ※ assignableTypes로 4조 화면 컨트롤러만 대상으로 한정한다 —
 * 전역으로 두면 타 조 화면의 오류까지 우리 오류 화면으로 덮어써서
 * 담당 조가 자기 예외를 보지 못한다.
 */
package com.endit.cmn.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.endit.controller.CommentController;

@Order(2)
@ControllerAdvice(assignableTypes = { CommentController.class })
public class GlobalViewExceptionHandler {

	final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 업무 예외(신고 없음) — 업무 오류 화면
	 *
	 * @param e
	 * @param model
	 * @return error/business_error
	 */
	@ExceptionHandler(ReportNotFoundException.class)
	public String handlerReportNotFoundException(ReportNotFoundException e, Model model) {
		log.debug("=============================");
		log.debug("handlerReportNotFoundException: {}", e.getMessage());
		log.debug("=============================");

		model.addAttribute("message", e.getMessage());
		model.addAttribute("errorTrace", ExceptionUtils.getStackTrace(e));

		return "error/business_error";
	}

	/**
	 * 그 외 모든 예외 — 시스템 오류 화면
	 *
	 * @param e
	 * @param model
	 * @return error/error
	 */
	@ExceptionHandler(Exception.class)
	public String handlerException(Exception e, Model model) {
		log.debug("=============================");
		log.debug("handlerException: {}", e.getMessage());
		log.debug("=============================");

		model.addAttribute("message", e.getMessage());
		model.addAttribute("errorTrace", ExceptionUtils.getStackTrace(e));

		return "error/error";
	}

}
