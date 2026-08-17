package com.endit.domain;

import com.endit.cmn.DTO;

public class ContentVO extends DTO {

	private int contentId;
	private String externalId;
	private String titleKo;
	private String titleOrg;
	private String overview;
	private String releaseYear;
	private int runtimeMin;
	private String country;
	private String posterUrl;
	private String backdropUrl;
	private String createdDt;

	public ContentVO() {
		super();
	}

	public ContentVO(int contentId, String externalId, String titleKo, String titleOrg, String overview,
			String releaseYear, int runtimeMin, String country, String posterUrl, String backdropUrl, String createdDt) {
		super();
		this.contentId = contentId;
		this.externalId = externalId;
		this.titleKo = titleKo;
		this.titleOrg = titleOrg;
		this.overview = overview;
		this.releaseYear = releaseYear;
		this.runtimeMin = runtimeMin;
		this.country = country;
		this.posterUrl = posterUrl;
		this.backdropUrl = backdropUrl;
		this.createdDt = createdDt;
	}

	public int getContentId() {
		return contentId;
	}

	public void setContentId(int contentId) {
		this.contentId = contentId;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getTitleKo() {
		return titleKo;
	}

	public void setTitleKo(String titleKo) {
		this.titleKo = titleKo;
	}

	public String getTitleOrg() {
		return titleOrg;
	}

	public void setTitleOrg(String titleOrg) {
		this.titleOrg = titleOrg;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String overview) {
		this.overview = overview;
	}

	public String getReleaseYear() {
		return releaseYear;
	}

	public void setReleaseYear(String releaseYear) {
		this.releaseYear = releaseYear;
	}

	public int getRuntimeMin() {
		return runtimeMin;
	}

	public void setRuntimeMin(int runtimeMin) {
		this.runtimeMin = runtimeMin;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getPosterUrl() {
		return posterUrl;
	}

	public void setPosterUrl(String posterUrl) {
		this.posterUrl = posterUrl;
	}

	public String getBackdropUrl() {
		return backdropUrl;
	}

	public void setBackdropUrl(String backdropUrl) {
		this.backdropUrl = backdropUrl;
	}

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	@Override
	public String toString() {
		return "ContentVO [contentId=" + contentId + ", externalId=" + externalId + ", titleKo=" + titleKo
				+ ", titleOrg=" + titleOrg + ", overview=" + overview + ", releaseYear=" + releaseYear + ", runtimeMin="
				+ runtimeMin + ", country=" + country + ", posterUrl=" + posterUrl + ", backdropUrl=" + backdropUrl
				+ ", createdDt=" + createdDt + ", toString()=" + super.toString() + "]";
	}

}
