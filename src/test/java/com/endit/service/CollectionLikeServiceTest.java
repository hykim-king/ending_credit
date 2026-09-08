package com.endit.service;

import static com.endit.support.DatabaseTestFixtures.insertCollection;
import static com.endit.support.DatabaseTestFixtures.insertMember;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.endit.domain.CollectionLikeItemVO;
import com.endit.domain.CollectionLikeVO;
import com.endit.domain.CollectionVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.CollectionLikeMapper;

/**
 * <pre>
 * Class Name  : CollectionLikeServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 컬렉션 좋아요 Service를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. gunwoo      최초 생성
 * 2026. 8. 28. jinyoung    중복 등록 멱등 처리 검증 반영
 * 2026. 8. 29. jinyoung    인증·공개 범위·본인 제한 및 상태 조회 검증 추가
 * 2026. 9. 08. jinyoung    회원·컬렉션 부모 픽스처를 운영 시퀀스와 분리
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 27.
 */
@SpringBootTest
@Transactional
@DisplayName("CollectionLikeService 통합 테스트")
class CollectionLikeServiceTest {

	private static final int MISSING_COLLECTION_ID = Integer.MAX_VALUE;

	@Autowired
	private CollectionLikeService collectionLikeService;

	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("좋아요 등록")
	void create() {
		// Given: 다른 회원이 소유한 공개 컬렉션과 좋아요할 회원을 준비한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());

		// When: 회원이 컬렉션에 좋아요를 등록한다.
		CollectionLikeVO result = collectionLikeService.create(memberId, collection.getCollectionId());

		// Then: 회원·컬렉션 번호와 등록 일시가 반환되어야 한다.
		assertEquals(memberId, result.getMemberId());
		assertEquals(collection.getCollectionId(), result.getCollectionId());
		assertNotNull(result.getCreatedDt());
	}

	@Test
	@DisplayName("이미 좋아요를 누른 컬렉션은 기존 정보를 반환")
	void createDuplicate() {
		// Given: 회원이 컬렉션에 좋아요를 먼저 등록한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());
		CollectionLikeVO created = collectionLikeService.create(memberId, collection.getCollectionId());

		// When: 동일한 컬렉션에 좋아요를 다시 등록한다.
		CollectionLikeVO duplicate = collectionLikeService.create(memberId, collection.getCollectionId());

		// Then: 새 행 대신 최초 등록 정보를 그대로 반환해야 한다.
		assertEquals(created.getMemberId(), duplicate.getMemberId());
		assertEquals(created.getCollectionId(), duplicate.getCollectionId());
		assertEquals(created.getCreatedDt(), duplicate.getCreatedDt());
	}

	@Test
	@DisplayName("잘못된 회원 번호이면 예외 발생")
	void createInvalidMemberId() {
		// Given: 좋아요 대상 컬렉션을 준비한다.
		CollectionVO collection = createCollection(createMemberId());

		// When, Then: 유효하지 않은 회원 번호로 등록하면 예외가 발생해야 한다.
		assertThrows(IllegalArgumentException.class,
				() -> collectionLikeService.create(0, collection.getCollectionId()));
	}

	@Test
	@DisplayName("좋아요 취소")
	void delete() {
		// Given: 회원이 컬렉션에 좋아요를 등록한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());
		collectionLikeService.create(memberId, collection.getCollectionId());

		// When: 등록한 좋아요를 취소한다.
		collectionLikeService.delete(memberId, collection.getCollectionId());

		// Then: 취소한 좋아요를 조회하면 예외가 발생해야 한다.
		assertThrows(NoSuchElementException.class,
				() -> collectionLikeService.get(memberId, collection.getCollectionId()));
	}

	@Test
	@DisplayName("좋아요가 없는 컬렉션 취소도 정상 처리")
	void deleteNotFound() {
		// Given: 좋아요를 등록하지 않은 회원과 컬렉션을 준비한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());

		// When, Then: 존재하지 않는 좋아요 취소도 예외 없이 처리되어야 한다.
		assertDoesNotThrow(() -> collectionLikeService.delete(memberId, collection.getCollectionId()));
	}

	@Test
	@DisplayName("좋아요 여부 단건 조회")
	void get() {
		// Given: 회원이 컬렉션에 좋아요를 등록한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());
		collectionLikeService.create(memberId, collection.getCollectionId());

		// When: 회원과 컬렉션 번호로 좋아요 정보를 조회한다.
		CollectionLikeVO result = collectionLikeService.get(memberId, collection.getCollectionId());

		// Then: 등록한 회원과 컬렉션 번호가 반환되어야 한다.
		assertEquals(memberId, result.getMemberId());
		assertEquals(collection.getCollectionId(), result.getCollectionId());
	}

