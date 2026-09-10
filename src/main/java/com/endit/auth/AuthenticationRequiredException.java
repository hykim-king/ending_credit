package com.endit.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

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
 * 2026. 9. 05. eunhu       @ResponseStatus 추가 - 전용 핸들러가 없는 컨트롤러에서도 401이 된다
 * ------------------------------------------------------------
 * </pre>
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationRequiredException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AuthenticationRequiredException(String message) {
		super(message);
	}
}
