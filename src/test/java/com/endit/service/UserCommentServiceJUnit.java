/**
 * <pre>
 * Class Name : UserCommentServiceJUnit
 * Description : 코멘트 Service JUnit
 *               서비스 고유 로직(검색어 숫자 검증)과 매퍼 위임을 검증한다.
 *               팀 테스트 규칙: 공용 더미(테이블당 10건) 위에서 @Transactional 롤백.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.endit.domain.UserCommentVO;

@SpringBootTest
@Transactional
@DisplayName("UserCommentService 테스트")
class UserCommentServiceJUnit {

	final Logger log = LoggerFactory.getLogger(getClass());

	// 공용 DB 더미 데이터의 실제 부모 값 (회의 규칙: 부모 값은 DB 기반 하드코딩)
	private static final long MEMBER_A = 9L;   // admin1@endit.com — 더미 코멘트 없음
	private static final long CONTENT_A = 9L;  // 어벤져스: 인피니티 워 — 더미 코멘트 없는 영화

	@Autowired
	UserCommentService service;

	private UserCommentVO comment01; // 회원A → 영화A

	private DTO dto; // paging/검색

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		comment01 = new UserCommentVO(0, MEMBER_A, CONTENT_A, null, "서비스 테스트 한줄평", UserCommentVO.SPOILER_NO,
				null, null);
		dto = new DTO();

		log.debug("comment01: {}", comment01);
	}

	@Test
	public void doSaveAndDoSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSaveAndDoSelectOne()*");
		log.debug("---------------------------");
		// 서비스가 매퍼에 바르게 위임하는지 왕복으로 확인
		// 1. 실행 전 건수
		// 2. 등록 → +1, PK 채번 확인
		// 3. 단건조회 대조

		// 1.
		int baseCnt = service.totalCnt();

		// 2.
		int flag = service.doSave(comment01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, service.totalCnt());
		assertTrue(comment01.getCommentId() > 0);

		// 3.
		UserCommentVO outVO = service.doSelectOne(comment01);
		assertNotNull(outVO);
		log.debug("outVO: {}", outVO);
		assertEquals(comment01.getCommentDetail(), outVO.getCommentDetail());
		assertEquals(comment01.getSpoiler(), outVO.getSpoiler());
	}

	@Test
	public void searchWordMustBeNumeric() {
		log.debug("---------------------------");
		log.debug("*searchWordMustBeNumeric()*");
		log.debug("---------------------------");
		// 코멘트 검색조건(회원/영화/컬렉션 ID)은 전부 숫자 컬럼 —
		// 문자가 오면 DB(ORA-01722)까지 가기 전에 서비스가 거른다
		// 1. 문자 검색어 → IllegalArgumentException
		// 2. 전각 숫자(１２３)도 거부 — 오라클은 ASCII 숫자만 변환할 수 있다
		// 3. 검색구분 없이 검색어만 오면 매퍼가 검색어를 무시하므로 막지 않는다(전체 목록)
		// 4. 숫자 검색어 → 정상 조회 (내가 등록한 1건만 잡힘)

		// 1.
		dto.setPageNo(1);
		dto.setPageSize(10);
		dto.setSearchDiv("10"); // 작성자 회원ID 검색
		dto.setSearchWord("abc");
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.doRetrieve(dto));
		log.debug("message: {}", e.getMessage());
		assertTrue(e.getMessage().contains("숫자"));

		// 2.
		dto.setSearchWord("１２３");
		assertThrows(IllegalArgumentException.class, () -> service.doRetrieve(dto));

		// 3.
		dto.setSearchDiv("");
		dto.setSearchWord("abc");
		List<UserCommentVO> all = service.doRetrieve(dto);
		assertTrue(all.size() > 0); // 더미가 있으므로 전체 목록이 나온다

		// 4.
		int flag = service.doSave(comment01);
		assertEquals(1, flag);

		dto.setSearchDiv("10");
		dto.setSearchWord(String.valueOf(MEMBER_A)); // 회원A는 더미 코멘트가 없어 내 1건만 잡힌다
		List<UserCommentVO> list = service.doRetrieve(dto);
		assertEquals(1, list.size());
		assertEquals(1, list.get(0).getTotalCnt());
	}

	@Test
	void beans() {
		log.debug("---------------------------");
		log.debug("*beans()*");
		log.debug("---------------------------");
		assertNotNull(service);
		log.debug("service: {}", service);
	}

}
