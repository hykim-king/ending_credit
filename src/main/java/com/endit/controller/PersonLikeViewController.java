package com.endit.controller;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.auth.CurrentMemberProvider;
import com.endit.domain.MemberVO;
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
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@Controller
@RequestMapping("/users/{memberId}/likes")
public class PersonLikeViewController {

	private static final String TYPE_PERSON = "person";
	private static final String TYPE_COLLECTION = "collection";

	private final CurrentMemberProvider currentMemberProvider;
	private final MemberService memberService;

	public PersonLikeViewController(
			CurrentMemberProvider currentMemberProvider, MemberService memberService) {

		this.currentMemberProvider = currentMemberProvider;
		this.memberService = memberService;
	}

	/**
	 * 회원 좋아요 화면 반환
	 *
	 * 실제 목록 데이터는 JavaScript가 유형별 REST API로 조회한다.
	 *
	 * @param memberId 조회할 회원 번호
	 * @param type     최초 표시할 좋아요 유형
	 * @param model    View에 전달할 데이터
	 * @return 회원 좋아요 View 이름
	 */
	@GetMapping
	public String likes(@PathVariable int memberId,
			@RequestParam(defaultValue = TYPE_PERSON) String type,
			Model model) {

		MemberVO member = memberService.getMember(memberId);

		if (member == null) {
			throw new NoSuchElementException("회원을 찾을 수 없습니다.");
		}

		long currentMemberId = currentMemberProvider.findCurrentMemberId().orElse(0);

		model.addAttribute("memberId", memberId);
		model.addAttribute("type", normalizeType(type));
		model.addAttribute("member", member);
		model.addAttribute("currentMemberId", currentMemberId);

		return "user/likes";
	}

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

		return TYPE_COLLECTION.equalsIgnoreCase(type.trim())
				? TYPE_COLLECTION
				: TYPE_PERSON;
	}
}
