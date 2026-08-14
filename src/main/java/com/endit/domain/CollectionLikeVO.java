package com.endit.domain;

/**
 * <pre>
 * Class Name  : CollectionLikeVO
 * Description : 회원이 좋아요를 누른 컬렉션 정보를 관리하는 VO
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13. gunwoo      최초 생성
 * 2026. 8. 14. jinyoung    주석 및 코드 형식 정리
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 13.
 */
public class CollectionLikeVO {

	private int memberId;          // 회원 번호
	private int collectionId;      // 컬렉션 번호
	private String createdDt;      // 좋아요 등록 일시

	public CollectionLikeVO() {
		super();
	}

	public CollectionLikeVO(int memberId, int collectionId, String createdDt) {
		super();
		this.memberId = memberId;
		this.collectionId = collectionId;
		this.createdDt = createdDt;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public int getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(int collectionId) {
		this.collectionId = collectionId;
	}

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	@Override
	public String toString() {
		return "CollectionLikeVO [memberId=" + memberId + ", collectionId=" + collectionId + ", createdDt="
				+ createdDt + "]";
	}
}