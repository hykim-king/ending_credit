package com.endit.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 구글 인증이 '실패'로 끝났을 때 시큐리티가 부르는 핸들러.
 * CustomOAuth2UserService가 던진 예외 메시지를 보고 화면을 갈라 보낸다.
 *   - SIGNUP_REQUIRED   -> 닉네임 입력 화면 (신규 가입)
 *   - EMAIL_DUPLICATED   -> 이미 사용 중인 이메일 안내
 *   - 그 외               -> 로그인 화면에 일반 오류
 */
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {

		String reason = exception.getMessage();
		log.debug("OAuth2 실패 처리 reason={}", reason);

		if ("SIGNUP_REQUIRED".equals(reason)) {
			// 신규 회원: 구글 정보는 세션에 저장돼 있으니 닉네임 입력 화면으로
			response.sendRedirect("/signup/oauth");
			return;
		}

		if ("EMAIL_DUPLICATED".equals(reason)) {
			// 이미 자체가입된 이메일: 안내 화면(또는 로그인 화면에 안내)
			response.sendRedirect("/signup/oauth/email-in-use");
			return;
		}

		// 그 외(구글 취소, 토큰 오류 등) -> 로그인 화면 일반 오류
		response.sendRedirect("/login?error=oauth");
	}
}