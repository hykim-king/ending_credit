package com.endit.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.domain.MemberVO;
import com.endit.service.MemberService;

/**
 * Class Name : MemberPageController
 * Description : 회원가입 / 로그인 화면과 회원가입 폼 제출을 처리하는 Controller.
 *
 *   - 화면(뷰 이름)만 반환한다. JSON 응답은 MemberApiController가 담당한다.
 *   - 로그인 처리(POST /login/email)는 SecurityConfig의 formLogin이 가로챈다
 *   - 경로가 /signup, /login 으로 갈라져 공통 prefix가 없음
 *
 */
@Controller
public class MemberPageController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** MemberServiceImpl 이 주입된다. */
	private final MemberService memberService;

	public MemberPageController(MemberService memberService) {
		this.memberService = memberService;
	}

	/**
	 * 폼 값을 MemberVO에 담을 때, 사용자가 건드리면 안 되는 필드를 막는다.
	 *
	 *   @ModelAttribute는 "요청에 들어온 파라미터 이름"과 같은 VO 필드를 전부 채운다.
	 *   화면 입력칸에 없는 값이라도, 요청에 직접 끼워 넣으면 그대로 들어간다.
	 *
	 *   예) POST /signup/email 에 role=ADMIN 을 몰래 붙여 보내면
	 *       → member.role = "ADMIN" 으로 채워지고
	 *       → MemberServiceImpl은 role이 비어 있을 때만 "USER"를 넣으므로 그대로 통과
	 *       → 누구나 관리자로 가입 = SecurityConfig의 /admin/** 방어가 뚫린다.
	 *
	 *   그래서 role을 아예 '바인딩 금지' 목록에 넣는다.
	 *   금지된 필드는 예외 없이 조용히 무시되어 null로 남고, 서비스가 "USER"로 채운다.
	 *
	 * memberId / createdDt / updatedDt 는 DB가 채우는 값이라 함께 막는다.
	 * profileImgUrl 은 가입 화면에서 받지 않으므로 막아 둔다.
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setDisallowedFields("role", "memberId", "createdDt", "updatedDt", "profileImgUrl");
	}

	// ===================== 회원가입 =====================

	/**
	 * 회원가입 화면.
	 *
	 * @param member 화면에서 사용할 빈 입력값 객체 (등록 실패 후 재진입 시 값 유지에도 쓰인다)
	 * @return member/signup
	 */
	@GetMapping("/signup")
	public String signupForm(@ModelAttribute("member") MemberVO member) {
		log.debug("signupForm()");

		return "member/signup";
	}

	/**
	 * 이메일 회원가입 처리.
	 *
	 *   - 성공: 로그인 화면으로 리다이렉트 (자동 로그인은 하지 않는다)
	 *   - 실패: 에러 메시지와 입력값을 담아 회원가입 화면을 다시 보여준다
	 *
	 * ControllerAdvice / 커스텀 예외를 아직 쓰지 않으므로 여기서 try-catch 로 처리한다.
	 *
	 * @param member 폼에서 넘어온 이메일 / 비밀번호 / 닉네임 / 소개
	 * @param model  실패 시 에러 메시지를 담을 모델
	 * @return 성공이면 redirect:/login, 실패면 member/signup
	 */
	@PostMapping("/signup/email")
	public String signupEmail(
			@ModelAttribute("member") MemberVO member,
			Model model) {

		log.debug("signupEmail(email={}, nickname={})", member.getEmail(), member.getNickname());

		try {
			long memberId = memberService.registerEmailMember(member);

			log.debug("회원가입 성공 memberId={}", memberId);

			// 가입만 시키고 로그인은 사용자가 직접 하도록 로그인 화면으로 보낸다.
			return "redirect:/login";

		} catch (IllegalStateException e) {
			// 서비스가 "EMAIL_DUPLICATED" / "NICKNAME_DUPLICATED" 코드 문자열을 던지므로
			// 화면에 보여줄 한국어 문구로 바꿔 준다.
			model.addAttribute("errorMessage", toDuplicatedMessage(e.getMessage()));

		} catch (IllegalArgumentException e) {
			// 필수값 누락. 서비스 메시지가 이미 한국어라 그대로 사용한다.
			model.addAttribute("errorMessage", e.getMessage());
		}

		log.debug("회원가입 실패 email={}", member.getEmail());

		// 입력값을 유지하되 비밀번호는 돌려주지 않는다.
		// registerEmailMember() 가 파라미터 객체의 password 를 BCrypt 해시로 덮어쓰기 때문에
		// 그대로 두면 화면 입력칸에 해시값이 찍힌다.
		member.setPassword(null);

		return "member/signup";
	}

	// ===================== 로그인 =====================

	/**
	 * 로그인 화면.
	 *
	 * SecurityConfig 가 loginPage / failureUrl 을 "/login/email" 로 잡고 있어
	 * 시큐리티는 로그인이 필요할 때와 실패했을 때 "/login/email" 로 리다이렉트한다.
	 * 두 경로를 함께 매핑해 어느 쪽으로 들어와도 같은 화면이 나오게 한다.
	 * (POST /login/email 은 시큐리티 필터가 처리하므로 여기서는 GET 만 받는다)
	 *
	 * @param error 로그인 실패로 되돌아온 경우에만 값이 있다 (?error)
	 * @param model 실패 메시지를 담을 모델
	 * @return member/login
	 */
	@GetMapping({ "/login", "/login/email" })
	public String loginForm(
			@RequestParam(required = false) String error,
			Model model) {

		log.debug("loginForm(error={})", error);

		if (error != null) {
			// 이메일이 없는 것인지 비밀번호가 틀린 것인지는 보안상 구분해서 알리지 않는다.
			model.addAttribute("errorMessage", "이메일 또는 비밀번호가 올바르지 않습니다.");
		}

		return "member/login";
	}

	// ===================== 내부 메서드 =====================

	/**
	 * 서비스가 던진 중복 코드 문자열을 화면용 한국어 메시지로 변환한다.
	 *
	 * @param code EMAIL_DUPLICATED / NICKNAME_DUPLICATED
	 * @return 화면에 보여줄 메시지
	 */
	private String toDuplicatedMessage(String code) {
		if ("EMAIL_DUPLICATED".equals(code)) {
			return "이미 사용 중인 이메일입니다.";
		}
		if ("NICKNAME_DUPLICATED".equals(code)) {
			return "이미 사용 중인 닉네임입니다.";
		}

		return "회원가입에 실패했습니다. 잠시 후 다시 시도해 주세요.";
	}
}
