package com.endit.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.endit.domain.MemberVO;
import com.endit.security.CustomUserDetails;

/**
 * Controller 통합 테스트용 로그인 인증 구성
 *
 * <p>{@code LoginMemberHelper}가 사용하는 {@link CustomUserDetails}를 현재 스레드의 SecurityContext에 설정.</p>
 */
public final class SecurityTestContext {

	/** 유틸리티 클래스의 인스턴스 생성 방지 */
	private SecurityTestContext() {
	}

	/**
	 * 주어진 회원을 현재 테스트 실행 스레드의 로그인 회원으로 등록
	 *
	 * @param member PK와 로그인 정보가 채워진 테스트 회원
	 */
	public static void login(MemberVO member) {
		CustomUserDetails principal = new CustomUserDetails(member);
		UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(principal,
						principal.getPassword(), principal.getAuthorities());

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	/** 다음 테스트에 남지 않도록 현재 스레드의 인증 정보 제거 */
	public static void clear() {
		SecurityContextHolder.clearContext();
	}
}
