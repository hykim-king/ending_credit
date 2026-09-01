package com.endit.service;

import com.endit.domain.NoticeSearchVO; 
import com.endit.domain.NoticeVO;
import com.endit.domain.PageResponse;

public interface NoticeService {

    PageResponse<NoticeVO> getPublicNoticeList(NoticeSearchVO search);

    NoticeVO getPublicNotice(Long noticeId, boolean increaseViewCount);

    PageResponse<NoticeVO> getAdminNoticeList(NoticeSearchVO search);

    NoticeVO getAdminNotice(Long noticeId);

    Long createNotice(NoticeVO notice, Long adminId);

    void updateNotice(Long noticeId, NoticeVO notice, Long adminId);
    
    void deleteNotice(Long noticeId, Long adminId);
}