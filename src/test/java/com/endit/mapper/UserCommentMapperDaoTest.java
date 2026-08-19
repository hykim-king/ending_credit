/**
 * <pre>
 * Class Name : UserCommentMapperDaoTest
 * Description : 코멘트 Mapper JUnit
 *               팀 테스트 규칙(2026-08-14 회의) 반영:
 *               - 공용 DB 더미 데이터(테이블당 10건)가 있는 상태를 전제로 돈다
 *               - 부모(회원·영화·컬렉션) 값은 더미의 실제 값을 하드코딩해 사용
 *               - @Transactional로 테스트 종료 시 데이터 전부 롤백(시퀀스 번호 소모만 남음 — 무해)
 *               - 전체 건수는 "실행 전 대비 증감"으로, 검색 검증은 내가 만든 행만 잡히는 조건으로 비교
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 12.  홍선기   최초 생성
 * 2026. 8. 13.  홍선기   @Transactional 적용(종료 시 롤백)
 * 2026. 8. 14.  홍선기   픽스처 제거, 공용 더미 기반으로 재작성(팀 테스트 규칙)
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.UserCommentVO;

@SpringBootTest
@Transactional
@DisplayName("UserCommentMapper 테스트")
class UserCommentMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	// 페이징 테스트 기준값
	private static final int PAGE_SIZE = 4;

	// 공용 DB 더미 데이터의 실제 부모 값 (회의 규칙: 부모 값은 DB 기반 하드코딩)
	// 더미 코멘트는 회원 1~8이 영화 1~8에, 회원 1~2가 컬렉션 2~3에 작성돼 있다.
	// → 더미 코멘트가 전혀 없는 회원 9·10을 쓰면 UNIQUE(회원×대상 1개) 충돌이 없다.
	private static final long MEMBER_A = 9L;     // admin1@endit.com — 더미 코멘트 없음
	private static final long MEMBER_B = 10L;    // admin2@endit.com — 더미 코멘트 없음
	private static final long CONTENT_A = 9L;    // 어벤져스: 인피니티 워 — 더미 코멘트 없는 영화
	private static final long CONTENT_B = 10L;   // 올드보이 — 더미 코멘트 없는 영화
	private static final long COLLECTION_A = 1L; // 인생 SF 영화 모음집(회원1 소유) — 더미 코멘트 없음

	@Autowired
	UserCommentMapper mapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private UserCommentVO comment01; // 회원A → 영화A
	private UserCommentVO comment02; // 회원A → 영화B (스포일러)
	private UserCommentVO comment03; // 회원A → 컬렉션A

	private DTO dto; // paging/검색

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		// 테스트 코멘트 준비 (commentId는 doSave의 selectKey가 채운다)
		comment01 = new UserCommentVO(0, MEMBER_A, CONTENT_A, null, "영화A 한줄평입니다", UserCommentVO.SPOILER_NO,
				"등록일_사용않함", "수정일_사용않함");
		comment02 = new UserCommentVO(0, MEMBER_A, CONTENT_B, null, "영화B 결말 언급 한줄평", UserCommentVO.SPOILER_YES,
				"등록일_사용않함", "수정일_사용않함");
		comment03 = new UserCommentVO(0, MEMBER_A, null, COLLECTION_A, "컬렉션A 한줄평입니다", UserCommentVO.SPOILER_NO,
				"등록일_사용않함", "수정일_사용않함");

		dto = new DTO();

		log.debug("comment01: {}", comment01);
		log.debug("comment02: {}", comment02);
		log.debug("comment03: {}", comment03);
	}

	@Test
	public void doSaveAndDoSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSaveAndDoSelectOne()*");
		log.debug("---------------------------");
		// 더미 데이터 위에서 돌므로 건수는 "실행 전 대비 +n"으로 비교한다
		// 1. 실행 전 건수
		// 2. 단건등록(comment01: 영화 대상, 스포일러 아님) → +1
		// 3. selectKey가 PK를 채웠는지 확인
		// 4. 단건조회 비교
		// 5. 단건등록(comment02: 스포일러 코멘트) 후 조회 비교 → +2

		// 1.
		int baseCnt = mapper.totalCnt();

		// 2.
		int flag = mapper.doSave(comment01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, mapper.totalCnt());

		// 3.
		assertTrue(comment01.getCommentId() > 0);

		// 4.
		UserCommentVO outVO = mapper.doSelectOne(comment01);
		assertNotNull(outVO);
		log.debug("outVO: {}", outVO);
		isSameComment(comment01, outVO);
		assertNotNull(outVO.getCreatedDt());
		assertNull(outVO.getUpdatedDt()); // 등록 직후에는 수정일이 없다

		// 4-1. join으로 채워지는 필드 검증 (영화 코멘트라 collectionTitle은 비어야 함)
		String expectedNickname = jdbcTemplate.queryForObject(
				"SELECT nickname FROM member WHERE member_id = ?", String.class, MEMBER_A);
		assertEquals(expectedNickname, outVO.getAuthorNickname());
		assertNull(outVO.getCollectionTitle());

		// 5.
		flag = mapper.doSave(comment02);
		assertEquals(1, flag);
		assertEquals(baseCnt + 2, mapper.totalCnt());

		UserCommentVO spoilerVO = mapper.doSelectOne(comment02);
		assertNotNull(spoilerVO);
		isSameComment(comment02, spoilerVO);
		assertEquals(UserCommentVO.SPOILER_YES, spoilerVO.getSpoiler());
	}

	@Test
	public void doUpdate() {
		log.debug("---------------------------");
		log.debug("*doUpdate()*");
		log.debug("---------------------------");
		// 1. 단건등록(comment01)
		// 2. 내용·스포일러 수정
		// 3. 단건조회로 수정 결과 비교

		// 1.
		int flag = mapper.doSave(comment01);
		assertEquals(1, flag);

		// 2.
		comment01.setCommentDetail("수정된 한줄평입니다");
		comment01.setSpoiler(UserCommentVO.SPOILER_YES);
		flag = mapper.doUpdate(comment01);
		assertEquals(1, flag);

		// 3.
		UserCommentVO outVO = mapper.doSelectOne(comment01);
		assertNotNull(outVO);
		isSameComment(comment01, outVO);
		assertNotNull(outVO.getUpdatedDt()); // 수정하면 수정일이 채워진다
	}

	@Test
	public void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");
		// 1. 실행 전 건수
		// 2. 단건등록(comment01) → +1
		// 3. 단건삭제
		// 4. 건수 원상복구 비교

		// 1.
		int baseCnt = mapper.totalCnt();

		// 2.
		int flag = mapper.doSave(comment01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, mapper.totalCnt());

		// 3.
		flag = mapper.doDelete(comment01);
		assertEquals(1, flag);

		// 4.
		assertEquals(baseCnt, mapper.totalCnt());
	}

	@Test
	public void doRetrieve() {
		log.debug("---------------------------");
		log.debug("*doRetrieve()*");
		log.debug("---------------------------");
		// 회원A(9번)는 더미 코멘트가 없으므로, 회원A로 9건을 등록하면
		// "작성자=회원A" 검색 결과는 정확히 그 9건이다 — 더미와 안 섞여 결정적이다
		// 1. 회원A로 9건 등록: 더미 영화 1~8 + 컬렉션A
		//    ※ 같은 회원이 같은 대상에 2번 못 쓰므로(UNIQUE) 대상을 전부 다르게 한다
		// 2. 회원A 검색 + 2페이지(페이지당 4건) → 4건, 총건수 9건
		// 3. 마지막 페이지(3페이지) → 1건

		// 1.
		final long[] contents = { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L }; // 더미 영화 1~8
		final int TOTAL_COUNT = contents.length + 1; // 영화 8건 + 컬렉션 1건 = 9건

		for (long content : contents) {
			mapper.doSave(new UserCommentVO(0, MEMBER_A, content, null, "영화 한줄평", UserCommentVO.SPOILER_NO,
					null, null));
		}
		mapper.doSave(new UserCommentVO(0, MEMBER_A, null, COLLECTION_A, "컬렉션 한줄평", UserCommentVO.SPOILER_NO,
				null, null));

		// 2.
		dto.setPageNo(2);
		dto.setPageSize(PAGE_SIZE);
		dto.setSearchDiv("10"); // 작성자 회원ID 검색
		dto.setSearchWord(String.valueOf(MEMBER_A));
		List<UserCommentVO> list = mapper.doRetrieve(dto);
		for (UserCommentVO vo : list) {
			log.debug(vo.toString());
		}
		assertEquals(PAGE_SIZE, list.size());
		assertEquals(TOTAL_COUNT, list.get(0).getTotalCnt());

		// 3.
		dto.setPageNo(3);
		list = mapper.doRetrieve(dto);
		assertEquals(TOTAL_COUNT - PAGE_SIZE * 2, list.size());
	}

	@Test
	public void targetOnlyOne() {
		log.debug("---------------------------");
		log.debug("*targetOnlyOne()*");
		log.debug("---------------------------");
		// 코멘트 대상은 영화/컬렉션 중 정확히 하나여야 한다 (CK_USER_COMMENT_TARGET_ONE)
		// 1. 실행 전 건수
		// 2. 둘 다 지정 → DB가 거부
		// 3. 둘 다 없음 → DB가 거부
		// 4. 건수 변동 없음

		// 1.
		int baseCnt = mapper.totalCnt();

		// 2.
		UserCommentVO bothTarget = new UserCommentVO(0, MEMBER_A, CONTENT_A, COLLECTION_A, "대상 두 개",
				UserCommentVO.SPOILER_NO, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(bothTarget));

		// 3.
		UserCommentVO noTarget = new UserCommentVO(0, MEMBER_A, null, null, "대상 없음",
				UserCommentVO.SPOILER_NO, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(noTarget));

		// 4.
		assertEquals(baseCnt, mapper.totalCnt());
	}

	@Test
	public void oneCommentPerTarget() {
		log.debug("---------------------------");
		log.debug("*oneCommentPerTarget()*");
		log.debug("---------------------------");
		// 같은 회원은 같은 대상에 코멘트를 1개만 쓸 수 있다 (UK_USER_COMMENT_CONTENT)
		// 1. 단건등록(comment01: 회원A → 영화A)
		// 2. 같은 회원A → 영화A 한 건 더 → DB가 거부
		// 3. 지우고 다시 쓰면 정상 등록된다 (물리삭제라 행이 사라지므로)

		// 1.
		int flag = mapper.doSave(comment01);
		assertEquals(1, flag);

		// 2.
		UserCommentVO second = new UserCommentVO(0, MEMBER_A, CONTENT_A, null, "같은 영화에 두 번째",
				UserCommentVO.SPOILER_NO, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(second));

		// 3.
		flag = mapper.doDelete(comment01);
		assertEquals(1, flag);
		flag = mapper.doSave(second);
		assertEquals(1, flag);
	}

	@Test
	public void oneCommentPerCollection() {
		log.debug("---------------------------");
		log.debug("*oneCommentPerCollection()*");
		log.debug("---------------------------");
		// 컬렉션 대상에도 같은 규칙이 걸려 있다 (UK_USER_COMMENT_COLLECTION)
		// 영화용(UK_USER_COMMENT_CONTENT)과 별개의 인덱스라 따로 확인한다
		// 1. 단건등록(comment03: 회원A → 컬렉션A)
		// 2. 같은 회원A → 컬렉션A 한 건 더 → DB가 거부
		// 3. 회원B는 같은 컬렉션에 쓸 수 있다 (회원이 다르므로)

		// 1.
		int flag = mapper.doSave(comment03);
		assertEquals(1, flag);

		// 1-1. 컬렉션 코멘트는 collectionTitle이 join으로 채워져야 한다
		UserCommentVO savedVO = mapper.doSelectOne(comment03);
		String expectedTitle = jdbcTemplate.queryForObject(
				"SELECT title FROM collection WHERE collection_id = ?", String.class, COLLECTION_A);
		assertEquals(expectedTitle, savedVO.getCollectionTitle());

		// 2.
		UserCommentVO second = new UserCommentVO(0, MEMBER_A, null, COLLECTION_A, "같은 컬렉션에 두 번째",
				UserCommentVO.SPOILER_NO, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(second));

		// 3.
		UserCommentVO otherMember = new UserCommentVO(0, MEMBER_B, null, COLLECTION_A, "다른 회원의 컬렉션 한줄평",
				UserCommentVO.SPOILER_NO, null, null);
		flag = mapper.doSave(otherMember);
		assertEquals(1, flag);
	}

	@Test
	void beans() {
		log.debug("---------------------------");
		log.debug("*beans()*");
		log.debug("---------------------------");
		assertNotNull(mapper);
		assertNotNull(jdbcTemplate);
		log.debug("mapper: {}", mapper);
	}

	private void isSameComment(UserCommentVO inVO, UserCommentVO outVO) {
		assertEquals(inVO.getCommentId(), outVO.getCommentId());
		assertEquals(inVO.getMemberId(), outVO.getMemberId());
		assertEquals(inVO.getContentId(), outVO.getContentId());
		assertEquals(inVO.getCollectionId(), outVO.getCollectionId());
		assertEquals(inVO.getCommentDetail(), outVO.getCommentDetail());
		assertEquals(inVO.getSpoiler(), outVO.getSpoiler());
	}

}
