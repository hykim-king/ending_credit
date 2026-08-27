package com.endit.domain;

import com.endit.cmn.DTO;

/**
 * 공지 검색/페이징 조건.
 * 기존 프로젝트의 DTO(pageNo, pageSize, searchWord)를 그대로 사용하고
 * NOTICE 전용 필터인 status, important만 추가한다.
 */
public class NoticeSearchVO extends DTO {

    private String status;
    private String important;

    public NoticeSearchVO() {
        super();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImportant() {
        return important;
    }

    public void setImportant(String important) {
        this.important = important;
    }

    public int getOffset() {
        return (getPageNo() - 1) * getPageSize();
    }

    @Override
    public String toString() {
        return "NoticeSearchVO [status=" + status
                + ", important=" + important
                + ", pageNo=" + getPageNo()
                + ", pageSize=" + getPageSize()
                + ", searchWord=" + getSearchWord() + "]";
    }
}
