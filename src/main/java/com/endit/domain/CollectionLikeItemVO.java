package com.endit.domain;

/**
 * <pre>
 * Class Name  : CollectionLikeItemVO
 * Description : 회원이 좋아요한 컬렉션 목록 화면에 표시할 정보를 담는 DTO
 *               (COLLECTION_LIKE + COLLECTION JOIN 결과)
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 18. gunwoo      최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 18.
 */
public class CollectionLikeItemVO {

	private int collectionId;      // 컬렉션 번호
	private String title;          // 컬렉션 제목
	private String description;    // 컬렉션 설명
	private String isPublic;       // 공개 여부 (Y/N)
	private String likedDt;        // 좋아요 등록 일시 (COLLECTION_LIKE.CREATED_DT)

	public CollectionLikeItemVO() {
		super();
	}

	public CollectionLikeItemVO(int collectionId, String title, String description, String isPublic,
			String likedDt) {
		super();
		this.collectionId = collectionId;
		this.title = title;
		this.description = description;
		this.isPublic = isPublic;
		this.likedDt = likedDt;
	}

	public int getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(int collectionId) {
		this.collectionId = collectionId;
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

	public String getLikedDt() {
		return likedDt;
	}

	public void setLikedDt(String likedDt) {
		this.likedDt = likedDt;
	}

	@Override
	public String toString() {
		return "CollectionLikeItemVO [collectionId=" + collectionId + ", title=" + title + ", description="
				+ description + ", isPublic=" + isPublic + ", likedDt=" + likedDt + "]";
	}

}