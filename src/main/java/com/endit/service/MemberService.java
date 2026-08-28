package com.endit.service;

import com.endit.domain.MemberVO;

/**
 * <pre>
 * Class Name : MemberService
 * Description : 회원 도메인 서비스 계약. 컨트롤러/소셜서비스가 이 인터페이스에 의존한다.
 *              (실제 동작은 MemberServiceImpl)
 * </pre>
 *
 * @since 2026. 8. 27.
 */
public interface MemberService {

	// ===================== 이메일 회원가입 =====================

	/**
	 * 이메일 회원가입. 비밀번호 BCrypt 해시 + 이메일/닉네임 중복 검사 후 저장.
	 * @return 생성된 memberId
	 * @throws IllegalArgumentException 필수값 누락
	 * @throws IllegalStateException    "EMAIL_DUPLICATED" / "NICKNAME_DUPLICATED"
	 */
	long registerEmailMember(MemberVO member);

	/** 이메일 중복 여부. 이미 쓰면 true */
	boolean isEmailUsed(String email);

	/** 닉네임 중복 여부. 이미 쓰면 true */
	boolean isNicknameUsed(String nickname);

	// ===================== 구글 소셜 =====================

	/**
	 * (providerCode, providerUserId)로 연결된 회원을 찾는다.
	 * @return 연결된 MemberVO, 없으면 null(=신규 소셜 사용자)
	 */
	MemberVO findBySocial(String providerCode, String providerUserId);

	/**
	 * 소셜 신규 가입: MEMBER + MEMBER_SOCIAL_ACCOUNT 를 함께 저장한다.
	 *  - 소셜 회원은 비밀번호가 없으므로 PASSWORD는 null로 저장된다.
	 * @param member          email, nickname 이 채워진 MemberVO
	 * @param providerCode    "GOOGLE"
	 * @param providerUserId  구글 sub (변하지 않는 고유 식별자)
	 * @param providerEmail   구글이 준 이메일 (없을 수 있음)
	 * @return 생성된 memberId
	 */
	long registerSocialMember(MemberVO member, String providerCode, String providerUserId, String providerEmail);

	// ===================== 회원 관리 =====================

	/** 회원 단건 조회 (계정 정보 화면 등). 없으면 null */
	MemberVO getMember(long memberId);

	/**
	 * 프로필 수정 (닉네임/소개/프로필이미지).
	 *  - 닉네임을 '바꿀 때만' 중복 검사한다(기존과 같으면 자기 자신이라 검사 생략).
	 */
	void updateProfile(MemberVO member);

	/**
	 * 비밀번호 변경 (이메일 회원 전용).
	 *  - 현재 비밀번호가 맞아야 새 비밀번호로 교체한다.
	 * @throws IllegalStateException "SOCIAL_ONLY_NO_PASSWORD"(소셜전용) / "PASSWORD_MISMATCH"(현재비번 불일치)
	 */
	void changePassword(long memberId, String currentPassword, String newPassword);

	/**
	 * 회원 탈퇴 (하드 삭제). 확인용 닉네임이 현재 닉네임과 일치해야 진행.
	 * @throws IllegalStateException "NICKNAME_CONFIRM_MISMATCH"
	 */
	void withdraw(long memberId, String confirmNickname);
}