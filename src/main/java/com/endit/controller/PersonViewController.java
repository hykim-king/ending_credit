package com.endit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 인물 화면 (P-01 상세, 인물 검색 결과)
 * 담당: 이기준
 */
@Controller
@RequestMapping("/people")
public class PersonViewController {

	/** 인물 검색 결과 — 데이터는 /api/people 로 조회 */
	@GetMapping
	public String list(
			@RequestParam(defaultValue = "") String searchWord,
			@RequestParam(defaultValue = "") String searchDiv,
			@RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "12") int pageSize,
			Model model) {

		model.addAttribute("searchWord", searchWord);
		model.addAttribute("searchDiv", searchDiv);
		model.addAttribute("pageNo", pageNo);
		model.addAttribute("pageSize", pageSize);
		return "person/list";
	}

	/** 인물 상세 화면 — 데이터는 /api/people/{personId} 로 조회 */
	@GetMapping("/{personId}")
	public String detail(
			@PathVariable int personId,
			Model model) {

		model.addAttribute("personId", personId);
		return "person/detail";
	}
}
