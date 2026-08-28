package com.endit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
 * 2026. 8. 28. jinyoung    임시 회원 번호 및 컬렉션 편집 모달 연결
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@Controller
@RequestMapping("/collections")
public class CollectionViewController {

	/** 컬렉션 목록 화면 */
	@GetMapping
	public String list() {
		return "collection/list";
	}

	/** 컬렉션 등록 화면 */
	@GetMapping("/new")
	public String createForm(
			@RequestParam(defaultValue = "1") int memberId,
			Model model) {
		// 등록과 수정이 같은 form.html을 사용하므로 JavaScript가 구분할 mode를 전달한다.
		model.addAttribute("formMode", "create");
		model.addAttribute("collectionId", 0);
		model.addAttribute("memberId", memberId);

		return "collection/form";
	}

	/** 컬렉션 상세 화면 */
	@GetMapping("/{collectionId}")
	public String detail(
			@PathVariable int collectionId,
			@RequestParam(defaultValue = "1") int memberId,
			Model model) {

		// 로그인 기능 병합 전에는 쿼리 파라미터의 임시 회원 번호를 사용한다.
		model.addAttribute("collectionId", collectionId);
		model.addAttribute("memberId", memberId);

		return "collection/detail";
	}

	/** 컬렉션 수정 화면 */
	@GetMapping("/{collectionId}/edit")
	public String updateForm(
			@PathVariable int collectionId,
			@RequestParam(defaultValue = "1") int memberId,
			Model model) {

		// formMode와 collectionId는 body의 data-* 속성으로 렌더링되어 JS에서 사용된다.
		model.addAttribute("formMode", "update");
		model.addAttribute("collectionId", collectionId);
		model.addAttribute("memberId", memberId);

		return "collection/form";
	}
}
