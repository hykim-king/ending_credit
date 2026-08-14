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
 * <pre>
 * Class Name  : CollectionLikeDaoTest
 * Description : 컬렉션 좋아요 Mapper의 등록, 조회 및 삭제 기능을 검증하는 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13. gunwoo      최초 생성
 * 2026. 8. 14. gunwoo      @BeforeEach + DB 실데이터 5건 하드코딩 방식으로 변경
 * 2026. 8. 14. jinyoung    주석, 로그 및 테스트 데이터 선언 정리
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 13.
 */
@SpringBootTest
@Transactional
@DisplayName("CollectionLikeMapper 테스트")
class CollectionLikeDaoTest {

	private static final Logger log = LoggerFactory.getLogger(CollectionLikeDaoTest.class);

	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	// 공용 DB의 COLLECTION_LIKE 테이블에 존재하는 데이터
	private CollectionLikeVO like01;

	// 부모 테이블에는 존재하지만 COLLECTION_LIKE에는 등록되지 않은 조합
	private CollectionLikeVO newLike;

	/**
	 * 공용 DB 더미 데이터를 이용한 컬렉션 좋아요 테스트 데이터 준비
	 */
	@BeforeEach
	void setUp() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ setUp()                      │");
		log.debug("└──────────────────────────────┘");

		// MEMBER_ID 1과 COLLECTION_ID 2의 조합은 COLLECTION_LIKE에 존재
		like01 = new CollectionLikeVO(1, 2, null);

		// MEMBER_ID 1과 COLLECTION_ID 1은 부모 테이블에 각각 존재하지만
		// 두 번호의 조합은 COLLECTION_LIKE에 존재하지 않음
		newLike = new CollectionLikeVO(1, 1, null);

		log.debug("* existingData: memberId-{}, collectionId-{}, createdDt-{}",
				like01.getMemberId(), like01.getCollectionId(), like01.getCreatedDt());

