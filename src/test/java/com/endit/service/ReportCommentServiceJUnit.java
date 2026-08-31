/**
 * 코멘트 신고 Service JUnit
 * 서비스 고유 로직 — OTHER 사유 사전검증, 승인 조합(upApproveReport),
 * 코멘트ID 검색 숫자 검증 — 을 검증한다.
 * 팀 테스트 규칙: 공용 더미(테이블당 10건) 위에서 @Transactional 롤백.
 */
package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.cmn.exception.ReportNotFoundException;
import com.endit.domain.ReportCommentVO;
import com.endit.domain.UserCommentVO;
import com.endit.mapper.ReportCommentMapper;
import com.endit.mapper.UserCommentMapper;

@SpringBootTest
@Transactional
@DisplayName("ReportCommentService 테스트")
class ReportCommentServiceJUnit {

	final Logger log = LoggerFactory.getLogger(getClass());

	// 공용 DB 더미 데이터의 실제 부모 값 (회의 규칙: 부모 값은 DB 기반 하드코딩)
	private static final long MEMBER_AUTHOR = 9L;     // admin1@endit.com — 더미 코멘트 없음
	private static final long MEMBER_REPORTER = 10L;  // admin2@endit.com — 신고자
	private static final long ADMIN_PROCESSOR = 9L;   // 신고 처리 관리자 (ROLE=ADMIN)
	private static final long CONTENT_A = 9L;          // 어벤져스: 인피니티 워 — 더미 코멘트 없는 영화
	private static final long MISSING_REPORT_ID = 999_999_999L; // 존재하지 않는 신고 번호

	@Autowired
	ReportCommentService service;

	@Autowired
	ReportCommentMapper reportMapper; // 사후검증용

	@Autowired
	UserCommentMapper commentMapper; // 사전조건(대상 코멘트) 구성·사후검증용

	private UserCommentVO comment01;  // 신고 대상 코멘트 (트랜잭션 안에서 생성)
	private ReportCommentVO report01; // 회원10이 comment01을 스포일러로 신고

	private DTO dto; // 검색

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		comment01 = new UserCommentVO(0, MEMBER_AUTHOR, CONTENT_A, null, "신고 대상 한줄평", UserCommentVO.SPOILER_NO,
				null, null);
		commentMapper.doSave(comment01);

		report01 = new ReportCommentVO(0, MEMBER_REPORTER, comment01.getCommentId(), ReportCommentVO.REASON_SPOILER,
				"스포일러 표시 없이 결말을 언급했습니다", null, null, null, null, null);

		dto = new DTO();

