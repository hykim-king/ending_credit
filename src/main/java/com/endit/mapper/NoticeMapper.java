package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.domain.NoticeVO;

@Mapper
public interface NoticeMapper {

    /** 공지 등록 */
    int insertNotice(NoticeVO notice);

    /** 공지 단건 조회 */
    NoticeVO selectNoticeById(Long noticeId);

    /** 공지 목록 조회 - 페이징 */
    List<NoticeVO> selectNoticeList(
            @Param("page") int page,
            @Param("size") int size
    );

    /** 전체 공지 개수 조회 */
    int countNoticeList();

    /** 공지 수정 */
    int updateNotice(NoticeVO notice);

    /** 조회수 1 증가 */
    int increaseViewCount(Long noticeId);

    /** 공지 삭제 */
    int deleteNotice(Long noticeId);
}