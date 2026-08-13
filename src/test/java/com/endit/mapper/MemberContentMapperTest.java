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
import com.endit.domain.MemberContentVO;

/**
 * <pre>
 * Class Name  : MemberContentMapperTest
 * Description : 회원별 콘텐츠 Mapper의 등록, 조회, 수정 및 삭제 기능을 검증하는 테스트
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
@DisplayName("MemberContentMapper 테스트")
class MemberContentMapperTest {

	// COMMON_CODE 테이블의 공통 코드값
	private static final String CODE_YES = "Y";
	private static final String CODE_NO = "N";
	private static final String MEMBER_ROLE_USER = "USER";
	private static final String MEMBER_STATUS_ACTIVE = "ACTIVE";

	private static final Logger log = LoggerFactory.getLogger(MemberContentMapperTest.class);

	@Autowired
	private MemberContentMapper mapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// 각 테스트에서 사용하는 회원별 콘텐츠 데이터
	private MemberContentVO testData;

	/**
	 * 테스트에 필요한 회원, 콘텐츠 및 회원별 콘텐츠 데이터 준비
	 */
	@BeforeEach
	void setUp() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ setUp()                      │");
		log.debug("└──────────────────────────────┘");

		// 복합 FK 조건을 만족시키기 위해 회원과 콘텐츠를 먼저 등록
		int memberId = nextId("SEQ_MEMBER");
		int contentId = nextId("SEQ_CONTENT");

		insertMember(memberId);
		insertContent(contentId);

		// 날짜 컬럼은 Mapper가 별점과 보고 싶어요 상태에 따라 처리
		testData = new MemberContentVO(
				memberId, contentId, 4, CODE_YES, null, null, null);

		log.debug("* testData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}, ratedDt-{}, watchlistDt-{}, updatedDt-{}",
				testData.getMemberId(), testData.getContentId(), testData.getRatingScore(), testData.getWatchlist(),
				testData.getRatedDt(), testData.getWatchlistDt(), testData.getUpdatedDt());
	}

	/**
	 * 회원별 콘텐츠 평가와 보고 싶어요 등록 검증
	 */
	@Test
	@DisplayName("회원별 콘텐츠 등록")
	void doSave() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSave()                     │");
		log.debug("└──────────────────────────────┘");

		// When: 회원별 콘텐츠를 등록하고 실제 저장된 데이터를 다시 조회
		int flag = mapper.doSave(testData);
		MemberContentVO outVO = mapper.doSelectOne(testData);

		// Then: 한 건이 등록되고 별점 및 보고 싶어요 처리 일시가 조회되어야 함
		assertEquals(1, flag);
		assertNotNull(outVO);

		log.debug("* savedData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}, ratedDt-{}, watchlistDt-{}, updatedDt-{}",
				outVO.getMemberId(), outVO.getContentId(), outVO.getRatingScore(), outVO.getWatchlist(),
				outVO.getRatedDt(), outVO.getWatchlistDt(), outVO.getUpdatedDt());

		assertEquals(testData.getMemberId(), outVO.getMemberId());
		assertEquals(testData.getContentId(), outVO.getContentId());
		assertEquals(testData.getRatingScore(), outVO.getRatingScore());
		assertEquals(testData.getWatchlist(), outVO.getWatchlist());
		
		assertNotNull(outVO.getRatedDt());
		assertNotNull(outVO.getWatchlistDt());
		assertNotNull(outVO.getUpdatedDt());
	}

	/**
	 * 회원 번호와 콘텐츠 번호를 이용한 단건 조회 검증
	 */
	@Test
	@DisplayName("회원별 콘텐츠 단건 조회")
	void doSelectOne() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSelectOne()                │");
		log.debug("└──────────────────────────────┘");

		// Given: 조회할 회원별 콘텐츠를 등록
		assertEquals(1, mapper.doSave(testData));

		// When: 복합 PK 전체를 이용해 단건 조회
		MemberContentVO outVO = mapper.doSelectOne(testData);

		// Then: 평가, 보고 싶어요 상태 및 처리 일시가 조회되어야 함
		assertNotNull(outVO);

		log.debug("* selectedData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}, ratedDt-{}, watchlistDt-{}, updatedDt-{}",
				outVO.getMemberId(), outVO.getContentId(), outVO.getRatingScore(), outVO.getWatchlist(),
				outVO.getRatedDt(), outVO.getWatchlistDt(), outVO.getUpdatedDt());

		assertEquals(testData.getMemberId(), outVO.getMemberId());
		assertEquals(testData.getContentId(), outVO.getContentId());
		assertEquals(testData.getRatingScore(), outVO.getRatingScore());
		assertEquals(testData.getWatchlist(), outVO.getWatchlist());
		
		assertNotNull(outVO.getRatedDt());
		assertNotNull(outVO.getWatchlistDt());
		assertNotNull(outVO.getUpdatedDt());
	}

	/**
	 * 별점과 보고 싶어요 상태 수정 검증
	 */
	@Test
	@DisplayName("회원별 콘텐츠 수정")
	void doUpdate() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doUpdate()                   │");
		log.debug("└──────────────────────────────┘");

		// Given: 수정할 회원별 콘텐츠를 등록하고 변경값을 설정
		assertEquals(1, mapper.doSave(testData));
		testData.setRatingScore(5);
		testData.setWatchlist(CODE_NO);

		log.debug("* updateData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}",
				testData.getMemberId(), testData.getContentId(), testData.getRatingScore(), testData.getWatchlist());

		// When: 별점과 보고 싶어요 상태를 수정하고 실제 반영된 데이터를 다시 조회
		int flag = mapper.doUpdate(testData);
		MemberContentVO outVO = mapper.doSelectOne(testData);

		// Then: 변경값과 날짜 처리 결과가 정확해야 함
		assertEquals(1, flag);
		assertNotNull(outVO);

		log.debug("* updatedData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}, ratedDt-{}, watchlistDt-{}, updatedDt-{}",
				outVO.getMemberId(), outVO.getContentId(), outVO.getRatingScore(), outVO.getWatchlist(),
				outVO.getRatedDt(), outVO.getWatchlistDt(), outVO.getUpdatedDt());

		assertEquals(testData.getRatingScore(), outVO.getRatingScore());
		assertEquals(testData.getWatchlist(), outVO.getWatchlist());
		
		assertNotNull(outVO.getRatedDt());
		assertNull(outVO.getWatchlistDt());
		assertNotNull(outVO.getUpdatedDt());
	}

	/**
	 * 회원 번호와 콘텐츠 번호를 이용한 삭제 검증
	 */
	@Test
	@DisplayName("회원별 콘텐츠 삭제")
	void doDelete() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doDelete()                   │");
		log.debug("└──────────────────────────────┘");

		// Given: 삭제할 회원별 콘텐츠를 등록하고 삭제 전 데이터를 조회
		assertEquals(1, mapper.doSave(testData));

		MemberContentVO beforeDeleteVO = mapper.doSelectOne(testData);

		assertNotNull(beforeDeleteVO);

		// When: 복합 PK 전체를 이용해 삭제
		int flag = mapper.doDelete(testData);

		// Then: 삭제한 회원별 콘텐츠는 조회되지 않아야 함
		MemberContentVO afterDeleteVO = mapper.doSelectOne(testData);

		assertEquals(1, flag);
		assertNull(afterDeleteVO);

		log.debug("* deletedData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}, ratedDt-{}, watchlistDt-{}, updatedDt-{}, result-{}건",
				beforeDeleteVO.getMemberId(), beforeDeleteVO.getContentId(), beforeDeleteVO.getRatingScore(), beforeDeleteVO.getWatchlist(), 
				beforeDeleteVO.getRatedDt(), beforeDeleteVO.getWatchlistDt(), beforeDeleteVO.getUpdatedDt(), flag);
	}

	/**
	 * 회원 번호 조건을 이용한 콘텐츠 목록 조회 검증
	 */
	@Test
	@DisplayName("회원별 콘텐츠 목록 조회")
	void doRetrieve() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieve()                 │");
		log.debug("└──────────────────────────────┘");

		// Given: 목록에서 조회할 회원별 콘텐츠와 검색 조건을 준비
		assertEquals(1, mapper.doSave(testData));

		DTO search = new DTO();
		search.setPageNo(1);
		search.setPageSize(10);
		search.setSearchDiv("10");
		search.setSearchWord(String.valueOf(testData.getMemberId()));

		// When: 회원 번호를 조건으로 콘텐츠 목록을 조회
		List<MemberContentVO> list = mapper.doRetrieve(search);

		// Then: 등록한 회원별 콘텐츠가 조회 결과에 포함되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}, ratedDt-{}, watchlistDt-{}, updatedDt-{}",
					item.getMemberId(), item.getContentId(), item.getRatingScore(), item.getWatchlist(),
					item.getRatedDt(), item.getWatchlistDt(), item.getUpdatedDt()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getMemberId() == testData.getMemberId()		// 회원 번호 일치 검증
						&& item.getContentId() == testData.getContentId()	// 콘텐츠 번호 일치 검증
				));
	}

	/**
	 * 콘텐츠 번호 조건을 이용한 회원별 콘텐츠 목록 조회 검증
	 */
	@Test
	@DisplayName("콘텐츠 번호 조건 회원별 콘텐츠 목록 조회")
	void doRetrieveByContentId() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieveByContentId()      │");
		log.debug("└──────────────────────────────┘");

		// Given: 검색 대상과 검색에서 제외할 회원별 콘텐츠를 등록
		assertEquals(1, mapper.doSave(testData));

		int comparisonContentId = nextId("SEQ_CONTENT");
		insertContent(comparisonContentId);

		MemberContentVO comparisonData = new MemberContentVO(
				testData.getMemberId(),
				comparisonContentId,
				3,
				CODE_NO,
				null,
				null,
				null);

		assertEquals(1, mapper.doSave(comparisonData));

		DTO search = new DTO();
		search.setPageNo(1);
		search.setPageSize(10);
		search.setSearchDiv("20");
		search.setSearchWord(String.valueOf(testData.getContentId()));

		// When: 콘텐츠 번호를 조건으로 회원별 콘텐츠 목록을 조회
		List<MemberContentVO> list = mapper.doRetrieve(search);

		// Then: 검색한 콘텐츠 번호에 해당하는 데이터만 조회되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("* retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}",
					item.getMemberId(), item.getContentId(),
					item.getRatingScore(), item.getWatchlist()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getMemberId() == testData.getMemberId()
						&& item.getContentId() == testData.getContentId()
				));

		assertFalse(list.stream()
				.anyMatch(item ->
						item.getContentId() == comparisonData.getContentId()
				));
	}

	/**
	 * 보고 싶어요 여부 조건을 이용한 회원별 콘텐츠 목록 조회 검증
	 */
	@Test
	@DisplayName("보고 싶어요 조건 회원별 콘텐츠 목록 조회")
	void doRetrieveByWatchlist() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieveByWatchlist()      │");
		log.debug("└──────────────────────────────┘");

		// Given: 보고 싶어요 Y와 N 상태의 데이터를 각각 등록
		assertEquals(1, mapper.doSave(testData));

		int comparisonContentId = nextId("SEQ_CONTENT");
		insertContent(comparisonContentId);

		MemberContentVO comparisonData = new MemberContentVO(
				testData.getMemberId(),
				comparisonContentId,
				4,
				CODE_NO,
				null,
				null,
				null);

		assertEquals(1, mapper.doSave(comparisonData));

		DTO search = new DTO();
		search.setPageNo(0);
		search.setPageSize(0);
		search.setSearchDiv("30");
		search.setSearchWord(CODE_YES);

		// When: 보고 싶어요 Y를 조건으로 목록을 조회
		List<MemberContentVO> list = mapper.doRetrieve(search);

		// Then: Y 상태의 데이터는 포함되고 N 상태의 데이터는 제외되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("* retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}",
					item.getMemberId(), item.getContentId(),
					item.getRatingScore(), item.getWatchlist()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getMemberId() == testData.getMemberId()
						&& item.getContentId() == testData.getContentId()
				));

		assertFalse(list.stream()
				.anyMatch(item ->
						item.getMemberId() == comparisonData.getMemberId()
						&& item.getContentId() == comparisonData.getContentId()
				));
	}

	/**
	 * 별점 조건을 이용한 회원별 콘텐츠 목록 조회 검증
	 */
	@Test
	@DisplayName("별점 조건 회원별 콘텐츠 목록 조회")
	void doRetrieveByRatingScore() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrieveByRatingScore()    │");
		log.debug("└──────────────────────────────┘");

		// Given: 서로 다른 별점을 가진 데이터를 각각 등록
		assertEquals(1, mapper.doSave(testData));

		int comparisonContentId = nextId("SEQ_CONTENT");
		insertContent(comparisonContentId);

		MemberContentVO comparisonData = new MemberContentVO(
				testData.getMemberId(),
				comparisonContentId,
				5,
				CODE_YES,
				null,
				null,
				null);

		assertEquals(1, mapper.doSave(comparisonData));

		DTO search = new DTO();
		search.setPageNo(0);
		search.setPageSize(0);
		search.setSearchDiv("40");
		search.setSearchWord(String.valueOf(testData.getRatingScore()));

		// When: 별점을 조건으로 회원별 콘텐츠 목록을 조회
		List<MemberContentVO> list = mapper.doRetrieve(search);

		// Then: 검색한 별점의 데이터만 조회되어야 함
		assertNotNull(list);
		assertFalse(list.isEmpty());

		log.debug("* retrievedCount: {}건", list.size());

		list.forEach(item ->
			log.debug("* retrievedData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}",
					item.getMemberId(), item.getContentId(),
					item.getRatingScore(), item.getWatchlist()));

		assertTrue(list.stream()
				.anyMatch(item ->
						item.getMemberId() == testData.getMemberId()
						&& item.getContentId() == testData.getContentId()
				));

		assertFalse(list.stream()
				.anyMatch(item ->
						item.getMemberId() == comparisonData.getMemberId()
						&& item.getContentId() == comparisonData.getContentId()
				));
	}

	/**
	 * 회원별 콘텐츠 목록 페이징 처리 검증
	 */
	@Test
	@DisplayName("회원별 콘텐츠 목록 페이징")
	void doRetrievePaging() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doRetrievePaging()           │");
		log.debug("└──────────────────────────────┘");

		// Given: 같은 회원의 콘텐츠 세 건을 등록
		assertEquals(1, mapper.doSave(testData));

		int secondContentId = nextId("SEQ_CONTENT");
		int thirdContentId = nextId("SEQ_CONTENT");

		insertContent(secondContentId);
		insertContent(thirdContentId);

		MemberContentVO secondData = new MemberContentVO(
				testData.getMemberId(), secondContentId, 3, CODE_NO, null, null, null);

		MemberContentVO thirdData = new MemberContentVO(
				testData.getMemberId(), thirdContentId, 5, CODE_YES, null, null, null);

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
		List<MemberContentVO> firstPage = mapper.doRetrieve(firstPageSearch);
		List<MemberContentVO> secondPage = mapper.doRetrieve(secondPageSearch);

		// Then: 1페이지에는 두 건, 2페이지에는 한 건이 조회되어야 함
		assertNotNull(firstPage);
		assertNotNull(secondPage);
		assertEquals(2, firstPage.size());
		assertEquals(1, secondPage.size());

		log.debug("* firstPageCount: {}건", firstPage.size());
		log.debug("* secondPageCount: {}건", secondPage.size());

		firstPage.forEach(item ->
			log.debug("* firstPageData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}",
					item.getMemberId(), item.getContentId(),
					item.getRatingScore(), item.getWatchlist()));

		secondPage.forEach(item ->
			log.debug("* secondPageData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}",
					item.getMemberId(), item.getContentId(),
					item.getRatingScore(), item.getWatchlist()));

		assertTrue(firstPage.stream()
				.noneMatch(firstItem ->
						secondPage.stream()
								.anyMatch(secondItem ->
										secondItem.getMemberId() == firstItem.getMemberId()
										&& secondItem.getContentId() == firstItem.getContentId()
								)
				));
	}

	/**
	 * 별점 없이 보고 싶어요만 등록한 경우의 날짜 처리 검증
	 */
	@Test
	@DisplayName("별점 없이 보고 싶어요 등록")
	void doSaveWithoutRating() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSaveWithoutRating()        │");
		log.debug("└──────────────────────────────┘");

		// Given: 별점은 NULL, 보고 싶어요는 Y로 설정
		testData.setRatingScore(null);
		testData.setWatchlist(CODE_YES);

		// When: 회원별 콘텐츠를 등록하고 실제 저장된 데이터를 조회
		int flag = mapper.doSave(testData);
		MemberContentVO outVO = mapper.doSelectOne(testData);

		// Then: 별점 일시는 NULL이고 보고 싶어요 등록 일시는 생성되어야 함
		assertEquals(1, flag);
		assertNotNull(outVO);

		log.debug("* savedData: memberId-{}, contentId-{}, ratingScore-{}, watchlist-{}, ratedDt-{}, watchlistDt-{}, updatedDt-{}",
				outVO.getMemberId(), outVO.getContentId(), outVO.getRatingScore(), outVO.getWatchlist(),
				outVO.getRatedDt(), outVO.getWatchlistDt(), outVO.getUpdatedDt());

		assertNull(outVO.getRatingScore());
		assertEquals(CODE_YES, outVO.getWatchlist());
		assertNull(outVO.getRatedDt());
		assertNotNull(outVO.getWatchlistDt());
		assertNotNull(outVO.getUpdatedDt());
	}

	/**
	 * 동일한 복합 PK를 이용한 중복 등록 예외 검증
	 */
	@Test
	@DisplayName("회원별 콘텐츠 중복 등록")
	void doSaveDuplicate() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doSaveDuplicate()            │");
		log.debug("└──────────────────────────────┘");

		// Given: 회원별 콘텐츠를 한 건 등록
		assertEquals(1, mapper.doSave(testData));

		// When, Then: 동일한 복합 PK를 다시 등록하면 예외가 발생해야 함
		log.debug("* duplicateData: memberId-{}, contentId-{}",
				testData.getMemberId(), testData.getContentId());

		assertThrows(
				DataIntegrityViolationException.class,
				() -> mapper.doSave(testData));
	}

	/**
	 * 존재하지 않는 회원별 콘텐츠 수정 결과 검증
	 */
	@Test
	@DisplayName("존재하지 않는 회원별 콘텐츠 수정")
	void doUpdateNotFound() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doUpdateNotFound()           │");
		log.debug("└──────────────────────────────┘");

		// Given: DB에 등록되지 않은 복합 PK를 준비
		MemberContentVO missingData = new MemberContentVO(
				testData.getMemberId(),
				nextId("SEQ_CONTENT"),
				3,
				CODE_NO,
				null,
				null,
				null);

		// When: 존재하지 않는 회원별 콘텐츠를 수정
		int flag = mapper.doUpdate(missingData);

		// Then: 수정된 행이 없으므로 0이 반환되어야 함
		log.debug("* updateResult: memberId-{}, contentId-{}, result-{}건",
				missingData.getMemberId(), missingData.getContentId(), flag);

		assertEquals(0, flag);
	}

	/**
	 * 존재하지 않는 회원별 콘텐츠 삭제 결과 검증
	 */
	@Test
	@DisplayName("존재하지 않는 회원별 콘텐츠 삭제")
	void doDeleteNotFound() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ doDeleteNotFound()           │");
		log.debug("└──────────────────────────────┘");

		// Given: DB에 등록되지 않은 복합 PK를 준비
		MemberContentVO missingData = new MemberContentVO();
		missingData.setMemberId(testData.getMemberId());
		missingData.setContentId(nextId("SEQ_CONTENT"));

		// When: 존재하지 않는 회원별 콘텐츠를 삭제
		int flag = mapper.doDelete(missingData);

		// Then: 삭제된 행이 없으므로 0이 반환되어야 함
		log.debug("* deleteResult: memberId-{}, contentId-{}, result-{}건",
				missingData.getMemberId(), missingData.getContentId(), flag);

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
	 * 회원별 콘텐츠 FK 검증에 필요한 테스트 회원 등록
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
				"junit_member_content_" + memberId + "@test.com",
				"junit-password",
				"JUnit회원콘텐츠" + memberId,
				MEMBER_ROLE_USER,
				MEMBER_STATUS_ACTIVE);
	}

	/**
	 * 회원별 콘텐츠 FK 검증에 필요한 테스트 콘텐츠 등록
	 *
	 * @param contentId 콘텐츠 번호
	 */
	private void insertContent(int contentId) {
		String sql = """
				INSERT INTO CONTENT (
					CONTENT_ID,
					EXTERNAL_ID,
					TITLE_ORG,
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
				contentId,
				"JUNIT_MEMBER_CONTENT_" + contentId,
				"JUnit Member Content " + contentId);
	}
}