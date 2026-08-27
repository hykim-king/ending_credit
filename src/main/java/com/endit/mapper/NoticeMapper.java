package com.endit.mapper;

import java.util.List;  

import org.apache.ibatis.annotations.Mapper;
import com.endit.domain.NoticeSearchVO;

import com.endit.domain.NoticeVO;

@Mapper
public interface NoticeMapper {

    /** 공지 등록 */
    int insertNotice(NoticeVO notice);

    /** 공지 단건 조회 */
    NoticeVO selectNoticeById(Long noticeId);

    /** 공지 수정 */
    int updateNotice(NoticeVO notice);

    /** 공지 삭제 */
    int deleteNotice(Long noticeId);
    
    /** 일반 사용자 공지 목록 */
    List<NoticeVO> selectPublicNoticeList(NoticeSearchVO search);

    /** 일반 사용자 공개 공지 개수 */
    int countPublicNoticeList();

    /** 일반 사용자 공지 단건 조회 */
    NoticeVO selectPublicNoticeById(Long noticeId);

    /** 공개 공지 조회수 증가 */
    int increasePublicViewCount(Long noticeId);

    /** 관리자 공지 목록 */
    List<NoticeVO> selectAdminNoticeList(NoticeSearchVO search);

    /** 관리자 공지 개수 */
    int countAdminNoticeList(NoticeSearchVO search);
    
    /** 테스트용 전체 공지 개수 */
    int totalCnt();

    /** 테스트용 전체 공지 삭제 */
    int deleteAll();
}