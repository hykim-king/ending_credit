package com.endit.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.endit.domain.MemberSocialAccountVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.MemberMapper;
import com.endit.mapper.MemberSocialAccountMapper;
import com.endit.service.MemberService;

/**
 * <pre>
 * Class Name : MemberServiceImpl
 * Description : MemberService 구현체. 회원 도메인의 실제 로직.
 *
 *   - 이메일 회원가입 (비번 해시 + 중복검사 + insert)
 *   - 구글 소셜 조회/가입 (MEMBER + MEMBER_SOCIAL_ACCOUNT)
 *   - 회원 관리 (조회 / 프로필 수정 / 비번 변경 / 탈퇴)
 *
 */
@Service
public class MemberServiceImpl implements MemberService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MemberMapper memberMapper;
	private final MemberSocialAccountMapper memberSocialAccountMapper;

	/** SecurityConfig에 등록한 BCryptPasswordEncoder 빈 주입 */
	private final PasswordEncoder passwordEncoder;

	public MemberServiceImpl(MemberMapper memberMapper,
			MemberSocialAccountMapper memberSocialAccountMapper,
			PasswordEncoder passwordEncoder) {
		this.memberMapper              = memberMapper;
		this.memberSocialAccountMapper = memberSocialAccountMapper;
		this.passwordEncoder           = passwordEncoder;
	}

	// ===================== 이메일 회원가입 =====================

	@Override
	public long registerEmailMember(MemberVO member) {
		log.debug("registerEmailMember(email={}, nickname={})", member.getEmail(), member.getNickname());

		// 1) 필수값
		if (member.getEmail() == null || member.getEmail().trim().isEmpty()) {
			throw new IllegalArgumentException("이메일은 필수입니다.");
		}
		if (member.getPassword() == null || member.getPassword().trim().isEmpty()) {
			throw new IllegalArgumentException("비밀번호는 필수입니다.");
		}
		if (member.getNickname() == null || member.getNickname().trim().isEmpty()) {
			throw new IllegalArgumentException("닉네임은 필수입니다.");
		}

		// 2) 중복 검사
		if (isEmailUsed(member.getEmail())) {
			throw new IllegalStateException("EMAIL_DUPLICATED");
		}
		if (isNicknameUsed(member.getNickname())) {
			throw new IllegalStateException("NICKNAME_DUPLICATED");
		}

		// 3) 비밀번호 해시
		member.setPassword(passwordEncoder.encode(member.getPassword()));

		// 4) 권한 기본값
		if (member.getRole() == null || member.getRole().trim().isEmpty()) {
			member.setRole("USER");
		}

		// 5) 저장 (insertMember 안에서 memberId 채번)
		memberMapper.insertMember(member);
		log.debug("이메일 가입 완료 memberId={}", member.getMemberId());
		return member.getMemberId();
	}

	@Override
	public boolean isEmailUsed(String email) {
		return memberMapper.selectMemberByEmail(email) != null;
	}

	@Override
	public boolean isNicknameUsed(String nickname) {
		return memberMapper.countByNickname(nickname) > 0;
	}

	// ===================== 구글 소셜 =====================

	@Override
	public MemberVO findBySocial(String providerCode, String providerUserId) {
		log.debug("findBySocial({}, {})", providerCode, providerUserId);

		// 1) 소셜 계정 테이블에서 (provider, sub)로 조회
		MemberSocialAccountVO social = memberSocialAccountMapper.selectByProvider(providerCode, providerUserId);
		if (social == null) {
			return null; // 연결된 계정 없음 = 신규 소셜 사용자
		}
		// 2) 연결된 회원 반환
		return memberMapper.selectMemberById(social.getMemberId());
	}

	@Override
	@Transactional // ※ 아래 주석 참고: autoCommit=true 환경에선 롤백이 안 먹어 수동 보상까지 넣음
	public long registerSocialMember(MemberVO member, String providerCode, String providerUserId, String providerEmail) {
		log.debug("registerSocialMember(email={}, nickname={}, provider={})",
				member.getEmail(), member.getNickname(), providerCode);

		// 1) 필수값 (소셜은 비번이 없으므로 이메일/닉네임만 확인)
		if (!StringUtils.hasText(member.getEmail())) {
			throw new IllegalArgumentException("이메일은 필수입니다.");
		}
		if (!StringUtils.hasText(member.getNickname())) {
			throw new IllegalArgumentException("닉네임은 필수입니다.");
		}

		// 2) 중복 검사 (MEMBER.EMAIL은 NOT NULL이라, 이미 쓰는 이메일이면 가입 불가 → 자동 병합 금지 정책)
		if (isEmailUsed(member.getEmail())) {
			throw new IllegalStateException("EMAIL_DUPLICATED");
		}
		if (isNicknameUsed(member.getNickname())) {
			throw new IllegalStateException("NICKNAME_DUPLICATED");
		}

		// 3) 소셜 회원은 비밀번호 없음
		member.setPassword(null);
		if (!StringUtils.hasText(member.getRole())) {
			member.setRole("USER");
		}

		// 4) MEMBER insert (memberId 채번)
		memberMapper.insertMember(member);
		long memberId = member.getMemberId();

		// 5) MEMBER_SOCIAL_ACCOUNT insert
		try {
			MemberSocialAccountVO social = new MemberSocialAccountVO();
			social.setMemberId(memberId);
			social.setProviderCode(providerCode);
			social.setProviderUserId(providerUserId);
			social.setProviderEmail(providerEmail);
			memberSocialAccountMapper.insertSocialAccount(social);
		} catch (RuntimeException e) {
			// 팀 공용 DataSourceConfig가 autoCommit=true라 @Transactional 롤백이 안 먹는다.
			// 그래서 소셜 계정 저장이 실패하면, 방금 만든 MEMBER를 직접 지워 '고아 회원'을 막는다.
			log.error("소셜 계정 저장 실패 → 방금 만든 MEMBER 수동 삭제 memberId={}", memberId, e);
			memberMapper.deleteMember(memberId);
			throw e;
		}

		log.debug("소셜 가입 완료 memberId={}", memberId);
		return memberId;
	}

	// ===================== 회원 관리 =====================

	@Override
	public MemberVO getMember(long memberId) {
		// 주의: 결과에 PASSWORD 해시가 포함되므로 화면(뷰)으로 그대로 넘기지 말 것.
		//       로그인 방식(Email/Google) 판별은 password가 null인지로 구분 가능(소셜=null).
		return memberMapper.selectMemberById(memberId);
	}

	@Override
	public void updateProfile(MemberVO member) {
		log.debug("updateProfile(memberId={}, nickname={})", member.getMemberId(), member.getNickname());

		MemberVO current = memberMapper.selectMemberById(member.getMemberId());
		if (current == null) {
			throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
		}
		if (!StringUtils.hasText(member.getNickname())) {
			throw new IllegalArgumentException("닉네임은 필수입니다.");
		}

		// 닉네임을 '바꿀 때만' 중복 검사 (기존과 같으면 자기 자신이므로 검사하면 오탐)
		if (!member.getNickname().equals(current.getNickname()) && isNicknameUsed(member.getNickname())) {
			throw new IllegalStateException("NICKNAME_DUPLICATED");
		}

		memberMapper.updateProfile(member);
	}

	@Override
	public void changePassword(long memberId, String currentPassword, String newPassword) {
		log.debug("changePassword(memberId={})", memberId);

		MemberVO member = memberMapper.selectMemberById(memberId);
		if (member == null) {
			throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
		}
		// 소셜 전용 회원(비번 없음)은 비밀번호 변경 불가 (T-03: 비번 영역 숨김)
		if (member.getPassword() == null) {
			throw new IllegalStateException("SOCIAL_ONLY_NO_PASSWORD");
		}
		// 현재 비밀번호 확인
		if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
			throw new IllegalStateException("PASSWORD_MISMATCH");
		}

		// 새 비밀번호 해시 후 변경 (updatePassword는 @Param(memberId, password) 시그니처)
		String newHashed = passwordEncoder.encode(newPassword);
		memberMapper.updatePassword(memberId, newHashed);
	}

	@Override
	public void withdraw(long memberId, String confirmNickname) {
		log.debug("withdraw(memberId={})", memberId);

		MemberVO member = memberMapper.selectMemberById(memberId);
		if (member == null) {
			throw new IllegalStateException("이미 없는 회원입니다.");
		}
		// 확인용 닉네임 일치 검사 (T-08)
		if (confirmNickname == null || !confirmNickname.trim().equals(member.getNickname())) {
			throw new IllegalStateException("NICKNAME_CONFIRM_MISMATCH");
		}

		// 하드 삭제. 자식 데이터(소셜계정/코멘트/컬렉션 등)는 FK ON DELETE CASCADE로 정리되어야 함.
		memberMapper.deleteMember(memberId);
	}
}