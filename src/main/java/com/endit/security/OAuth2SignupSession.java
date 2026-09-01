package com.endit.security;

import java.io.Serializable;

/**
 * 신규 소셜(구글) 회원이 닉네임을 입력하기 전까지, 구글에서 받은 정보를 세션에 잠깐 담아두는 객체.
 * 닉네임 제출 시 이 정보 + 닉네임을 합쳐 실제 가입(registerSocialMember)에 사용한다.
 * 세션에 담기므로 Serializable.
 */
public class OAuth2SignupSession implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 세션에 담을 때 쓰는 고정 key. 컨트롤러/핸들러가 이 이름으로 꺼낸다. */
	public static final String SESSION_KEY = "oauth2SignupSession";
	
	/** 이메일 중복으로 막힌 경우, 안내 페이지에서 보여줄 이메일을 담는 세션 key */
	public static final String BLOCKED_EMAIL_KEY = "oauthBlockedEmail";

	private final String providerCode;    // "GOOGLE"
	private final String providerUserId;  // 구글 sub
	private final String email;           // 구글 이메일
	private final String nameCandidate;   // 구글 이름 (닉네임 기본값 후보)

	public OAuth2SignupSession(String providerCode, String providerUserId, String email, String nameCandidate) {
		this.providerCode   = providerCode;
		this.providerUserId = providerUserId;
		this.email          = email;
		this.nameCandidate  = nameCandidate;
	}

	public String getProviderCode() {
		return providerCode;
	}
	public String getProviderUserId() {
		return providerUserId;
	}
	public String getEmail() {
		return email;
	}
	public String getNameCandidate() {
		return nameCandidate;
	}
}