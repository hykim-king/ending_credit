package com.endit.domain;

import com.endit.cmn.DTO;

public class ContentGenreVO extends DTO {

	private int contentId;
	private int genreId;
	private String genreName;
	// TMDB 장르 id. 화면이 장르명을 번들에서 찾을 때 키로 쓴다 - GENRE.name은 한국어뿐이라 영어를 담을 곳이 없다
	private String externalGenreId;

	public ContentGenreVO() {
		super();
	}

	public ContentGenreVO(int contentId, int genreId) {
		super();
		this.contentId = contentId;
		this.genreId = genreId;
	}

	public int getContentId() {
		return contentId;
	}

	public void setContentId(int contentId) {
		this.contentId = contentId;
	}

	public int getGenreId() {
		return genreId;
	}

	public void setGenreId(int genreId) {
		this.genreId = genreId;
	}

	public String getGenreName() {
		return genreName;
	}

	public void setGenreName(String genreName) {
		this.genreName = genreName;
	}

	public String getExternalGenreId() {
		return externalGenreId;
	}

	public void setExternalGenreId(String externalGenreId) {
		this.externalGenreId = externalGenreId;
	}

	@Override
	public String toString() {
		return "ContentGenreVO [contentId=" + contentId + ", genreId=" + genreId + ", genreName=" + genreName
				+ ", toString()=" + super.toString() + "]";
	}

}