	@Test
	@DisplayName("좋아요를 누르지 않은 컬렉션 조회 시 예외 발생")
	void getNotFound() {
		// Given: 좋아요 기록이 없는 회원을 준비한다.
		int memberId = createMemberId();

		// When, Then: 존재하지 않는 좋아요를 조회하면 예외가 발생해야 한다.
		assertThrows(NoSuchElementException.class,
				() -> collectionLikeService.get(memberId, MISSING_COLLECTION_ID));
	}

	@Test
	@DisplayName("컬렉션 좋아요 상태 조회")
	void isLiked() {
		// Given: 좋아요를 등록하지 않은 회원과 컬렉션을 준비한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());

		// Then: 등록 전 좋아요 상태는 false여야 한다.
		assertFalse(collectionLikeService.isLiked(memberId, collection.getCollectionId()));

		// When: 컬렉션에 좋아요를 등록한다.
		collectionLikeService.create(memberId, collection.getCollectionId());

		// Then: 등록 후 좋아요 상태는 true여야 한다.
		assertTrue(collectionLikeService.isLiked(memberId, collection.getCollectionId()));
	}

	@Test
	@DisplayName("회원별 좋아요 컬렉션 목록 조회 시 기본 페이징과 전체 건수 설정")
	void retrieveByMember() {
		// Given: 한 회원이 서로 다른 두 컬렉션에 좋아요를 등록한다.
		int memberId = createMemberId();

		CollectionVO first = createCollection(createMemberId());
		CollectionVO second = createCollection(createMemberId());
		collectionLikeService.create(memberId, first.getCollectionId());
		collectionLikeService.create(memberId, second.getCollectionId());

		DTO param = new DTO();

		// When: 회원별 좋아요 컬렉션 목록을 조회한다.
		List<CollectionLikeItemVO> result = collectionLikeService
				.retrieveByMember(memberId, param, OptionalLong.of(memberId));

		// Then: 두 컬렉션과 기본 페이징 및 전체 건수가 반환되어야 한다.
		assertEquals(2, result.size());
		assertEquals(1, param.getPageNo());
		assertEquals(12, param.getPageSize());
		assertEquals(2, param.getTotalCnt());
	}

	@Test
	@DisplayName("좋아요한 컬렉션이 없으면 빈 목록 반환")
	void retrieveByMemberEmpty() {
		// Given: 좋아요 기록이 없는 회원과 조회 조건을 준비한다.
		int memberId = createMemberId();
		DTO param = new DTO();

		// When: 회원별 좋아요 컬렉션 목록을 조회한다.
		List<CollectionLikeItemVO> result = collectionLikeService
				.retrieveByMember(memberId, param, OptionalLong.empty());

		// Then: 빈 목록을 반환하고 전체 건수는 0이어야 한다.
		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	@Test
	@DisplayName("조회 조건이 null이면 예외 발생")
	void retrieveByMemberNull() {
		// Given: 조회할 회원을 준비한다.
		int memberId = createMemberId();

		// When, Then: 조회 조건이 null이면 예외가 발생해야 한다.
		assertThrows(IllegalArgumentException.class,
				() -> collectionLikeService.retrieveByMember(memberId, null, OptionalLong.empty()));
	}

	@Test
	@DisplayName("컬렉션별 좋아요 회원 목록 조회")
	void retrieveByCollection() {
		// Given: 회원이 컬렉션에 좋아요를 등록한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());
		collectionLikeService.create(memberId, collection.getCollectionId());

		// When: 컬렉션별 좋아요 회원 목록을 조회한다.
		List<CollectionLikeVO> result = collectionLikeService.retrieveByCollection(collection.getCollectionId());

		// Then: 좋아요한 회원 한 명이 반환되어야 한다.
		assertEquals(1, result.size());
		assertEquals(memberId, result.get(0).getMemberId());
	}

	@Test
	@DisplayName("컬렉션 좋아요 개수 조회")
	void countByCollection() {
		// Given: 좋아요가 없는 컬렉션과 좋아요할 회원을 준비한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());

		// Then: 등록 전 좋아요 수는 0이어야 한다.
		assertEquals(0, collectionLikeService.countByCollection(collection.getCollectionId()));

		// When: 컬렉션에 좋아요를 등록한다.
		collectionLikeService.create(memberId, collection.getCollectionId());

		// Then: 등록 후 좋아요 수는 1이어야 한다.
		assertEquals(1, collectionLikeService.countByCollection(collection.getCollectionId()));
	}

	@Test
	@DisplayName("잘못된 컬렉션 번호이면 예외 발생")
	void invalidCollectionId() {
		// When, Then: 유효하지 않은 컬렉션 번호로 집계하면 예외가 발생해야 한다.
		assertThrows(IllegalArgumentException.class, () -> collectionLikeService.countByCollection(0));
	}

	@Test
	@DisplayName("본인 컬렉션 좋아요는 403 예외")
	void createOwnCollectionLike() {
		// Given: 회원 본인이 소유한 컬렉션을 준비한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(memberId);

		// When, Then: 본인 컬렉션에 좋아요를 등록하면 권한 예외가 발생해야 한다.
		assertThrows(ForbiddenOperationException.class,
				() -> collectionLikeService.create(memberId, collection.getCollectionId()));
	}

	@Test
	@DisplayName("비공개 컬렉션 비소유자 좋아요는 404 예외")
	void createPrivateCollectionLike() {
		// Given: 다른 회원이 소유한 비공개 컬렉션을 준비한다.
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId(), "N");

		// When, Then: 비소유자가 좋아요를 등록하면 조회 불가 예외가 발생해야 한다.
		assertThrows(NoSuchElementException.class,
				() -> collectionLikeService.create(memberId, collection.getCollectionId()));
	}

