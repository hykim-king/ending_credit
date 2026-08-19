package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.PersonLikeVO;

/**
 * <pre>
 * Class Name  : PersonLikeMapperDaoTest
 * Description : 인물 좋아요 Mapper의 등록, 조회 및 삭제 기능을 검증하는 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13. jinyoung    최초 생성
 * 2026. 8. 14. jinyoung    공용 DB 더미 데이터 기반 테스트 구조로 변경
 * 2026. 8. 14. jinyoung    테스트 시작 전 전체 삭제 및 건수 검증 추가
 * 2026. 8. 19. jinyoung    목록 조회용 인물 JOIN 결과 검증 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 13.
 */
@SpringBootTest
@Transactional
@DisplayName("PersonLikeMapper 테스트")
class PersonLikeMapperDaoTest {

	// 공용 DB의 부모 테이블 더미 데이터에서 사용하는 번호
	private static final int TEST_MEMBER_ID = 10;
	private static final int TEST_PERSON_ID = 1;
	private static final int SECOND_PERSON_ID = 2;
	private static final int THIRD_PERSON_ID = 3;
	private static final int MISSING_PERSON_ID = 999_999_999;

	private static final Logger log = LoggerFactory.getLogger(PersonLikeMapperDaoTest.class);

	@Autowired
	private PersonLikeMapper mapper;

	// 각 테스트에서 사용하는 인물 좋아요 데이터
	private PersonLikeVO testData;

	/**
	 * 공용 DB의 회원과 인물 더미 데이터를 이용한 입력 데이터 준비
	 */
	@BeforeEach
	void setUp() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ setUp()                      │");
		log.debug("└──────────────────────────────┘");

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		log.debug("* initializedData: totalCnt-{}건", mapper.totalCnt());

		// MEMBER_ID 10과 PERSON_ID 1은 공용 DB에 존재하는 부모 더미 데이터
		// 두 번호의 조합은 PERSON_LIKE 더미 데이터에 없으므로 등록 테스트에 사용 가능
		// CREATED_DT는 Mapper에서 SYSDATE로 등록
		testData = new PersonLikeVO(TEST_MEMBER_ID, TEST_PERSON_ID, null);

