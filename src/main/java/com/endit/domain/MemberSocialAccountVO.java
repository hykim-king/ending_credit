package com.endit.domain;

public class MemberSocialAccountVO {

	private Long memberSocialAccountId;
	private Long memberId;
	private String providerCode;
	private String providerUserId;
	private String providerEmail;
	private String connectedDt;
	
	
	public MemberSocialAccountVO() {
		super();
	}


	public Long getMemberSocialAccountId() {
		return memberSocialAccountId;
	}


	public void setMemberSocialAccountId(Long memberSocialAccountId) {
		this.memberSocialAccountId = memberSocialAccountId;
	}


	public Long getMemberId() {
		return memberId;
	}


	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}


	public String getProviderCode() {
		return providerCode;
	}


	public void setProviderCode(String providerCode) {
		this.providerCode = providerCode;
	}


	public String getProviderUserId() {
		return providerUserId;
	}


	public void setProviderUserId(String providerUserId) {
		this.providerUserId = providerUserId;
	}


	public String getProviderEmail() {
		return providerEmail;
	}


	public void setProviderEmail(String providerEmail) {
		this.providerEmail = providerEmail;
	}


	public String getConnectedDt() {
		return connectedDt;
	}


	public void setConnectedDt(String connectedDt) {
		this.connectedDt = connectedDt;
	}


	@Override
	public String toString() {
		return "MemberSocialAccount [memberSocialAccountId=" + memberSocialAccountId + ", memberId=" + memberId
				+ ", providerCode=" + providerCode + ", providerUserId=" + providerUserId + ", providerEmail="
				+ providerEmail + ", connectedDt=" + connectedDt + "]";
	}
	
	
}
