/**
 * AI 가 뽑아낸 검색 의도
 *
 * AI 는 "무엇을 찾고 싶은지"만 정하고, 영화 목록은 우리 DB 가 낸다.
 * 그래서 AI 가 없는 영화를 지어낼 자리가 없다.
 *
 * MyBatis parameterType 으로도 쓰이므로 record 가 아니라 일반 클래스로 둔다.
 */
package com.endit.ai.dto;

import java.util.List;

public class SearchIntent {

	/** 순위 목록만 원할 때 */
	public static final String INTENT_RANKING = "ranking";
	/** 제목으로 콕 집어 찾을 때 */
	public static final String INTENT_TITLE = "title_search";
	/** 내용·분위기로 찾을 때 */
	public static final String INTENT_KEYWORD = "keyword_search";
	/** 공지사항 검색 */
	public static final String INTENT_NOTICE = "notice_search";
	/** 사이트 사용법 질문 */
	public static final String INTENT_SITE_HELP = "site_help";
	/** 영화와도 사이트와도 무관한 질문 */
	public static final String INTENT_OUT_OF_SCOPE = "out_of_scope";

	private String intent;
	private List<String> keywords;
	private String title;
	private String genre;
	private Integer yearFrom;
	private Integer yearTo;
	private String sort;
	private int limit;
	private String message;
	private String faqKey;
	private List<String> excludeTitles;
	private String provider;

	public SearchIntent() {
		super();
	}

	/** AI 를 부르지 못했을 때. 검색을 막지 않고 최신순 목록으로 떨어뜨린다. */
	public static SearchIntent fallbackRanking() {
		SearchIntent vo = new SearchIntent();
		vo.setIntent(INTENT_RANKING);
		vo.setSort("latest");
		vo.setLimit(20);
		vo.setMessage("AI 분석을 건너뛰고 최신순으로 보여드립니다.");
		vo.setProvider("none");
		return vo;
	}

	/** 영화와도 사이트와도 무관해 아무것도 뒤질 필요가 없는가 */
	public boolean isOutOfScope() {
		return INTENT_OUT_OF_SCOPE.equals(intent);
	}

	/** 공지사항을 찾는 질문인가 */
	public boolean isNoticeSearch() {
		return INTENT_NOTICE.equals(intent);
	}

	/** 사이트 사용법을 묻는 질문인가 */
	public boolean isSiteHelp() {
		return INTENT_SITE_HELP.equals(intent);
	}

	public String getIntent() { return intent; }
	public void setIntent(String intent) { this.intent = intent; }

	public List<String> getKeywords() { return keywords; }
	public void setKeywords(List<String> keywords) { this.keywords = keywords; }

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	public String getGenre() { return genre; }
	public void setGenre(String genre) { this.genre = genre; }

	public Integer getYearFrom() { return yearFrom; }
	public void setYearFrom(Integer yearFrom) { this.yearFrom = yearFrom; }

	public Integer getYearTo() { return yearTo; }
	public void setYearTo(Integer yearTo) { this.yearTo = yearTo; }

	public String getSort() { return sort; }
	public void setSort(String sort) { this.sort = sort; }

	public int getLimit() { return limit; }
	public void setLimit(int limit) { this.limit = limit; }

	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }

	public List<String> getExcludeTitles() { return excludeTitles; }
	public void setExcludeTitles(List<String> excludeTitles) { this.excludeTitles = excludeTitles; }

	public String getFaqKey() { return faqKey; }
	public void setFaqKey(String faqKey) { this.faqKey = faqKey; }

	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }

	@Override
	public String toString() {
		return "SearchIntent [intent=" + intent + ", keywords=" + keywords + ", title=" + title
				+ ", genre=" + genre + ", yearFrom=" + yearFrom + ", yearTo=" + yearTo
				+ ", sort=" + sort + ", limit=" + limit
				+ ", faqKey=" + faqKey + ", provider=" + provider + "]";
	}
}
