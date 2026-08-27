package com.endit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.endit.domain.NoticeVO;
import com.endit.service.NoticeService;

import jakarta.servlet.http.HttpSession;

@Controller
public class NoticePageController {

    private final NoticeService noticeService;

    public NoticePageController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /** T-06 사용자 공지 목록 */
    @GetMapping("/notices")
    public String noticeList() {
        return "notice/noticeList";
    }

    /** T-06 사용자 공지 상세 */
    @GetMapping("/notices/{noticeId}")
    public String noticeDetail(@PathVariable Long noticeId, Model model) {
        model.addAttribute("noticeId", noticeId);
        return "notice/noticeDetail";
    }

    /** AD-12 관리자 공지 관리 목록 */
    @GetMapping("/admin/notices")
    public String adminNoticeList(HttpSession session) {
        NoticeSessionSupport.requireAdminId(session);
        return "admin/notice/adminNoticeList";
    }

    /** AD-13 관리자 공지 신규 등록 */
    @GetMapping("/admin/notices/new")
    public String adminNoticeCreate(HttpSession session, Model model) {
        NoticeSessionSupport.requireAdminId(session);

        NoticeVO notice = new NoticeVO();
        notice.setImportant("N");
        notice.setStatus("DRAFT");

        model.addAttribute("notice", notice);
        model.addAttribute("isEdit", false);

        return "admin/notice/adminNoticeForm";
    }

    /** AD-13 관리자 공지 수정 */
    @GetMapping("/admin/notices/{noticeId}")
    public String adminNoticeEdit(
            @PathVariable Long noticeId,
            HttpSession session,
            Model model
    ) {
        NoticeSessionSupport.requireAdminId(session);

        NoticeVO notice = noticeService.getAdminNotice(noticeId);
        model.addAttribute("notice", notice);
        model.addAttribute("isEdit", true);

        return "admin/notice/adminNoticeForm";
    }
}
