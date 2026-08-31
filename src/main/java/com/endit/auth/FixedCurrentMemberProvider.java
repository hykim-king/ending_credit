package com.endit.auth;

import java.util.OptionalLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.endit.domain.MemberVO;
import com.endit.mapper.MemberMapper;

/**
 * <pre>
 * Class Name  : FixedCurrentMemberProvider
 * Description : 실제 인증 기능 병합 전 app 프로필에서 사용하는 고정 회원 Provider
 *
 * 회원 번호는 서버 설정으로만 주입하며 HTTP 요청에서는 받지 않는다.
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    설정 기반 개발 인증 및 시작 시 회원 검증 추가
 * ------------------------------------------------------------
 * </pre>
 */
@Component
@Profile("app")
public class FixedCurrentMemberProvider implements CurrentMemberProvider, InitializingBean {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MemberMapper memberMapper;
	private final long memberId;

	public FixedCurrentMemberProvider(
			MemberMapper memberMapper,
			@Value("${endit.dev-auth.member-id}") long memberId) {

		this.memberMapper = memberMapper;
		this.memberId = memberId;
	}

	/** 설정 오류를 요청 처리 시점까지 숨기지 않고 애플리케이션 시작 단계에서 확인한다. */
	@Override
	public void afterPropertiesSet() {
		if (memberId <= 0) {
			throw new IllegalStateException(
					"endit.dev-auth.member-id는 양수여야 합니다.");
		}

		MemberVO member = memberMapper.selectMemberById(memberId);

		if (member == null) {
			throw new IllegalStateException(
					"개발 인증 회원을 찾을 수 없습니다. memberId=" + memberId);
		}

		log.warn(
				"개발용 고정 회원 인증이 활성화되었습니다. memberId={}, profile=app",
				memberId);
	}

	@Override
	public OptionalLong findCurrentMemberId() {
		return OptionalLong.of(memberId);
	}
}
