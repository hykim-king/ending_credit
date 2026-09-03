/**
 * 코멘트 좋아요 Mapper JUnit
 * 팀 테스트 규칙(2026-08-14 회의) 반영:
 * - 공용 DB 더미 데이터(테이블당 10건)가 있는 상태를 전제로 돈다
 * - 부모 값은 더미의 실제 값을 하드코딩해 사용
 * - @Transactional로 테스트 종료 시 데이터 전부 롤백(시퀀스 번호 소모만 남음 — 무해)
 * - 좋아요 대상 코멘트를 트랜잭션 안에서 직접 만들어 쓰므로
 * 그 코멘트에는 더미 좋아요가 있을 수 없다(집계 검증이 결정적)
 */
package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.CommentLikeVO;
import com.endit.domain.UserCommentVO;

@SpringBootTest
@Transactional
@DisplayName("CommentLikeMapper 테스트")
class CommentLikeMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	// 공용 DB 더미 데이터의 실제 부모 값 (회의 규칙: 부모 값은 DB 기반 하드코딩)
	private static final long MEMBER_AUTHOR = 9L; // admin1@endit.com — 더미 코멘트가 없어 새 코멘트 작성용
	private static final long MEMBER_LIKER = 10L; // admin2@endit.com — 좋아요 누르는 회원
	private static final long CONTENT_A = 9L;     // 어벤져스: 인피니티 워 — 더미 코멘트 없는 영화

	@Autowired
	CommentLikeMapper mapper;

	@Autowired
	UserCommentMapper commentMapper;

	private UserCommentVO comment01; // 좋아요 대상 코멘트 (회원9 작성, 트랜잭션 안에서 생성)
	private CommentLikeVO like01;    // 회원10이 comment01에 누르는 좋아요

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		// 1. 좋아요를 달 코멘트를 트랜잭션 안에서 직접 등록 (회원9 → 영화9)
		//    방금 만든 코멘트라 더미 좋아요가 붙어 있을 수 없다
		comment01 = new UserCommentVO(0, MEMBER_AUTHOR, CONTENT_A, null, "좋아요 대상 한줄평", UserCommentVO.SPOILER_NO,
				null, null);
		commentMapper.doSave(comment01);

		// 2. 테스트 좋아요 준비
		like01 = new CommentLikeVO(MEMBER_LIKER, comment01.getCommentId(), "등록일_사용않함");

		log.debug("comment01: {}", comment01);
		log.debug("like01: {}", like01);
	}

	@Test
	public void doSaveAndCheck() {
		log.debug("---------------------------");
		log.debug("*doSaveAndCheck()*");
		log.debug("---------------------------");
		// 1. 실행 전: 전체 건수 기록, 내 코멘트에는 좋아요 0
		// 2. 좋아요 등록 → 전체 +1
		// 3. 눌렀는지 확인(likeCheck) / 좋아요 수(getLikeCnt) 비교

		// 1.
		int baseCnt = mapper.totalCnt();
		assertEquals(0, mapper.likeCheck(like01));
		assertEquals(0, mapper.getLikeCnt(comment01.getCommentId()));

		// 2.
		int flag = mapper.doSave(like01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, mapper.totalCnt());

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
		// 1. 실행 전 건수
		// 2. 좋아요 등록 → +1
		// 3. 같은 좋아요 한 번 더 → 거부, 건수 그대로

		// 1.
		int baseCnt = mapper.totalCnt();

		// 2.
		int flag = mapper.doSave(like01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, mapper.totalCnt());

		// 3.
		assertThrows(DataIntegrityViolationException.class, () -> mapper.doSave(like01));
		assertEquals(baseCnt + 1, mapper.totalCnt());
	}

	@Test
	public void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");
		// 좋아요 취소
		// 1. 실행 전 건수
		// 2. 좋아요 등록
		// 3. 좋아요 취소
		// 4. 확인/집계 모두 0, 건수 원상복구

		// 1.
		int baseCnt = mapper.totalCnt();

		// 2.
		int flag = mapper.doSave(like01);
		assertEquals(1, flag);

		// 3.
		flag = mapper.doDelete(like01);
		assertEquals(1, flag);

		// 4.
		assertEquals(0, mapper.likeCheck(like01));
		assertEquals(0, mapper.getLikeCnt(comment01.getCommentId()));
		assertEquals(baseCnt, mapper.totalCnt());
	}

	@Test
	public void cascadeOnCommentDelete() {
		log.debug("---------------------------");
		log.debug("*cascadeOnCommentDelete()*");
		log.debug("---------------------------");
		// 코멘트를 지우면 좋아요도 FK ON DELETE CASCADE로 함께 지워진다
		// 1. 실행 전 건수
		// 2. 좋아요 등록 → +1
		// 3. 대상 코멘트 삭제
		// 4. 좋아요도 함께 사라져 건수 원상복구

		// 1.
		int baseCnt = mapper.totalCnt();

		// 2.
		int flag = mapper.doSave(like01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, mapper.totalCnt());

		// 3.
		flag = commentMapper.doDelete(comment01);
		assertEquals(1, flag);

		// 4.
		assertEquals(baseCnt, mapper.totalCnt());
		assertEquals(0, mapper.likeCheck(like01));
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
