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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionVO;

/**
 * <pre>
 * Class Name  : CollectionMapperTest
 * Description : 컬렉션 Mapper의 등록, 조회, 수정 및 삭제 기능을 검증하는 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13.	jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 13.
 */
@SpringBootTest
@Transactional
@DisplayName("CollectionMapper 테스트")
class CollectionMapperTest {

	// COMMON_CODE 테이블의 공통 코드값
	private static final String CODE_YES = "Y";
	private static final String CODE_NO = "N";
	private static final String MEMBER_ROLE_USER = "USER";
	private static final String MEMBER_STATUS_ACTIVE = "ACTIVE";

	private static final Logger log = LoggerFactory.getLogger(CollectionMapperTest.class);

	@Autowired
	private CollectionMapper mapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// 각 테스트에서 사용하는 컬렉션 데이터
	private CollectionVO testData;

	/**
	 * 테스트에 필요한 회원과 컬렉션 입력 데이터 준비
	 */
	@BeforeEach
	void setUp() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ setUp()                      │");
		log.debug("└──────────────────────────────┘");

		// FK 조건을 만족시키기 위해 회원 데이터를 먼저 등록
		int memberId = nextId("SEQ_MEMBER");
		insertMember(memberId);

		// COLLECTION_ID는 Mapper의 selectKey에서 생성하므로 0으로 설정
		testData = new CollectionVO(
				0, memberId, "JUnit 컬렉션", "JUnit 컬렉션 설명", CODE_YES, null, null);

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
			log.debug("* retrievedData: collectionId-{}, memberId-{}, title-{}, description-{}, isPublic-{}, createdDt-{}, updatedDt-{}",
					item.getCollectionId(), item.getMemberId(), item.getTitle(), item.getDescription(),
					item.getIsPublic(), item.getCreatedDt(), item.getUpdatedDt()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getCollectionId() == testData.getCollectionId()	// 컬렉션 번호 일치 검증
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

		// Given: 같은 회원의 컬렉션 세 건을 등록
		assertEquals(1, mapper.doSave(testData));

		CollectionVO secondData = new CollectionVO(
				0,
				testData.getMemberId(),
				"JUnit 두 번째 컬렉션",
				"페이징 검증용 두 번째 컬렉션",
				CODE_YES,
				null,
				null);

		CollectionVO thirdData = new CollectionVO(
				0,
				testData.getMemberId(),
				"JUnit 세 번째 컬렉션",
				"페이징 검증용 세 번째 컬렉션",
				CODE_NO,
				null,
				null);

		assertEquals(1, mapper.doSave(secondData));
		assertEquals(1, mapper.doSave(thirdData));

		DTO firstPageSearch = new DTO();
		firstPageSearch.setPageNo(1);
		firstPageSearch.setPageSize(2);
		firstPageSearch.setSearchDiv("20");
		firstPageSearch.setSearchWord(String.valueOf(testData.getMemberId()));

		DTO secondPageSearch = new DTO();
		secondPageSearch.setPageNo(2);
		secondPageSearch.setPageSize(2);
		secondPageSearch.setSearchDiv("20");
		secondPageSearch.setSearchWord(String.valueOf(testData.getMemberId()));

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
				nextId("SEQ_COLLECTION"),
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
		missingData.setCollectionId(nextId("SEQ_COLLECTION"));

		// When: 존재하지 않는 컬렉션을 삭제
		int flag = mapper.doDelete(missingData);

		// Then: 삭제된 행이 없으므로 0이 반환되어야 함
		log.debug("* deleteResult: collectionId-{}, result-{}건",
				missingData.getCollectionId(), flag);

		assertEquals(0, flag);
	}

	/**
	 * 테스트용 시퀀스의 다음 값 조회
	 *
	 * @param sequenceName 조회할 시퀀스명
	 * @return 시퀀스의 다음 번호
	 */
	private int nextId(String sequenceName) {
		// 시퀀스명은 바인딩할 수 없으므로 테스트 내부의 고정된 이름만 전달
		String sql = """
				SELECT %s.NEXTVAL
				  FROM DUAL
				""".formatted(sequenceName);

		Integer id = jdbcTemplate.queryForObject(sql, Integer.class);

		return id == null ? 0 : id;
	}

	/**
	 * 컬렉션 FK 검증에 필요한 테스트 회원 등록
	 *
	 * @param memberId 회원 번호
	 */
	private void insertMember(int memberId) {
		String sql = """
				INSERT INTO MEMBER (
					MEMBER_ID,
					EMAIL,
					PASSWORD,
					NICKNAME,
					ROLE,
					STATUS,
					CREATED_DT
				) VALUES (
					?,
					?,
					?,
					?,
					?,
					?,
					SYSDATE
				)
				""";

		jdbcTemplate.update(
				sql,
				memberId,
				"junit_collection_" + memberId + "@test.com",
				"junit-password",
				"JUnit컬렉션" + memberId,
				MEMBER_ROLE_USER,
				MEMBER_STATUS_ACTIVE);
	}
}