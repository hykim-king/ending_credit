/**
 * 파이썬 /embed 에 보내는 요청 - 좌표로 바꿀 글 묶음 (수업 05 ChatRequestVO 형태)
 */
package com.endit.domain;

import java.util.List;

import com.endit.cmn.DTO;

public class EmbedRequestVO extends DTO {

	private List<String> texts;

	public EmbedRequestVO() {
		super();
	}

	public EmbedRequestVO(List<String> texts) {
		super();
		this.texts = texts;
	}

	public List<String> getTexts() { return texts; }
	public void setTexts(List<String> texts) { this.texts = texts; }

	@Override
	public String toString() {
		return "EmbedRequestVO [texts=" + (null == texts ? 0 : texts.size()) + "건]";
	}
}
