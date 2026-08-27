package com.endit.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.endit.cmn.LoginMember;
import com.endit.domain.MemberVO;

public class CustomUserDetails implements UserDetails {

	private static final long serialVersionUID = 1L;

	/** 꺼내 쓸 깨끗한 로그인 정보 (비밀번호는 안 담김) */
	private final LoginMember loginMember;

	/** 로그인 검증용 비밀번호 해시. 시큐리티가 getPassword()로 꺼내 사용자가 입력한 값과 비교한다. */
	private final String password;

	/** DB에서 조회한 MemberVO 하나로 만들어진다. */
	public CustomUserDetails(MemberVO member) {
		this.loginMember = new LoginMember(
				member.getMemberId(),
				member.getEmail(),
				member.getNickname(),
				member.getRole(),
				member.getProfileImgUrl()
		);
		this.password = member.getPassword();
	}

	/** 로그인한 회원의 깨끗한 정보 객체를 돌려준다. */
	public LoginMember getLoginMember() {
		return loginMember;
	}

	// ─────────────────────────────────────────────
	//  아래는 스프링 시큐리티가 요구하는 규격 메서드들
	// ─────────────────────────────────────────────

	/**
	 * 이 회원의 권한 목록.
	 * DB의 role("USER"/"ADMIN")에 "ROLE_" 접두어를 붙여 시큐리티 권한으로 변환한다.
	 * (시큐리티는 권한을 비교할 때 관례적으로 "ROLE_" 접두어를 쓴다)
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		String role = (loginMember.getRole() == null) ? "USER" : loginMember.getRole();
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}

	/** 저장된 비밀번호 해시. 시큐리티가 입력 비번을 해시해서 이 값과 비교한다. */
	@Override
	public String getPassword() {
		return password;
	}

	/** 로그인 아이디 역할. 우리 서비스는 이메일을 아이디로 쓴다. */
	@Override
	public String getUsername() {
		return loginMember.getEmail();
	}

	// ─── 계정 상태 플래그 (지금은 전부 true) ───
	// ※ 나중에 MEMBER.STATUS(ACTIVE/SUSPENDED)를 도입하면,
	//    isEnabled() 를 (status == "ACTIVE") 로 바꿔 정지 회원의 로그인을 막을 수 있다.
	//    그때 CustomUserDetails 생성자에서 status 값도 함께 받으면 된다.

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}