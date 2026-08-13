/**
 * <pre>
 * Class Name : UserCommentMapperDaoTest
 * Description : 코멘트 Mapper JUnit
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
import static com.endit.mapper.MapperTestFixture.COLLECTION_A;
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
import com.endit.domain.UserCommentVO;

@SpringBootTest
class UserCommentMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	// 페이징 테스트 기준값
	private static final int PAGE_SIZE = 4;

	@Autowired
	UserCommentMapper mapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private UserCommentVO comment01; // 회원A → 영화A
	private UserCommentVO comment02; // 회원A → 영화B (스포일러)
	private UserCommentVO comment03; // 회원B → 컬렉션A

	private DTO dto; // paging/검색

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		// 1. 부모 데이터(회원·영화·컬렉션) 심기 — 코멘트는 FK 때문에 부모 없이 못 들어간다
		MapperTestFixture.seed(jdbcTemplate);

		// 2. 테스트 코멘트 준비 (commentId는 doSave의 selectKey가 채운다)
		comment01 = new UserCommentVO(0, MEMBER_A, CONTENT_A, null, "영화A 한줄평입니다", UserCommentVO.SPOILER_NO,
				"등록일_사용않함", "수정일_사용않함");
		comment02 = new UserCommentVO(0, MEMBER_A, CONTENT_B, null, "영화B 결말 언급 한줄평", UserCommentVO.SPOILER_YES,
				"등록일_사용않함", "수정일_사용않함");
		comment03 = new UserCommentVO(0, MEMBER_B, null, COLLECTION_A, "컬렉션A 한줄평입니다", UserCommentVO.SPOILER_NO,
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
		// 테스트는 항상 동일한 결과가 나와야 하므로(Test Isolation) 데이터를 초기화하고 시작합니다.
		// 1. 전체삭제
		// 2. 단건등록(comment01: 영화 대상, 스포일러 아님)
		// 3. selectKey가 PK를 채웠는지 확인
		// 4. 단건조회 비교
		// 5. 단건등록(comment02: 스포일러 코멘트) 후 조회 비교

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(comment01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		// 3.
		assertTrue(comment01.getCommentId() > 0);

		// 4.
		UserCommentVO outVO = mapper.doSelectOne(comment01);
		assertNotNull(outVO);
		log.debug("outVO: {}", outVO);
		isSameComment(comment01, outVO);
		assertNotNull(outVO.getCreatedDt());
		assertNull(outVO.getUpdatedDt()); // 등록 직후에는 수정일이 없다

		// 5.
		flag = mapper.doSave(comment02);
		assertEquals(1, flag);
		assertEquals(2, mapper.totalCnt());

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
		// 1. 전체삭제
		// 2. 단건등록(comment01)
		// 3. 내용·스포일러 수정
		// 4. 단건조회로 수정 결과 비교

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(comment01);
		assertEquals(1, flag);

		// 3.
		comment01.setCommentDetail("수정된 한줄평입니다");
		comment01.setSpoiler(UserCommentVO.SPOILER_YES);
		flag = mapper.doUpdate(comment01);
		assertEquals(1, flag);

		// 4.
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
		// 1. 전체삭제
		// 2. 단건등록(comment01)
		// 3. 단건삭제
		// 4. 건수비교

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(comment01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		// 3.
		flag = mapper.doDelete(comment01);
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
		// 2. 9건 등록: 회원 3명 × (영화A, 영화B, 컬렉션A)
		//    ※ 같은 회원이 같은 대상에 2번 못 쓰므로(UNIQUE) 회원×대상 조합으로 만든다
		// 3. 2페이지 조회(페이지당 4건) → 4건, 총건수 9건
		// 4. 마지막 페이지(3페이지) → 1건
		// 5. 검색: 회원A가 쓴 것만 → 3건

		long[] members = { MEMBER_A, MEMBER_B, ADMIN };
		Long[] contents = { CONTENT_A, CONTENT_B };
		final int COMMENTS_PER_MEMBER = contents.length + 1; // 영화 2건 + 컬렉션 1건
		final int TOTAL_COUNT = members.length * COMMENTS_PER_MEMBER; // 9건

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		for (long member : members) {
			for (Long content : contents) {
				mapper.doSave(new UserCommentVO(0, member, content, null, "영화 한줄평", UserCommentVO.SPOILER_NO,
						null, null));
			}
			mapper.doSave(new UserCommentVO(0, member, null, COLLECTION_A, "컬렉션 한줄평", UserCommentVO.SPOILER_NO,
					null, null));
		}
		assertEquals(TOTAL_COUNT, mapper.totalCnt());

		// 3.
		dto.setPageNo(2);
		dto.setPageSize(PAGE_SIZE);
		List<UserCommentVO> list = mapper.doRetrieve(dto);
		for (UserCommentVO vo : list) {
			log.debug(vo.toString());
		}
		assertEquals(PAGE_SIZE, list.size());
		assertEquals(TOTAL_COUNT, list.get(0).getTotalCnt());

		// 4.
		dto.setPageNo(3);
		list = mapper.doRetrieve(dto);
		assertEquals(TOTAL_COUNT - PAGE_SIZE * 2, list.size());

		// 5.
		dto.setPageNo(1);
		dto.setSearchDiv("10");
		dto.setSearchWord(String.valueOf(MEMBER_A));
		list = mapper.doRetrieve(dto);
		assertEquals(COMMENTS_PER_MEMBER, list.size());
	}

	@Test
	public void targetOnlyOne() {
		log.debug("---------------------------");
		log.debug("*targetOnlyOne()*");
		log.debug("---------------------------");
		// 코멘트 대상은 영화/컬렉션 중 정확히 하나여야 한다 (CK_USER_COMMENT_TARGET_ONE)
		// 1. 전체삭제
		// 2. 둘 다 지정 → DB가 거부
		// 3. 둘 다 없음 → DB가 거부

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		UserCommentVO bothTarget = new UserCommentVO(0, MEMBER_A, CONTENT_A, COLLECTION_A, "대상 두 개",
				UserCommentVO.SPOILER_NO, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(bothTarget));

		// 3.
		UserCommentVO noTarget = new UserCommentVO(0, MEMBER_A, null, null, "대상 없음",
				UserCommentVO.SPOILER_NO, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(noTarget));

		assertEquals(0, mapper.totalCnt());
	}

	@Test
	public void oneCommentPerTarget() {
		log.debug("---------------------------");
		log.debug("*oneCommentPerTarget()*");
		log.debug("---------------------------");
		// 같은 회원은 같은 대상에 코멘트를 1개만 쓸 수 있다 (UK_USER_COMMENT_CONTENT)
		// 1. 전체삭제
		// 2. 단건등록(comment01: 회원A → 영화A)
		// 3. 같은 회원A → 영화A 한 건 더 → DB가 거부
		// 4. 지우고 다시 쓰면 정상 등록된다 (물리삭제라 행이 사라지므로)

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(comment01);
		assertEquals(1, flag);

		// 3.
		UserCommentVO second = new UserCommentVO(0, MEMBER_A, CONTENT_A, null, "같은 영화에 두 번째",
				UserCommentVO.SPOILER_NO, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(second));

		// 4.
		flag = mapper.doDelete(comment01);
		assertEquals(1, flag);
		flag = mapper.doSave(second);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());
	}

	@Test
	public void oneCommentPerCollection() {
		log.debug("---------------------------");
		log.debug("*oneCommentPerCollection()*");
		log.debug("---------------------------");
		// 컬렉션 대상에도 같은 규칙이 걸려 있다 (UK_USER_COMMENT_COLLECTION)
		// 영화용(UK_USER_COMMENT_CONTENT)과 별개의 인덱스라 따로 확인한다
		// 1. 전체삭제
		// 2. 단건등록(comment03: 회원B → 컬렉션A)
		// 3. 같은 회원B → 컬렉션A 한 건 더 → DB가 거부
		// 4. 회원A는 같은 컬렉션에 쓸 수 있다 (회원이 다르므로)

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(comment03);
		assertEquals(1, flag);

		// 3.
		UserCommentVO second = new UserCommentVO(0, MEMBER_B, null, COLLECTION_A, "같은 컬렉션에 두 번째",
				UserCommentVO.SPOILER_NO, null, null);
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(second));

		// 4.
		UserCommentVO otherMember = new UserCommentVO(0, MEMBER_A, null, COLLECTION_A, "다른 회원의 컬렉션 한줄평",
				UserCommentVO.SPOILER_NO, null, null);
		flag = mapper.doSave(otherMember);
		assertEquals(1, flag);
		assertEquals(2, mapper.totalCnt());
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
