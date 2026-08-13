/**
 * <pre>
 * Class Name : CommentLikeMapperDaoTest
 * Description : 코멘트 좋아요 Mapper JUnit
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

import static com.endit.mapper.MapperTestFixture.CONTENT_A;
import static com.endit.mapper.MapperTestFixture.MEMBER_A;
import static com.endit.mapper.MapperTestFixture.MEMBER_B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.CommentLikeVO;
import com.endit.domain.UserCommentVO;

@SpringBootTest
@Transactional
class CommentLikeMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	CommentLikeMapper mapper;

	@Autowired
	UserCommentMapper commentMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private UserCommentVO comment01; // 좋아요 대상 코멘트 (회원A 작성)
	private CommentLikeVO like01;    // 회원B가 comment01에 누르는 좋아요

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		// 1. 부모 데이터(회원·영화·컬렉션) 심기
		MapperTestFixture.seed(jdbcTemplate);

		// 2. 좋아요를 달 코멘트 1건 등록 (회원A → 영화A)
		commentMapper.deleteAll();
		comment01 = new UserCommentVO(0, MEMBER_A, CONTENT_A, null, "좋아요 대상 한줄평", UserCommentVO.SPOILER_NO,
				null, null);
		commentMapper.doSave(comment01);

		// 3. 테스트 좋아요 준비
		like01 = new CommentLikeVO(MEMBER_B, comment01.getCommentId(), "등록일_사용않함");

		log.debug("comment01: {}", comment01);
		log.debug("like01: {}", like01);
	}

	@Test
	public void doSaveAndCheck() {
		log.debug("---------------------------");
		log.debug("*doSaveAndCheck()*");
		log.debug("---------------------------");
		// 1. 전체삭제
		// 2. 좋아요 등록
		// 3. 눌렀는지 확인(likeCheck) / 좋아요 수(getLikeCnt) 비교

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());
		assertEquals(0, mapper.likeCheck(like01));

		// 2.
		int flag = mapper.doSave(like01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		// 3.
		assertEquals(1, mapper.likeCheck(like01));
		assertEquals(1, mapper.getLikeCnt(comment01.getCommentId()));
	}

	@Test
	public void duplicateLike() {
		log.debug("---------------------------");
		log.debug("*duplicateLike()*");
		log.debug("---------------------------");
		// 같은 코멘트에 두 번 누르면 PK(MEMBER_ID, COMMENT_ID) 위반으로 DB가 거부한다
		// 1. 전체삭제
		// 2. 좋아요 등록
		// 3. 같은 좋아요 한 번 더 → 거부

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(like01);
		assertEquals(1, flag);

		// 3.
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(like01));
		assertEquals(1, mapper.totalCnt());
	}

	@Test
	public void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");
		// 좋아요 취소
		// 1. 전체삭제
		// 2. 좋아요 등록
		// 3. 좋아요 취소
		// 4. 확인/집계 모두 0

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(like01);
		assertEquals(1, flag);

		// 3.
		flag = mapper.doDelete(like01);
		assertEquals(1, flag);

		// 4.
		assertEquals(0, mapper.likeCheck(like01));
		assertEquals(0, mapper.getLikeCnt(comment01.getCommentId()));
	}

	@Test
	public void cascadeOnCommentDelete() {
		log.debug("---------------------------");
		log.debug("*cascadeOnCommentDelete()*");
		log.debug("---------------------------");
		// 코멘트를 지우면 좋아요도 FK ON DELETE CASCADE로 함께 지워진다
		// 1. 전체삭제
		// 2. 좋아요 등록
		// 3. 코멘트 삭제
		// 4. 좋아요도 0건

		// 1.
		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		// 2.
		int flag = mapper.doSave(like01);
		assertEquals(1, flag);

		// 3.
		flag = commentMapper.doDelete(comment01);
		assertEquals(1, flag);

		// 4.
		assertEquals(0, mapper.totalCnt());
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
