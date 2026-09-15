/**
 * 사이트 사용법 안내 한 건 - FaqAnswers 가 보관하는 원문
 */
package com.endit.domain;

public class AiHelpAnswerVO {

	private String title;
	private String body;
	private String linkUrl;
	private String linkLabel;

	public AiHelpAnswerVO() {
		super();
	}

	public AiHelpAnswerVO(String title, String body, String linkUrl, String linkLabel) {
		super();
		this.title = title;
		this.body = body;
		this.linkUrl = linkUrl;
		this.linkLabel = linkLabel;
	}

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getBody() { return body; }
	public void setBody(String body) { this.body = body; }
	public String getLinkUrl() { return linkUrl; }
	public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
	public String getLinkLabel() { return linkLabel; }
	public void setLinkLabel(String linkLabel) { this.linkLabel = linkLabel; }

	@Override
	public String toString() {
		return "AiHelpAnswerVO [title=" + title + ", linkUrl=" + linkUrl + "]";
	}
}
