/**
 * AI 검색 응답
 *
 * intent 갈래에 따라 셋 중 하나가 채워진다.
 *   items   - 영화 검색 결과
 *   notices - 공지 검색 결과
 *   help    - 사이트 사용법 안내
 */
package com.endit.ai.dto;

import java.util.List;

public record AiSearchResponse(
		SearchIntent intent,
		List<AiSearchItem> items,
		List<AiNoticeItem> notices,
		AiHelpAnswer help,
		int totalCnt,
		String message) {

	public static AiSearchResponse ofMovies(SearchIntent intent, List<AiSearchItem> items) {
		return new AiSearchResponse(intent, items, List.of(), null,
				items.size(), intent.getMessage());
	}

	public static AiSearchResponse ofNotices(SearchIntent intent, List<AiNoticeItem> notices) {
		return new AiSearchResponse(intent, List.of(), notices, null,
				notices.size(), intent.getMessage());
	}

	public static AiSearchResponse ofHelp(SearchIntent intent, AiHelpAnswer help) {
		return new AiSearchResponse(intent, List.of(), List.of(), help, 0, "");
	}

	/** 영화와도 사이트와도 무관한 질문. 아무것도 뒤지지 않고 안내만 돌려준다. */
	public static AiSearchResponse outOfScope(SearchIntent intent) {
		return new AiSearchResponse(intent, List.of(), List.of(), null, 0, intent.getMessage());
	}
}
