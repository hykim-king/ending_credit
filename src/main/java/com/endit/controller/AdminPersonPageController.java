package com.endit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.endit.domain.PersonVO;
import com.endit.service.PersonService;

import jakarta.servlet.http.HttpSession;

/**
 * 관리자 인물 화면(AD-05 인물 관리 목록 / AD-06 인물 등록·수정)의 경로를 처리하는 Controller
 *
 * 목록 데이터와 저장은 JavaScript가 REST API로 처리한다 - 공지 관리 화면과 같은 구조다.
 */
@Controller
public class AdminPersonPageController {

	private static final String PERSON_LIST_VIEW = "admin/people/adminPersonList";
	private static final String PERSON_FORM_VIEW = "admin/people/adminPersonForm";

	private final PersonService personService;

	public AdminPersonPageController(PersonService personService) {
		this.personService = personService;
	}

	/** AD-05 인물 관리 목록 */
	@GetMapping("/admin/people")
	public String adminPersonList(HttpSession session) {
		NoticeSessionSupport.requireAdminId(session);

		return PERSON_LIST_VIEW;
	}

	/** AD-06 인물 신규 등록 */
	@GetMapping("/admin/people/new")
	public String adminPersonCreate(HttpSession session, Model model) {
		NoticeSessionSupport.requireAdminId(session);

		model.addAttribute("person", new PersonVO());
		model.addAttribute("isEdit", false);

		return PERSON_FORM_VIEW;
	}

	/** AD-06 인물 수정 */
	@GetMapping("/admin/people/{personId}")
	public String adminPersonEdit(@PathVariable int personId, HttpSession session, Model model) {
		NoticeSessionSupport.requireAdminId(session);

		PersonVO person = personService.get(personId);

		// PersonService.get은 없는 인물에 null을 돌려준다(컨트롤러가 404로 바꾸는 계약)
		if (person == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 인물입니다.");
		}

		model.addAttribute("person", person);
		model.addAttribute("isEdit", true);

		return PERSON_FORM_VIEW;
	}

}
