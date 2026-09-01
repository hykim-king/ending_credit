package com.endit.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.endit.cmn.MessageVO;
import com.endit.domain.NoticeSearchVO;
import com.endit.domain.NoticeVO;
import com.endit.domain.PageResponse;
import com.endit.service.NoticeService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin/notices")
public class AdminNoticeApiController {

    private final NoticeService noticeService;

    public AdminNoticeApiController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /** AD-12 공지 관리 목록 */
    @GetMapping
    public PageResponse<NoticeVO> getAdminNoticeList(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String important,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session
    ) {
        NoticeSessionSupport.requireAdminId(session);

        NoticeSearchVO search = new NoticeSearchVO();
        search.setSearchWord(query);
        search.setStatus(status);
        search.setImportant(important);
        search.setPageNo(page);
        search.setPageSize(size);

        return noticeService.getAdminNoticeList(search);
    }

    /** 관리자 공지 단건 조회 */
    @GetMapping("/{noticeId}")
    public NoticeVO getAdminNotice(
            @PathVariable Long noticeId,
            HttpSession session
    ) {
        NoticeSessionSupport.requireAdminId(session);
        return noticeService.getAdminNotice(noticeId);
    }

    /** AD-13 신규 등록 */
    @PostMapping
    public ResponseEntity<Map<String, Long>> createNotice(
            @RequestBody NoticeVO notice,
            HttpSession session
    ) {
        Long adminId = NoticeSessionSupport.requireAdminId(session);
        Long noticeId = noticeService.createNotice(notice, adminId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("noticeId", noticeId));
    }

    /** AD-13 수정 */
    @PatchMapping("/{noticeId}")
    public ResponseEntity<Void> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody NoticeVO notice,
            HttpSession session
    ) {
        Long adminId = NoticeSessionSupport.requireAdminId(session);
        noticeService.updateNotice(noticeId, notice, adminId);
        return ResponseEntity.noContent().build();
    }

    /** AD-13 삭제 */
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(
            @PathVariable Long noticeId,
            HttpSession session
    ) {
        Long adminId = NoticeSessionSupport.requireAdminId(session);
        noticeService.deleteNotice(noticeId, adminId);
        return ResponseEntity.noContent().build();
    }

    /** Notice에서 발생한 상태 예외를 원래 HTTP 상태로 반환하고 스택트레이스는 노출하지 않는다. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<MessageVO> handleResponseStatusException(ResponseStatusException exception) {
        String message = exception.getReason() == null
                ? "요청을 처리할 수 없습니다."
                : exception.getReason();

        MessageVO body = new MessageVO(
                String.valueOf(exception.getStatusCode().value()),
                message,
                "No Detail Message."
        );

        return ResponseEntity.status(exception.getStatusCode()).body(body);
    }

    /** 예상하지 못한 Notice API 예외도 내부 상세정보를 외부에 노출하지 않는다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageVO> handleException(Exception exception) {
        MessageVO body = new MessageVO(
                "500",
                "서비스 처리 중 오류가 발생했습니다.",
                "No Detail Message."
        );

        return ResponseEntity.status(500).body(body);
    }
}
