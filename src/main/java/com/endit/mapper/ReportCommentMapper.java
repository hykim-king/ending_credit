/**
 * <pre>
 * Class Name : ReportCommentMapper
 * Description : 코멘트 신고 Mapper
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
package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.WorkDiv;
import com.endit.domain.ReportCommentVO;

@Mapper // MyBatis 매퍼 인터페이스임을 선언(구현체는 MyBatis가 자동 생성)
public interface ReportCommentMapper extends WorkDiv<ReportCommentVO> {
	// doSave    : 신고 접수 (상태는 DB DEFAULT 'RECEIVED')
	// doUpdate  : 신고 처리 — CK_REPORT_PROCESS_COMPLETE 때문에
	//             상태·처리자·처리일시를 반드시 한 UPDATE로 갱신한다
	// doSelectOne/doRetrieve/doDelete : WorkDiv 표준

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
	 * Method Name : deleteAll
	 * Description : JUnit전용: 모든 신고 삭제
	 *               ⚠️ 로컬 개발 DB에서만 사용할 것
	 * </pre>
	 *
	 * @return int(삭제건수)
	 */
	int deleteAll();
}
