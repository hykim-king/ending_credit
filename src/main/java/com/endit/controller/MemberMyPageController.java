package com.endit.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.endit.cmn.LoginMember;
import com.endit.security.LoginMemberHelper;

/**
 * Class Name  : MemberMyPageController
 * Description : 마이페이지 및 회원 프로필 화면의 경로를 처리하는 Controller
 *
 *   - 화면(뷰 이름)만 반환한다. 실제 데이터는 화면의 JavaScript가
 *     MemberMyPageApiController(/api/members/**)를 호출해 가져온다.
 *     (팀 관례: CollectionViewController와 동일한 방식)
 *   - 그래서 Model에는 조회 키(memberId)와 화면 분기용 플래그만 담는다.
 *   - "내" 화면(/members/me/**)은 URL로 회원 번호를 받지 않는다.
 *     로그인한 사람이 누구인지는 LoginMemberHelper가 세션에서 꺼내 준다.
 */
@Controller
@RequestMapping("/members")
public class MemberMyPageController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** 비로그인 상태로 "내" 화면에 들어왔을 때 보낼 곳 */
	private static final String REDIRECT_LOGIN = "redirect:/login";

	// ===================== 내 마이페이지 =====================

	/**
	 * 내 마이페이지 화면.
	 *
	 * 비로그인이면 로그인 화면으로 보낸다.
	 * (LoginMemberHelper.getMemberId()는 비로그인 시 예외를 던지므로,
	 *  화면 컨트롤러에서는 예외 대신 isLogin()으로 먼저 확인한다)
	 *
	 * @param model 화면에서 쓸 조회 키를 담을 모델
	 * @return member/mypage
	 */
	@GetMapping("/me")
	public String myPage(Model model) {
		log.debug("myPage()");

		LoginMember me = LoginMemberHelper.getLoginMember();

		if (me == null) {
			return REDIRECT_LOGIN;
		}

		// 화면 JS가 /api/members/me/account 를 호출할 때 쓸 최소 정보만 전달한다.
		model.addAttribute("memberId", me.getMemberId());
		model.addAttribute("nickname", me.getNickname());

		return "member/mypage";
	}

	/**
	 * 프로필 수정 화면.
	 *
	 * 등록/수정 폼과 달리 대상이 항상 '나' 하나뿐이라 formMode는 두지 않는다.
	 *
	 * @param model 화면에서 쓸 조회 키를 담을 모델
	 * @return member/profile-form
	 */
	@GetMapping("/me/edit")
	public String editForm(Model model) {
		log.debug("editForm()");

		LoginMember me = LoginMemberHelper.getLoginMember();

		if (me == null) {
			return REDIRECT_LOGIN;
		}

		model.addAttribute("memberId", me.getMemberId());

		return "member/profile-form";
	}

	/**
	 * 계정 설정 화면 (비밀번호 변경 / 회원 탈퇴).
	 *
	 * @param model 화면에서 쓸 조회 키를 담을 모델
	 * @return member/settings
	 */
	@GetMapping("/me/settings")
	public String settings(Model model) {
		log.debug("settings()");

		LoginMember me = LoginMemberHelper.getLoginMember();

		if (me == null) {
			return REDIRECT_LOGIN;
		}

		model.addAttribute("memberId", me.getMemberId());

		return "member/settings";
	}

	// ===================== 다른 유저 프로필 =====================

	/**
	 * 다른 유저의 공개 프로필 화면.
	 *
	 * 회원 정보를 여기서 조회하지 않는다. 화면 JS가
	 * GET /api/members/{memberId} 로 '공개 필드만' 받아 가므로
	 * 이메일 등 민감 정보가 이 경로로 새어 나가지 않는다.
	 *
	 * 경로에 [0-9]+ 숫자 제약을 두어 /members/me 와 매핑이 겹치지 않게 한다.
	 *
	 * 로그인 본인이 자기 번호로 들어오면 /members/me 로 리다이렉트해
	 * '내 프로필'을 항상 /me 경로로 통일한다.
	 * 따라서 이 화면에는 항상 '다른 유저'만 표시된다.
	 *
	 * @param memberId 조회할 회원 번호
	 * @param model    화면에서 쓸 조회 키를 담을 모델
	 * @return member/profile
	 */
	@GetMapping("/{memberId:[0-9]+}")
	public String profile(
			@PathVariable long memberId,
			Model model) {

		log.debug("profile(memberId={})", memberId);

		LoginMember me = LoginMemberHelper.getLoginMember();

		if (me != null && me.getMemberId() != null && me.getMemberId() == memberId)
		{
			return "redirect:/members/me";
		}

		model.addAttribute("memberId", memberId);

		return "member/profile";
	}
}
