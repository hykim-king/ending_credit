package com.endit.domain;

import com.endit.cmn.DTO;

public class GenreVO extends DTO {

	private int genreId;
	private String externalGenreId;
	private String name;

	public GenreVO() {
		super();
	}

	public GenreVO(int genreId, String externalGenreId, String name) {
		super();
		this.genreId = genreId;
		this.externalGenreId = externalGenreId;
		this.name = name;
	}

	public int getGenreId() {
		return genreId;
	}

	public void setGenreId(int genreId) {
		this.genreId = genreId;
	}

	public String getExternalGenreId() {
		return externalGenreId;
	}

	public void setExternalGenreId(String externalGenreId) {
		this.externalGenreId = externalGenreId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "GenreVO [genreId=" + genreId + ", externalGenreId=" + externalGenreId + ", name=" + name
				+ ", toString()=" + super.toString() + "]";
	}

}
