/**
 * <pre>
 * Class Name  : CollectionLikeDaoTest
 * Description : CollectionLikeMapper의 CRUD 기능을 검증하는 JUnit 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13.	gunwoo        최초생성
 * 2026. 8. 14.	gunwoo        @BeforeEach + DB 실데이터 5건 하드코딩 방식으로 변경
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 13.
 */
package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.endit.domain.CollectionLikeVO;

/**
 * ===== 클래스 레벨 애노테이션 설명 =====
 *
 * @SpringBootTest
 *   - 이 테스트를 실행할 때 스프링 부트 애플리케이션 전체를 띄운다.
 *
 * @Transactional
 *   - 테스트 클래스(또는 메서드)에 이 애노테이션을 붙이면,
 *     테스트 안에서 실행한 insert/update/delete가 테스트가 끝나는 순간
 *     자동으로 ROLLBACK 된다.
 *   - "테스트니까 DB를 더럽히지 말자"는 안전장치.
 */
@SpringBootTest
@Transactional
class CollectionLikeDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * @Autowired
	 *   - 스프링이 만들어둔 CollectionLikeMapper의 "실제 구현체"를
	 *     자동으로 주입(연결)해준다.
	 */
	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	// ===== 테스트용 데이터 =====
	// endit_test의 COLLECTION_LIKE 테이블에 실제로 존재하는 값을 그대로 하드코딩
	private CollectionLikeVO like01;
	private CollectionLikeVO like02;
	private CollectionLikeVO like03;
	private CollectionLikeVO like04;
	private CollectionLikeVO like05;

	// 아직 COLLECTION_LIKE에 없는 조합 (insert/delete 테스트용)
	private CollectionLikeVO newLike;

	@BeforeEach
	void setUp() {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");

		like01 = new CollectionLikeVO(1, 2, null);
		like02 = new CollectionLikeVO(2, 1, null);
		like03 = new CollectionLikeVO(3, 1, null);
		like04 = new CollectionLikeVO(4, 3, null);
		like05 = new CollectionLikeVO(5, 4, null);

		// MEMBER_ID=1, COLLECTION_ID=1은 각각 부모 테이블에 실존하지만
		// 두 값의 조합(1,1)은 COLLECTION_LIKE에 아직 없는 상태
		newLike = new CollectionLikeVO(1, 1, null);

		log.debug("like01: {}", like01);
		log.debug("like02: {}", like02);
		log.debug("like03: {}", like03);
		log.debug("like04: {}", like04);
		log.debug("like05: {}", like05);
		log.debug("newLike: {}", newLike);
	}

	/**
	 * [테스트 1] 좋아요 등록(insert)이 정상적으로 되는지 확인
	 */
	@Test
	@DisplayName("좋아요 등록 테스트")
	void testInsertCollectionLike() {
		log.debug("---------------------------");
		log.debug("*testInsertCollectionLike()*");
		log.debug("---------------------------");

		int result = collectionLikeMapper.insertCollectionLike(newLike);

		assertEquals(1, result, "insert된 행 수는 1이어야 합니다.");
		log.debug("insert 결과 : {}", result);
	}

	/**
	 * [테스트 2] 이미 등록되어 있는 좋아요가 조회되는지 확인
	 */
	@Test
	@DisplayName("좋아요 단건 조회 테스트")
	void testSelectCollectionLike() {
		log.debug("---------------------------");
		log.debug("*testSelectCollectionLike()*");
		log.debug("---------------------------");

		CollectionLikeVO result = collectionLikeMapper.selectCollectionLike(like01);

		assertNotNull(result, "이미 등록된 데이터가 조회되어야 합니다.");
		assertEquals(like01.getMemberId(), result.getMemberId());
		assertEquals(like01.getCollectionId(), result.getCollectionId());
		log.debug("조회 결과 : {}", result);
	}

	/**
	 * [테스트 3] 없는 데이터를 조회하면 null이 나오는지 확인
	 */
	@Test
	@DisplayName("존재하지 않는 좋아요는 조회되지 않아야 한다")
	void testSelectCollectionLike_notExists() {
		log.debug("---------------------------");
		log.debug("*testSelectCollectionLike_notExists()*");
		log.debug("---------------------------");

		CollectionLikeVO result = collectionLikeMapper.selectCollectionLike(newLike);

		assertNull(result, "아직 등록되지 않은 데이터는 null이어야 합니다.");
		log.debug("조회 결과 : {}", result);
	}

	/**
	 * [테스트 4] 특정 회원이 좋아요 누른 컬렉션 목록이 잘 나오는지 확인
	 */
	@Test
	@DisplayName("특정 회원이 좋아요 누른 컬렉션 목록 조회 테스트")
	void testSelectCollectionLikeListByMember() {
		log.debug("---------------------------");
		log.debug("*testSelectCollectionLikeListByMember()*");
		log.debug("---------------------------");

		List<CollectionLikeVO> list = collectionLikeMapper.selectCollectionLikeListByMember(like01.getMemberId());

		assertNotNull(list);
		assertTrue(list.size() > 0, "최소 1건 이상 조회되어야 합니다.");
		log.debug("회원별 좋아요 목록 : {}", list);
	}

	/**
	 * [테스트 5] 특정 컬렉션에 좋아요 누른 회원 목록이 잘 나오는지 확인
	 */
	@Test
	@DisplayName("특정 컬렉션에 좋아요 누른 회원 목록 조회 테스트")
	void testSelectCollectionLikeListByCollection() {
		log.debug("---------------------------");
		log.debug("*testSelectCollectionLikeListByCollection()*");
		log.debug("---------------------------");

		List<CollectionLikeVO> list = collectionLikeMapper.selectCollectionLikeListByCollection(like01.getCollectionId());

		assertNotNull(list);
		assertTrue(list.size() > 0, "최소 1건 이상 조회되어야 합니다.");
		log.debug("컬렉션별 좋아요 목록 : {}", list);
	}

	/**
	 * [테스트 6] 특정 컬렉션의 좋아요 총 개수가 잘 세어지는지 확인
	 */
	@Test
	@DisplayName("좋아요 개수 조회 테스트")
	void testSelectCollectionLikeCount() {
		log.debug("---------------------------");
		log.debug("*testSelectCollectionLikeCount()*");
		log.debug("---------------------------");

		int count = collectionLikeMapper.selectCollectionLikeCount(like01.getCollectionId());

		assertTrue(count >= 1, "좋아요 개수는 1개 이상이어야 합니다.");
		log.debug("좋아요 개수 : {}", count);
	}

	/**
	 * [테스트 7] 좋아요 취소(delete)가 정상적으로 되는지 확인
	 */
	@Test
	@DisplayName("좋아요 취소(삭제) 테스트")
	void testDeleteCollectionLike() {
		log.debug("---------------------------");
		log.debug("*testDeleteCollectionLike()*");
		log.debug("---------------------------");

		// Given : 삭제할 좋아요를 먼저 등록
		collectionLikeMapper.insertCollectionLike(newLike);

		// When
		int result = collectionLikeMapper.deleteCollectionLike(newLike);

		// Then
		assertEquals(1, result, "delete된 행 수는 1이어야 합니다.");

		CollectionLikeVO afterDelete = collectionLikeMapper.selectCollectionLike(newLike);
		assertNull(afterDelete, "삭제 후에는 조회되지 않아야 합니다.");
		log.debug("삭제 결과 : {}건", result);
	}

}