package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;

/**
 * <pre>
 * Class Name  : CollectionItemMapperDaoTest
 * Description : 컬렉션 콘텐츠 Mapper의 등록, 조회 및 삭제 기능을 검증하는 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13. jinyoung    최초 생성
 * 2026. 8. 14. jinyoung    공용 DB 더미 데이터 기반 테스트 구조로 변경
 * 2026. 8. 14. jinyoung    테스트 시작 전 전체 삭제 및 건수 검증 추가
 * 2026. 8. 19. jinyoung    목록 조회 콘텐츠 조인 및 페이징 검증 보완
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 13.
 */
@SpringBootTest
@Transactional
//@Disabled("deleteAll() 전체 삭제를 제거하고 테스트 데이터를 격리할 때까지 비활성화")
@DisplayName("CollectionItem 테스트")
class CollectionItemMapperDaoTest {

	// 공용 DB의 부모 테이블 더미 데이터에서 사용하는 번호
	private static final int TEST_COLLECTION_ID = 10;
	private static final int TEST_CONTENT_ID = 1;
	private static final int SECOND_CONTENT_ID = 2;
	private static final int THIRD_CONTENT_ID = 3;
	private static final int MISSING_CONTENT_ID = 999_999_999;

	private static final Logger log = LoggerFactory.getLogger(CollectionItemMapperDaoTest.class);

	@Autowired
	private CollectionItemMapper mapper;

	// 각 테스트에서 사용하는 컬렉션 콘텐츠 데이터
	private CollectionItemVO testData;

	/**
	 * 공용 DB의 컬렉션과 콘텐츠 더미 데이터를 이용한 입력 데이터 준비
	 */
	@BeforeEach
	void setUp() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ setUp()                      │");
		log.debug("└──────────────────────────────┘");

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		log.debug("* initializedData: totalCnt-{}건", mapper.totalCnt());

		// COLLECTION_ID 10과 CONTENT_ID 1은 공용 DB에 존재하는 부모 더미 데이터
		// 두 번호의 조합은 COLLECTION_ITEM 더미 데이터에 없으므로 등록 테스트에 사용 가능
		// ADDED_DT는 Mapper에서 SYSDATE로 등록
		testData = new CollectionItemVO(TEST_COLLECTION_ID, TEST_CONTENT_ID, null);

