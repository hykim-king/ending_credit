package com.endit.domain;

/**
 * <pre>
 * Class Name  : CollectionCommentCreateRequest
 * Description : 컬렉션 상세 댓글 등록 요청 DTO
 *
 * 작성 회원 번호와 컬렉션 번호는 클라이언트 요청 본문에서 받지 않는다.
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 9. 05. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 */
public class CollectionCommentCreateRequest {

	private String commentDetail;
	private String spoiler;

	public CollectionCommentCreateRequest() {
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
}