		log.debug("* testData: memberId-{}, personId-{}, createdDt-{}",
				testData.getMemberId(), testData.getPersonId(), testData.getCreatedDt());
	}

	/**
	 * 회원의 인물 좋아요 등록 검증
	 */
	@Test
	@DisplayName("인물 좋아요 등록")
	void doSave() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSave()                     │");
		log.debug("└──────────────────────────────┘");

		// When: 인물 좋아요를 등록하고 실제 저장된 데이터를 다시 조회
		int flag = mapper.doSave(testData);
		PersonLikeVO outVO = mapper.doSelectOne(testData);

		// Then: 한 건이 등록되고 DB에서 생성한 등록 일시가 조회되어야 함
		assertEquals(1, flag);
		assertNotNull(outVO);

		log.debug("* savedData: memberId-{}, personId-{}, createdDt-{}",
				outVO.getMemberId(), outVO.getPersonId(), outVO.getCreatedDt());

		assertEquals(testData.getMemberId(), outVO.getMemberId());
		assertEquals(testData.getPersonId(), outVO.getPersonId());
		assertNotNull(outVO.getCreatedDt());
	}

	/**
	 * 회원 번호와 인물 번호를 이용한 단건 조회 검증
	 */
	@Test
	@DisplayName("인물 좋아요 단건 조회")
	void doSelectOne() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSelectOne()                │");
		log.debug("└──────────────────────────────┘");

		// Given: 조회할 인물 좋아요를 등록
		assertEquals(1, mapper.doSave(testData));

		// When: 복합 PK 전체를 이용해 단건 조회
		PersonLikeVO outVO = mapper.doSelectOne(testData);

		// Then: 등록한 키값과 좋아요 등록 일시가 조회되어야 함
		assertNotNull(outVO);

		log.debug("* selectedData: memberId-{}, personId-{}, createdDt-{}",
				outVO.getMemberId(), outVO.getPersonId(), outVO.getCreatedDt());

		assertEquals(testData.getMemberId(), outVO.getMemberId());
		assertEquals(testData.getPersonId(), outVO.getPersonId());
		assertNotNull(outVO.getCreatedDt());
	}

	/**
	 * 회원 번호와 인물 번호를 이용한 삭제 검증
	 */
	@Test
	@DisplayName("인물 좋아요 삭제")
	void doDelete() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doDelete()                   │");
		log.debug("└──────────────────────────────┘");

		// Given: 삭제할 인물 좋아요를 등록하고 삭제 전 데이터를 조회
		assertEquals(1, mapper.doSave(testData));

		PersonLikeVO beforeDeleteVO = mapper.doSelectOne(testData);

		assertNotNull(beforeDeleteVO);

		// When: 복합 PK 전체를 이용해 삭제
		int flag = mapper.doDelete(testData);

		// Then: 삭제한 인물 좋아요는 조회되지 않아야 함
		PersonLikeVO afterDeleteVO = mapper.doSelectOne(testData);

		assertEquals(1, flag);
		assertNull(afterDeleteVO);

		log.debug("* deletedData: memberId-{}, personId-{}, createdDt-{}, result-{}건",
				beforeDeleteVO.getMemberId(), beforeDeleteVO.getPersonId(), beforeDeleteVO.getCreatedDt(), flag);
	}

	/**
	 * 회원 번호 조건을 이용한 인물 좋아요 목록 조회 검증
	 */
	@Test
	@DisplayName("회원별 인물 좋아요 목록 조회")
	void doRetrieve() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieve()                 │");
		log.debug("└──────────────────────────────┘");

		// Given: 목록에서 조회할 인물 좋아요와 검색 조건을 준비
		assertEquals(1, mapper.doSave(testData));

		DTO search = new DTO();
		search.setPageNo(1);
		search.setPageSize(10);
		search.setSearchDiv("10");
		search.setSearchWord(String.valueOf(testData.getMemberId()));

		// When: 회원 번호를 조건으로 인물 좋아요 목록을 조회
		List<PersonLikeVO> list = mapper.doRetrieve(search);

		// Then: 등록한 인물 좋아요와 JOIN된 인물 정보가 조회되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("* retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: memberId-{}, personId-{}, createdDt-{}",
					item.getMemberId(), item.getPersonId(), item.getCreatedDt()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getMemberId() == testData.getMemberId()	// 회원 번호 일치 검증
						&& item.getPersonId() == testData.getPersonId()	// 인물 번호 일치 검증
						&& item.getExternalId() != null
						&& item.getNameOrg() != null
				));
	}

	/**
	 * 인물 번호 조건을 이용한 인물 좋아요 목록 조회 검증
	 */
	@Test
	@DisplayName("인물 번호 조건 좋아요 목록 조회")
	void doRetrieveByPersonId() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieveByPersonId()       │");
		log.debug("└──────────────────────────────┘");

		// Given: 검색 대상과 검색에서 제외할 인물 좋아요를 등록
		assertEquals(1, mapper.doSave(testData));

		PersonLikeVO comparisonData = new PersonLikeVO(
				testData.getMemberId(),
				SECOND_PERSON_ID,
				null);

		assertEquals(1, mapper.doSave(comparisonData));

		DTO search = new DTO();
		search.setPageNo(1);
		search.setPageSize(10);
		search.setSearchDiv("20");
		search.setSearchWord(String.valueOf(testData.getPersonId()));

		// When: 인물 번호를 조건으로 좋아요 목록을 조회
		List<PersonLikeVO> list = mapper.doRetrieve(search);

		// Then: 검색한 인물 번호의 좋아요만 조회되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("* retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: memberId-{}, personId-{}, createdDt-{}",
					item.getMemberId(), item.getPersonId(), item.getCreatedDt()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getMemberId() == testData.getMemberId()
						&& item.getPersonId() == testData.getPersonId()
				));

		assertFalse(list.stream()
				.anyMatch(item ->
						item.getPersonId() == comparisonData.getPersonId()
				));
	}

	/**
	 * 인물 좋아요 목록 페이징 처리 검증
	 */
	@Test
	@DisplayName("인물 좋아요 목록 페이징")
	void doRetrievePaging() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrievePaging()           │");
		log.debug("└──────────────────────────────┘");

		// Given: 같은 회원의 인물 좋아요 세 건을 등록
		assertEquals(1, mapper.doSave(testData));

		PersonLikeVO secondData = new PersonLikeVO(
				testData.getMemberId(), SECOND_PERSON_ID, null);

		PersonLikeVO thirdData = new PersonLikeVO(
				testData.getMemberId(), THIRD_PERSON_ID, null);

		assertEquals(1, mapper.doSave(secondData));
		assertEquals(1, mapper.doSave(thirdData));

		DTO firstPageSearch = new DTO();
		firstPageSearch.setPageNo(1);
		firstPageSearch.setPageSize(2);
		firstPageSearch.setSearchDiv("10");
		firstPageSearch.setSearchWord(String.valueOf(testData.getMemberId()));

		DTO secondPageSearch = new DTO();
		secondPageSearch.setPageNo(2);
		secondPageSearch.setPageSize(2);
		secondPageSearch.setSearchDiv("10");
		secondPageSearch.setSearchWord(String.valueOf(testData.getMemberId()));

		// When: 페이지 크기를 두 건으로 설정해 1페이지와 2페이지를 조회
		List<PersonLikeVO> firstPage = mapper.doRetrieve(firstPageSearch);
		List<PersonLikeVO> secondPage = mapper.doRetrieve(secondPageSearch);

		// Then: 1페이지에는 두 건, 2페이지에는 한 건이 조회되어야 함
		assertNotNull(firstPage);
		assertNotNull(secondPage);
		assertEquals(2, firstPage.size());
		assertEquals(1, secondPage.size());
		assertTrue(firstPage.stream().allMatch(item -> item.getExternalId() != null && item.getNameOrg() != null));
		assertTrue(secondPage.stream().allMatch(item -> item.getExternalId() != null && item.getNameOrg() != null));

		log.debug("* firstPageCount: {}건", firstPage.size());
		log.debug("* secondPageCount: {}건", secondPage.size());

		firstPage.forEach(item ->
			log.debug("* firstPageData: memberId-{}, personId-{}, createdDt-{}",
					item.getMemberId(), item.getPersonId(), item.getCreatedDt()));

		secondPage.forEach(item ->
			log.debug("* secondPageData: memberId-{}, personId-{}, createdDt-{}",
					item.getMemberId(), item.getPersonId(), item.getCreatedDt()));

		assertTrue(firstPage.stream()
				.noneMatch(firstItem ->
						secondPage.stream()
								.anyMatch(secondItem ->
										secondItem.getMemberId() == firstItem.getMemberId()
										&& secondItem.getPersonId() == firstItem.getPersonId()
								)
				));
	}

	/**
	 * 동일한 복합 PK를 이용한 중복 등록 예외 검증
	 */
	@Test
	@DisplayName("인물 좋아요 중복 등록")
	void doSaveDuplicate() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSaveDuplicate()            │");
		log.debug("└──────────────────────────────┘");

		// Given: 인물 좋아요를 한 건 등록
		assertEquals(1, mapper.doSave(testData));

		// When, Then: 동일한 복합 PK를 다시 등록하면 예외가 발생해야 함
		log.debug("* duplicateData: memberId-{}, personId-{}",
				testData.getMemberId(), testData.getPersonId());

		assertThrows(
				DataIntegrityViolationException.class,
				() -> mapper.doSave(testData));
	}

	/**
	 * 존재하지 않는 인물 좋아요 삭제 결과 검증
	 */
	@Test
	@DisplayName("존재하지 않는 인물 좋아요 삭제")
	void doDeleteNotFound() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doDeleteNotFound()           │");
		log.debug("└──────────────────────────────┘");

		// Given: DB에 등록되지 않은 복합 PK를 준비
		PersonLikeVO missingData = new PersonLikeVO(
				testData.getMemberId(),
				MISSING_PERSON_ID,
				null);

		// When: 존재하지 않는 인물 좋아요를 삭제
		int flag = mapper.doDelete(missingData);

		// Then: 삭제된 행이 없으므로 0이 반환되어야 함
		log.debug("* deleteResult: memberId-{}, personId-{}, result-{}건",
				missingData.getMemberId(), missingData.getPersonId(), flag);

		assertEquals(0, flag);
	}
	
}
