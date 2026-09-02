package com.endit.domain;

import java.util.List;

// AD-03 영화 등록의 단일 POST 페이로드(API-052) - content, genreIds, imageUrls, credits를 한 번에 받는다
public class AdminContentCreateRequest {

	private ContentVO content;
	private List<Integer> genreIds;
	private List<String> imageUrls;
	private List<ContentCreditVO> credits;

	public AdminContentCreateRequest() {
		super();
	}

	public ContentVO getContent() {
		return content;
	}

	public void setContent(ContentVO content) {
		this.content = content;
	}

	public List<Integer> getGenreIds() {
		return genreIds;
	}

	public void setGenreIds(List<Integer> genreIds) {
		this.genreIds = genreIds;
	}

	public List<String> getImageUrls() {
		return imageUrls;
	}

	public void setImageUrls(List<String> imageUrls) {
		this.imageUrls = imageUrls;
	}

	public List<ContentCreditVO> getCredits() {
		return credits;
	}

	public void setCredits(List<ContentCreditVO> credits) {
		this.credits = credits;
	}

	@Override
	public String toString() {
		return "AdminContentCreateRequest [content=" + content + ", genreIds=" + genreIds
				+ ", imageUrls=" + imageUrls + ", credits=" + credits + "]";
	}

}
