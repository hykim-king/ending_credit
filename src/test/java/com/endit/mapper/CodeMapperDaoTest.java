/**
 * 공통코드 조회 Mapper JUnit
 * 팀 테스트 규칙: 공용 더미 위에서 @Transactional 롤백 (조회 전용이라 쓰기 없음)
 */
package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.CodeUtil;
import com.endit.domain.CodeVO;
import com.endit.domain.ReportCommentVO;

@SpringBootTest
@Transactional
@DisplayName("CodeMapper 테스트")
class CodeMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	CodeMapper mapper;

	@Test
	public void doRetrieve() {
		log.debug("---------------------------");
		log.debug("*doRetrieve()*");
		log.debug("---------------------------");
		// 신고 화면이 쓰는 두 그룹을 IN절 한 번으로 조회
		// 1. 두 그룹 조회
		// 2. CodeUtil로 그룹별 분리
		// 3. 신고 사유 4종(CHECK 제약과 동일 값)이 전부 들어 있는지 확인

		// 1.
		String[] codeStr = { "REPORT_REASON", "REPORT_STATUS" };
		Map<String, Object> codeMap = new HashMap<>();
		codeMap.put("code", codeStr);

		List<CodeVO> codeList = mapper.doRetrieve(codeMap);
		assertNotNull(codeList);
		for (CodeVO vo : codeList) {
			log.debug(vo.toString());
		}

		// 2.
		List<CodeVO> reasonList = CodeUtil.getCodeList(codeList, "REPORT_REASON");
		List<CodeVO> statusList = CodeUtil.getCodeList(codeList, "REPORT_STATUS");
		assertTrue(reasonList.size() > 0);
		assertTrue(statusList.size() > 0);

		// 3. (개수 단정 대신 포함 여부로 — 더미가 늘어도 안 깨지게)
		String[] reasons = { ReportCommentVO.REASON_SPOILER, ReportCommentVO.REASON_INAPPROPRIATE,
				ReportCommentVO.REASON_SPAM, ReportCommentVO.REASON_OTHER };
		for (String reason : reasons) {
			assertTrue(reasonList.stream().anyMatch(vo -> reason.equals(vo.getCode())),
					"REPORT_REASON에 " + reason + "이 없음");
		}
		String[] statuses = { ReportCommentVO.STATUS_RECEIVED, ReportCommentVO.STATUS_PROCESSING,
				ReportCommentVO.STATUS_ACCEPTED, ReportCommentVO.STATUS_REJECTED };
		for (String status : statuses) {
			assertTrue(statusList.stream().anyMatch(vo -> status.equals(vo.getCode())),
					"REPORT_STATUS에 " + status + "가 없음");
		}
	}

	@Test
	void beans() {
		log.debug("---------------------------");
		log.debug("*beans()*");
		log.debug("---------------------------");
		assertNotNull(mapper);
		log.debug("mapper: {}", mapper);
	}

}
