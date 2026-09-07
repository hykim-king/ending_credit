package com.endit.domain;

public class NoticeVO {

    private Long noticeId;
    private Integer displayNo;
    private String title;
    private String content;
    private String important;
    private String status;
    private int viewCount;
    private Long createdId;
    private String createdNickname;
    private String createdDt;
    private Long updatedId;
    private String updatedDt;

    // 기본 생성자
    public NoticeVO() {
    }

    // 전체 필드 생성자
    public NoticeVO(Long noticeId, String title, String content,
            String important, String status, int viewCount,
            Long createdId, String createdNickname, String createdDt,
            Long updatedId, String updatedDt) {

        this.noticeId = noticeId;
        this.title = title;
        this.content = content;
        this.important = important;
        this.status = status;
        this.viewCount = viewCount;
        this.createdId = createdId;
        this.createdNickname = createdNickname;
        this.createdDt = createdDt;
        this.updatedId = updatedId;
        this.updatedDt = updatedDt;
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
    }

    /** 화면에 보여 줄 번호. DB PK(NOTICE_ID)와 분리된 최신순 순번이다. */
    public Integer getDisplayNo() {
        return displayNo;
    }

    public void setDisplayNo(Integer displayNo) {
        this.displayNo = displayNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImportant() {
        return important;
    }

    public void setImportant(String important) {
        this.important = important;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public Long getCreatedId() {
        return createdId;
    }

    public void setCreatedId(Long createdId) {
        this.createdId = createdId;
    }


    public String getCreatedNickname() {
        return createdNickname;
    }

    public void setCreatedNickname(String createdNickname) {
        this.createdNickname = createdNickname;
    }

    public String getCreatedDt() {
        return createdDt;
    }

    public void setCreatedDt(String createdDt) {
        this.createdDt = createdDt;
    }

    public Long getUpdatedId() {
        return updatedId;
    }

    public void setUpdatedId(Long updatedId) {
        this.updatedId = updatedId;
    }

    public String getUpdatedDt() {
        return updatedDt;
    }

    public void setUpdatedDt(String updatedDt) {
        this.updatedDt = updatedDt;
    }

    @Override
    public String toString() {
        return "NoticeVO [noticeId=" + noticeId
                + ", displayNo=" + displayNo
                + ", title=" + title
                + ", content=" + content
                + ", important=" + important
                + ", status=" + status
                + ", viewCount=" + viewCount
                + ", createdId=" + createdId
                + ", createdNickname=" + createdNickname
                + ", createdDt=" + createdDt
                + ", updatedId=" + updatedId
                + ", updatedDt=" + updatedDt + "]";
    }
}