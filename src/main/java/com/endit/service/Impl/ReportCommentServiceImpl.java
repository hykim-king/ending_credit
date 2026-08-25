/**
 * <pre>
 * Class Name : ReportCommentServiceImpl
 * Description : 코멘트 신고 Service 구현체
 *               - doSave: 사유가 OTHER면 상세 필수(CK_REPORT_OTHER_DETAIL)를 먼저 검증
 *               - upApproveReport: 승인 = 신고 상태만 ACCEPTED로 저장(팀 결정: 데이터 삭제 없음).
 *                 코멘트는 그대로 두고, 코멘트 조회가 "승인된 신고 존재"를 blindReason으로
 *                 실어 화면에서 사유별 안내 문구로 가린다
 *               - 검색조건은 10=처리상태/20=사유(문자), 30=코멘트ID(숫자) — 30만 숫자 검증
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
package com.endit.service.Impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.cmn.exception.ReportNotFoundException;
import com.endit.domain.ReportCommentVO;
import com.endit.mapper.ReportCommentMapper;
import com.endit.service.ReportCommentService;

@Service
public class ReportCommentServiceImpl implements ReportCommentService {

	final Logger log = LoggerFactory.getLogger(getClass());

	// 코멘트ID 검색 구분값 (매직넘버 해결)
	public static final String SEARCH_DIV_COMMENT_ID = "30";

	// ASCII 숫자 1~18자리만 허용 — 전각·아랍 숫자와 NUMBER 오버플로 차단 (UserCommentServiceImpl과 동일 기준)
	private static final String NUMERIC_PATTERN = "\\d{1,18}";

	private final ReportCommentMapper reportCommentMapper;

	public ReportCommentServiceImpl(ReportCommentMapper reportCommentMapper) {
		super();
		this.reportCommentMapper = reportCommentMapper;
		log.debug("reportCommentMapper: {}", reportCommentMapper);
	}

	@Override
	public List<ReportCommentVO> doRetrieve(DTO param) {
		log.debug("=============================");
		log.debug("{}()", "doRetrieve");
		log.debug("=============================");

		// 1. 코멘트ID 검색(30)만 숫자 컬럼 — 문자가 오면 ORA-01722가 나므로 먼저 거른다
		checkNumericSearchWord(param);

		// 2. 목록 조회
		return reportCommentMapper.doRetrieve(param);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int doSave(ReportCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doSave");
		log.debug("=============================");

		// 1. 사유가 OTHER면 상세 필수 — DB 제약(CK_REPORT_OTHER_DETAIL)이 최후 방어지만
		//    사용자에게 친절한 메시지를 주기 위해 먼저 거른다
		checkOtherReasonDetail(param);

		// 2. 접수 (처리상태는 DB DEFAULT 'RECEIVED')
		return reportCommentMapper.doSave(param);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int doUpdate(ReportCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doUpdate");
		log.debug("=============================");

		// 승인(ACCEPTED)은 코멘트 삭제까지 묶인 upApproveReport로만 한다 —
		// 일반 doUpdate로 ACCEPTED가 들어오면 "코멘트가 남는 승인"이 생겨 ⓑ안 불변식이 깨진다
		if (ReportCommentVO.STATUS_ACCEPTED.equals(param.getStatus())) {
			throw new IllegalArgumentException("승인 처리는 upApproveReport로만 할 수 있습니다.");
		}

		// 신고 처리(반려 등) — 상태·처리자·메모·처리일시가 한 UPDATE로 갱신된다
		return reportCommentMapper.doUpdate(param);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int doDelete(ReportCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doDelete");
		log.debug("=============================");

		return reportCommentMapper.doDelete(param);
	}

	@Override
	public ReportCommentVO doSelectOne(ReportCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doSelectOne");
		log.debug("=============================");

		return reportCommentMapper.doSelectOne(param);
	}

	@Override
	public int totalCnt() {
		log.debug("=============================");
		log.debug("{}()", "totalCnt");
		log.debug("=============================");

		return reportCommentMapper.totalCnt();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int upApproveReport(ReportCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "upApproveReport");
		log.debug("=============================");

		// 1. 신고 존재 확인
		ReportCommentVO outVO = reportCommentMapper.doSelectOne(param);
		if (null == outVO) {
			throw new ReportNotFoundException("신고가 존재하지 않습니다. reportId=" + param.getReportId());
		}

		// 2. 승인 처리 — 신고 상태만 ACCEPTED로 저장한다 (팀 결정: 데이터 삭제 없음.
		//    삭제하면 처리자·처리내용 기록이 무의미해지고, soft delete 미구현 상태의
		//    관리자 임의 hard delete는 위험하기 때문). 코멘트는 그대로 남고,
		//    코멘트 목록·단건조회가 blindReason으로 이 승인 건을 실어 화면에서 가린다.
		//    CK_REPORT_PROCESS_COMPLETE: 완료 상태면 처리자·처리일시 필수 —
		//    처리자 없이 승인하려 하면 여기서 DB가 거부하고 롤백된다.
		param.setStatus(ReportCommentVO.STATUS_ACCEPTED);
		int flag = reportCommentMapper.doUpdate(param);
		if (1 != flag) {
			throw new RuntimeException("신고 승인 처리에 실패했습니다.");
		}

		return 1;
	}

	/**
	 * 코멘트ID 검색(searchDiv 30)일 때만 검색어가 숫자인지 확인한다.
	 * (10=처리상태, 20=사유는 문자 코드값이라 검사하지 않는다)
	 *
	 * @param param
	 */
	private void checkNumericSearchWord(DTO param) {
		if (null == param || StringUtils.isEmpty(param.getSearchWord())) {
			return;
		}
		if (SEARCH_DIV_COMMENT_ID.equals(param.getSearchDiv())
				&& false == param.getSearchWord().matches(NUMERIC_PATTERN)) {
			throw new IllegalArgumentException("코멘트 번호 검색은 숫자만 입력할 수 있습니다: " + param.getSearchWord());
		}
	}

	/**
	 * 사유가 OTHER(기타)면 상세 내용이 있어야 한다.
	 *
	 * @param param
	 */
	private void checkOtherReasonDetail(ReportCommentVO param) {
		if (ReportCommentVO.REASON_OTHER.equals(param.getReason()) && StringUtils.isEmpty(param.getDetail())) {
			throw new IllegalArgumentException("기타(OTHER) 사유는 상세 내용을 입력해야 합니다.");
		}
	}

}
