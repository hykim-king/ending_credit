/**
 * 코멘트 좋아요(COMMENT_LIKE) VO
 * PK가 (MEMBER_ID, COMMENT_ID) 복합키라 같은 코멘트에 중복 좋아요가 DB에서 막힌다.
 */
package com.endit.domain;

import com.endit.cmn.DTO;

public class CommentLikeVO extends DTO {

	private long memberId;    // 좋아요 누른 회원ID (PK 1)
	private long commentId;   // 대상 코멘트ID (PK 2)
	private String createdDt; // 좋아요 누른 일시

	public CommentLikeVO() {
		super();
	}

	public CommentLikeVO(long memberId, long commentId, String createdDt) {
		super();
		this.memberId = memberId;
		this.commentId = commentId;
		this.createdDt = createdDt;
	}

	public long getMemberId() {
		return memberId;
	}

	public void setMemberId(long memberId) {
		this.memberId = memberId;
	}

	public long getCommentId() {
		return commentId;
	}

	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	@Override
	public String toString() {
		return "CommentLikeVO [memberId=" + memberId + ", commentId=" + commentId + ", createdDt=" + createdDt
				+ ", toString()=" + super.toString() + "]";
	}

}
