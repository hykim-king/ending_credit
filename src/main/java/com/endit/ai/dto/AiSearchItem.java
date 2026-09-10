/**
 * AI 검색 결과 한 건
 *
 * 화면 카드에 필요한 것만 담는다. 전부 DB 에서 나온 값이라 지어낸 값이 섞일 수 없다.
 */
package com.endit.ai.dto;

public class AiSearchItem {

	private long contentId;
	private String titleKo;
	private String titleOrg;
	private String releaseYear;
	private String posterUrl;
	private String overview;
	private Double avgRating;
	private int commentCnt;

	public AiSearchItem() {
		super();
	}

	public long getContentId() { return contentId; }
	public void setContentId(long contentId) { this.contentId = contentId; }

	public String getTitleKo() { return titleKo; }
	public void setTitleKo(String titleKo) { this.titleKo = titleKo; }

	public String getTitleOrg() { return titleOrg; }
	public void setTitleOrg(String titleOrg) { this.titleOrg = titleOrg; }

	public String getReleaseYear() { return releaseYear; }
	public void setReleaseYear(String releaseYear) { this.releaseYear = releaseYear; }

	public String getPosterUrl() { return posterUrl; }
	public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

	public String getOverview() { return overview; }
	public void setOverview(String overview) { this.overview = overview; }

	public Double getAvgRating() { return avgRating; }
	public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }

	public int getCommentCnt() { return commentCnt; }
	public void setCommentCnt(int commentCnt) { this.commentCnt = commentCnt; }

	@Override
	public String toString() {
		return "AiSearchItem [contentId=" + contentId + ", titleKo=" + titleKo
				+ ", releaseYear=" + releaseYear + ", avgRating=" + avgRating
				+ ", commentCnt=" + commentCnt + "]";
	}
}
