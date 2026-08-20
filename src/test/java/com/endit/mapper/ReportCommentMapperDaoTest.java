/**
 * <pre>
 * Class Name : ReportCommentMapperDaoTest
 * Description : 코멘트 신고 Mapper JUnit
 *               팀 테스트 규칙(2026-08-14 회의) 반영:
 *               - 공용 DB 더미 데이터(테이블당 10건)가 있는 상태를 전제로 돈다
 *               - 부모 값은 더미의 실제 값을 하드코딩해 사용
 *               - @Transactional로 테스트 종료 시 데이터 전부 롤백(시퀀스 번호 소모만 남음 — 무해)
 *               - 신고 대상 코멘트를 트랜잭션 안에서 직접 만들어 쓰므로
 *                 "대상 코멘트" 검색은 내 신고만 잡는다(더미 신고 10건과 안 섞임)
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 12.  홍선기   최초 생성
 * 2026. 8. 13.  홍선기   @Transactional 적용(종료 시 롤백)
 * 2026. 8. 14.  홍선기   픽스처 제거, 공용 더미 기반으로 재작성(팀 테스트 규칙)
 * 2026. 8. 19.  홍선기   join 필드(신고자·처리자 닉네임, 대상 코멘트 내용) 검증 추가 — 8/18 공지 보완점2
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 12.
 */
package com.endit.mapper;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.ReportCommentVO;
import com.endit.domain.UserCommentVO;

