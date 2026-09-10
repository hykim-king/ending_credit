package com.endit.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <pre>
 * Class Name  : ForbiddenOperationException
 * Description : 인증은 되었지만 요청 작업의 권한이 없을 때 발생하는 예외
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    권한 없음 예외 최초 생성
 * 2026. 9. 05. eunhu       @ResponseStatus 추가 - 전용 핸들러가 없는 컨트롤러에서도 403이 된다
 * ------------------------------------------------------------
 * </pre>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenOperationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ForbiddenOperationException(String message) {
		super(message);
	}
}
