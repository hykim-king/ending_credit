package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;
import com.endit.domain.CollectionLikeVO;
import com.endit.domain.CollectionVO;

/**
 * <pre>
 * Class Name  : CollectionMapperDaoTest
 * Description : 컬렉션 Mapper의 등록, 조회, 수정 및 삭제 기능을 검증하는 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13. jinyoung    최초 생성
 * 2026. 8. 14. jinyoung    공용 DB 더미 데이터 기반 테스트 구조로 변경
 * 2026. 8. 14. jinyoung    테스트 시작 전 전체 삭제 및 건수 검증 추가
 * 2026. 8. 19. jinyoung    작성자 조인 및 작품·좋아요·코멘트 집계 검증 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 13.
 */
@SpringBootTest
@Transactional
@DisplayName("CollectionMapper 테스트")
class CollectionMapperDaoTest {

	// COMMON_CODE 테이블의 공통 코드값
	private static final String CODE_YES = "Y";
	private static final String CODE_NO = "N";

	// 공용 DB의 MEMBER 더미 데이터에서 사용하는 회원 번호
	private static final int TEST_MEMBER_ID = 10;
	private static final int MISSING_COLLECTION_ID = 999_999_999;

	private static final Logger log = LoggerFactory.getLogger(CollectionMapperDaoTest.class);

	@Autowired
	private CollectionMapper mapper;

	@Autowired
	private CollectionItemMapper collectionItemMapper;

	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	// 각 테스트에서 사용하는 컬렉션 데이터
	private CollectionVO testData;

	/**
	 * 공용 DB의 회원 더미 데이터를 이용한 컬렉션 입력 데이터 준비
	 */
	@BeforeEach
	void setUp() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ setUp()                      │");
		log.debug("└──────────────────────────────┘");

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		log.debug("* initializedData: totalCnt-{}건", mapper.totalCnt());

		// MEMBER_ID 10은 공용 DB의 MEMBER 테이블에 등록된 더미 데이터
		// COLLECTION_ID는 Mapper의 selectKey에서 생성하므로 0으로 설정
		testData = new CollectionVO(
				0, TEST_MEMBER_ID, "JUnit 컬렉션", "JUnit 컬렉션 설명", CODE_YES, null, null);