@SpringBootTest
@Transactional
@DisplayName("ReportCommentMapper 테스트")
class ReportCommentMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	// 페이징 테스트 기준값
	private static final int PAGE_SIZE = 10;

	// 공용 DB 더미 데이터의 실제 부모 값 (회의 규칙: 부모 값은 DB 기반 하드코딩)
	private static final long MEMBER_AUTHOR = 9L;     // admin1@endit.com — 더미 코멘트가 없어 새 코멘트 작성용
	private static final long MEMBER_REPORTER = 10L;  // admin2@endit.com — 신고자
	private static final long MEMBER_REPORTER_B = 1L; // 영화왕김철수 — 두 번째 신고자
	private static final long ADMIN_PROCESSOR = 9L;   // 신고 처리 관리자 (ROLE=ADMIN)
	private static final long CONTENT_A = 9L;          // 어벤져스: 인피니티 워 — 더미 코멘트 없는 영화
	private static final long CONTENT_B = 10L;         // 올드보이 — 더미 코멘트 없는 영화

	@Autowired
	ReportCommentMapper mapper;

	@Autowired
	UserCommentMapper commentMapper;

	private UserCommentVO comment01;  // 신고 대상 코멘트 (회원9 → 영화9, 트랜잭션 안에서 생성)
	private UserCommentVO comment02;  // 신고 대상 코멘트 (회원9 → 영화10)
	private ReportCommentVO report01; // 회원10이 comment01을 스포일러로 신고

	private DTO dto; // paging/검색

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		// 1. 신고 대상 코멘트를 트랜잭션 안에서 직접 등록
		comment01 = new UserCommentVO(0, MEMBER_AUTHOR, CONTENT_A, null, "신고 대상 한줄평(영화9)", UserCommentVO.SPOILER_NO,
				null, null);
		comment02 = new UserCommentVO(0, MEMBER_AUTHOR, CONTENT_B, null, "신고 대상 한줄평(영화10)", UserCommentVO.SPOILER_NO,
				null, null);
		commentMapper.doSave(comment01);
		commentMapper.doSave(comment02);

		// 2. 테스트 신고 준비 (reportId는 doSave의 selectKey가 채운다)
		report01 = new ReportCommentVO(0, MEMBER_REPORTER, comment01.getCommentId(), ReportCommentVO.REASON_SPOILER,
				"스포일러 표시 없이 결말을 언급했습니다", null, null, null, null, null);

		dto = new DTO();

		log.debug("comment01: {}", comment01);
		log.debug("comment02: {}", comment02);
		log.debug("report01: {}", report01);
	}

	@Test
	public void doSaveAndDoSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSaveAndDoSelectOne()*");
		log.debug("---------------------------");
		// 더미 데이터 위에서 돌므로 건수는 "실행 전 대비 +n"으로 비교한다
		// 1. 실행 전 건수
		// 2. 신고 접수 → +1
		// 3. selectKey가 PK를 채웠는지 확인
		// 4. 단건조회 비교 — 접수 직후 상태는 DB DEFAULT인 RECEIVED

		// 1.
		int baseCnt = mapper.totalCnt();

		// 2.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, mapper.totalCnt());

		// 3.
		assertTrue(report01.getReportId() > 0);

		// 4.
		ReportCommentVO outVO = mapper.doSelectOne(report01);
		assertNotNull(outVO);
		log.debug("outVO: {}", outVO);
		assertEquals(report01.getReportMemberId(), outVO.getReportMemberId());
		assertEquals(report01.getCommentId(), outVO.getCommentId());
		assertEquals(report01.getReason(), outVO.getReason());
		assertEquals(report01.getDetail(), outVO.getDetail());
		assertEquals(ReportCommentVO.STATUS_RECEIVED, outVO.getStatus());
		assertNotNull(outVO.getCreatedDt());
		assertNull(outVO.getProcessedByMemberId()); // 처리 전이므로 처리자 없음
		assertNull(outVO.getProcessedDt());
		// join 필드 — 신고자 닉네임(더미 회원10), 처리자 없음, 대상 코멘트 본문
		assertEquals("ENDIT운영팀장", outVO.getReporterNickname());
		assertNull(outVO.getProcessorNickname());
		assertEquals(comment01.getCommentDetail(), outVO.getCommentDetail());
	}

	@Test
	public void otherReasonNeedsDetail() {
		log.debug("---------------------------");
		log.debug("*otherReasonNeedsDetail()*");
		log.debug("---------------------------");
		// 사유가 OTHER(기타)면 상세 내용이 필수다 (CK_REPORT_OTHER_DETAIL)
		// 1. OTHER + 상세 없음 → DB가 거부
		// 2. OTHER + 상세 있음 → 정상 접수

		// 1.
		ReportCommentVO noDetail = new ReportCommentVO(0, MEMBER_REPORTER, comment01.getCommentId(),
				ReportCommentVO.REASON_OTHER, null, null, null, null, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(noDetail));

		// 2.
		ReportCommentVO withDetail = new ReportCommentVO(0, MEMBER_REPORTER, comment01.getCommentId(),
				ReportCommentVO.REASON_OTHER, "광고 링크가 들어 있습니다", null, null, null, null, null);
		int flag = mapper.doSave(withDetail);
		assertEquals(1, flag);
	}

	@Test
	public void doUpdateProcess() {
		log.debug("---------------------------");
		log.debug("*doUpdateProcess()*");
		log.debug("---------------------------");
		// 신고 처리(승인) — 상태·처리자·처리일시가 한 UPDATE로 채워진다
		// 1. 신고 접수
		// 2. 관리자가 승인 처리
		// 3. 단건조회로 처리 결과 비교

		// 1.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);

		// 2.
		report01.setStatus(ReportCommentVO.STATUS_ACCEPTED);
		report01.setProcessedByMemberId(ADMIN_PROCESSOR);
		report01.setProcessNote("신고 승인 - 해당 코멘트 삭제 처리");
		flag = mapper.doUpdate(report01);
		assertEquals(1, flag);

		// 3.
		ReportCommentVO outVO = mapper.doSelectOne(report01);
		assertNotNull(outVO);
		log.debug("outVO: {}", outVO);
		assertEquals(ReportCommentVO.STATUS_ACCEPTED, outVO.getStatus());
		assertEquals(Long.valueOf(ADMIN_PROCESSOR), outVO.getProcessedByMemberId());
		assertEquals(report01.getProcessNote(), outVO.getProcessNote());
		assertNotNull(outVO.getProcessedDt());
		assertEquals("ENDIT수석관리자", outVO.getProcessorNickname()); // 처리자(더미 회원9) 닉네임 join
	}

	@Test
	public void processCompleteNeedsAdmin() {
		log.debug("---------------------------");
		log.debug("*processCompleteNeedsAdmin()*");
		log.debug("---------------------------");
		// 완료 상태(ACCEPTED/REJECTED)인데 처리자가 없으면 DB가 거부한다 (CK_REPORT_PROCESS_COMPLETE)
		// 1. 신고 접수
		// 2. 처리자 없이 승인 처리 → 거부

		// 1.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);

		// 2.
		report01.setStatus(ReportCommentVO.STATUS_ACCEPTED);
		report01.setProcessedByMemberId(null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doUpdate(report01));
	}

	@Test
	public void cascadeOnCommentDelete() {
		log.debug("---------------------------");
		log.debug("*cascadeOnCommentDelete()*");
		log.debug("---------------------------");
		// 코멘트를 지우면 신고 이력도 FK ON DELETE CASCADE로 함께 사라진다
		// (신고 승인 시 코멘트를 삭제하면 이력 소멸을 수용하기로 한 팀 결정의 근거 확인)
		// 1. 실행 전 건수
		// 2. 신고 접수 → +1
		// 3. 신고 대상 코멘트 삭제
		// 4. 신고도 함께 사라져 건수 원상복구

		// 1.
		int baseCnt = mapper.totalCnt();

		// 2.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, mapper.totalCnt());

		// 3.
		flag = commentMapper.doDelete(comment01);
		assertEquals(1, flag);

		// 4.
		assertEquals(baseCnt, mapper.totalCnt());
	}

	@Test
	public void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");
		// 1. 실행 전 건수
		// 2. 신고 접수 → +1
		// 3. 신고 단건삭제
		// 4. 건수 원상복구 비교

		// 1.
		int baseCnt = mapper.totalCnt();

		// 2.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, mapper.totalCnt());

		// 3.
		flag = mapper.doDelete(report01);
		assertEquals(1, flag);

		// 4.
		assertEquals(baseCnt, mapper.totalCnt());
	}

	@Test
	public void doRetrieve() {
		log.debug("---------------------------");
		log.debug("*doRetrieve()*");
		log.debug("---------------------------");
		// 방금 만든 코멘트에 신고 2건을 넣고 "대상 코멘트" 검색(searchDiv 30)으로 조회하면
		// 내 신고만 정확히 잡힌다 — 더미 신고 10건과 안 섞여 결정적이다
		// 1. 신고 2건 접수 (회원10: 스포일러, 회원1: 스팸 — 대상은 둘 다 comment01)
		// 2. 대상 코멘트 검색 → 2건, 총건수 2건
		// 3. 사유 검색(SPAM)은 더미에도 있으므로 개수 대신 내 신고 포함 여부로 확인

		// 1.
		ReportCommentVO report02 = new ReportCommentVO(0, MEMBER_REPORTER_B, comment01.getCommentId(),
				ReportCommentVO.REASON_SPAM, "반복 도배 코멘트입니다", null, null, null, null, null);
		mapper.doSave(report01);
		mapper.doSave(report02);

		// 2.
		dto.setPageNo(1);
		dto.setPageSize(PAGE_SIZE);
		dto.setSearchDiv("30"); // 대상 코멘트ID 검색
		dto.setSearchWord(String.valueOf(comment01.getCommentId()));
		List<ReportCommentVO> list = mapper.doRetrieve(dto);
		for (ReportCommentVO vo : list) {
			log.debug(vo.toString());
		}
		assertEquals(2, list.size());
		assertEquals(2, list.get(0).getTotalCnt());
		assertNotNull(list.get(0).getReporterNickname()); // join — 신고자 닉네임
		assertNotNull(list.get(0).getCommentDetail());    // join — 대상 코멘트 요약(200자)

		// 3.
		dto.setSearchDiv("20"); // 신고 사유 검색
		dto.setSearchWord(ReportCommentVO.REASON_SPAM);
		list = mapper.doRetrieve(dto);
		assertTrue(list.stream().anyMatch(vo -> vo.getReportId() == report02.getReportId()));
	}

	@Test
	void beans() {
		log.debug("---------------------------");
		log.debug("*beans()*");
		log.debug("---------------------------");
		assertNotNull(mapper);
		assertNotNull(commentMapper);
		log.debug("mapper: {}", mapper);
	}

}
