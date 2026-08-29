package com.endit.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.endit.cmn.LoginMember;


/**
 * 로그인한 회원 정보를 꺼내는 팀 공용 유틸.
 * getLoginMember() // 현재 로그인 회원 정보
 * isLogin() // 로그인 여부
 * getMemberId() 현재 로그인 회원 번호
 */
public class LoginMemberHelper {

	/** 인스턴스로 만들 필요 없는 static 전용 유틸이라 생성자를 막는다. */
	private LoginMemberHelper() {
		
	}

	/**
	 * 현재 로그인한 회원 정보.
	 * @return 로그인 상태면 LoginMember, 비로그인이면 null
	 */
	public static LoginMember getLoginMember() {
		// 시큐리티가 보관 중인 현재 인증 정보를 꺼낸다
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		if (auth == null) {
			return null; // 인증 정보 없음 = 비로그인
		}
		
		Object principal = auth.getPrincipal();
		
		// 이메일 로그인 사용자는 principal이 CustomUserDetails 다.
		// (비로그인/익명 사용자는 principal이 "anonymousUser" 문자열이라 아래 조건에 안 걸려 null 반환)
		if(principal instanceof CustomUserDetails)
		{
			return ((CustomUserDetails) principal).getLoginMember();
		}
		
		// TODO(구글 OAuth): 구글 로그인 사용자는 principal이 CustomUserDetails가 아니라
		//   OAuth2User(커스텀)라서, 구글 파트 만들 때 여기에 그 케이스를 한 줄 추가한다.
		
		return null; // 그 외는 로그인 안 한 것으로 취급
	}
	
	/**
	 * 로그인 여부.
	 * @return 로그인 했으면 true
	 */
	public static boolean isLogin() {
		return getLoginMember() != null;
	}
	
	/**
	 * 현재 로그인한 회원 번호(memberId).
	 *  - 반드시 로그인해야만 하는 곳(코멘트 작성, 프로필 수정, 탈퇴 등)에서 편하게 쓰기 위한 것.
	 * @return memberId
	 * @throws IllegalStateException 비로그인 상태
	 */
	public static Long getMemberId() {
		LoginMember me = getLoginMember();
		
		if(me == null)
		{
			throw new IllegalStateException("로그인이 필요합니다.");
		}
		
		return me.getMemberId();
	}

}
