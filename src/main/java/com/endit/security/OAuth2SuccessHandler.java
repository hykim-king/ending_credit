package com.endit.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 구글 로그인이 '성공'으로 끝났을 때 부르는 핸들러.
 * 기존 소셜 회원이 정상 로그인된 경우이므로 홈으로 보낸다.
 * (returnUrl 복귀 기능은 나중에 얹을 수 있다.)
 */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		log.debug("OAuth2 로그인 성공 principal={}", authentication.getName());

		response.sendRedirect("/");
	}
}