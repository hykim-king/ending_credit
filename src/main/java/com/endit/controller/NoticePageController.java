package com.endit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.endit.domain.NoticeVO;
import com.endit.service.NoticeService;

import jakarta.servlet.http.HttpServletResponse;

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
    public String noticeDetail(
            @PathVariable Long noticeId,
            Model model
    ) {
        model.addAttribute("noticeId", noticeId);

        return "notice/noticeDetail";
    }

    /** AD-12 관리자 공지 관리 목록 */
    @GetMapping("/admin/notices")
    public String adminNoticeList() {

        return "admin/notice/adminNoticeList";
    }

    /** AD-13 관리자 공지 신규 등록 */
    @GetMapping("/admin/notices/new")
    public String adminNoticeCreate(
            Model model
    ) {

        NoticeVO notice = new NoticeVO();

        notice.setImportant("N");

        // 게시 상태 선택 기능을 사용하지 않으므로
        // 신규 공지는 항상 공개 상태로 생성
        notice.setStatus("PUBLISHED");

        model.addAttribute("notice", notice);
        model.addAttribute("isEdit", false);

        return "admin/notice/adminNoticeForm";
    }

    /** AD-13 관리자 공지 수정 */
    @GetMapping("/admin/notices/{noticeId}")
    public String adminNoticeEdit(
            @PathVariable Long noticeId,
            Model model
    ) {

        NoticeVO notice =
                noticeService.getAdminNotice(noticeId);

        model.addAttribute("notice", notice);
        model.addAttribute("isEdit", true);

        return "admin/notice/adminNoticeForm";
    }

    /** 화면 요청에서 발생한 Notice 상태 예외를 원래 HTTP 상태로 유지하고 상세 스택은 노출하지 않는다. */
    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletResponse response,
            Model model
    ) {

        response.setStatus(
                exception.getStatusCode().value()
        );

        model.addAttribute(
                "message",
                exception.getReason() == null
                        ? "요청을 처리할 수 없습니다."
                        : exception.getReason()
        );

        model.addAttribute("errorTrace", "");

        return "error/business_error";
    }

    /** 예상하지 못한 Notice 화면 예외도 상세 스택을 노출하지 않는다. */
    @ExceptionHandler(Exception.class)
    public String handleException(
            Exception exception,
            HttpServletResponse response,
            Model model
    ) {

        response.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        );

        model.addAttribute(
                "message",
                "서비스 처리 중 오류가 발생했습니다."
        );

        model.addAttribute("errorTrace", "");

        return "error/error";
    }
}