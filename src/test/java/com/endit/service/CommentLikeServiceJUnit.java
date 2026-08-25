/**
 * <pre>
 * Class Name : CommentLikeServiceJUnit
 * Description : 코멘트 좋아요 Service JUnit
 *               핵심은 upToggleLike — 등록↔취소 토글이 한 번씩 번갈아 동작하는지.
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

import static com.endit.service.CommentLikeService.LIKE_OFF;
import static com.endit.service.CommentLikeService.LIKE_ON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.CommentLikeVO;
import com.endit.domain.UserCommentVO;
import com.endit.mapper.UserCommentMapper;

@SpringBootTest
@Transactional
@DisplayName("CommentLikeService 테스트")
class CommentLikeServiceJUnit {

	final Logger log = LoggerFactory.getLogger(getClass());

	// 공용 DB 더미 데이터의 실제 부모 값 (회의 규칙: 부모 값은 DB 기반 하드코딩)
	private static final long MEMBER_AUTHOR = 9L; // admin1@endit.com — 더미 코멘트 없음
	private static final long MEMBER_LIKER = 10L; // admin2@endit.com — 좋아요 누르는 회원
	private static final long CONTENT_A = 9L;     // 어벤져스: 인피니티 워 — 더미 코멘트 없는 영화

	@Autowired
	CommentLikeService service;

	@Autowired
	UserCommentMapper commentMapper; // 사전조건(대상 코멘트) 구성용

	private UserCommentVO comment01; // 좋아요 대상 코멘트 (트랜잭션 안에서 생성)
	private CommentLikeVO like01;    // 회원10이 comment01에 누르는 좋아요

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		// 방금 만든 코멘트라 더미 좋아요가 붙어 있을 수 없다 → 집계가 결정적
		comment01 = new UserCommentVO(0, MEMBER_AUTHOR, CONTENT_A, null, "토글 대상 한줄평", UserCommentVO.SPOILER_NO,
				null, null);
		commentMapper.doSave(comment01);

		like01 = new CommentLikeVO(MEMBER_LIKER, comment01.getCommentId(), null);

		log.debug("comment01: {}", comment01);
		log.debug("like01: {}", like01);
	}

	@Test
	public void upToggleLike() {
		log.debug("---------------------------");
		log.debug("*upToggleLike()*");
		log.debug("---------------------------");
		// 1. 처음: 안 누른 상태
		// 2. 1차 토글 → 등록(LIKE_ON), 좋아요 수 1
		// 3. 2차 토글 → 취소(LIKE_OFF), 좋아요 수 0

		// 1.
		assertEquals(0, service.likeCheck(like01));
		assertEquals(0, service.getLikeCnt(comment01.getCommentId()));

		// 2.
		assertEquals(LIKE_ON, service.upToggleLike(like01));
		assertEquals(1, service.likeCheck(like01));
		assertEquals(1, service.getLikeCnt(comment01.getCommentId()));

		// 3.
		assertEquals(LIKE_OFF, service.upToggleLike(like01));
		assertEquals(0, service.likeCheck(like01));
		assertEquals(0, service.getLikeCnt(comment01.getCommentId()));
	}

	@Test
	public void doSaveAndDoDelete() {
		log.debug("---------------------------");
		log.debug("*doSaveAndDoDelete()*");
		log.debug("---------------------------");
		// 단순 위임 확인 — 등록/취소
		// 1. 실행 전 건수
		// 2. 등록 → +1
		// 3. 취소 → 원상

		// 1.
		int baseCnt = service.totalCnt();

		// 2.
		int flag = service.doSave(like01);
		assertEquals(1, flag);
		assertEquals(baseCnt + 1, service.totalCnt());

		// 3.
		flag = service.doDelete(like01);
		assertEquals(1, flag);
		assertEquals(baseCnt, service.totalCnt());
	}

	@Test
	void beans() {
		log.debug("---------------------------");
		log.debug("*beans()*");
		log.debug("---------------------------");
		assertNotNull(service);
		assertNotNull(commentMapper);
		log.debug("service: {}", service);
	}

}
