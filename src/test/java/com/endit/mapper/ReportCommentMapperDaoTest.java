/**
 * <pre>
 * Class Name : ReportCommentMapperDaoTest
 * Description : 코멘트 신고 Mapper JUnit
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

import static com.endit.mapper.MapperTestFixture.ADMIN;
import static com.endit.mapper.MapperTestFixture.CONTENT_A;
import static com.endit.mapper.MapperTestFixture.CONTENT_B;
import static com.endit.mapper.MapperTestFixture.MEMBER_A;
import static com.endit.mapper.MapperTestFixture.MEMBER_B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.endit.cmn.DTO;
import com.endit.domain.ReportCommentVO;
import com.endit.domain.UserCommentVO;

@SpringBootTest
class ReportCommentMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	// 페이징 테스트 기준값
	private static final int PAGE_SIZE = 10;

	@Autowired
	ReportCommentMapper mapper;

	@Autowired
	UserCommentMapper commentMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private UserCommentVO comment01; // 신고 대상 코멘트 (회원A → 영화A)
	private UserCommentVO comment02; // 신고 대상 코멘트 (회원B → 영화B)
	private ReportCommentVO report01; // 회원B가 comment01을 스포일러로 신고

	private DTO dto; // paging/검색

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		// 1. 부모 데이터(회원·영화·컬렉션) 심기
		MapperTestFixture.seed(jdbcTemplate);

		// 2. 신고 대상 코멘트 등록
		commentMapper.deleteAll();
		comment01 = new UserCommentVO(0, MEMBER_A, CONTENT_A, null, "신고 대상 한줄평(회원A)", UserCommentVO.SPOILER_NO,
				null, null);
		comment02 = new UserCommentVO(0, MEMBER_B, CONTENT_B, null, "신고 대상 한줄평(회원B)", UserCommentVO.SPOILER_NO,
				null, null);
		commentMapper.doSave(comment01);
		commentMapper.doSave(comment02);

		// 3. 테스트 신고 준비 (reportId는 doSave의 selectKey가 채운다)
		report01 = new ReportCommentVO(0, MEMBER_B, comment01.getCommentId(), ReportCommentVO.REASON_SPOILER,
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
		// 1. 전체삭제
		// 2. 신고 접수
		// 3. selectKey가 PK를 채웠는지 확인
		// 4. 단건조회 비교 — 접수 직후 상태는 DB DEFAULT인 RECEIVED

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

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
	}

	@Test
	public void otherReasonNeedsDetail() {
		log.debug("---------------------------");
		log.debug("*otherReasonNeedsDetail()*");
		log.debug("---------------------------");
		// 사유가 OTHER(기타)면 상세 내용이 필수다 (CK_REPORT_OTHER_DETAIL)
		// 1. 전체삭제
		// 2. OTHER + 상세 없음 → DB가 거부
		// 3. OTHER + 상세 있음 → 정상 접수

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		ReportCommentVO noDetail = new ReportCommentVO(0, MEMBER_B, comment01.getCommentId(),
				ReportCommentVO.REASON_OTHER, null, null, null, null, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(noDetail));

		// 3.
		ReportCommentVO withDetail = new ReportCommentVO(0, MEMBER_B, comment01.getCommentId(),
				ReportCommentVO.REASON_OTHER, "광고 링크가 들어 있습니다", null, null, null, null, null);
		int flag = mapper.doSave(withDetail);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());
	}

	@Test
	public void doUpdateProcess() {
		log.debug("---------------------------");
		log.debug("*doUpdateProcess()*");
		log.debug("---------------------------");
		// 신고 처리(승인) — 상태·처리자·처리일시가 한 UPDATE로 채워진다
		// 1. 전체삭제
		// 2. 신고 접수
		// 3. 관리자가 승인 처리
		// 4. 단건조회로 처리 결과 비교

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);

		// 3.
		report01.setStatus(ReportCommentVO.STATUS_ACCEPTED);
		report01.setProcessedByMemberId(ADMIN);
		report01.setProcessNote("신고 승인 - 해당 코멘트 삭제 처리");
		flag = mapper.doUpdate(report01);
		assertEquals(1, flag);

		// 4.
		ReportCommentVO outVO = mapper.doSelectOne(report01);
		assertNotNull(outVO);
		log.debug("outVO: {}", outVO);
		assertEquals(ReportCommentVO.STATUS_ACCEPTED, outVO.getStatus());
		assertEquals(Long.valueOf(ADMIN), outVO.getProcessedByMemberId());
		assertEquals(report01.getProcessNote(), outVO.getProcessNote());
		assertNotNull(outVO.getProcessedDt());
	}

	@Test
	public void processCompleteNeedsAdmin() {
		log.debug("---------------------------");
		log.debug("*processCompleteNeedsAdmin()*");
		log.debug("---------------------------");
		// 완료 상태(ACCEPTED/REJECTED)인데 처리자가 없으면 DB가 거부한다 (CK_REPORT_PROCESS_COMPLETE)
		// 1. 전체삭제
		// 2. 신고 접수
		// 3. 처리자 없이 승인 처리 → 거부

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);

		// 3.
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
		// 1. 전체삭제
		// 2. 신고 접수
		// 3. 신고 대상 코멘트 삭제
		// 4. 신고도 0건

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);

		// 3.
		flag = commentMapper.doDelete(comment01);
		assertEquals(1, flag);

		// 4.
		assertEquals(0, mapper.totalCnt());
	}

	@Test
	public void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");
		// 1. 전체삭제
		// 2. 신고 접수
		// 3. 신고 단건삭제
		// 4. 건수비교

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(report01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		// 3.
		flag = mapper.doDelete(report01);
		assertEquals(1, flag);

		// 4.
		assertEquals(0, mapper.totalCnt());
	}

	@Test
	public void doRetrieve() {
		log.debug("---------------------------");
		log.debug("*doRetrieve()*");
		log.debug("---------------------------");
		// 1. 전체삭제
		// 2. 신고 2건 접수 (회원B→comment01 스포일러, 회원A→comment02 스팸)
		// 3. 상태 검색(RECEIVED) → 2건
		// 4. 사유 검색(SPAM) → 1건

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		ReportCommentVO report02 = new ReportCommentVO(0, MEMBER_A, comment02.getCommentId(),
				ReportCommentVO.REASON_SPAM, "반복 도배 코멘트입니다", null, null, null, null, null);
		mapper.doSave(report01);
		mapper.doSave(report02);
		assertEquals(2, mapper.totalCnt());

		// 3.
		dto.setPageNo(1);
		dto.setPageSize(PAGE_SIZE);
		dto.setSearchDiv("10");
		dto.setSearchWord(ReportCommentVO.STATUS_RECEIVED);
		List<ReportCommentVO> list = mapper.doRetrieve(dto);
		for (ReportCommentVO vo : list) {
			log.debug(vo.toString());
		}
		assertEquals(2, list.size());
		assertEquals(2, list.get(0).getTotalCnt());

		// 4.
		dto.setSearchDiv("20");
		dto.setSearchWord(ReportCommentVO.REASON_SPAM);
		list = mapper.doRetrieve(dto);
		assertEquals(1, list.size());
	}

	@Test
	void beans() {
		log.debug("---------------------------");
		log.debug("*beans()*");
		log.debug("---------------------------");
		assertNotNull(mapper);
		assertNotNull(commentMapper);
		assertNotNull(jdbcTemplate);
		log.debug("mapper: {}", mapper);
	}

}
