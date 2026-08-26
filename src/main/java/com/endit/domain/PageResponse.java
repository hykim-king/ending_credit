package com.endit.domain;

import java.util.List;

/** 공통 페이지 응답 DTO */
public class PageResponse<T> {

    private List<T> list;
    private int pageNo;
    private int pageSize;
    private int totalCnt;
    private int totalPages;

    public PageResponse() {
    }

    public PageResponse(List<T> list, int pageNo, int pageSize, int totalCnt) {
        this.list = list;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalCnt = totalCnt;
        this.totalPages = totalCnt == 0
                ? 0
                : (int) Math.ceil(totalCnt / (double) pageSize);
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalCnt() {
        return totalCnt;
    }

    public void setTotalCnt(int totalCnt) {
        this.totalCnt = totalCnt;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
