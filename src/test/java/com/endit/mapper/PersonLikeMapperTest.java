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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.PersonLikeVO;

/**
 * <pre>
 * Class Name  : PersonLikeMapperTest
 * Description : 인물 좋아요 Mapper의 등록, 조회 및 삭제 기능을 검증하는 테스트
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
@DisplayName("PersonLikeMapper 테스트")
class PersonLikeMapperTest {

	// COMMON_CODE 테이블의 공통 코드값
	private static final String MEMBER_ROLE_USER = "USER";
	private static final String MEMBER_STATUS_ACTIVE = "ACTIVE";

	private static final Logger log = LoggerFactory.getLogger(PersonLikeMapperTest.class);

	@Autowired
	private PersonLikeMapper mapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// 각 테스트에서 사용하는 인물 좋아요 데이터
	private PersonLikeVO testData;

	/**
	 * 테스트에 필요한 회원, 인물 및 인물 좋아요 데이터 준비
	 */
	@BeforeEach
	void setUp() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ setUp()                      │");
		log.debug("└──────────────────────────────┘");

		// 복합 FK 조건을 만족시키기 위해 회원과 인물을 먼저 등록
		int memberId = nextId("SEQ_MEMBER");
		int personId = nextId("SEQ_PERSON");

		insertMember(memberId);
		insertPerson(personId);

		// CREATED_DT는 Mapper에서 SYSDATE로 등록
		testData = new PersonLikeVO(memberId, personId, null);

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

		// Then: 등록한 인물 좋아요가 조회 결과에 포함되어야 함
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

		int comparisonPersonId = nextId("SEQ_PERSON");
		insertPerson(comparisonPersonId);

		PersonLikeVO comparisonData = new PersonLikeVO(
				testData.getMemberId(),
				comparisonPersonId,
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

		int secondPersonId = nextId("SEQ_PERSON");
		int thirdPersonId = nextId("SEQ_PERSON");

		insertPerson(secondPersonId);
		insertPerson(thirdPersonId);

		PersonLikeVO secondData = new PersonLikeVO(
				testData.getMemberId(), secondPersonId, null);

		PersonLikeVO thirdData = new PersonLikeVO(
				testData.getMemberId(), thirdPersonId, null);

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
				nextId("SEQ_PERSON"),
				null);

		// When: 존재하지 않는 인물 좋아요를 삭제
		int flag = mapper.doDelete(missingData);

		// Then: 삭제된 행이 없으므로 0이 반환되어야 함
		log.debug("* deleteResult: memberId-{}, personId-{}, result-{}건",
				missingData.getMemberId(), missingData.getPersonId(), flag);

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
	 * 인물 좋아요 FK 검증에 필요한 테스트 회원 등록
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
				"junit_person_like_" + memberId + "@test.com",
				"junit-password",
				"JUnit인물좋아요" + memberId,
				MEMBER_ROLE_USER,
				MEMBER_STATUS_ACTIVE);
	}

	/**
	 * 인물 좋아요 FK 검증에 필요한 테스트 인물 등록
	 *
	 * @param personId 인물 번호
	 */
	private void insertPerson(int personId) {
		String sql = """
				INSERT INTO PERSON (
					PERSON_ID,
					EXTERNAL_ID,
					NAME_ORG,
					CREATED_DT
				) VALUES (
					?,
					?,
					?,
					SYSDATE
				)
				""";

		jdbcTemplate.update(
				sql,
				personId,
				"JUNIT_PERSON_LIKE_" + personId,
				"JUnit Person Like " + personId);
	}
}