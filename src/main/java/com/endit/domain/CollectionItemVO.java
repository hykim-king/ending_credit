package com.endit.domain;

/**
 * <pre>
 * Class Name  : CollectionItemVO
 * Description : 컬렉션에 포함된 콘텐츠 정보를 관리하는 VO
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
public class CollectionItemVO {

	private int collectionId;    // 컬렉션 번호
	private int contentId;       // 콘텐츠 번호
	private String addedDt;      // 콘텐츠 추가 일시

	public CollectionItemVO() {
		super();
	}

	public CollectionItemVO(int collectionId, int contentId, String addedDt) {
		super();
		this.collectionId = collectionId;
		this.contentId = contentId;
		this.addedDt = addedDt;
	}

	public int getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(int collectionId) {
		this.collectionId = collectionId;
	}

	public int getContentId() {
		return contentId;
	}

	public void setContentId(int contentId) {
		this.contentId = contentId;
	}

	public String getAddedDt() {
		return addedDt;
	}

	public void setAddedDt(String addedDt) {
		this.addedDt = addedDt;
	}

	@Override
	public String toString() {
		return "CollectionItemVO [collectionId=" + collectionId + ", contentId=" + contentId + ", addedDt="
				+ addedDt + "]";
	}
}
