package com.endit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.endit.auth.CurrentMemberProvider;

/**
 * <pre>
 * Class Name  : CollectionViewController
 * Description : 컬렉션 목록, 등록, 상세 및 수정 화면의 경로를 처리하는 Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * 2026. 8. 29. jinyoung    상세 화면에 인증 회원 식별 정보 전달
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@Controller
@RequestMapping("/collections")
public class CollectionViewController {

	private final CurrentMemberProvider currentMemberProvider;

	public CollectionViewController(CurrentMemberProvider currentMemberProvider) {
		this.currentMemberProvider = currentMemberProvider;
	}

	/** 컬렉션 목록 화면 */
	@GetMapping
	public String list() {
		return "collection/list";
	}

	/** 컬렉션 등록 화면 */
	@GetMapping("/new")
	public String createForm(Model model) {
		// 등록과 수정이 같은 form.html을 사용하므로 JavaScript가 구분할 mode를 전달한다.
		model.addAttribute("formMode", "create");
		model.addAttribute("collectionId", 0);

		return "collection/form";
	}

	/** 컬렉션 상세 화면 */
	@GetMapping("/{collectionId}")
	public String detail(
			@PathVariable int collectionId,
			Model model) {

		// 상세 데이터는 REST API로 조회하고, View에는 조회 키와 화면 권한 판별값만 전달한다.
		model.addAttribute("collectionId", collectionId);
		model.addAttribute(
				"currentMemberId",
				currentMemberProvider.findCurrentMemberId().orElse(0));

		return "collection/detail";
	}

	/** 컬렉션 수정 화면 */
	@GetMapping("/{collectionId}/edit")
	public String updateForm(
			@PathVariable int collectionId,
			Model model) {

		// formMode와 collectionId는 body의 data-* 속성으로 렌더링되어 JS에서 사용된다.
		model.addAttribute("formMode", "update");
		model.addAttribute("collectionId", collectionId);

		return "collection/form";
	}
}
