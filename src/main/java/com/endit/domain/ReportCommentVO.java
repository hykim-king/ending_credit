/**
 * <pre>
 * Class Name : ReportCommentVO
 * Description : 코멘트 신고(REPORT_COMMENT) VO
 *               신고 대상은 코멘트뿐이다(COMMENT_ID NOT NULL).
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

public class ReportCommentVO extends DTO {

	// 신고 사유 (CK_REPORT_REASON) — OTHER는 상세 내용(detail) 필수 (CK_REPORT_OTHER_DETAIL)
	public static final String REASON_SPOILER = "SPOILER";
	public static final String REASON_INAPPROPRIATE = "INAPPROPRIATE";
	public static final String REASON_SPAM = "SPAM";
	public static final String REASON_OTHER = "OTHER";

	// 처리 상태 (CK_REPORT_STATUS) — 접수 시 DB DEFAULT가 RECEIVED를 넣는다
	public static final String STATUS_RECEIVED = "RECEIVED";
	public static final String STATUS_PROCESSING = "PROCESSING";
	public static final String STATUS_ACCEPTED = "ACCEPTED";
	public static final String STATUS_REJECTED = "REJECTED";

	private long reportId;             // 신고ID (PK, SEQ_REPORT 채번)
	private long reportMemberId;       // 신고자 회원ID
	private long commentId;            // 신고 대상 코멘트ID
	private String reason;             // 신고 사유
	private String detail;             // 신고 상세 내용 (OTHER면 필수)
	private String status;             // 처리 상태
	private Long processedByMemberId;  // 처리한 관리자 회원ID (완료 전이면 null)
	private String processNote;        // 처리 내용 메모
	private String createdDt;          // 신고 접수일
	private String processedDt;        // 처리 완료일

	public ReportCommentVO() {
		super();
	}

	public ReportCommentVO(long reportId, long reportMemberId, long commentId, String reason, String detail,
			String status, Long processedByMemberId, String processNote, String createdDt, String processedDt) {
		super();
		this.reportId = reportId;
		this.reportMemberId = reportMemberId;
		this.commentId = commentId;
		this.reason = reason;
		this.detail = detail;
		this.status = status;
		this.processedByMemberId = processedByMemberId;
		this.processNote = processNote;
		this.createdDt = createdDt;
		this.processedDt = processedDt;
	}

	public long getReportId() {
		return reportId;
	}

	public void setReportId(long reportId) {
		this.reportId = reportId;
	}

	public long getReportMemberId() {
		return reportMemberId;
	}

	public void setReportMemberId(long reportMemberId) {
		this.reportMemberId = reportMemberId;
	}

	public long getCommentId() {
		return commentId;
	}

	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getProcessedByMemberId() {
		return processedByMemberId;
	}

	public void setProcessedByMemberId(Long processedByMemberId) {
		this.processedByMemberId = processedByMemberId;
	}

	public String getProcessNote() {
		return processNote;
	}

	public void setProcessNote(String processNote) {
		this.processNote = processNote;
	}

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	public String getProcessedDt() {
		return processedDt;
	}

	public void setProcessedDt(String processedDt) {
		this.processedDt = processedDt;
	}

	@Override
	public String toString() {
		return "ReportCommentVO [reportId=" + reportId + ", reportMemberId=" + reportMemberId + ", commentId="
				+ commentId + ", reason=" + reason + ", detail=" + detail + ", status=" + status
				+ ", processedByMemberId=" + processedByMemberId + ", processNote=" + processNote + ", createdDt="
				+ createdDt + ", processedDt=" + processedDt + ", toString()=" + super.toString() + "]";
	}

}
