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

import com.endit.domain.CollectionLikeItemVO;
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
 * 2026. 8. 14. jinyoung    테스트 시작 전 전체 삭제 및 건수 검증 추가
 * 2026. 8. 18. gunwoo      회원별 좋아요 컬렉션 목록 JOIN/페이징 테스트 추가
 * 2026. 8. 19. jinyoung    페이징 동률 정렬 및 페이지 간 비중복 검증 추가
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

	// 조회, 목록 및 개수 검증에 사용하는 컬렉션 좋아요 데이터
	private CollectionLikeVO like01;

	// 등록, 미존재 조회 및 삭제 검증에 사용하는 컬렉션 좋아요 데이터
	private CollectionLikeVO newLike;

	/**
	 * 공용 DB의 회원과 컬렉션 더미 데이터를 이용한 테스트 데이터 준비
	 */
	@BeforeEach
	void setUp() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ setUp()                      │");
		log.debug("└──────────────────────────────┘");

		collectionLikeMapper.deleteAll();
		assertEquals(0, collectionLikeMapper.totalCnt());

		log.debug("* initializedData: totalCnt-{}건", collectionLikeMapper.totalCnt());

		// MEMBER_ID 1과 COLLECTION_ID 2는 공용 DB에 존재하는 부모 더미 데이터
		like01 = new CollectionLikeVO(1, 2, null);

		// MEMBER_ID 1과 COLLECTION_ID 1도 공용 DB에 존재하는 부모 더미 데이터
		newLike = new CollectionLikeVO(1, 1, null);

		log.debug("* testData01: memberId-{}, collectionId-{}, createdDt-{}",
				like01.getMemberId(), like01.getCollectionId(), like01.getCreatedDt());

		log.debug("* testData02: memberId-{}, collectionId-{}, createdDt-{}",
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

		// When: 회원·컬렉션 조합을 등록
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

		// Given: 조회할 컬렉션 좋아요를 등록
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like01));

		// When: 회원 번호와 컬렉션 번호를 이용해 단건 조회
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

		// When: 등록하지 않은 회원·컬렉션 조합을 조회
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

		// Given: 목록에서 조회할 컬렉션 좋아요를 등록
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like01));

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

		// Given: 목록에서 조회할 컬렉션 좋아요를 등록
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like01));

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

		// Given: 개수를 확인할 컬렉션 좋아요를 등록
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like01));

		// When: 컬렉션 번호를 이용해 좋아요 개수를 조회
		int count = collectionLikeMapper.selectCollectionLikeCount(like01.getCollectionId());

		// Then: 등록한 컬렉션 좋아요 한 건이 조회되어야 함
		assertEquals(1, count, "좋아요 개수는 한 개여야 합니다.");

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

	// ===================================================================
	// 아래부터는 selectLikedCollectionListByMember (JOIN + 페이징) 검증
	// ===================================================================

	/**
	 * JOIN 결과로 컬렉션의 제목/설명 등 화면 정보가 함께 조회되는지 검증
	 */
	@Test
	@DisplayName("회원별 좋아요 컬렉션 JOIN 결과 확인")
	void testSelectLikedCollectionListByMember_join() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectLikedCollectionList_join() │");
		log.debug("└──────────────────────────────┘");

		// Given: 좋아요 한 건 등록 (COLLECTION_ID=2는 공용 DB에 TITLE/DESCRIPTION이 있는 더미 데이터)
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like01));

		// When: 회원별 좋아요 컬렉션 목록(JOIN)을 1페이지, 페이지당 10건으로 조회
		List<CollectionLikeItemVO> list =
				collectionLikeMapper.selectLikedCollectionListByMember(like01.getMemberId(), 1, 10);

		// Then: COLLECTION 테이블과 조인되어 title이 채워져 있어야 함 (COLLECTION_LIKE만으로는 얻을 수 없는 정보)
		assertNotNull(list);
		assertEquals(1, list.size());

		CollectionLikeItemVO item = list.get(0);
		assertEquals(like01.getCollectionId(), item.getCollectionId());
		assertNotNull(item.getTitle(), "JOIN을 통해 컬렉션 제목이 채워져야 합니다.");
		assertNotNull(item.getLikedDt(), "좋아요 등록 일시가 채워져야 합니다.");

		log.debug("* joinedItem: {}", item);
	}

	/**
	 * 1페이지 조회 시 pageSize만큼 결과가 나오는지 검증
	 */
	@Test
	@DisplayName("좋아요 컬렉션 목록 1페이지 조회")
	void testSelectLikedCollectionListByMember_firstPage() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectLikedCollectionList_firstPage() │");
		log.debug("└──────────────────────────────┘");

		// Given: 회원1이 컬렉션1, 컬렉션2 두 곳에 좋아요 등록
		assertEquals(1, collectionLikeMapper.insertCollectionLike(newLike)); // memberId=1, collectionId=1
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like01));  // memberId=1, collectionId=2

		// When: pageSize=1로 1페이지 조회
		List<CollectionLikeItemVO> firstPage =
				collectionLikeMapper.selectLikedCollectionListByMember(1, 1, 1);

		// Then: 정확히 1건만 조회되어야 함
		assertNotNull(firstPage);
		assertEquals(1, firstPage.size(), "pageSize가 1이면 1건만 조회되어야 합니다.");

		log.debug("* firstPage: {}", firstPage);
	}

	/**
	 * 마지막 페이지 조회 시 남은 건수만큼만 결과가 나오는지 검증
	 */
	@Test
	@DisplayName("좋아요 컬렉션 목록 마지막 페이지 조회")
	void testSelectLikedCollectionListByMember_lastPage() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectLikedCollectionList_lastPage() │");
		log.debug("└──────────────────────────────┘");

		// Given: 회원1이 컬렉션1, 컬렉션2 두 곳에 좋아요 등록 (총 2건)
		assertEquals(1, collectionLikeMapper.insertCollectionLike(newLike)); // memberId=1, collectionId=1
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like01));  // memberId=1, collectionId=2

		// When: pageSize=1로 2페이지(마지막 페이지) 조회
		List<CollectionLikeItemVO> lastPage =
				collectionLikeMapper.selectLikedCollectionListByMember(1, 2, 1);
		List<CollectionLikeItemVO> firstPage =
				collectionLikeMapper.selectLikedCollectionListByMember(1, 1, 1);

		// Then: 각 페이지에 한 건씩 조회되고 페이지 간 데이터가 중복되지 않아야 함
		assertNotNull(lastPage);
		assertNotNull(firstPage);
		assertEquals(1, firstPage.size(), "1페이지에는 한 건이 조회되어야 합니다.");
		assertEquals(1, lastPage.size(), "2페이지에는 남은 1건만 조회되어야 합니다.");
		assertEquals(like01.getCollectionId(), firstPage.get(0).getCollectionId(),
				"최신 좋아요가 1페이지에 조회되어야 합니다.");
		assertEquals(newLike.getCollectionId(), lastPage.get(0).getCollectionId(),
				"이전 좋아요가 2페이지에 조회되어야 합니다.");
		assertTrue(firstPage.stream()
				.noneMatch(firstItem -> lastPage.stream()
						.anyMatch(lastItem ->
								lastItem.getCollectionId() == firstItem.getCollectionId())),
				"페이지 간 동일한 컬렉션이 중복 조회되면 안 됩니다.");

		log.debug("* firstPage: {}", firstPage);
		log.debug("* lastPage: {}", lastPage);
	}

	/**
	 * 좋아요한 컬렉션이 없는 회원을 조회하면 빈 리스트가 반환되는지 검증
	 */
	@Test
	@DisplayName("좋아요한 컬렉션이 없으면 빈 리스트 반환")
	void testSelectLikedCollectionListByMember_empty() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectLikedCollectionList_empty() │");
		log.debug("└──────────────────────────────┘");

		// Given: 아무 데이터도 등록하지 않음 (setUp에서 이미 deleteAll 처리됨)

		// When: 좋아요를 누른 적 없는 회원 번호로 조회
		List<CollectionLikeItemVO> list =
				collectionLikeMapper.selectLikedCollectionListByMember(1, 1, 10);

		// Then: null이 아니라 빈 리스트여야 함
		assertNotNull(list, "결과가 없어도 null이 아니라 빈 리스트여야 합니다.");
		assertTrue(list.isEmpty(), "좋아요한 컬렉션이 없으면 빈 리스트여야 합니다.");

		log.debug("* emptyResult: size-{}", list.size());
	}

	/**
	 * 목록 조회 결과 건수와 count 쿼리 결과가 일치하는지 검증
	 */
	@Test
	@DisplayName("좋아요 컬렉션 목록과 count 결과 일치 확인")
	void testSelectLikedCollectionListByMember_countMatches() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ selectLikedCollectionList_countMatches() │");
		log.debug("└──────────────────────────────┘");

		// Given: 회원1이 컬렉션1, 컬렉션2 두 곳에 좋아요 등록
		assertEquals(1, collectionLikeMapper.insertCollectionLike(newLike)); // memberId=1, collectionId=1
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like01));  // memberId=1, collectionId=2

		// When: 페이지 크기를 충분히 크게 잡아 전체를 한 번에 조회 + count 쿼리도 함께 조회
		List<CollectionLikeItemVO> list =
				collectionLikeMapper.selectLikedCollectionListByMember(1, 1, 10);
		int totalCount = collectionLikeMapper.selectLikedCollectionCountByMember(1);

		// Then: 목록 건수와 count 쿼리 결과가 같아야 함
		assertEquals(totalCount, list.size(), "목록 조회 건수와 count 쿼리 결과가 일치해야 합니다.");
		assertEquals(2, totalCount);

		log.debug("* listSize: {}, totalCount: {}", list.size(), totalCount);
	}

}