		log.debug("* testData: memberId-{}, collectionId-{}, createdDt-{}",
				newLike.getMemberId(), newLike.getCollectionId(), newLike.getCreatedDt());
	}

	/**
	 * 컬렉션 좋아요 등록 검증
	 */
	@Test
	@DisplayName("컬렉션 좋아요 등록")
	void testInsertCollectionLike() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ insertCollectionLike()       │");
		log.debug("└──────────────────────────────┘");

		// When: 공용 DB에 존재하지 않는 회원·컬렉션 조합을 등록
		int result = collectionLikeMapper.insertCollectionLike(newLike);

		// Then: 한 건이 등록되어야 함
		assertEquals(1, result, "등록된 행 수는 1이어야 합니다.");

		log.debug("* insertedData: memberId-{}, collectionId-{}, result-{}건",
				newLike.getMemberId(), newLike.getCollectionId(), result);
	}

	/**
	 * 회원 번호와 컬렉션 번호를 이용한 단건 조회 검증
	 */
	@Test
	@DisplayName("컬렉션 좋아요 단건 조회")
	void testSelectCollectionLike() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectCollectionLike()       │");
		log.debug("└──────────────────────────────┘");

		// When: 공용 DB에 등록된 회원·컬렉션 조합을 조회
		CollectionLikeVO result = collectionLikeMapper.selectCollectionLike(like01);

		// Then: 등록된 키값과 좋아요 등록 일시가 조회되어야 함
		assertNotNull(result, "등록된 데이터가 조회되어야 합니다.");
		assertEquals(like01.getMemberId(), result.getMemberId());
		assertEquals(like01.getCollectionId(), result.getCollectionId());
		assertNotNull(result.getCreatedDt());

		log.debug("* selectedData: memberId-{}, collectionId-{}, createdDt-{}",
				result.getMemberId(), result.getCollectionId(), result.getCreatedDt());
	}

	/**
	 * 존재하지 않는 컬렉션 좋아요 단건 조회 검증
	 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 좋아요 조회")
	void testSelectCollectionLike_notExists() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectNotExists()            │");
		log.debug("└──────────────────────────────┘");

		// When: COLLECTION_LIKE에 등록되지 않은 조합을 조회
		CollectionLikeVO result = collectionLikeMapper.selectCollectionLike(newLike);

		// Then: 조회 결과가 없어야 함
		assertNull(result, "등록되지 않은 데이터는 null이어야 합니다.");

		log.debug("* selectedData: {}", result);
	}

	/**
	 * 회원 번호를 이용한 컬렉션 좋아요 목록 조회 검증
	 */
	@Test
	@DisplayName("회원별 컬렉션 좋아요 목록 조회")
	void testSelectCollectionLikeListByMember() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectListByMember()         │");
		log.debug("└──────────────────────────────┘");

		// When: 회원 번호를 이용해 컬렉션 좋아요 목록을 조회
		List<CollectionLikeVO> list =
				collectionLikeMapper.selectCollectionLikeListByMember(like01.getMemberId());

		// Then: 해당 회원의 컬렉션 좋아요가 한 건 이상 조회되어야 함
		assertNotNull(list);
		assertTrue(list.size() > 0, "최소 한 건 이상 조회되어야 합니다.");

		log.debug("* retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: memberId-{}, collectionId-{}, createdDt-{}",
					item.getMemberId(), item.getCollectionId(), item.getCreatedDt()));
	}

	/**
	 * 컬렉션 번호를 이용한 회원 좋아요 목록 조회 검증
	 */
	@Test
	@DisplayName("컬렉션별 좋아요 회원 목록 조회")
	void testSelectCollectionLikeListByCollection() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectListByCollection()     │");
		log.debug("└──────────────────────────────┘");

		// When: 컬렉션 번호를 이용해 좋아요 회원 목록을 조회
		List<CollectionLikeVO> list =
				collectionLikeMapper.selectCollectionLikeListByCollection(like01.getCollectionId());

		// Then: 해당 컬렉션의 좋아요 회원이 한 명 이상 조회되어야 함
		assertNotNull(list);
		assertTrue(list.size() > 0, "최소 한 건 이상 조회되어야 합니다.");

		log.debug("* retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: memberId-{}, collectionId-{}, createdDt-{}",
					item.getMemberId(), item.getCollectionId(), item.getCreatedDt()));
	}

	/**
	 * 컬렉션 번호를 이용한 좋아요 개수 조회 검증
	 */
	@Test
	@DisplayName("컬렉션 좋아요 개수 조회")
	void testSelectCollectionLikeCount() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectCollectionLikeCount()  │");
		log.debug("└──────────────────────────────┘");

		// When: 컬렉션 번호를 이용해 좋아요 개수를 조회
		int count = collectionLikeMapper.selectCollectionLikeCount(like01.getCollectionId());

		// Then: 좋아요 개수가 한 개 이상이어야 함
		assertTrue(count >= 1, "좋아요 개수는 한 개 이상이어야 합니다.");

		log.debug("* likeCount: collectionId-{}, count-{}건",
				like01.getCollectionId(), count);
	}

	/**
	 * 컬렉션 좋아요 삭제 검증
	 */
	@Test
	@DisplayName("컬렉션 좋아요 삭제")
	void testDeleteCollectionLike() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ deleteCollectionLike()       │");
		log.debug("└──────────────────────────────┘");

		// Given: 삭제할 컬렉션 좋아요를 먼저 등록
		assertEquals(1, collectionLikeMapper.insertCollectionLike(newLike));

		// When: 회원 번호와 컬렉션 번호를 이용해 좋아요를 삭제
		int result = collectionLikeMapper.deleteCollectionLike(newLike);

		// Then: 한 건이 삭제되고 다시 조회되지 않아야 함
		assertEquals(1, result, "삭제된 행 수는 1이어야 합니다.");

		CollectionLikeVO afterDelete =
				collectionLikeMapper.selectCollectionLike(newLike);

		assertNull(afterDelete, "삭제 후에는 조회되지 않아야 합니다.");

		log.debug("* deletedData: memberId-{}, collectionId-{}, result-{}건",
				newLike.getMemberId(), newLike.getCollectionId(), result);
	}
}