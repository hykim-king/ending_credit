package com.endit.auth;

import java.util.OptionalLong;

/**
 * <pre>
 * Class Name  : CurrentMemberProvider
 * Description : 현재 요청의 로그인 회원을 제공하는 인증 경계
 *
 * 실제 인증 기능이 병합되면 이 인터페이스의 구현체만 교체하고,
 * Controller와 Service는 같은 계약을 계속 사용한다.
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    임시·실제 인증 교체용 Provider 계약 최초 생성
 * ------------------------------------------------------------
 * </pre>
 */
public interface CurrentMemberProvider {

	/** 현재 로그인 회원 번호를 조회한다. 비회원이면 빈 값을 반환한다. */
	OptionalLong findCurrentMemberId();

	/**
	 * 현재 로그인 회원 번호를 반환한다.
	 *
	 * @throws AuthenticationRequiredException 로그인 회원이 없을 때
	 */
	default long requireMemberId() {
		return findCurrentMemberId().orElseThrow(
				() -> new AuthenticationRequiredException("로그인이 필요한 기능입니다."));
	}
}
