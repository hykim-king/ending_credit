package com.endit.domain;

import com.endit.cmn.DTO;

public class ContentGenreVO extends DTO {

	private int contentId;
	private int genreId;

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

	@Override
	public String toString() {
		return "ContentGenreVO [contentId=" + contentId + ", genreId=" + genreId + ", toString()=" + super.toString()
				+ "]";
	}

}
