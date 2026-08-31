/**
 * <pre>
 * Class Name : AdminController
 * Description : ⚠️ 관리자 임시 조회 전용 Controller (4조 데모용)
 *               대시보드(AD-01)·영화(AD-02)·인물(AD-05)·회원(AD-07)·공지(AD-12)의
 *               "목록 조회만" 임시 제공한다. 등록·수정·삭제 없음.
 *               담당 조(1·2조)가 해당 관리 기능을 구현하면 이 컨트롤러는 대체·삭제한다.
 *               신고(AD-09·10)는 4조 정식 구현(/admin/report/**)이다.
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 25.  홍선기   최초 생성 (임시)
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 25.
 */
package com.endit.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.endit.service.AdminService;

@Controller
@RequestMapping("/admin")
public class AdminController {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		super();
		this.adminService = adminService;
		log.debug("adminService: {}", adminService);
	}

	/**
	 * 대시보드 (AD-01, 임시) — 테이블별 건수와 미처리 신고 수
	 */
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		String viewName = "admin/dashboard";
		log.debug("=============================");
		log.debug("{}()", "dashboard");
		log.debug("=============================");

		model.addAttribute("summary", adminService.getSummary());
		return viewName;
	}

	/**
	 * 영화 관리 목록 (AD-02, 임시 조회 전용)
	 */
	@GetMapping("/content")
	public String content(Model model) {
		String viewName = "admin/content_list";
		log.debug("=============================");
		log.debug("{}()", "content");
		log.debug("=============================");

		model.addAttribute("rows", adminService.getContentList());
		return viewName;
	}

	/**
	 * 인물 관리 목록 (AD-05, 임시 조회 전용)
	 */
	@GetMapping("/person")
	public String person(Model model) {
		String viewName = "admin/person_list";
		log.debug("=============================");
		log.debug("{}()", "person");
		log.debug("=============================");

		model.addAttribute("rows", adminService.getPersonList());
		return viewName;
	}

	/**
	 * 회원 관리 목록 (AD-07, 임시 조회 전용)
	 */
	@GetMapping("/member")
	public String member(Model model) {
		String viewName = "admin/member_list";
		log.debug("=============================");
		log.debug("{}()", "member");
		log.debug("=============================");

		model.addAttribute("rows", adminService.getMemberList());
		return viewName;
	}

	/**
	 * 공지 관리 목록 (AD-12, 임시 조회 전용)
	 */
	@GetMapping("/notice")
	public String notice(Model model) {
		String viewName = "admin/notice_list";
		log.debug("=============================");
		log.debug("{}()", "notice");
		log.debug("=============================");

		model.addAttribute("rows", adminService.getNoticeList());
		return viewName;
	}

}
