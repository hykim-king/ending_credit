package com.endit.domain;

public class MemberVO {

	private Long memberId;
	private String email;
	private String password;
	private String nickname;
	private String introduction;
	private String profileImgUrl;
	private String role;
	private String createdDt;
	private String updatedDt;
	
	public MemberVO() {
		super();
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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getIntroduction() {
		return introduction;
	}

	public void setIntroduction(String introduction) {
		this.introduction = introduction;
	}

	public String getProfileImgUrl() {
		return profileImgUrl;
	}

	public void setProfileImgUrl(String profileImgUrl) {
		this.profileImgUrl = profileImgUrl;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	public String getUpdatedDt() {
		return updatedDt;
	}

	public void setUpdatedDt(String updatedDt) {
		this.updatedDt = updatedDt;
	}

	@Override
	public String toString() {
		return "MemberVO [memberId=" + memberId + ", email=" + email + ", nickname=" + nickname
				+ ", introduction=" + introduction + ", profileImgUrl=" + profileImgUrl + ", role=" + role
				+ ", createdDt=" + createdDt + ", updatedDt=" + updatedDt + "]";
	}
	
}
