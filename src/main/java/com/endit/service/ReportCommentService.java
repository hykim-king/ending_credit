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
	 * Description : 신고 승인 — 신고 상태만 ACCEPTED로 저장한다(팀 결정: 데이터 삭제 없음).
	 *               코멘트는 그대로 남고, 코멘트 조회가 승인된 신고의 사유(blindReason)를
	 *               실어 화면에서 사유별 안내 문구로 가린다.
	 *
	 * </pre>
	 *
	 * @param param (reportId, processedByMemberId, processNote 필수)
	 * @return 1(성공)
	 */
	int upApproveReport(ReportCommentVO param);

}
