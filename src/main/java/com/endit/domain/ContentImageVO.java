package com.endit.domain;

import com.endit.cmn.DTO;

public class ContentImageVO extends DTO {

	private int imageId;
	private int contentId;
	private String imageUrl;
	private String createdDt;

	public ContentImageVO() {
		super();
	}

	public ContentImageVO(int imageId, int contentId, String imageUrl, String createdDt) {
		super();
		this.imageId = imageId;
		this.contentId = contentId;
		this.imageUrl = imageUrl;
		this.createdDt = createdDt;
	}

	public int getImageId() {
		return imageId;
	}

	public void setImageId(int imageId) {
		this.imageId = imageId;
	}

	public int getContentId() {
		return contentId;
	}

	public void setContentId(int contentId) {
		this.contentId = contentId;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	@Override
	public String toString() {
		return "ContentImageVO [imageId=" + imageId + ", contentId=" + contentId + ", imageUrl=" + imageUrl
				+ ", createdDt=" + createdDt + ", toString()=" + super.toString() + "]";
	}

}
