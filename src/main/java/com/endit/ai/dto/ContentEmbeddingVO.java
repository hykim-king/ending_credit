/**
 * 임베딩 적재·조회용 VO
 *
 * 적재 대상 조회 때는 contentId+overview 가, 저장 때는 contentId+embedding+model 이,
 * 검색 때는 contentId+embedding 이 채워진다. MyBatis 용이라 일반 클래스다.
 */
package com.endit.ai.dto;

public class ContentEmbeddingVO {

	private long contentId;
	private String titleKo;
	private String genres;
	private String overview;
	private String embedding;   // 좌표 JSON 문자열 "[0.01,-0.04,...]"
	private String model;

	public ContentEmbeddingVO() {
		super();
	}

	public long getContentId() { return contentId; }
	public void setContentId(long contentId) { this.contentId = contentId; }

	public String getTitleKo() { return titleKo; }
	public void setTitleKo(String titleKo) { this.titleKo = titleKo; }

	public String getGenres() { return genres; }
	public void setGenres(String genres) { this.genres = genres; }

	public String getOverview() { return overview; }
	public void setOverview(String overview) { this.overview = overview; }

	public String getEmbedding() { return embedding; }
	public void setEmbedding(String embedding) { this.embedding = embedding; }

	public String getModel() { return model; }
	public void setModel(String model) { this.model = model; }

	@Override
	public String toString() {
		return "ContentEmbeddingVO [contentId=" + contentId + ", model=" + model
				+ ", embedding.len=" + (null == embedding ? 0 : embedding.length()) + "]";
	}
}