	@Test
	@DisplayName("비공개 좋아요 컬렉션은 소유자에게만 목록 노출")
	void retrievePrivateLikedCollection() {
		// Given: 회원이 다른 회원의 비공개 컬렉션을 좋아요한 상태를 직접 등록한다.
		int likerId = createMemberId();
		int ownerId = createMemberId();
		CollectionVO collection = createCollection(ownerId, "N");
		assertEquals(1, collectionLikeMapper
				.insertCollectionLike(new CollectionLikeVO(likerId, collection.getCollectionId(), null)));

		// When: 비회원과 컬렉션 소유자 관점에서 좋아요 목록을 각각 조회한다.
		DTO anonymousParam = new DTO();
		List<CollectionLikeItemVO> anonymousResult = collectionLikeService
				.retrieveByMember(likerId, anonymousParam, OptionalLong.empty());

		DTO ownerParam = new DTO();
		List<CollectionLikeItemVO> ownerResult = collectionLikeService
				.retrieveByMember(likerId, ownerParam, OptionalLong.of(ownerId));

		// Then: 비회원에게는 숨기고 컬렉션 소유자에게만 노출해야 한다.
		assertTrue(anonymousResult.isEmpty());
		assertEquals(0, anonymousParam.getTotalCnt());
		assertEquals(1, ownerResult.size());
		assertEquals(collection.getCollectionId(), ownerResult.get(0).getCollectionId());
	}

	/**
	 * 외래 키를 만족하는 테스트 회원을 운영 시퀀스와 분리해 등록
	 *
	 * @return 등록된 테스트 회원 번호
	 */
	private int createMemberId() {
		String token = createToken();
		MemberVO member = new MemberVO();

		member.setEmail("collection-like-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("좋아요" + token.substring(0, 8));
		member.setIntroduction("컬렉션 좋아요 통합 테스트 회원");
		member.setRole("USER");

		return insertMember(jdbcTemplate, member).getMemberId().intValue();
	}

	/**
	 * 공개 테스트 컬렉션을 운영 시퀀스와 분리해 등록
	 *
	 * @param memberId 컬렉션 소유 회원 번호
	 * @return 등록된 테스트 컬렉션
	 */
	private CollectionVO createCollection(int memberId) {
		return createCollection(memberId, "Y");
	}

	/**
	 * 공개 여부를 지정한 테스트 컬렉션을 운영 시퀀스와 분리해 등록
	 *
	 * @param memberId 컬렉션 소유 회원 번호
	 * @param isPublic 공개 여부
	 * @return 등록된 테스트 컬렉션
	 */
	private CollectionVO createCollection(int memberId, String isPublic) {
		CollectionVO collection = new CollectionVO(0, memberId, "좋아요 통합 테스트 컬렉션 " + createToken(),
				"컬렉션 좋아요 Service 통합 테스트", isPublic, null, null);

		return insertCollection(jdbcTemplate, collection);
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
