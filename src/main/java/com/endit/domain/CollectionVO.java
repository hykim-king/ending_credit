/**
 * <pre>
 * Class Name  : CollectionVO
 * Description : 회원이 생성한 콘텐츠 컬렉션 정보를 관리하는 VO
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 12.	jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 12.
 */
package com.endit.domain;

public class CollectionVO {

	private int collectionId;      // 컬렉션 번호
	private int memberId;          // 작성 회원 번호
	private String title;          // 컬렉션 제목
	private String description;    // 컬렉션 설명
	private String isPublic;       // 공개 여부(Y: 공개, N: 비공개)
	private String createdDt;      // 생성 일시
	private String updatedDt;      // 최종 수정 일시

	public CollectionVO() {
		super();
	}

	public CollectionVO(int collectionId, int memberId, String title, String description, String isPublic,
			String createdDt, String updatedDt) {
		super();
		this.collectionId = collectionId;
		this.memberId = memberId;
		this.title = title;
		this.description = description;
		this.isPublic = isPublic;
		this.createdDt = createdDt;
		this.updatedDt = updatedDt;
	}

	public int getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(int collectionId) {
		this.collectionId = collectionId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
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

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	public String getUpdatedDt() {
		return updatedDt;
	}

	public void setUpdatedDt(String updatedDt) {
		this.updatedDt = updatedDt;
	}

	@Override
	public String toString() {
		return "CollectionVO [collectionId=" + collectionId + ", memberId=" + memberId + ", title=" + title
				+ ", description=" + description + ", isPublic=" + isPublic + ", createdDt=" + createdDt
				+ ", updatedDt=" + updatedDt + "]";
	}
}
