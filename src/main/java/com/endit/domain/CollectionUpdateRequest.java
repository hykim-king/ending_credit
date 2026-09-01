package com.endit.domain;

import java.util.List;

/**
 * <pre>
 * Class Name  : CollectionUpdateRequest
 * Description : 컬렉션 수정 요청 DTO
 *
 * 작성 회원 번호는 클라이언트 요청에서 받지 않는다.
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    title·description·isPublic·contentIds 수정 요청 정의
 * ------------------------------------------------------------
 * </pre>
 */
public class CollectionUpdateRequest {

	private String title;
	private String description;
	private String isPublic;
	private List<Integer> contentIds;

	public CollectionUpdateRequest() {
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(String isPublic) {
		this.isPublic = isPublic;
	}

	public List<Integer> getContentIds() {
		return contentIds;
	}

	public void setContentIds(List<Integer> contentIds) {
		this.contentIds = contentIds;
	}
}