		log.debug("* testData: collectionId-{}, memberId-{}, title-{}, description-{}, isPublic-{}, createdDt-{}, updatedDt-{}",
				testData.getCollectionId(), testData.getMemberId(), testData.getTitle(), testData.getDescription(),
				testData.getIsPublic(), testData.getCreatedDt(), testData.getUpdatedDt());
	}

	/**
	 * 컬렉션 등록과 시퀀스 번호 생성 검증
	 */
	@Test
	@DisplayName("컬렉션 등록")
	void doSave() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSave()                     │");
		log.debug("└──────────────────────────────┘");

		// When: 컬렉션을 등록하고 실제 저장된 데이터를 다시 조회
		int flag = mapper.doSave(testData);
		CollectionVO outVO = mapper.doSelectOne(testData);

		// Then: 한 건이 등록되고 컬렉션 번호와 생성 일시가 조회되어야 함
		assertEquals(1, flag);
		assertTrue(testData.getCollectionId() > 0);
		assertNotNull(outVO);

		log.debug("* savedData: collectionId-{}, memberId-{}, title-{}, description-{}, isPublic-{}, createdDt-{}, updatedDt-{}",
				outVO.getCollectionId(), outVO.getMemberId(), outVO.getTitle(), outVO.getDescription(),
				outVO.getIsPublic(), outVO.getCreatedDt(), outVO.getUpdatedDt());

		assertEquals(testData.getCollectionId(), outVO.getCollectionId());
		assertEquals(testData.getMemberId(), outVO.getMemberId());
		assertNotNull(outVO.getCreatedDt());
	}

	/**
	 * 컬렉션 번호를 이용한 단건 조회 검증
	 */
	@Test
	@DisplayName("컬렉션 단건 조회")
	void doSelectOne() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSelectOne()                │");
		log.debug("└──────────────────────────────┘");

		// Given: 조회할 컬렉션을 등록
		assertEquals(1, mapper.doSave(testData));

		CollectionItemVO item = new CollectionItemVO(testData.getCollectionId(), 1, null);
		assertEquals(1, collectionItemMapper.doSave(item));

		CollectionLikeVO like = new CollectionLikeVO(
				testData.getMemberId(), testData.getCollectionId(), null);
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like));

		// When: 컬렉션 번호로 단건 조회
		CollectionVO outVO = mapper.doSelectOne(testData);

		// Then: 등록한 값과 조회한 값이 같아야 함
		assertNotNull(outVO);

		log.debug("* selectedData: collectionId-{}, memberId-{}, title-{}, description-{}, isPublic-{}, createdDt-{}, updatedDt-{}",
				outVO.getCollectionId(), outVO.getMemberId(), outVO.getTitle(), outVO.getDescription(),
				outVO.getIsPublic(), outVO.getCreatedDt(), outVO.getUpdatedDt());

		assertEquals(testData.getCollectionId(), outVO.getCollectionId());
		assertEquals(testData.getMemberId(), outVO.getMemberId());
		assertEquals(testData.getTitle(), outVO.getTitle());
		assertEquals(testData.getDescription(), outVO.getDescription());
		assertEquals(testData.getIsPublic(), outVO.getIsPublic());
		assertNotNull(outVO.getCreatedDt());
		assertNotNull(outVO.getNickname());
		assertEquals(1, outVO.getItemCount());
		assertEquals(1, outVO.getLikeCount());
		assertEquals(0, outVO.getCommentCount());
	}

	/**
	 * 컬렉션 제목, 설명 및 공개 여부 수정 검증
	 */
	@Test
	@DisplayName("컬렉션 수정")
	void doUpdate() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doUpdate()                   │");
		log.debug("└──────────────────────────────┘");

		// Given: 수정할 컬렉션을 등록하고 변경값을 설정
		assertEquals(1, mapper.doSave(testData));
		testData.setTitle("수정된 JUnit 컬렉션");
		testData.setDescription("수정된 JUnit 컬렉션 설명");
		testData.setIsPublic(CODE_NO);

		log.debug("* updateData: collectionId-{}, memberId-{}, title-{}, description-{}, isPublic-{}",
				testData.getCollectionId(), testData.getMemberId(), testData.getTitle(),
				testData.getDescription(), testData.getIsPublic());

		// When: 컬렉션을 수정하고 실제 반영된 데이터를 다시 조회
		int flag = mapper.doUpdate(testData);
		CollectionVO outVO = mapper.doSelectOne(testData);

		// Then: 변경값과 수정 일시가 반영되어야 함
		assertEquals(1, flag);
		assertNotNull(outVO);

		log.debug("* updatedData: collectionId-{}, memberId-{}, title-{}, description-{}, isPublic-{}, createdDt-{}, updatedDt-{}",
				outVO.getCollectionId(), outVO.getMemberId(), outVO.getTitle(), outVO.getDescription(),
				outVO.getIsPublic(), outVO.getCreatedDt(), outVO.getUpdatedDt());

		assertEquals(testData.getTitle(), outVO.getTitle());
		assertEquals(testData.getDescription(), outVO.getDescription());
		assertEquals(testData.getIsPublic(), outVO.getIsPublic());
		assertNotNull(outVO.getUpdatedDt());
	}

	/**
	 * 컬렉션 삭제 검증
	 */
	@Test
	@DisplayName("컬렉션 삭제")
	void doDelete() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doDelete()                   │");
		log.debug("└──────────────────────────────┘");

		// Given: 삭제할 컬렉션을 등록하고 삭제 전 데이터를 조회
		assertEquals(1, mapper.doSave(testData));

		CollectionVO beforeDeleteVO = mapper.doSelectOne(testData);

		assertNotNull(beforeDeleteVO);

		// When: 컬렉션을 삭제
		int flag = mapper.doDelete(testData);

		// Then: 삭제한 컬렉션은 조회되지 않아야 함
		CollectionVO afterDeleteVO = mapper.doSelectOne(testData);

		assertEquals(1, flag);
		assertNull(afterDeleteVO);

		log.debug("* deletedData: collectionId-{}, memberId-{}, title-{}, description-{}, isPublic-{}, createdDt-{}, updatedDt-{}, result-{}건",
				beforeDeleteVO.getCollectionId(), beforeDeleteVO.getMemberId(), beforeDeleteVO.getTitle(),
				beforeDeleteVO.getDescription(), beforeDeleteVO.getIsPublic(), beforeDeleteVO.getCreatedDt(),
				beforeDeleteVO.getUpdatedDt(), flag);
	}

	/**
	 * 회원 번호 조건을 이용한 컬렉션 목록 조회 검증
	 */
	@Test
	@DisplayName("회원별 컬렉션 목록 조회")
	void doRetrieve() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieve()                 │");
		log.debug("└──────────────────────────────┘");

		// Given: 목록에서 조회할 컬렉션과 검색 조건을 준비
		assertEquals(1, mapper.doSave(testData));

		DTO search = new DTO();
		search.setPageNo(1);
		search.setPageSize(10);
		search.setSearchDiv("20");
		search.setSearchWord(String.valueOf(testData.getMemberId()));

		// When: 회원 번호를 조건으로 컬렉션 목록을 조회
		List<CollectionVO> list = mapper.doRetrieve(search);

		// Then: 등록한 컬렉션이 조회 결과에 포함되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: collectionId-{}, memberId-{}, title-{}, nickname-{}, itemCount-{}, likeCount-{}, description-{}, isPublic-{}, createdDt-{}, updatedDt-{}",
					item.getCollectionId(), item.getMemberId(), item.getTitle(), item.getNickname(),
					item.getItemCount(), item.getLikeCount(), item.getDescription(), item.getIsPublic(),
					item.getCreatedDt(), item.getUpdatedDt()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getCollectionId() == testData.getCollectionId()	// 컬렉션 번호 일치 검증
						&& item.getNickname() != null							// 작성자 조인 결과 검증
						&& item.getUpdatedDt() != null							// 목록 표시용 최근 수정일 검증
				));
	}
	
	/**
	 * 제목 조건을 이용한 컬렉션 목록 조회 검증
	 */
	@Test
	@DisplayName("제목 조건 컬렉션 목록 조회")
	void doRetrieveByTitle() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieveByTitle()          │");
		log.debug("└──────────────────────────────┘");

		// Given: 제목 검색 대상과 검색에서 제외할 컬렉션을 등록
		String searchTitle = "JUnit 제목검색 " + testData.getMemberId();
		testData.setTitle(searchTitle);

		assertEquals(1, mapper.doSave(testData));

		CollectionVO comparisonData = new CollectionVO(
				0,
				testData.getMemberId(),
				"검색 제외 컬렉션",
				"제목 검색에서 제외할 비교 데이터",
				CODE_YES,
				null,
				null);

		assertEquals(1, mapper.doSave(comparisonData));

		DTO search = new DTO();
		search.setPageNo(1);
		search.setPageSize(10);
		search.setSearchDiv("10");
		search.setSearchWord(searchTitle);

		// When: 제목을 조건으로 컬렉션 목록을 조회
		List<CollectionVO> list = mapper.doRetrieve(search);

		// Then: 검색 제목이 일치하는 컬렉션만 조회되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("* retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: collectionId-{}, memberId-{}, title-{}, description-{}, isPublic-{}, createdDt-{}, updatedDt-{}",
					item.getCollectionId(), item.getMemberId(), item.getTitle(), item.getDescription(),
					item.getIsPublic(), item.getCreatedDt(), item.getUpdatedDt()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getCollectionId() == testData.getCollectionId()
				));

		assertFalse(list.stream()
				.anyMatch(item ->
						item.getCollectionId() == comparisonData.getCollectionId()
				));
	}

	/**
	 * 컬렉션 목록 페이징 처리 검증
	 */
	@Test
	@DisplayName("컬렉션 목록 페이징")
	void doRetrievePaging() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrievePaging()           │");
		log.debug("└──────────────────────────────┘");

		// Given: 같은 검색어를 포함하는 컬렉션 세 건을 등록
		String pagingTitle = "JUnit 페이징 검증";
		testData.setTitle(pagingTitle + " 첫 번째 컬렉션");
		assertEquals(1, mapper.doSave(testData));

		CollectionVO secondData = new CollectionVO(
				0,
				testData.getMemberId(),
				pagingTitle + " 두 번째 컬렉션",
				"페이징 검증용 두 번째 컬렉션",
				CODE_YES,
				null,
				null);

		CollectionVO thirdData = new CollectionVO(
				0,
				testData.getMemberId(),
				pagingTitle + " 세 번째 컬렉션",
				"페이징 검증용 세 번째 컬렉션",
				CODE_NO,
				null,
				null);

		assertEquals(1, mapper.doSave(secondData));
		assertEquals(1, mapper.doSave(thirdData));

		DTO firstPageSearch = new DTO();
		firstPageSearch.setPageNo(1);
		firstPageSearch.setPageSize(2);
		firstPageSearch.setSearchDiv("10");
		firstPageSearch.setSearchWord(pagingTitle);

		DTO secondPageSearch = new DTO();
		secondPageSearch.setPageNo(2);
		secondPageSearch.setPageSize(2);
		secondPageSearch.setSearchDiv("10");
		secondPageSearch.setSearchWord(pagingTitle);

		// When: 페이지 크기를 두 건으로 설정해 1페이지와 2페이지를 조회
		List<CollectionVO> firstPage = mapper.doRetrieve(firstPageSearch);
		List<CollectionVO> secondPage = mapper.doRetrieve(secondPageSearch);

		// Then: 1페이지에는 두 건, 2페이지에는 한 건이 조회되어야 함
		assertNotNull(firstPage);
		assertNotNull(secondPage);
		assertEquals(2, firstPage.size());
		assertEquals(1, secondPage.size());

		log.debug("* firstPageCount: {}건", firstPage.size());
		log.debug("* secondPageCount: {}건", secondPage.size());

		firstPage.forEach(item ->
			log.debug("* firstPageData: collectionId-{}, memberId-{}, title-{}",
					item.getCollectionId(), item.getMemberId(), item.getTitle()));

		secondPage.forEach(item ->
			log.debug("* secondPageData: collectionId-{}, memberId-{}, title-{}",
					item.getCollectionId(), item.getMemberId(), item.getTitle()));

		assertTrue(firstPage.stream()
				.noneMatch(firstItem ->
						secondPage.stream()
								.anyMatch(secondItem ->
										secondItem.getCollectionId() == firstItem.getCollectionId()
								)
				));
	}

	/**
	 * 공개 여부를 입력하지 않았을 때 기본값 등록 검증
	 */
	@Test
	@DisplayName("컬렉션 공개 여부 기본값 등록")
	void doSaveDefaultIsPublic() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSaveDefaultIsPublic()      │");
		log.debug("└──────────────────────────────┘");

		// Given: 공개 여부를 입력하지 않은 컬렉션을 준비
		testData.setIsPublic(null);

		// When: 컬렉션을 등록하고 실제 저장된 데이터를 조회
		int flag = mapper.doSave(testData);
		CollectionVO outVO = mapper.doSelectOne(testData);

		// Then: 공개 여부가 기본값 Y로 등록되어야 함
		assertEquals(1, flag);
		assertNotNull(outVO);

		log.debug("* savedData: collectionId-{}, memberId-{}, title-{}, isPublic-{}, createdDt-{}",
				outVO.getCollectionId(), outVO.getMemberId(), outVO.getTitle(),
				outVO.getIsPublic(), outVO.getCreatedDt());

		assertEquals(CODE_YES, outVO.getIsPublic());
		assertNotNull(outVO.getCreatedDt());
	}

	/**
	 * 존재하지 않는 컬렉션 수정 결과 검증
	 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 수정")
	void doUpdateNotFound() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doUpdateNotFound()           │");
		log.debug("└──────────────────────────────┘");

		// Given: DB에 등록되지 않은 컬렉션 정보를 준비
		CollectionVO missingData = new CollectionVO(
				MISSING_COLLECTION_ID,
				testData.getMemberId(),
				"존재하지 않는 컬렉션",
				"수정 결과 0 검증",
				CODE_YES,
				null,
				null);

		// When: 존재하지 않는 컬렉션을 수정
		int flag = mapper.doUpdate(missingData);

		// Then: 수정된 행이 없으므로 0이 반환되어야 함
		log.debug("* updateResult: collectionId-{}, result-{}건",
				missingData.getCollectionId(), flag);

		assertEquals(0, flag);
	}

	/**
	 * 존재하지 않는 컬렉션 삭제 결과 검증
	 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 삭제")
	void doDeleteNotFound() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doDeleteNotFound()           │");
		log.debug("└──────────────────────────────┘");

		// Given: DB에 등록되지 않은 컬렉션 번호를 준비
		CollectionVO missingData = new CollectionVO();
		missingData.setCollectionId(MISSING_COLLECTION_ID);

		// When: 존재하지 않는 컬렉션을 삭제
		int flag = mapper.doDelete(missingData);

		// Then: 삭제된 행이 없으므로 0이 반환되어야 함
		log.debug("* deleteResult: collectionId-{}, result-{}건",
				missingData.getCollectionId(), flag);

		assertEquals(0, flag);
	}

}
