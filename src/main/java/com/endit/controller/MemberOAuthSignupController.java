package com.endit.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.domain.MemberVO;
import com.endit.security.CustomOAuth2User;
import com.endit.security.OAuth2SignupSession;
import com.endit.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 구글 신규 회원의 닉네임 입력 화면과 가입 처리.
 *
 * 흐름:
 *   1) 구글 인증 후 CustomOAuth2UserService가 신규로 판단 -> 구글 정보를 세션에 저장하고 여기로 보냄
 *   2) GET /signup/oauth  : 세션의 구글 정보 확인 후 닉네임 입력 화면 표시
 *   3) POST /signup/oauth : 세션 정보 + 입력 닉네임으로 가입 -> 자동 로그인 -> 홈
 */
@Controller
public class MemberOAuthSignupController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MemberService memberService;

	public MemberOAuthSignupController(MemberService memberService) {
		this.memberService = memberService;
	}

	/**
	 * 닉네임 입력 화면.
	 * 세션에 구글 정보가 없으면 정상 진입이 아니므로 로그인 화면으로 보낸다.
	 */
	@GetMapping("/signup/oauth")
	public String signupForm(HttpSession session, Model model) {

		OAuth2SignupSession signup =
				(OAuth2SignupSession) session.getAttribute(OAuth2SignupSession.SESSION_KEY);

		if (signup == null) {
			// 구글 인증을 거치지 않고 URL로 직접 들어온 경우
			return "redirect:/login";
		}

		// 구글 이메일과 이름(닉네임 기본값 후보)을 화면에 전달
		model.addAttribute("email", signup.getEmail());
		model.addAttribute("nameCandidate", signup.getNameCandidate());

		return "member/signupOauth";
	}

	/**
	 * 닉네임 제출 -> 가입 -> 자동 로그인.
	 */
	@PostMapping("/signup/oauth")
	public String signup(
			@RequestParam String nickname,
			HttpServletRequest request,
			HttpSession session,
			Model model) {

		OAuth2SignupSession signup =
				(OAuth2SignupSession) session.getAttribute(OAuth2SignupSession.SESSION_KEY);

		if (signup == null) {
			return "redirect:/login";
		}

		// 가입에 쓸 회원 정보 조립 (이메일은 세션의 구글 이메일, 닉네임은 입력값)
		MemberVO member = new MemberVO();
		member.setEmail(signup.getEmail());
		member.setNickname(nickname);

		try {
			// MEMBER + MEMBER_SOCIAL_ACCOUNT 저장 (비번 null은 서비스가 처리)
			memberService.registerSocialMember(
					member,
					signup.getProviderCode(),
					signup.getProviderUserId(),
					signup.getEmail());
		} catch (IllegalStateException e) {
			
			String code = e.getMessage();
			
			if ("EMAIL_DUPLICATED".equals(code)) {
			    // 닉네임 입력 사이에 그 이메일로 자체가입이 생긴 드문 경우 -> 안내 페이지로
			    session.removeAttribute(OAuth2SignupSession.SESSION_KEY);
			    session.setAttribute(OAuth2SignupSession.BLOCKED_EMAIL_KEY, signup.getEmail());
			    return "redirect:/signup/oauth/email-in-use";
			}
			
			// 닉네임 중복 등 -> 다시 입력 화면
			model.addAttribute("email", signup.getEmail());
			model.addAttribute("errorMessage", toMessage(e.getMessage()));
			return "member/signupOauth";
		}

		// 가입 완료 -> 임시 세션 정보 제거
		session.removeAttribute(OAuth2SignupSession.SESSION_KEY);

		// 방금 가입한 회원으로 조회 후 자동 로그인 처리
		MemberVO saved = memberService.findBySocial(signup.getProviderCode(), signup.getProviderUserId());
		autoLogin(saved, request);

		return "redirect:/";
	}

	/**
	 * 이메일 중복으로 구글 가입이 막힌 경우의 안내 화면.
	 * 세션에 담긴 이메일을 보여주고, 한 번 쓴 뒤 지운다.
	 */
	@GetMapping("/signup/oauth/email-in-use")
	public String emailInUse(HttpSession session, Model model) {

	    String blockedEmail = (String) session.getAttribute(OAuth2SignupSession.BLOCKED_EMAIL_KEY);

	    if (blockedEmail == null) {
	        // 정상 경로가 아니면(직접 URL 접근 등) 로그인으로
	        return "redirect:/login";
	    }

	    model.addAttribute("email", blockedEmail);
	    session.removeAttribute(OAuth2SignupSession.BLOCKED_EMAIL_KEY); // 한 번 보여주고 정리

	    return "member/oauthEmailInUse";
	}

	/**
	 * 방금 가입한 소셜 회원을 시큐리티 인증 상태로 만들어 세션에 넣는다(자동 로그인).
	 * 구글 인증을 이미 통과했으므로 별도 로그인 없이 바로 로그인 상태로 만든다.
	 */
	private void autoLogin(MemberVO member, HttpServletRequest request) {

		// principal: 우리가 만든 구글용 그릇. attributes에는 최소 식별자(sub)만 넣는다.
		OAuth2User principal = new CustomOAuth2User(
				member,
				java.util.Map.of("sub", member.getMemberId())); // 최소 정보

		String role = (member.getRole() == null) ? "USER" : member.getRole();

		// OAuth2 로그인 인증 토큰 생성
		org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken authentication =
				new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
						principal,
						List.of(new SimpleGrantedAuthority("ROLE_" + role)),
						"google"); // registrationId

		// SecurityContext에 넣고 세션에 저장 -> 이후 요청부터 로그인 상태 유지
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		request.getSession().setAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

		log.debug("소셜 신규 가입 후 자동 로그인 memberId={}", member.getMemberId());
	}

	/** 서비스가 던진 코드 문자열을 화면 문구로 변환 */
	private String toMessage(String code) {
		if ("NICKNAME_DUPLICATED".equals(code)) {
			return "이미 사용 중인 닉네임입니다.";
		}
		if ("EMAIL_DUPLICATED".equals(code)) {
			return "이미 사용 중인 이메일입니다.";
		}
		return "가입에 실패했습니다. 다시 시도해 주세요.";
	}
}