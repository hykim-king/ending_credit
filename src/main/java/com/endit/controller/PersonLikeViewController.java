package com.endit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <pre>
 * Class Name  : PersonLikeViewController
 * Description : 회원이 좋아요한 인물 목록 화면의 View 경로를 처리하는 Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
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

	/**
	 * 회원 좋아요 화면 반환
	 *
	 * 현재는 인물 좋아요만 연결되어 있으며 실제 목록 데이터는
	 * JavaScript가 PersonLikeController의 REST API로 조회한다.
	 *
	 * @param memberId 조회할 회원 번호
	 * @param type 최초 표시할 좋아요 유형
	 * @param model View에 전달할 데이터
	 * @return 회원 좋아요 View 이름
	 */
	@GetMapping
	public String likes(
			@PathVariable int memberId,
			@RequestParam(defaultValue = TYPE_PERSON) String type,
			Model model) {

		model.addAttribute("memberId", memberId);
		model.addAttribute("type", normalizeType(type));

		return "user/likes";
	}

	/**
	 * 현재 구현된 인물 좋아요 외의 유형은 person으로 보정
	 */
	private String normalizeType(String type) {
		if (type != null
				&& TYPE_PERSON.equalsIgnoreCase(
						type.trim())) {

			return TYPE_PERSON;
		}

		return TYPE_PERSON;
	}
}