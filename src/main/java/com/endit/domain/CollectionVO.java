package com.endit.domain;

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
 * 2026. 8. 19. jinyoung    조회용 작성자 및 집계 정보 추가
 * 2026. 9. 01. jinyoung    목록 콜라주용 대표 포스터 정보 추가
 * 2026. 9. 01. jinyoung    목록 작성자 프로필 이미지 정보 추가
 * 2026. 9. 02. jinyoung    현재 회원의 목록 좋아요 여부 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 12.
 */
public class CollectionVO {

	private int collectionId;      // 컬렉션 번호
	private int memberId;          // 작성 회원 번호
	private String title;          // 컬렉션 제목
	private String description;    // 컬렉션 설명
	private String isPublic;       // 공개 여부(Y: 공개, N: 비공개)
	private String createdDt;      // 생성 일시
	private String updatedDt;      // 최종 수정 일시
	private String nickname;       // 작성자 닉네임
	private String profileImgUrl;  // 작성자 프로필 이미지 URL
	private int itemCount;         // 포함 작품 수
	private int likeCount;         // 좋아요 수
	private boolean likedByCurrentMember; // 현재 회원의 좋아요 여부
	private int commentCount;      // 코멘트 수
	private String previewPosterUrl1; // 목록 대표 포스터 URL 1
	private String previewPosterUrl2; // 목록 대표 포스터 URL 2
	private String previewPosterUrl3; // 목록 대표 포스터 URL 3
	private String previewPosterUrl4; // 목록 대표 포스터 URL 4
	private String previewPosterUrl5; // 목록 대표 포스터 URL 5

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

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getProfileImgUrl() {
		return profileImgUrl;
	}

	public void setProfileImgUrl(String profileImgUrl) {
		this.profileImgUrl = profileImgUrl;
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public int getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(int likeCount) {
		this.likeCount = likeCount;
	}

	public boolean isLikedByCurrentMember() {
		return likedByCurrentMember;
	}

	public void setLikedByCurrentMember(boolean likedByCurrentMember) {
		this.likedByCurrentMember = likedByCurrentMember;
	}

	public int getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}

	public String getPreviewPosterUrl1() {
		return previewPosterUrl1;
	}

	public void setPreviewPosterUrl1(String previewPosterUrl1) {
		this.previewPosterUrl1 = previewPosterUrl1;
	}

	public String getPreviewPosterUrl2() {
		return previewPosterUrl2;
	}

	public void setPreviewPosterUrl2(String previewPosterUrl2) {
		this.previewPosterUrl2 = previewPosterUrl2;
	}

	public String getPreviewPosterUrl3() {
		return previewPosterUrl3;
	}

	public void setPreviewPosterUrl3(String previewPosterUrl3) {
		this.previewPosterUrl3 = previewPosterUrl3;
	}

	public String getPreviewPosterUrl4() {
		return previewPosterUrl4;
	}

	public void setPreviewPosterUrl4(String previewPosterUrl4) {
		this.previewPosterUrl4 = previewPosterUrl4;
	}

	public String getPreviewPosterUrl5() {
		return previewPosterUrl5;
	}

	public void setPreviewPosterUrl5(String previewPosterUrl5) {
		this.previewPosterUrl5 = previewPosterUrl5;
	}

	@Override
	public String toString() {
		return "CollectionVO [collectionId=" + collectionId + ", memberId=" + memberId + ", title=" + title
				+ ", description=" + description + ", isPublic=" + isPublic + ", createdDt=" + createdDt
				+ ", updatedDt=" + updatedDt + ", nickname=" + nickname + ", profileImgUrl=" + profileImgUrl
				+ ", itemCount=" + itemCount + ", likeCount=" + likeCount + ", likedByCurrentMember="
				+ likedByCurrentMember + ", commentCount=" + commentCount + ", previewPosterUrl1="
				+ previewPosterUrl1 + ", previewPosterUrl2=" + previewPosterUrl2
				+ ", previewPosterUrl3=" + previewPosterUrl3 + ", previewPosterUrl4=" + previewPosterUrl4
				+ ", previewPosterUrl5=" + previewPosterUrl5 + "]";
	}
}