		log.debug("comment01: {}", comment01);
		log.debug("report01: {}", report01);
	}

	@Test
	public void otherReasonNeedsDetail() {
		log.debug("---------------------------");
		log.debug("*otherReasonNeedsDetail()*");
		log.debug("---------------------------");
		// 사유가 OTHER면 상세 필수 — DB 제약까지 가기 전에 서비스가 먼저 거른다
		// (메시지 문구로 서비스 검증인지 확인)
		ReportCommentVO noDetail = new ReportCommentVO(0, MEMBER_REPORTER, comment01.getCommentId(),
				ReportCommentVO.REASON_OTHER, null, null, null, null, null, null);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.doSave(noDetail));
		log.debug("message: {}", e.getMessage());
		assertTrue(e.getMessage().contains("기타"));
	}

	@Test
	public void upApproveReport() {
		log.debug("---------------------------");
		log.debug("*upApproveReport()*");
		log.debug("---------------------------");
		// 승인(팀 결정): 신고 상태만 ACCEPTED로 저장 — 데이터 삭제 없음.
		// 코멘트는 그대로 남고, 조회 시 blindReason(승인된 신고 사유)이 실려
		// 화면에서 안내 문구로 가려진다
		// 1. 신고 접수 (이 시점 코멘트 blindReason은 없어야 한다)
		// 2. 승인 (처리자·메모 필수)
		// 3. 신고는 ACCEPTED로 남고, 코멘트도 남으며 blindReason=SPOILER

		// 1.
		int flag = service.doSave(report01);
		assertEquals(1, flag);
		assertNull(commentMapper.doSelectOne(comment01).getBlindReason());

		// 2.
		report01.setProcessedByMemberId(ADMIN_PROCESSOR);
		report01.setProcessNote("신고 승인 - 코멘트는 안내 문구로 가림");
		flag = service.upApproveReport(report01);
		assertEquals(1, flag);

		// 3.
		assertEquals(ReportCommentVO.STATUS_ACCEPTED, reportMapper.doSelectOne(report01).getStatus());
		UserCommentVO afterVO = commentMapper.doSelectOne(comment01);
		assertNotNull(afterVO);
		assertEquals(ReportCommentVO.REASON_SPOILER, afterVO.getBlindReason());

		// 4. 완료된 신고는 재처리 금지 — 재승인도, 반려 전환도 거부된다 (이력 보호)
		assertThrows(IllegalArgumentException.class, () -> service.upApproveReport(report01));
		report01.setStatus(ReportCommentVO.STATUS_REJECTED);
		assertThrows(IllegalArgumentException.class, () -> service.doUpdate(report01));
	}

	@Test
	public void doUpdateCannotAccept() {
		log.debug("---------------------------");
		log.debug("*doUpdateCannotAccept()*");
		log.debug("---------------------------");
		// 승인은 upApproveReport 전용 — 처리자·메모 검증과 상태 전이를
		// 한 경로로 일원화하기 위해 일반 doUpdate의 ACCEPTED는 서비스가 거부한다
		// 1. 신고 접수
		// 2. doUpdate로 ACCEPTED 시도 → IllegalArgumentException
		// 3. REJECTED(반려)는 정상 처리

		// 1.
		int flag = service.doSave(report01);
		assertEquals(1, flag);

		// 2.
		report01.setStatus(ReportCommentVO.STATUS_ACCEPTED);
		report01.setProcessedByMemberId(ADMIN_PROCESSOR);
		report01.setProcessNote("일반 doUpdate로 승인 시도");
		assertThrows(IllegalArgumentException.class, () -> service.doUpdate(report01));

		// 3.
		report01.setStatus(ReportCommentVO.STATUS_REJECTED);
		report01.setProcessNote("정상 리뷰로 판단되어 기각");
		flag = service.doUpdate(report01);
		assertEquals(1, flag);
		assertEquals(ReportCommentVO.STATUS_REJECTED, reportMapper.doSelectOne(report01).getStatus());
	}

	@Test
	public void upApproveReportNotFound() {
		log.debug("---------------------------");
		log.debug("*upApproveReportNotFound()*");
		log.debug("---------------------------");
		// 없는 신고를 승인하려 하면 업무 예외
		ReportCommentVO missing = new ReportCommentVO();
		missing.setReportId(MISSING_REPORT_ID);
		missing.setProcessedByMemberId(ADMIN_PROCESSOR);

		assertThrows(ReportNotFoundException.class, () -> service.upApproveReport(missing));
	}

	@Test
	public void commentIdSearchMustBeNumeric() {
		log.debug("---------------------------");
		log.debug("*commentIdSearchMustBeNumeric()*");
		log.debug("---------------------------");
		// 검색 30(코멘트ID)만 숫자 컬럼 — 문자면 서비스가 거르고,
		// 10(처리상태)은 문자 코드값이라 그대로 통과해야 한다
		// 1. 30 + 문자 → IllegalArgumentException
		// 2. 10 + 문자(RECEIVED) → 정상 실행

		// 1.
		dto.setPageNo(1);
		dto.setPageSize(10);
		dto.setSearchDiv("30");
		dto.setSearchWord("abc");
		assertThrows(IllegalArgumentException.class, () -> service.doRetrieve(dto));

		// 2.
		dto.setSearchDiv("10");
		dto.setSearchWord(ReportCommentVO.STATUS_RECEIVED);
		List<ReportCommentVO> list = service.doRetrieve(dto);
		assertNotNull(list);
		log.debug("list.size(): {}", list.size());
	}

	@Test
	void beans() {
		log.debug("---------------------------");
		log.debug("*beans()*");
		log.debug("---------------------------");
		assertNotNull(service);
		assertNotNull(reportMapper);
		assertNotNull(commentMapper);
		log.debug("service: {}", service);
	}

}
