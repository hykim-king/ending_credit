/**
 * <pre>
 * Class Name : UserCommentVO
 * Description : 코멘트(USER_COMMENT) VO
 *               영화(CONTENT) 또는 컬렉션(COLLECTION) 중 한쪽에만 다는 한줄평.
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 12.  홍선기   최초 생성
 * 2026. 8. 19.  홍선기   조회 전용 join 필드 추가(작성자 닉네임·좋아요 수·별점) — 8/18 공지 보완점2
 * 2026. 9. 05.  이진영   회원 기록 댓글의 대상 정보·로그인 회원 좋아요 여부 추가
 * 2026. 9. 05.  이진영   컬렉션 상세 댓글 작성자 프로필 이미지 추가
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 12.
 */
package com.endit.domain;

import com.endit.cmn.DTO;

public class UserCommentVO extends DTO {

	// 스포일러 여부 값 (CK_USER_COMMENT_SPOILER: 'Y'/'N')
	public static final String SPOILER_YES = "Y";
	public static final String SPOILER_NO = "N";

	private long commentId;       // 코멘트ID (PK, SEQ_USER_COMMENT 채번)
	private long memberId;        // 작성자 회원ID
	private Long contentId;       // 대상 영화ID (영화 코멘트일 때만, 아니면 null)
	private Long collectionId;    // 대상 컬렉션ID (컬렉션 코멘트일 때만, 아니면 null)
	private String commentDetail; // 코멘트 내용 (COMMENT는 오라클 예약어라 컬럼명이 COMMENT_DETAIL)
	private String spoiler;       // 스포일러 여부 'Y'/'N'
	private String createdDt;     // 등록일
	private String updatedDt;     // 수정일

	// ── 조회 전용(join 결과) — 목록/단건조회에서만 채워진다. 등록·수정 파라미터로는 쓰지 않는다 ──
	private String nickname;      // 작성자 닉네임 (MEMBER join)
	private String profileImgUrl; // 작성자 프로필 이미지 URL (MEMBER join)
	private int likeCnt;          // 좋아요 수 (COMMENT_LIKE 집계, 없으면 0)
	private Integer ratingScore;  // 작성자가 그 영화에 준 별점 (MEMBER_CONTENT join — 없거나 컬렉션 코멘트면 null)
	private String blindReason;   // 승인(ACCEPTED)된 신고의 사유 — 값이 있으면 화면에서 안내 문구로 가린다(팀 결정: 삭제 없음)
	private String targetType;    // 대상 유형(MOVIE 또는 COLLECTION)
	private String targetTitle;   // 영화 또는 컬렉션 제목
	private String releaseYear;   // 영화 개봉 연도(컬렉션 코멘트면 null)
	private String collectionAuthorNickname; // 컬렉션 작성자 닉네임(영화 코멘트면 null)
	private boolean likedByMember; // 조회 중인 로그인 회원의 좋아요 여부

	public UserCommentVO() {
		super();
	}

	public UserCommentVO(long commentId, long memberId, Long contentId, Long collectionId, String commentDetail,
			String spoiler, String createdDt, String updatedDt) {
		super();
		this.commentId = commentId;
		this.memberId = memberId;
		this.contentId = contentId;
		this.collectionId = collectionId;
		this.commentDetail = commentDetail;
		this.spoiler = spoiler;
		this.createdDt = createdDt;
		this.updatedDt = updatedDt;
	}

	public long getCommentId() {
		return commentId;
	}

	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}

	public long getMemberId() {
		return memberId;
	}

	public void setMemberId(long memberId) {
		this.memberId = memberId;
	}

	public Long getContentId() {
		return contentId;
	}

	public void setContentId(Long contentId) {
		this.contentId = contentId;
	}

	public Long getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(Long collectionId) {
		this.collectionId = collectionId;
	}

	public String getCommentDetail() {
		return commentDetail;
	}

	public void setCommentDetail(String commentDetail) {
		this.commentDetail = commentDetail;
	}

	public String getSpoiler() {
		return spoiler;
	}

	public void setSpoiler(String spoiler) {
		this.spoiler = spoiler;
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

	public int getLikeCnt() {
		return likeCnt;
	}

	public void setLikeCnt(int likeCnt) {
		this.likeCnt = likeCnt;
	}

	public Integer getRatingScore() {
		return ratingScore;
	}

	public void setRatingScore(Integer ratingScore) {
		this.ratingScore = ratingScore;
	}

	public String getBlindReason() {
		return blindReason;
	}

	public void setBlindReason(String blindReason) {
		this.blindReason = blindReason;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public String getTargetTitle() {
		return targetTitle;
	}

	public void setTargetTitle(String targetTitle) {
		this.targetTitle = targetTitle;
	}

	public String getReleaseYear() {
		return releaseYear;
	}

	public void setReleaseYear(String releaseYear) {
		this.releaseYear = releaseYear;
	}

	public String getCollectionAuthorNickname() {
		return collectionAuthorNickname;
	}

	public void setCollectionAuthorNickname(String collectionAuthorNickname) {
		this.collectionAuthorNickname = collectionAuthorNickname;
	}

	public boolean isLikedByMember() {
		return likedByMember;
	}

	public void setLikedByMember(boolean likedByMember) {
		this.likedByMember = likedByMember;
	}

	@Override
	public String toString() {
		return "UserCommentVO [commentId=" + commentId + ", memberId=" + memberId + ", contentId=" + contentId
				+ ", collectionId=" + collectionId + ", commentDetail=" + commentDetail + ", spoiler=" + spoiler
				+ ", createdDt=" + createdDt + ", updatedDt=" + updatedDt + ", nickname=" + nickname
				+ ", profileImgUrl=" + profileImgUrl
				+ ", likeCnt=" + likeCnt + ", ratingScore=" + ratingScore + ", blindReason=" + blindReason
				+ ", targetType=" + targetType + ", targetTitle=" + targetTitle + ", releaseYear=" + releaseYear
				+ ", collectionAuthorNickname=" + collectionAuthorNickname + ", likedByMember=" + likedByMember
				+ ", toString()=" + super.toString() + "]";
	}

}
