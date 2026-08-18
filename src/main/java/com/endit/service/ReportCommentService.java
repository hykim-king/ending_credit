/**
 * <pre>
 * Class Name : ReportCommentService
 * Description : 코멘트 신고 Service 인터페이스
 *               CRUD 계약은 WorkDiv를 그대로 상속한다(학원 표준).
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 18.  홍선기   최초 생성
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 18.
 */
package com.endit.service;

import com.endit.cmn.WorkDiv;
import com.endit.domain.ReportCommentVO;

public interface ReportCommentService extends WorkDiv<ReportCommentVO> {

	/**
	 *
	 * <pre>
	 * Method Name : totalCnt
	 * Description : 신고 총건수
	 *
	 * </pre>
	 *
	 * @return int(총건수)
	 */
	int totalCnt();

	/**
	 *
	 * <pre>
	 * Method Name : upApproveReport
	 * Description : 신고 승인 — 승인 처리(상태·처리자·처리일시)와 대상 코멘트 삭제를
	 *               한 트랜잭션으로 조합한다(조합 메서드는 up~ 접두).
	 *               팀 잠정 결정 ⓑ안: 코멘트를 삭제하면 이 신고 이력도
	 *               FK ON DELETE CASCADE로 함께 사라지는 것을 수용한다.
	 *
	 * </pre>
	 *
	 * @param param (reportId, processedByMemberId, processNote 필수)
	 * @return 1(성공)
	 */
	int upApproveReport(ReportCommentVO param);

}
