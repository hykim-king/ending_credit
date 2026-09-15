/**
 * AI 검색 응답
 *
 * intent 갈래에 따라 셋 중 하나가 채워진다.
 *   items   - 영화 검색 결과
 *   notices - 공지 검색 결과
 *   help    - 사이트 사용법 안내
 */
package com.endit.domain;

import java.util.List;

public class AiSearchResponseVO {

	private SearchIntentResponseVO intent;
	private List<AiSearchItemVO> items;
	private List<AiNoticeItemVO> notices;
	private AiHelpAnswerVO help;
	private int totalCnt;
	private String message;

	public AiSearchResponseVO() {
		super();
	}

	public AiSearchResponseVO(SearchIntentResponseVO intent, List<AiSearchItemVO> items,
			List<AiNoticeItemVO> notices, AiHelpAnswerVO help, int totalCnt, String message) {
		super();
		this.intent = intent;
		this.items = items;
		this.notices = notices;
		this.help = help;
		this.totalCnt = totalCnt;
		this.message = message;
	}

	public static AiSearchResponseVO ofMovies(SearchIntentResponseVO intent, List<AiSearchItemVO> items) {
		return new AiSearchResponseVO(intent, items, List.of(), null,
				items.size(), intent.getMessage());
	}

	public static AiSearchResponseVO ofNotices(SearchIntentResponseVO intent, List<AiNoticeItemVO> notices) {
		return new AiSearchResponseVO(intent, List.of(), notices, null,
				notices.size(), intent.getMessage());
	}

	public static AiSearchResponseVO ofHelp(SearchIntentResponseVO intent, AiHelpAnswerVO help) {
		return new AiSearchResponseVO(intent, List.of(), List.of(), help, 0, "");
	}

	/** 영화와도 사이트와도 무관한 질문. 아무것도 뒤지지 않고 안내만 돌려준다. */
	public static AiSearchResponseVO outOfScope(SearchIntentResponseVO intent) {
		return new AiSearchResponseVO(intent, List.of(), List.of(), null, 0, intent.getMessage());
	}

	public SearchIntentResponseVO getIntent() { return intent; }
	public void setIntent(SearchIntentResponseVO intent) { this.intent = intent; }
	public List<AiSearchItemVO> getItems() { return items; }
	public void setItems(List<AiSearchItemVO> items) { this.items = items; }
	public List<AiNoticeItemVO> getNotices() { return notices; }
	public void setNotices(List<AiNoticeItemVO> notices) { this.notices = notices; }
	public AiHelpAnswerVO getHelp() { return help; }
	public void setHelp(AiHelpAnswerVO help) { this.help = help; }
	public int getTotalCnt() { return totalCnt; }
	public void setTotalCnt(int totalCnt) { this.totalCnt = totalCnt; }
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }

	@Override
	public String toString() {
		return "AiSearchResponseVO [intent=" + intent + ", totalCnt=" + totalCnt + ", message=" + message + "]";
	}
}
