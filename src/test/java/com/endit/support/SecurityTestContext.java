package com.endit.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.endit.domain.MemberVO;
import com.endit.security.CustomUserDetails;

/**
 * Controller 통합 테스트에 실제 애플리케이션과 같은 로그인 principal을 구성하는 지원 클래스.
 *
 * <p>{@code LoginMemberHelper}는 요청 헤더나 설정값이 아니라 SecurityContext의
 * {@link CustomUserDetails}에서 회원 번호를 읽는다. 따라서 테스트도 임시
 * {@code X-Member-Id} 헤더나 삭제된 개발 설정값을 흉내 내지 않고, 운영 코드와 동일한
 * 인증 객체를 사용해야 인증 방식 변경을 함께 검증할 수 있다.</p>
 */
public final class SecurityTestContext {

	private SecurityTestContext() {
		// static 테스트 지원 메서드만 제공하므로 인스턴스 생성을 막는다.
	}

	/**
	 * 주어진 회원을 현재 테스트 실행 스레드의 로그인 회원으로 등록한다.
	 *
	 * @param member PK와 로그인 정보가 채워진 테스트 회원
	 */
	public static void login(MemberVO member) {
		CustomUserDetails principal = new CustomUserDetails(member);
		UsernamePasswordAuthenticationToken authentication =
				UsernamePasswordAuthenticationToken.authenticated(
						principal,
						principal.getPassword(),
						principal.getAuthorities());

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	/**
	 * 테스트가 끝난 뒤 ThreadLocal 인증 정보가 다음 테스트로 새지 않도록 제거한다.
	 */
	public static void clear() {
		SecurityContextHolder.clearContext();
	}
}
