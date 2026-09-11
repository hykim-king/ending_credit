package com.endit.controller;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.cmn.LoginMember;
import com.endit.domain.MemberVO;
import com.endit.security.LoginMemberHelper;
import com.endit.service.MemberService;

/**
 * <pre>
 * Class Name  : PersonLikeViewController
 * Description : 회원이 좋아요한 인물 및 컬렉션 목록 화면의 View를 처리하는 Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * 2026. 8. 28. jinyoung    컬렉션 좋아요 유형 지원
 * 2026. 9. 01. jinyoung    U-07 목록 조회자 식별용 인증 회원 전달
 * 2026. 9. 03. jinyoung    좋아요 화면 회원 정보 및 조회 유형 처리 정리
 * 2026. 9. 05. jinyoung    본인 전용 좋아요 경로 및 LoginMemberHelper 적용
 * 2026. 9. 10. heetae      다른 회원 좋아요 경로 지원 및 조회자·대상 회원 분리
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@Controller
@RequestMapping("/members")
public class PersonLikeViewController {

	private static final String TYPE_PERSON = "person"; 		// 인물 좋아요 유형
	private static final String TYPE_COLLECTION = "collection"; // 컬렉션 좋아요 유형
	private final MemberService memberService;

	/**
	 * 화면 처리에 필요한 의존성 주입
	 *
	 * @param memberService 회원 Service
	 */
	public PersonLikeViewController(MemberService memberService) {
		this.memberService = memberService;
	}

	/**
	 * 회원의 좋아요 화면 반환 실제 목록 데이터는 JavaScript가 유형별 REST API로 조회
	 *
	 * URL에 회원 번호가 없으면 로그인 회원 본인의 좋아요, 있으면 그 회원의 좋아요를 보여 준다.
	 * 본인 번호로 들어오면 본인 전용 경로로 리다이렉트해 주소를 하나로 통일한다.
	 *
	 * @param memberId 조회할 회원 번호, 본인 좋아요면 null
	 * @param type     최초 표시할 좋아요 유형
	 * @param model    View에 전달할 데이터
	 * @return 회원 좋아요 View 이름
	 */
	@GetMapping({ "/likes", "/{memberId:[0-9]+}/likes" })
	public String likes(
			@PathVariable(required = false) Long memberId,
			@RequestParam(defaultValue = TYPE_PERSON) String type,
			Model model) {

		// 조회자는 로그인 회원. 다른 회원 좋아요는 비로그인도 볼 수 있어 null이 될 수 있다.
		LoginMember viewer = LoginMemberHelper.getLoginMember();
		Long currentMemberId = (viewer == null) ? null : viewer.getMemberId();

		String normalizedType = normalizeType(type);

		// 본인 번호로 들어온 요청은 본인 전용 경로로 통일한다.
		if (memberId != null && currentMemberId != null && memberId.longValue() == currentMemberId.longValue()) {
			return "redirect:/members/likes?type=" + normalizedType;
		}

		// 대상은 URL에 있으면 그 회원, 없으면 로그인 회원
		int targetMemberId = (memberId != null)
				? Math.toIntExact(memberId)
				: Math.toIntExact(LoginMemberHelper.getMemberId());

		MemberVO member = memberService.getMember(targetMemberId);

		if (member == null) {
			throw new NoSuchElementException("회원을 찾을 수 없습니다.");
		}

		model.addAttribute("memberId", targetMemberId);
		model.addAttribute("type", normalizedType);
		model.addAttribute("member", member);
		model.addAttribute("currentMemberId", currentMemberId);
		model.addAttribute("owner", currentMemberId != null && currentMemberId.longValue() == targetMemberId);

		return "member/likes";
	}

	// 내부 조회 조건·응답 구성

	/**
	 * 지원하는 좋아요 유형을 보정하고 그 외의 값은 인물 유형으로 처리
	 *
	 * @param type 요청한 좋아요 유형
	 * @return 보정한 좋아요 유형
	 */
	private String normalizeType(String type) {

		if (type == null) {
			return TYPE_PERSON;
		}

		return TYPE_COLLECTION.equalsIgnoreCase(type.trim()) ? TYPE_COLLECTION : TYPE_PERSON;
	}
}
