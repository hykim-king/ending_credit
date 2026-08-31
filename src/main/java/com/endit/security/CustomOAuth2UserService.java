package com.endit.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.endit.domain.MemberVO;
import com.endit.service.MemberService;

import jakarta.servlet.http.HttpSession;

/**
 * 구글 인증이 끝나면 시큐리티가 이 서비스를 호출한다.
 * 구글이 준 정보(sub/email/name)로 세 갈래를 처리한다.
 *   1) (GOOGLE, sub)로 기존 소셜 회원이면        -> 그대로 로그인
 *   2) 신규인데 이메일이 이미 자체가입돼 있으면    -> 예외("EMAIL_DUPLICATED")로 막음
 *   3) 신규이고 이메일도 안 겹치면               -> 구글 정보를 세션에 저장하고 예외("SIGNUP_REQUIRED")
 *
 * 2)3)의 예외는 OAuth2FailureHandler가 받아 각 화면으로 보낸다.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MemberService memberService;

	public CustomOAuth2UserService(MemberService memberService) {
		this.memberService = memberService;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		// 1) 구글과 통신해 사용자 정보를 받아온다.
		OAuth2User oAuth2User = super.loadUser(userRequest);

		// 2) 제공자 코드 (yaml registration 이름 "google" -> "GOOGLE")
		String providerCode = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

		// 3) 구글이 준 정보 꺼내기
		String sub   = oAuth2User.getAttribute("sub");
		String email = oAuth2User.getAttribute("email");
		String name  = oAuth2User.getAttribute("name");

		log.debug("loadUser(provider={}, sub={}, email={}, name={})", providerCode, sub, email, name);

		// 4) 기존 소셜 회원인지 확인
		MemberVO member = memberService.findBySocial(providerCode, sub);
		if (member != null) {
			
			// 기존 회원 -> 그대로 로그인
			log.debug("기존 소셜 회원 로그인 memberId={}", member.getMemberId());
			return new CustomOAuth2User(member, oAuth2User.getAttributes());
		}

		// 신규인데 이메일이 이미 자체가입으로 쓰이는 경우 -> 가입 막기
		if (memberService.isEmailUsed(email)) {
		    log.debug("소셜 가입 차단: 이미 사용 중인 이메일 email={}", email);
		    // 안내 페이지에서 보여줄 이메일만 세션에 담는다 (구글 정보 전체는 저장 안 함)
		    getSession().setAttribute(OAuth2SignupSession.BLOCKED_EMAIL_KEY, email);
		    throw new OAuth2AuthenticationException(new OAuth2Error("EMAIL_DUPLICATED"), "EMAIL_DUPLICATED");
		}

		// 신규 -> 구글 정보를 세션에 저장하고 닉네임 입력 화면으로 보낸다.
		OAuth2SignupSession signup = new OAuth2SignupSession(providerCode, sub, email, name);
		getSession().setAttribute(OAuth2SignupSession.SESSION_KEY, signup);

		log.debug("소셜 신규 가입 대기 -> 닉네임 입력 필요 email={}", email);
		throw new OAuth2AuthenticationException(new OAuth2Error("SIGNUP_REQUIRED"), "SIGNUP_REQUIRED");
	}

	/** 현재 요청의 HttpSession을 꺼낸다. (이 서비스는 요청 흐름 안에서 실행되므로 세션에 접근 가능) */
	private HttpSession getSession() {
		ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		return attr.getRequest().getSession(true); // 없으면 만든다 (임시 저장용이라 필요)
	}
}