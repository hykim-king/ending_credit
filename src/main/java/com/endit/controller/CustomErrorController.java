package com.endit.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 전역 오류 화면 컨트롤러 (김건우)
 * 컨트롤러 밖에서 처리되지 못한 오류(404, 매핑 없는 URL 등)가 서블릿 /error 로
 * 넘어올 때 스프링 기본 Whitelabel 대신 우리 오류 화면(error/error)을 보여준다.
 */
@Controller
public class CustomErrorController implements ErrorController {

	@RequestMapping("/error")
	public String errorPage() {
		return "error/error";
	}
}
