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
 * 2026. 9. 05.  이진영   댓글 대상·작성자 프로필·로그인 회원 좋아요 정보 추가
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

	/** 코멘트 정보 생성 */
	public UserCommentVO() {
		super();
	}

	/**
	 * 코멘트 정보 생성
	 *
	 * @param commentId 코멘트 번호
	 * @param memberId 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @param collectionId 컬렉션 번호
	 * @param commentDetail 코멘트 내용
	 * @param spoiler 스포일러 여부 (Y/N)
	 * @param createdDt 등록 일시
	 * @param updatedDt 수정 일시
	 */
	public UserCommentVO(
			long commentId,
			long memberId,
			Long contentId,
			Long collectionId,
			String commentDetail,
			String spoiler,
			String createdDt,
			String updatedDt) {
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

	/**
	 * 코멘트 번호 조회
	 *
	 * @return 코멘트 번호
	 */
	public long getCommentId() {
		return commentId;
	}

	/**
	 * 코멘트 번호 설정
	 *
	 * @param commentId 코멘트 번호
	 */
	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}

	/**
	 * 회원 번호 조회
	 *
	 * @return 회원 번호
	 */
	public long getMemberId() {
		return memberId;
	}

	/**
	 * 회원 번호 설정
	 *
	 * @param memberId 회원 번호
	 */
	public void setMemberId(long memberId) {
		this.memberId = memberId;
	}

	/**
	 * 콘텐츠 번호 조회
	 *
	 * @return 콘텐츠 번호, 해당하지 않으면 null
	 */
	public Long getContentId() {
		return contentId;
	}

	/**
	 * 콘텐츠 번호 설정
	 *
	 * @param contentId 콘텐츠 번호
	 */
	public void setContentId(Long contentId) {
		this.contentId = contentId;
	}

	/**
	 * 컬렉션 번호 조회
	 *
	 * @return 컬렉션 번호, 해당하지 않으면 null
	 */
	public Long getCollectionId() {
		return collectionId;
	}

	/**
	 * 컬렉션 번호 설정
	 *
	 * @param collectionId 컬렉션 번호
	 */
	public void setCollectionId(Long collectionId) {
		this.collectionId = collectionId;
	}

	/**
	 * 코멘트 내용 조회
	 *
	 * @return 코멘트 내용
	 */
	public String getCommentDetail() {
		return commentDetail;
	}

	/**
	 * 코멘트 내용 설정
	 *
	 * @param commentDetail 코멘트 내용
	 */
	public void setCommentDetail(String commentDetail) {
		this.commentDetail = commentDetail;
	}

	/**
	 * 스포일러 여부 (Y/N) 조회
	 *
	 * @return 스포일러 여부 (Y/N)
	 */
	public String getSpoiler() {
		return spoiler;
	}

	/**
	 * 스포일러 여부 (Y/N) 설정
	 *
	 * @param spoiler 스포일러 여부 (Y/N)
	 */
	public void setSpoiler(String spoiler) {
		this.spoiler = spoiler;
	}

	/**
	 * 등록 일시 조회
	 *
	 * @return 등록 일시
	 */
	public String getCreatedDt() {
		return createdDt;
	}

	/**
	 * 등록 일시 설정
	 *
	 * @param createdDt 등록 일시
	 */
	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	/**
	 * 수정 일시 조회
	 *
	 * @return 수정 일시
	 */
	public String getUpdatedDt() {
		return updatedDt;
	}

	/**
	 * 수정 일시 설정
	 *
	 * @param updatedDt 수정 일시
	 */
	public void setUpdatedDt(String updatedDt) {
		this.updatedDt = updatedDt;
	}

	/**
	 * 작성자 닉네임 조회
	 *
	 * @return 작성자 닉네임
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * 작성자 닉네임 설정
	 *
	 * @param nickname 작성자 닉네임
	 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	/**
	 * 작성자 프로필 이미지 URL 조회
	 *
	 * @return 작성자 프로필 이미지 URL
	 */
	public String getProfileImgUrl() {
		return profileImgUrl;
	}

	/**
	 * 작성자 프로필 이미지 URL 설정
	 *
	 * @param profileImgUrl 작성자 프로필 이미지 URL
	 */
	public void setProfileImgUrl(String profileImgUrl) {
		this.profileImgUrl = profileImgUrl;
	}

	/**
	 * 좋아요 수 조회
	 *
	 * @return 좋아요 수
	 */
	public int getLikeCnt() {
		return likeCnt;
	}

	/**
	 * 좋아요 수 설정
	 *
	 * @param likeCnt 좋아요 수
	 */
	public void setLikeCnt(int likeCnt) {
		this.likeCnt = likeCnt;
	}

	/**
	 * 별점 조회
	 *
	 * @return 별점, 해당하지 않으면 null
	 */
	public Integer getRatingScore() {
		return ratingScore;
	}

	/**
	 * 별점 설정
	 *
	 * @param ratingScore 별점
	 */
	public void setRatingScore(Integer ratingScore) {
		this.ratingScore = ratingScore;
	}

	/**
	 * 신고 승인에 따른 가림 사유 조회
	 *
	 * @return 신고 승인에 따른 가림 사유
	 */
	public String getBlindReason() {
		return blindReason;
	}

	/**
	 * 신고 승인에 따른 가림 사유 설정
	 *
	 * @param blindReason 신고 승인에 따른 가림 사유
	 */
	public void setBlindReason(String blindReason) {
		this.blindReason = blindReason;
	}

	/**
	 * 대상 유형 (MOVIE/COLLECTION) 조회
	 *
	 * @return 대상 유형 (MOVIE/COLLECTION)
	 */
	public String getTargetType() {
		return targetType;
	}

	/**
	 * 대상 유형 (MOVIE/COLLECTION) 설정
	 *
	 * @param targetType 대상 유형 (MOVIE/COLLECTION)
	 */
	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	/**
	 * 영화 또는 컬렉션 제목 조회
	 *
	 * @return 영화 또는 컬렉션 제목
	 */
	public String getTargetTitle() {
		return targetTitle;
	}

	/**
	 * 영화 또는 컬렉션 제목 설정
	 *
	 * @param targetTitle 영화 또는 컬렉션 제목
	 */
	public void setTargetTitle(String targetTitle) {
		this.targetTitle = targetTitle;
	}

	/**
	 * 영화 개봉 연도 조회
	 *
	 * @return 영화 개봉 연도, 해당하지 않으면 null
	 */
	public String getReleaseYear() {
		return releaseYear;
	}

	/**
	 * 영화 개봉 연도 설정
	 *
	 * @param releaseYear 영화 개봉 연도
	 */
	public void setReleaseYear(String releaseYear) {
		this.releaseYear = releaseYear;
	}

	/**
	 * 컬렉션 작성자 닉네임 조회
	 *
	 * @return 컬렉션 작성자 닉네임
	 */
	public String getCollectionAuthorNickname() {
		return collectionAuthorNickname;
	}

	/**
	 * 컬렉션 작성자 닉네임 설정
	 *
	 * @param collectionAuthorNickname 컬렉션 작성자 닉네임
	 */
	public void setCollectionAuthorNickname(String collectionAuthorNickname) {
		this.collectionAuthorNickname = collectionAuthorNickname;
	}

	/**
	 * 로그인 회원의 좋아요 여부 조회
	 *
	 * @return 로그인 회원의 좋아요 여부
	 */
	public boolean isLikedByMember() {
		return likedByMember;
	}

	/**
	 * 로그인 회원의 좋아요 여부 설정
	 *
	 * @param likedByMember 로그인 회원의 좋아요 여부
	 */
	public void setLikedByMember(boolean likedByMember) {
		this.likedByMember = likedByMember;
	}

	/**
	 * function toString() { [native code] } 조회
	 *
	 * @return function toString() { [native code] }
	 */
	@Override
	public String toString() {
		return "UserCommentVO [commentId=" + commentId + ", memberId=" + memberId + ", contentId=" + contentId
				+ ", collectionId=" + collectionId + ", commentDetail=" + commentDetail + ", spoiler=" + spoiler
				+ ", createdDt=" + createdDt + ", updatedDt=" + updatedDt + ", nickname=" + nickname + ", profileImgUrl=" + profileImgUrl
				+ ", likeCnt=" + likeCnt + ", ratingScore=" + ratingScore + ", blindReason=" + blindReason
				+ ", targetType=" + targetType + ", targetTitle=" + targetTitle + ", releaseYear=" + releaseYear
				+ ", collectionAuthorNickname=" + collectionAuthorNickname + ", likedByMember=" + likedByMember
				+ ", toString()=" + super.toString() + "]";
	}

}
