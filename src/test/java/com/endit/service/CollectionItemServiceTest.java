package com.endit.service;

import static com.endit.support.DatabaseTestFixtures.insertContent;
import static com.endit.support.DatabaseTestFixtures.insertMember;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.endit.auth.ForbiddenOperationException;
import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;
import com.endit.domain.CollectionVO;
import com.endit.domain.ContentVO;
import com.endit.domain.MemberContentVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.CollectionMapper;
import com.endit.mapper.MemberContentMapper;

/**
 * <pre>
 * Class Name  : CollectionItemServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 컬렉션 작품 Service를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    Spring Bean·DB 기반 컬렉션 작품 통합 테스트 생성
 * 2026. 8. 29. jinyoung    인증 회원 및 컬렉션 작품 소유권 검증 추가
 * 2026. 8. 31. jinyoung    컬렉션 작품 평균 별점 조회 검증 추가
 * 2026. 9. 05. jinyoung    대상 외 부모 데이터를 운영 시퀀스와 분리
 * 2026. 9. 09. jinyoung    작품 추가·삭제 시 수정 일시 갱신 및 기존 정보 보존 검증
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@SpringBootTest
@Transactional
@DisplayName("CollectionItemService 통합 테스트")
class CollectionItemServiceTest {

	private static final int MISSING_CONTENT_ID = Integer.MAX_VALUE;

	@Autowired
	private CollectionItemService collectionItemService;

	@Autowired
	private CollectionMapper collectionMapper;

	@Autowired
	private MemberContentMapper memberContentMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("컬렉션 작품 목록 조회 시 컬렉션 조건과 페이징 설정")
	void retrieve() {
		// Given: 두 회원의 별점이 등록된 작품 하나를 컬렉션에 추가한다.
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		
		collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(), createItem(content.getContentId()));
		saveRating(collection.getMemberId(), content.getContentId(), 3);
		saveRating(createMemberId(), content.getContentId(), 5);
		DTO param = new DTO();

		// When: 컬렉션 소유자가 작품 목록을 조회한다.
		List<CollectionItemVO> result = collectionItemService
				.retrieve(collection.getCollectionId(), param, viewer(collection));

		// Then: 작품 정보, 평균 별점, 기본 페이징과 컬렉션 검색 조건이 반환되어야 한다.
		assertEquals(1, result.size());
		assertEquals(collection.getCollectionId(), result.get(0).getCollectionId());
		assertEquals(content.getContentId(), result.get(0).getContentId());
		assertEquals(content.getTitleKo(), result.get(0).getTitleKo());
		assertEquals(4.0, result.get(0).getAverageRating());
		assertEquals(1, param.getPageNo());
		assertEquals(12, param.getPageSize());
		assertEquals(1, param.getTotalCnt());
		assertEquals("10", param.getSearchDiv());
		assertEquals(String.valueOf(collection.getCollectionId()), param.getSearchWord());
	}

	@Test
	@DisplayName("컬렉션 작품이 없으면 빈 목록 반환")
	void retrieveEmpty() {
		// Given: 작품이 등록되지 않은 컬렉션과 조회 조건을 준비한다.
		CollectionVO collection = createCollection();
		DTO param = new DTO();

		// When: 컬렉션 작품 목록을 조회한다.
		List<CollectionItemVO> result = collectionItemService
				.retrieve(collection.getCollectionId(), param,viewer(collection));

		// Then: 빈 목록을 반환하고 전체 건수는 0이어야 한다.
		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	@Test
	@DisplayName("컬렉션 작품 단건 조회")
	void get() {
		// Given: 컬렉션에 작품 하나를 등록한다.
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		
		collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(), createItem(content.getContentId()));

		// When: 컬렉션과 콘텐츠 번호로 작품을 조회한다.
		CollectionItemVO result = collectionItemService
				.get(collection.getCollectionId(), content.getContentId(), viewer(collection));

		// Then: 등록한 복합 키와 추가 일시가 반환되어야 한다.
		assertEquals(collection.getCollectionId(), result.getCollectionId());
		assertEquals(content.getContentId(), result.getContentId());
		assertNotNull(result.getAddedDt());
		assertNotNull(collectionMapper.doSelectOne(collection).getUpdatedDt());
	}

	@Test
	@DisplayName("존재하지 않는 컬렉션 작품 조회 시 예외 발생")
	void getNotFound() {
		// Given: 작품이 등록되지 않은 컬렉션을 준비한다.
		CollectionVO collection = createCollection();

		// When, Then: 존재하지 않는 콘텐츠 번호로 조회하면 예외가 발생해야 한다.
		assertThrows(NoSuchElementException.class,
				() -> collectionItemService.get(
						collection.getCollectionId(), MISSING_CONTENT_ID, viewer(collection)));
	}

	@Test
	@DisplayName("컬렉션 작품 추가 시 수정 일시 갱신 및 기존 정보 보존")
	void create() {
		// Given: 수정 일시를 과거로 설정한 컬렉션과 추가할 콘텐츠를 준비한다.
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		CollectionVO before = prepareUpdatedDtCheck(collection);
		CollectionVO otherCollection = prepareUpdatedDtCheck(createCollection());

		// When: 컬렉션에 작품을 추가한다.
		CollectionItemVO result = collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(), createItem(content.getContentId()));

		// Then: 작품이 등록되고 대상 컬렉션의 수정 일시만 갱신되어야 한다.
		assertEquals(collection.getCollectionId(), result.getCollectionId());
		assertEquals(content.getContentId(), result.getContentId());
		assertNotNull(result.getAddedDt());
		assertCollectionUpdated(before);
		assertEquals(otherCollection.getUpdatedDt(), collectionMapper.doSelectOne(otherCollection).getUpdatedDt());
	}

	@Test
	@DisplayName("이미 포함된 작품은 추가하지 않음")
	void createDuplicate() {
		// Given: 컬렉션에 작품 하나를 먼저 등록한다.
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		
		collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(), createItem(content.getContentId()));

		CollectionVO before = prepareUpdatedDtCheck(collection);

		// When: 같은 작품을 다시 추가하여 중복 예외가 발생하는지 확인한다.
		assertThrows(IllegalStateException.class, 
				() -> collectionItemService.create(
						collection.getMemberId(), collection.getCollectionId(), createItem(content.getContentId())));

		// Then: 중복 추가가 거절된 컬렉션의 수정 일시는 유지되어야 한다.
		assertEquals(before.getUpdatedDt(), collectionMapper.doSelectOne(collection).getUpdatedDt());
	}

	@Test
	@DisplayName("비소유자의 컬렉션 작품 추가는 403 예외")
	void createByNonOwner() {
		// Given: 컬렉션 소유자와 다른 회원 및 추가할 콘텐츠를 준비한다.
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		
		int otherMemberId = createMemberId();
		CollectionVO before = prepareUpdatedDtCheck(collection);

		// When: 비소유자가 작품을 추가하여 권한 예외가 발생하는지 확인한다.
		assertThrows(ForbiddenOperationException.class, 
				() -> collectionItemService.create(
						otherMemberId, collection.getCollectionId(), createItem(content.getContentId())));

		// Then: 추가 권한이 없는 요청으로 컬렉션의 수정 일시가 바뀌지 않아야 한다.
		assertEquals(before.getUpdatedDt(), collectionMapper.doSelectOne(collection).getUpdatedDt());
	}

	@Test
	@DisplayName("컬렉션 작품 삭제 시 수정 일시 갱신 및 기존 정보 보존")
	void delete() {
		// Given: 컬렉션에 삭제할 작품을 등록한다.
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		
		collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(), createItem(content.getContentId()));

		CollectionVO before = prepareUpdatedDtCheck(collection);
		CollectionVO otherCollection = prepareUpdatedDtCheck(createCollection());

		// When: 소유자가 컬렉션 작품을 삭제한다.
		collectionItemService.delete(
				collection.getMemberId(), collection.getCollectionId(), content.getContentId());

		// Then: 작품이 삭제되고 대상 컬렉션의 수정 일시만 갱신되어야 한다.
		assertCollectionUpdated(before);
		assertEquals(otherCollection.getUpdatedDt(), collectionMapper.doSelectOne(otherCollection).getUpdatedDt());
		assertThrows(NoSuchElementException.class, 
				() -> collectionItemService.get(
						collection.getCollectionId(), content.getContentId(), viewer(collection)));
	}

	@Test
	@DisplayName("비소유자의 컬렉션 작품 삭제 시 수정 일시 유지")
	void deleteByNonOwner() {
		// Given: 작품이 등록된 컬렉션과 소유자가 아닌 회원을 준비한다.
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(), createItem(content.getContentId()));
		int otherMemberId = createMemberId();
		CollectionVO before = prepareUpdatedDtCheck(collection);

		// When: 비소유자가 작품을 삭제하여 권한 예외가 발생하는지 확인한다.
		assertThrows(ForbiddenOperationException.class,
				() -> collectionItemService.delete(
						otherMemberId, collection.getCollectionId(), content.getContentId()));

		// Then: 삭제가 거절된 작품과 컬렉션의 수정 일시는 유지되어야 한다.
		assertNotNull(collectionItemService.get(
				collection.getCollectionId(), content.getContentId(), viewer(collection)));
		assertEquals(before.getUpdatedDt(), collectionMapper.doSelectOne(collection).getUpdatedDt());
	}

	@Test
	@DisplayName("존재하지 않는 컬렉션 작품 삭제 시 수정 일시 유지")
	void deleteNotFound() {
		// Given: 작품이 없는 컬렉션의 수정 일시를 과거로 설정한다.
		CollectionVO collection = createCollection();
		CollectionVO before = prepareUpdatedDtCheck(collection);

		// When: 없는 작품을 삭제하여 작품 조회 예외가 발생하는지 확인한다.
		assertThrows(NoSuchElementException.class,
				() -> collectionItemService.delete(
						collection.getMemberId(), collection.getCollectionId(), MISSING_CONTENT_ID));

		// Then: 삭제할 작품이 없으므로 컬렉션의 수정 일시는 유지되어야 한다.
		assertEquals(before.getUpdatedDt(), collectionMapper.doSelectOne(collection).getUpdatedDt());
	}

	@Test
	@DisplayName("잘못된 컬렉션 번호이면 예외 발생")
	void invalidCollectionId() {
		// When, Then: 유효하지 않은 컬렉션 번호로 조회하면 예외가 발생해야 한다.
		assertThrows(IllegalArgumentException.class,
				() -> collectionItemService.retrieve(0, new DTO(), OptionalLong.empty()));
	}

	/**
	 * 대기 없이 갱신 여부를 비교하도록 테스트 컬렉션의 수정 일시를 과거로 설정
	 *
	 * @param collection 테스트 컬렉션
	 * @return 변경 전 컬렉션 정보
	 */
	private CollectionVO prepareUpdatedDtCheck(CollectionVO collection) {
		// Given: 테스트 대상 한 건의 수정 일시를 하루 전으로 설정한다.
		assertEquals(1, jdbcTemplate.update(
				"UPDATE COLLECTION SET UPDATED_DT = SYSDATE - 1 WHERE COLLECTION_ID = ?",
				collection.getCollectionId()));

		// When: 이후 갱신 여부와 기본 정보 보존을 비교할 기준값을 조회한다.
		return collectionMapper.doSelectOne(collection);
	}

	/**
	 * 수정 일시가 갱신되고 컬렉션 기본 정보가 유지되었는지 검증
	 *
	 * @param before 변경 전 컬렉션 정보
	 */
	private void assertCollectionUpdated(CollectionVO before) {
		// When: 작품 변경 이후의 컬렉션 정보를 다시 조회한다.
		CollectionVO after = collectionMapper.doSelectOne(before);

		// Then: 수정 일시는 기존 값보다 최신이어야 한다.
		assertNotNull(after.getUpdatedDt());
		assertTrue(after.getUpdatedDt().compareTo(before.getUpdatedDt()) > 0);

		// Then: 식별자, 작성자, 제목, 설명, 공개 여부와 생성 일시는 유지되어야 한다.
		assertEquals(before.getCollectionId(), after.getCollectionId());
		assertEquals(before.getMemberId(), after.getMemberId());
		assertEquals(before.getTitle(), after.getTitle());
		assertEquals(before.getDescription(), after.getDescription());
		assertEquals(before.getIsPublic(), after.getIsPublic());
		assertEquals(before.getCreatedDt(), after.getCreatedDt());
	}

	/**
	 * 외래 키를 만족하는 회원과 컬렉션을 현재 트랜잭션에 등록
	 *
	 * @return 컬렉션 정보
	 */
	private CollectionVO createCollection() {
		int memberId = createMemberId();

		CollectionVO collection = new CollectionVO(
				0, memberId, "작품 통합 테스트 컬렉션", "컬렉션 작품 Service 통합 테스트", "Y", null, null);

		assertEquals(1, collectionMapper.doSave(collection));

		return collection;
	}

	/**
	 * 외래 키를 만족하는 테스트 회원을 현재 트랜잭션에 등록
	 *
	 * @return 등록된 테스트 회원 번호
	 */
	private int createMemberId() {
		String token = createToken();

		MemberVO member = new MemberVO();
		member.setEmail("item-service-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("작품서비스" + token.substring(0, 8));
		member.setIntroduction("컬렉션 작품 통합 테스트 회원");
		member.setRole("USER");

		return insertMember(jdbcTemplate, member).getMemberId().intValue();
	}

	/**
	 * 외래 키를 만족하는 콘텐츠를 현재 트랜잭션에 등록
	 *
	 * @return 콘텐츠 정보
	 */
	private ContentVO createContent() {
		String token = createToken();

		ContentVO content = new ContentVO(0, "INTEGRATION_" + token, "통합 테스트 콘텐츠", "Integration Test Content",
				"컬렉션 작품 Service 통합 테스트 콘텐츠", "2026-08-26", 120, "Korea", "https://example.com/poster.jpg",
				"https://example.com/backdrop.jpg", null);

		// 컬렉션 작품 Service만 검증하도록 CONTENT 시퀀스와 부모 준비를 분리한다.
		return insertContent(jdbcTemplate, content);
	}

	/**
	 * 컬렉션 작품 등록 요청 생성
	 *
	 * @param contentId 콘텐츠 번호
	 * @return 컬렉션 작품 정보
	 */
	private CollectionItemVO createItem(int contentId) {
		return new CollectionItemVO(0, contentId, null);
	}

	/**
	 * 콘텐츠 평균 별점 검증에 사용할 회원 평가 등록
	 *
	 * @param memberId    회원 번호
	 * @param contentId   콘텐츠 번호
	 * @param ratingScore 별점
	 */
	private void saveRating(int memberId, int contentId, int ratingScore) {
		MemberContentVO rating = new MemberContentVO(memberId, contentId, ratingScore, "N", null, null, null);
		assertEquals(1, memberContentMapper.doSave(rating));
	}

	/**
	 * 테스트 컬렉션 소유자를 현재 조회 회원으로 사용
	 *
	 * @param collection 컬렉션 정보
	 * @return 컬렉션 소유자의 회원 번호
	 */
	private OptionalLong viewer(CollectionVO collection) {
		return OptionalLong.of(collection.getMemberId());
	}

	/**
	 * DB 고유 제약조건 충돌을 피할 테스트 식별자 생성
	 *
	 * @return 하이픈을 제외한 UUID 문자열
	 */
	private String createToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
