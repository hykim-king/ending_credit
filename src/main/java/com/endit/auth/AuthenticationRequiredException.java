package com.endit.auth;

/**
 * <pre>
 * Class Name  : AuthenticationRequiredException
 * Description : 현재 요청에서 인증된 회원을 확인할 수 없을 때 발생하는 예외
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    인증 필요 예외 최초 생성
 * ------------------------------------------------------------
 * </pre>
 */
public class AuthenticationRequiredException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AuthenticationRequiredException(String message) {
		super(message);
	}
}
