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

	@Override
	public String toString() {
		return "UserCommentVO [commentId=" + commentId + ", memberId=" + memberId + ", contentId=" + contentId
				+ ", collectionId=" + collectionId + ", commentDetail=" + commentDetail + ", spoiler=" + spoiler
				+ ", createdDt=" + createdDt + ", updatedDt=" + updatedDt + ", toString()=" + super.toString() + "]";
	}

}
