package com.endit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <pre>
 * Class Name  : MemberContentViewController 
 * Description : 회원 평가와 보고싶어요 기록 화면의 View 경로를 처리하는 Controller
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
@RequestMapping("/users/{memberId}/records")
public class MemberContentViewController {

	private static final String TAB_RATINGS = "ratings";
	private static final String TAB_WATCHLIST = "watchlist";

	/**
	 * 회원 기록 화면 반환
	 *
	 * 평가와 보고싶어요는 같은 HTML에서 탭으로 전환하고,
	 * 실제 목록 데이터는 JavaScript가 REST API로 조회한다.
	 *
	 * @param memberId 조회할 회원 번호
	 * @param tab 최초 표시할 기록 탭
	 * @param model View에 전달할 데이터
	 * @return 회원 기록 View 이름
	 */
	@GetMapping
	public String records(
			@PathVariable int memberId,
			@RequestParam(defaultValue = TAB_RATINGS) String tab,
			Model model) {

		model.addAttribute("memberId", memberId);
		model.addAttribute("tab", normalizeTab(tab));

		return "user/records";
	}

	/**
	 * 현재 구현된 평가와 보고싶어요 외의 탭은 기본 평가 탭으로 보정
	 */
	private String normalizeTab(String tab) {
		if (tab != null
				&& TAB_WATCHLIST.equalsIgnoreCase(
						tab.trim())) {

			return TAB_WATCHLIST;
		}

		return TAB_RATINGS;
	}
}