package com.endit.mapper;

import java.util.List; 

import org.apache.ibatis.annotations.Mapper;

import com.endit.domain.NoticeVO;

@Mapper
public interface NoticeMapper {

    // 등록
    int doSave(NoticeVO inVO);

    // 단건 조회
    NoticeVO doSelectOne(Long noticeId);

    // 전체 조회
    List<NoticeVO> doRetrieve();

    // 수정
    int doUpdate(NoticeVO inVO);

    // 삭제
    int doDelete(Long noticeId);
}