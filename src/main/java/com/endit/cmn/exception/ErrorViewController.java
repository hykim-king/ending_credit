package com.endit.cmn.exception;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 오류 화면 컨트롤러(4조)
 * REST advice가 브라우저 이동에서 예외를 만나면 여기로 302 리다이렉트한다.
 * 세션에 잠깐 담아둔 메시지를 꺼내 학원 19장 이원화 화면으로 렌더한다.
 * (Spring Boot 기본 /error 는 건드리지 않으려고 /error-page 경로를 쓴다.)
 */
@Controller
public class ErrorViewController {

	private static final String MSG_KEY = "enditErrorMessage";

	/** 업무 오류 화면 */
	@GetMapping("/error-page/business")
	public String business(HttpServletRequest request, Model model) {
		model.addAttribute("message", popMessage(request, "요청을 처리할 수 없습니다."));
		return "error/business_error";
	}

	/** 시스템 오류 화면 */
	@GetMapping("/error-page/system")
	public String system(HttpServletRequest request, Model model) {
		model.addAttribute("message", popMessage(request, "서비스 처리 중 오류가 발생했습니다."));
		return "error/error";
	}

	/** 세션에 담긴 메시지를 한 번 꺼내고 지운다(새로고침 시 재노출 방지) */
	private String popMessage(HttpServletRequest request, String fallback) {
		Object m = request.getSession().getAttribute(MSG_KEY);
		request.getSession().removeAttribute(MSG_KEY);
		return null == m ? fallback : m.toString();
	}
}
