/**
 * 사이트 사용법 안내 한 건 - FaqAnswers 가 보관하는 원문
 */
package com.endit.ai.dto;

public record AiHelpAnswer(
		String title,
		String body,
		String linkUrl,
		String linkLabel) {
}