		log.debug("* testData: collectionId-{}, contentId-{}, addedDt-{}",
				testData.getCollectionId(), testData.getContentId(), testData.getAddedDt());
	}

	/**
	 * 컬렉션 콘텐츠 등록 검증
	 */
	@Test
	@DisplayName("컬렉션 콘텐츠 등록")
	void doSave() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSave()                     │");
		log.debug("└──────────────────────────────┘");

		// When: 컬렉션 콘텐츠를 등록하고 실제 저장된 데이터를 다시 조회
		int flag = mapper.doSave(testData);
		CollectionItemVO outVO = mapper.doSelectOne(testData);

		// Then: 한 건이 등록되고 DB에서 생성한 추가 일시가 조회되어야 함
		assertEquals(1, flag);
		assertNotNull(outVO);

		log.debug("* savedData: collectionId-{}, contentId-{}, addedDt-{}",
				outVO.getCollectionId(), outVO.getContentId(), outVO.getAddedDt());

		assertEquals(testData.getCollectionId(), outVO.getCollectionId());
		assertEquals(testData.getContentId(), outVO.getContentId());
		assertNotNull(outVO.getAddedDt());
	}

	/**
	 * 컬렉션 번호와 콘텐츠 번호를 이용한 단건 조회 검증
	 */
	@Test
	@DisplayName("컬렉션 콘텐츠 단건 조회")
	void doSelectOne() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSelectOne()                │");
		log.debug("└──────────────────────────────┘");

		// Given: 조회할 컬렉션 콘텐츠를 등록
		assertEquals(1, mapper.doSave(testData));

		// When: 복합 PK 전체를 이용해 단건 조회
		CollectionItemVO outVO = mapper.doSelectOne(testData);

		// Then: 등록한 키값과 추가 일시가 조회되어야 함
		assertNotNull(outVO);

		log.debug("* selectedData: collectionId-{}, contentId-{}, addedDt-{}",
				outVO.getCollectionId(), outVO.getContentId(), outVO.getAddedDt());

		assertEquals(testData.getCollectionId(), outVO.getCollectionId());
		assertEquals(testData.getContentId(), outVO.getContentId());
		assertNotNull(outVO.getAddedDt());
	}

	/**
	 * 컬렉션 번호와 콘텐츠 번호를 이용한 삭제 검증
	 */
	@Test
	@DisplayName("컬렉션 콘텐츠 삭제")
	void doDelete() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doDelete()                   │");
		log.debug("└──────────────────────────────┘");

		// Given: 삭제할 컬렉션 콘텐츠를 등록하고 삭제 전 데이터를 조회
		assertEquals(1, mapper.doSave(testData));

		CollectionItemVO beforeDeleteVO = mapper.doSelectOne(testData);

		assertNotNull(beforeDeleteVO);

		// When: 복합 PK 전체를 이용해 삭제
		int flag = mapper.doDelete(testData);

		// Then: 삭제한 컬렉션 콘텐츠는 조회되지 않아야 함
		CollectionItemVO afterDeleteVO = mapper.doSelectOne(testData);

		assertEquals(1, flag);
		assertNull(afterDeleteVO);

		log.debug("* deletedData: collectionId-{}, contentId-{}, addedDt-{}, result-{}건",
				beforeDeleteVO.getCollectionId(), beforeDeleteVO.getContentId(), beforeDeleteVO.getAddedDt(), flag);
	}

	/**
	 * 컬렉션 번호 조건을 이용한 콘텐츠 목록 조회 검증
	 */
	@Test
	@DisplayName("컬렉션별 콘텐츠 목록 조회")
	void doRetrieve() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieve()                 │");
		log.debug("└──────────────────────────────┘");

		// Given: 목록에서 조회할 컬렉션 콘텐츠와 검색 조건을 준비
		assertEquals(1, mapper.doSave(testData));

		DTO search = new DTO();
		search.setPageNo(1);
		search.setPageSize(10);
		search.setSearchDiv("10");
		search.setSearchWord(String.valueOf(testData.getCollectionId()));

		// When: 컬렉션 번호를 조건으로 콘텐츠 목록을 조회
		List<CollectionItemVO> list = mapper.doRetrieve(search);

		// Then: 등록한 컬렉션 콘텐츠가 조회 결과에 포함되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: collectionId-{}, contentId-{}, titleKo-{}, releaseYear-{}, addedDt-{}",
					item.getCollectionId(), item.getContentId(), item.getTitleKo(), item.getReleaseYear(),
					item.getAddedDt()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getCollectionId() == testData.getCollectionId()	// 컬렉션 번호 일치 검증
						&& item.getContentId() == testData.getContentId()		// 콘텐츠 번호 일치 검증
						&& item.getExternalId() != null							// 콘텐츠 조인 결과 검증
				));
	}

	/**
	 * 콘텐츠 번호 조건을 이용한 컬렉션 콘텐츠 목록 조회 검증
	 */
	@Test
	@DisplayName("콘텐츠 번호 조건 컬렉션 콘텐츠 목록 조회")
	void doRetrieveByContentId() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieveByContentId()      │");
		log.debug("└──────────────────────────────┘");

		// Given: 검색 대상과 검색에서 제외할 컬렉션 콘텐츠를 등록
		assertEquals(1, mapper.doSave(testData));

		CollectionItemVO comparisonData = new CollectionItemVO(
				testData.getCollectionId(),
				SECOND_CONTENT_ID,
				null);

		assertEquals(1, mapper.doSave(comparisonData));

		DTO search = new DTO();
		search.setPageNo(1);
		search.setPageSize(10);
		search.setSearchDiv("20");
		search.setSearchWord(String.valueOf(testData.getContentId()));

		// When: 콘텐츠 번호를 조건으로 컬렉션 콘텐츠 목록을 조회
		List<CollectionItemVO> list = mapper.doRetrieve(search);

		// Then: 검색한 콘텐츠 번호에 해당하는 데이터만 조회되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("* retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: collectionId-{}, contentId-{}, addedDt-{}",
					item.getCollectionId(), item.getContentId(), item.getAddedDt()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getCollectionId() == testData.getCollectionId()
						&& item.getContentId() == testData.getContentId()
				));

		assertFalse(list.stream()
				.anyMatch(item ->
						item.getContentId() == comparisonData.getContentId()
				));
	}

	/**
	 * 컬렉션 콘텐츠 목록 페이징 처리 검증
	 */
	@Test
	@DisplayName("컬렉션 콘텐츠 목록 페이징")
	void doRetrievePaging() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrievePaging()           │");
		log.debug("└──────────────────────────────┘");

		// Given: 같은 컬렉션에 콘텐츠 세 건을 등록
		assertEquals(1, mapper.doSave(testData));

		CollectionItemVO secondData = new CollectionItemVO(
				testData.getCollectionId(), SECOND_CONTENT_ID, null);

		CollectionItemVO thirdData = new CollectionItemVO(
				testData.getCollectionId(), THIRD_CONTENT_ID, null);

		assertEquals(1, mapper.doSave(secondData));
		assertEquals(1, mapper.doSave(thirdData));

		DTO firstPageSearch = new DTO();
		firstPageSearch.setPageNo(1);
		firstPageSearch.setPageSize(2);
		firstPageSearch.setSearchDiv("10");
		firstPageSearch.setSearchWord(String.valueOf(testData.getCollectionId()));

		DTO secondPageSearch = new DTO();
		secondPageSearch.setPageNo(2);
		secondPageSearch.setPageSize(2);
		secondPageSearch.setSearchDiv("10");
		secondPageSearch.setSearchWord(String.valueOf(testData.getCollectionId()));

		// When: 페이지 크기를 두 건으로 설정해 1페이지와 2페이지를 조회
		List<CollectionItemVO> firstPage = mapper.doRetrieve(firstPageSearch);
		List<CollectionItemVO> secondPage = mapper.doRetrieve(secondPageSearch);

		// Then: 1페이지에는 두 건, 2페이지에는 한 건이 조회되어야 함
		assertNotNull(firstPage);
		assertNotNull(secondPage);
		assertEquals(2, firstPage.size());
		assertEquals(1, secondPage.size());

		log.debug("* firstPageCount: {}건", firstPage.size());
		log.debug("* secondPageCount: {}건", secondPage.size());

		firstPage.forEach(item ->
			log.debug("* firstPageData: collectionId-{}, contentId-{}, titleKo-{}, addedDt-{}",
					item.getCollectionId(), item.getContentId(), item.getTitleKo(), item.getAddedDt()));

		secondPage.forEach(item ->
			log.debug("* secondPageData: collectionId-{}, contentId-{}, titleKo-{}, addedDt-{}",
					item.getCollectionId(), item.getContentId(), item.getTitleKo(), item.getAddedDt()));

		assertTrue(firstPage.stream().allMatch(item -> item.getExternalId() != null));
		assertTrue(secondPage.stream().allMatch(item -> item.getExternalId() != null));

		assertTrue(firstPage.stream()
				.noneMatch(firstItem ->
						secondPage.stream()
								.anyMatch(secondItem ->
										secondItem.getCollectionId() == firstItem.getCollectionId()
										&& secondItem.getContentId() == firstItem.getContentId()
								)
				));
	}

	/**
	 * 동일한 복합 PK를 이용한 중복 등록 예외 검증
	 */
	@Test
	@DisplayName("컬렉션 콘텐츠 중복 등록")
	void doSaveDuplicate() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSaveDuplicate()            │");
		log.debug("└──────────────────────────────┘");

		// Given: 컬렉션 콘텐츠를 한 건 등록
		assertEquals(1, mapper.doSave(testData));

		// When, Then: 동일한 복합 PK를 다시 등록하면 예외가 발생해야 함
		log.debug("* duplicateData: collectionId-{}, contentId-{}",
				testData.getCollectionId(), testData.getContentId());

		assertThrows(
				DataIntegrityViolationException.class,
				() -> mapper.doSave(testData));
	}

	/**
	 * 존재하지 않는 컬렉션 콘텐츠 삭제 결과 검증
	 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 콘텐츠 삭제")
	void doDeleteNotFound() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doDeleteNotFound()           │");
		log.debug("└──────────────────────────────┘");

		// Given: DB에 등록되지 않은 복합 PK를 준비
		CollectionItemVO missingData = new CollectionItemVO(
				testData.getCollectionId(),
				MISSING_CONTENT_ID,
				null);

		// When: 존재하지 않는 컬렉션 콘텐츠를 삭제
		int flag = mapper.doDelete(missingData);

		// Then: 삭제된 행이 없으므로 0이 반환되어야 함
		log.debug("* deleteResult: collectionId-{}, contentId-{}, result-{}건",
				missingData.getCollectionId(), missingData.getContentId(), flag);

		assertEquals(0, flag);
	}
	
}
