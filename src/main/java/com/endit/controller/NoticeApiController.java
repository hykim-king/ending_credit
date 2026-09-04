package com.endit.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.endit.cmn.LoginMember;
import com.endit.cmn.MessageVO;
import com.endit.domain.NoticeSearchVO;
import com.endit.domain.NoticeVO;
import com.endit.domain.PageResponse;
import com.endit.security.LoginMemberHelper;
import com.endit.service.NoticeService;

@RestController
@RequestMapping("/api/notices")
public class NoticeApiController {

    private final NoticeService noticeService;

    public NoticeApiController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /** T-06 공개 공지 목록 */
    @GetMapping
    public PageResponse<NoticeVO> getNoticeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        NoticeSearchVO search = new NoticeSearchVO();
        search.setPageNo(page);
        search.setPageSize(size);

        return noticeService.getPublicNoticeList(search);
    }

    /**
     * T-06 공개 공지 상세.
     * 비회원/일반회원은 상세 조회할 때마다 조회수 +1,
     * 관리자는 조회해도 조회수가 증가하지 않는다.
     */
    @GetMapping("/{noticeId}")
    public NoticeVO getNotice(@PathVariable Long noticeId) {
        LoginMember loginMember = LoginMemberHelper.getLoginMember();

        boolean increaseViewCount =
                loginMember == null || !loginMember.isAdmin();

        return noticeService.getPublicNotice(noticeId, increaseViewCount);
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
