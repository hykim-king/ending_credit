/**
 * <pre>
 * Class Name : GlobalViewExceptionHandler
 * Description : 화면(뷰) 예외 처리
 *               화면 요청 중 예외가 나면 message/errorTrace를 담아 오류 화면으로 보낸다.
 *               업무 예외 → error/business_error, 그 외 → error/error (학원 19장 이원화).
 *               ※ @Order(2): 현 단계는 AJAX용 advice(@Order(1))가 우선한다 —
 *                 화면 단계에서 화면/AJAX 분기 재설계 예정(작업메모 참조).
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 18.  홍선기   최초 생성
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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Order(2)
@ControllerAdvice
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
