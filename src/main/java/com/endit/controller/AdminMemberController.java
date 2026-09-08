/**
 * 회원 관리 Controller (AD-07)
 * 목록은 화면(뷰) 반환, 강퇴는 fetch(AJAX)용 MessageVO 반환.
 * /admin/users 는 설계서 경로이자 1조 관리자 사이드바가 가리키는 주소라 함께 받는다.
 */
package com.endit.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.MemberVO;
import com.endit.service.AdminMemberService;

@Controller
public class AdminMemberController {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final AdminMemberService adminMemberService;

	public AdminMemberController(AdminMemberService adminMemberService) {
		super();
		this.adminMemberService = adminMemberService;
		log.debug("adminMemberService: {}", adminMemberService);
	}

	/**
	 *
	 * <pre>
	 * Method Name : doRetrieve
	 * Description : 회원 관리 목록 (AD-07)
	 *
	 * </pre>
	 *
	 * @param searchDiv 검색구분(email/nickname, 빈값이면 둘 다)
	 * @param searchWord 검색어
	 * @param pageSize 페이지당 건수
	 * @param pageNo 페이지 번호
	 * @param model 뷰 전달용
	 * @return admin/member_list
	 */
	@GetMapping({ "/admin/users", "/admin/users/doRetrieve" })
	public String doRetrieve(
			@RequestParam(name = "searchDiv", defaultValue = "") String searchDiv,
			@RequestParam(name = "searchWord", defaultValue = "") String searchWord,
			@RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
			@RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
			Model model) {
		String viewName = "admin/member_list";
		log.debug("=============================");
		log.debug("{}()", "doRetrieve");
		log.debug("searchDiv: {}, searchWord: {}, pageSize: {}, pageNo: {}", searchDiv, searchWord, pageSize, pageNo);
		log.debug("=============================");

		DTO dto = new DTO();
		dto.setSearchDiv(searchDiv);
		dto.setSearchWord(searchWord);
		dto.setPageSize(pageSize);
		dto.setPageNo(pageNo);

		List<MemberVO> list = adminMemberService.doRetrieve(dto);
		int totalCnt = adminMemberService.totalCnt(dto);

		DTO pageDTO = new DTO(dto.getPageNo(), dto.getPageSize(), totalCnt);

		model.addAttribute("list", list);
		model.addAttribute("totalCnt", totalCnt);
		model.addAttribute("pageDTO", pageDTO);
		model.addAttribute("dto", dto);

		return viewName;
	}

	/**
	 *
	 * <pre>
	 * Method Name : upWithdrawMember
	 * Description : 회원 강퇴 — 회원 행 삭제(FK CASCADE로 활동 데이터도 함께 삭제)
	 *
	 * </pre>
	 *
	 * @param memberId 회원번호
	 * @return MessageVO(id: 1성공)
	 */
	@PostMapping("/admin/users/upWithdrawMember")
	@ResponseBody
	public MessageVO upWithdrawMember(@RequestParam(name = "memberId") long memberId) {
		log.debug("=============================");
		log.debug("{}()", "upWithdrawMember");
		log.debug("memberId: {}", memberId);
		log.debug("=============================");

		int flag = adminMemberService.upWithdrawMember(memberId);
		String message = 1 == flag ? "회원을 강퇴했습니다." : "회원 강퇴에 실패 했습니다.";

		return new MessageVO(flag + "", message);
	}
}
