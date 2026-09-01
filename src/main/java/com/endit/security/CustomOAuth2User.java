package com.endit.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.endit.cmn.LoginMember;
import com.endit.domain.MemberVO;

/**
 * 구글 로그인 사용자를 담는 principal.
 * 이메일 로그인의 CustomUserDetails에 대응하는 구글 버전으로, 안에 LoginMember를 품는다.
 * 로그인 방식과 상관없이 getLoginMember()로 정보를 꺼낼 수 있다.
 */
public class CustomOAuth2User implements OAuth2User {

	/** 꺼내 쓸 깨끗한 로그인 정보 */
	private final LoginMember loginMember;

	/** 구글이 준 원본 정보(sub, email, name 등). OAuth2User 규격이 요구한다. */
	private final Map<String, Object> attributes;

	/**
	 * @param member     DB에서 조회/생성한 회원
	 * @param attributes 구글이 준 원본 사용자 정보
	 */
	public CustomOAuth2User(MemberVO member, Map<String, Object> attributes) {
		this.loginMember = new LoginMember(
				member.getMemberId(),
				member.getEmail(),
				member.getNickname(),
				member.getRole(),
				member.getProfileImgUrl()
		);
		this.attributes = attributes;
	}

	/** 로그인한 회원의 깨끗한 정보 */
	public LoginMember getLoginMember() {
		return loginMember;
	}

	// ─── OAuth2User 규격 메서드 ───

	/** 구글이 준 원본 정보 그대로 */
	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/** 권한. role("USER"/"ADMIN")에 "ROLE_" 접두어를 붙인다. */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		String role = (loginMember.getRole() == null) ? "USER" : loginMember.getRole();
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}

	/** 사용자 식별자. 구글의 고유값 sub를 쓴다. */
	@Override
	public String getName() {
		return String.valueOf(attributes.get("sub"));
	}
}