/**
 * 코멘트 신고 Mapper
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
}
