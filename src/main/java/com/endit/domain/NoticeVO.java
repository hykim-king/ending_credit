package com.endit.domain;

public class NoticeVO {

    private Long noticeId;
    private String title;
    private String content;
    private String important;
    private String status;
    private int viewCount;
    private Long createdId;
    private String createdDt;
    private Long updatedId;
    private String updatedDt;

    public NoticeVO() {
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
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
                + ", title=" + title
                + ", content=" + content
                + ", important=" + important
                + ", status=" + status
                + ", viewCount=" + viewCount
                + ", createdId=" + createdId
                + ", createdDt=" + createdDt
                + ", updatedId=" + updatedId
                + ", updatedDt=" + updatedDt + "]";
    }
}