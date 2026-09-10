/**
 * AI 검색이 돌려주는 공지 한 건
 *
 * MyBatis resultType 으로 쓰므로 record 가 아니라 일반 클래스로 둔다.
 */
package com.endit.ai.dto;

public class AiNoticeItem {

	private long noticeId;
	private String title;
	private String createdDt;

	public AiNoticeItem() {
		super();
	}

	public long getNoticeId() { return noticeId; }
	public void setNoticeId(long noticeId) { this.noticeId = noticeId; }

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	public String getCreatedDt() { return createdDt; }
	public void setCreatedDt(String createdDt) { this.createdDt = createdDt; }

	@Override
	public String toString() {
		return "AiNoticeItem [noticeId=" + noticeId + ", title=" + title
				+ ", createdDt=" + createdDt + "]";
	}
}
