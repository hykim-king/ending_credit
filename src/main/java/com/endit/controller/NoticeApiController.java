package com.endit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.domain.NoticeSearchVO;
import com.endit.domain.NoticeVO;
import com.endit.domain.PageResponse;
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

    /** T-06 공개 공지 상세. 조회 시 VIEW_COUNT + 1 */
    @GetMapping("/{noticeId}")
    public NoticeVO getNotice(@PathVariable Long noticeId) {
        return noticeService.getPublicNotice(noticeId);
    }
}
