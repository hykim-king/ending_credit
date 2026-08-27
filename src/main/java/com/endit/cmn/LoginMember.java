package com.endit.cmn;

/**
 * <pre>
 * Class Name : LoginMember
 * Description : "지금 로그인한 사람이 누구인지"를 담아두는 정보 묶음.
 *
 *  [이게 뭐냐]
 *   - 로그인에 성공하면 이 객체가 하나 만들어진다.
 *   - 이 객체가 세션(로그인 상태를 서버가 기억하는 통) 안에 보관되어,
 *     로그아웃하기 전까지 "이 사용자는 로그인된 상태"라는 게 유지된다.
 *
 *  [누가 쓰냐]
 *   - 팀원들은 LoginMemberHelper.getLoginMember() 한 줄로 이 객체를 꺼내 쓴다.
 *     예) 코멘트 담당자 → getMemberId() 로 작성자 연결
 *         관리자 담당자 → isAdmin() 으로 관리자 여부 확인
 *         헤더 화면     → getNickname(), getProfileImgUrl() 로 화면 표시
 */
public class LoginMember {

	/** 회원 고유 번호 (MEMBER.MEMBER_ID) */
	private Long memberId;

	/** 이메일 (MEMBER.EMAIL) */
	private String email;

	/** 닉네임 (MEMBER.NICKNAME) */
	private String nickname;

	/** 권한 (MEMBER.ROLE) */
	private String role;

	/** 프로필 이미지 주소 (MEMBER.PROFILE_IMG_URL) */
	private String profileImgUrl;

	/** 기본 생성자 */
	public LoginMember() {
		super();
	}

	/** 로그인 성공 시 값들을 한 번에 채워 넣는 생성자 */
	public LoginMember(Long memberId, String email, String nickname, String role, String profileImgUrl) {
		this.memberId      = memberId;
		this.email         = email;
		this.nickname      = nickname;
		this.role          = role;
		this.profileImgUrl = profileImgUrl;
	}

	public Long getMemberId() {
		return memberId;
	}
	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}

	public String getProfileImgUrl() {
		return profileImgUrl;
	}
	public void setProfileImgUrl(String profileImgUrl) {
		this.profileImgUrl = profileImgUrl;
	}

	/**
	 * 관리자인지 편하게 확인하는 메서드.
	 * role 이 "ADMIN" 이면 true.  (예: if (loginMember.isAdmin()) { ... })
	 */
	public boolean isAdmin() {
		return "ADMIN".equals(this.role);
	}

	@Override
	public String toString() {
		return "LoginMember [memberId=" + memberId + ", email=" + email
				+ ", nickname=" + nickname + ", role=" + role
				+ ", profileImgUrl=" + profileImgUrl + "]";
	}
}