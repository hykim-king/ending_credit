package com.endit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 인물 상세 화면 (P-01)
 * 담당: 이기준
 */
@Controller
@RequestMapping("/people")
public class PersonViewController {

	/** 인물 상세 화면 — 데이터는 /api/people/{personId} 로 조회 */
	@GetMapping("/{personId}")
	public String detail(
			@PathVariable int personId,
			Model model) {

		model.addAttribute("personId", personId);
		return "person/detail";
	}
}
