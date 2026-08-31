package com.endit.service;

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
import org.springframework.transaction.annotation.Transactional;

import com.endit.auth.ForbiddenOperationException;
import com.endit.cmn.DTO;
import com.endit.domain.CollectionLikeItemVO;
import com.endit.domain.CollectionLikeVO;
import com.endit.domain.CollectionVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.CollectionMapper;
import com.endit.mapper.CollectionLikeMapper;
import com.endit.mapper.MemberMapper;

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
	private CollectionMapper collectionMapper;

	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	/** 실제 DB 좋아요 등록 결과 검증 */
	@Test
	@DisplayName("좋아요 등록")
	void create() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());

		CollectionLikeVO result = collectionLikeService.create(
				memberId, collection.getCollectionId());

		assertEquals(memberId, result.getMemberId());
		assertEquals(collection.getCollectionId(), result.getCollectionId());
		assertNotNull(result.getCreatedDt());
	}

	/** 실제 DB에 이미 등록된 좋아요의 멱등 처리 검증 */
	@Test
	@DisplayName("이미 좋아요를 누른 컬렉션은 기존 정보를 반환")
	void createDuplicate() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());
		CollectionLikeVO created =
				collectionLikeService.create(memberId, collection.getCollectionId());

		CollectionLikeVO duplicate =
				collectionLikeService.create(memberId, collection.getCollectionId());

		assertEquals(created.getMemberId(), duplicate.getMemberId());
		assertEquals(created.getCollectionId(), duplicate.getCollectionId());
		assertEquals(created.getCreatedDt(), duplicate.getCreatedDt());
	}

	/** 잘못된 회원 번호에 대한 입력값 검증 */
	@Test
	@DisplayName("잘못된 회원 번호이면 예외 발생")
	void createInvalidMemberId() {
		CollectionVO collection = createCollection(createMemberId());

		assertThrows(
				IllegalArgumentException.class,
				() -> collectionLikeService.create(0, collection.getCollectionId()));
	}

	/** 실제 DB에서 컬렉션 좋아요 취소 결과 검증 */
	@Test
	@DisplayName("좋아요 취소")
	void delete() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());
		collectionLikeService.create(memberId, collection.getCollectionId());

		collectionLikeService.delete(memberId, collection.getCollectionId());

		assertThrows(
				NoSuchElementException.class,
				() -> collectionLikeService.get(memberId, collection.getCollectionId()));
	}

	/** 좋아요가 없는 컬렉션 취소의 멱등 결과 검증 */
	@Test
	@DisplayName("좋아요가 없는 컬렉션 취소도 정상 처리")
	void deleteNotFound() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());

		assertDoesNotThrow(
				() -> collectionLikeService.delete(
						memberId, collection.getCollectionId()));
	}

	/** 실제 DB에 등록한 좋아요의 단건 조회 검증 */
	@Test
	@DisplayName("좋아요 여부 단건 조회")
	void get() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());
		collectionLikeService.create(memberId, collection.getCollectionId());

		CollectionLikeVO result = collectionLikeService.get(
				memberId, collection.getCollectionId());

		assertEquals(memberId, result.getMemberId());
		assertEquals(collection.getCollectionId(), result.getCollectionId());
	}

	/** 존재하지 않는 좋아요 단건 조회 결과 검증 */
	@Test
	@DisplayName("좋아요를 누르지 않은 컬렉션 조회 시 예외 발생")
	void getNotFound() {
		int memberId = createMemberId();

		assertThrows(
				NoSuchElementException.class,
				() -> collectionLikeService.get(memberId, MISSING_COLLECTION_ID));
	}

	/** 등록 여부에 따른 좋아요 상태 조회 결과 검증 */
	@Test
	@DisplayName("컬렉션 좋아요 상태 조회")
	void isLiked() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());

		assertFalse(collectionLikeService.isLiked(
				memberId, collection.getCollectionId()));

		collectionLikeService.create(memberId, collection.getCollectionId());

		assertTrue(collectionLikeService.isLiked(
				memberId, collection.getCollectionId()));
	}

	/** 실제 DB 목록 조회와 기본 페이징 및 전체 건수 설정 검증 */
	@Test
	@DisplayName("회원별 좋아요 컬렉션 목록 조회 시 기본 페이징과 전체 건수 설정")
	void retrieveByMember() {
		int memberId = createMemberId();
		CollectionVO first = createCollection(createMemberId());
		CollectionVO second = createCollection(createMemberId());
		collectionLikeService.create(memberId, first.getCollectionId());
		collectionLikeService.create(memberId, second.getCollectionId());

		DTO param = new DTO();

		List<CollectionLikeItemVO> result =
				collectionLikeService.retrieveByMember(
						memberId, param, OptionalLong.of(memberId));

		assertEquals(2, result.size());
		assertEquals(1, param.getPageNo());
		assertEquals(12, param.getPageSize());
		assertEquals(2, param.getTotalCnt());
	}

	/** 좋아요한 컬렉션이 없을 때 실제 DB 조회 결과 검증 */
	@Test
	@DisplayName("좋아요한 컬렉션이 없으면 빈 목록 반환")
	void retrieveByMemberEmpty() {
		int memberId = createMemberId();
		DTO param = new DTO();

		List<CollectionLikeItemVO> result =
				collectionLikeService.retrieveByMember(
						memberId, param, OptionalLong.empty());

		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	/** null 조회 조건에 대한 입력값 검증 */
	@Test
	@DisplayName("조회 조건이 null이면 예외 발생")
	void retrieveByMemberNull() {
		int memberId = createMemberId();

		assertThrows(
				IllegalArgumentException.class,
				() -> collectionLikeService.retrieveByMember(
						memberId, null, OptionalLong.empty()));
	}

	/** 실제 DB 컬렉션별 좋아요 회원 목록 조회 검증 */
	@Test
	@DisplayName("컬렉션별 좋아요 회원 목록 조회")
	void retrieveByCollection() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());
		collectionLikeService.create(memberId, collection.getCollectionId());

		List<CollectionLikeVO> result =
				collectionLikeService.retrieveByCollection(collection.getCollectionId());

		assertEquals(1, result.size());
		assertEquals(memberId, result.get(0).getMemberId());
	}

	/** 실제 DB 컬렉션 좋아요 개수 조회 검증 */
	@Test
	@DisplayName("컬렉션 좋아요 개수 조회")
	void countByCollection() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId());

		assertEquals(0, collectionLikeService.countByCollection(collection.getCollectionId()));

		collectionLikeService.create(memberId, collection.getCollectionId());

		assertEquals(1, collectionLikeService.countByCollection(collection.getCollectionId()));
	}

	/** 잘못된 컬렉션 번호에 대한 입력값 검증 */
	@Test
	@DisplayName("잘못된 컬렉션 번호이면 예외 발생")
	void invalidCollectionId() {
		assertThrows(
				IllegalArgumentException.class,
				() -> collectionLikeService.countByCollection(0));
	}

	/** 본인 소유 컬렉션에는 좋아요를 등록할 수 없음을 검증 */
	@Test
	@DisplayName("본인 컬렉션 좋아요는 403 예외")
	void createOwnCollectionLike() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(memberId);

		assertThrows(
				ForbiddenOperationException.class,
				() -> collectionLikeService.create(
						memberId, collection.getCollectionId()));
	}

	/** 비공개 컬렉션은 비소유자가 좋아요를 등록할 수 없음을 검증 */
	@Test
	@DisplayName("비공개 컬렉션 비소유자 좋아요는 404 예외")
	void createPrivateCollectionLike() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(createMemberId(), "N");

		assertThrows(
				NoSuchElementException.class,
				() -> collectionLikeService.create(
						memberId, collection.getCollectionId()));
	}

	/** 기존 좋아요 대상이 비공개로 존재할 때 목록 공개 범위를 검증 */
	@Test
	@DisplayName("비공개 좋아요 컬렉션은 소유자에게만 목록 노출")
	void retrievePrivateLikedCollection() {
		int likerId = createMemberId();
		int ownerId = createMemberId();
		CollectionVO collection = createCollection(ownerId, "N");
		assertEquals(1, collectionLikeMapper.insertCollectionLike(
				new CollectionLikeVO(
						likerId, collection.getCollectionId(), null)));

		DTO anonymousParam = new DTO();
		List<CollectionLikeItemVO> anonymousResult =
				collectionLikeService.retrieveByMember(
						likerId, anonymousParam, OptionalLong.empty());

		DTO ownerParam = new DTO();
		List<CollectionLikeItemVO> ownerResult =
				collectionLikeService.retrieveByMember(
						likerId, ownerParam, OptionalLong.of(ownerId));

		assertTrue(anonymousResult.isEmpty());
		assertEquals(0, anonymousParam.getTotalCnt());
		assertEquals(1, ownerResult.size());
		assertEquals(collection.getCollectionId(),
				ownerResult.get(0).getCollectionId());
	}

	/** 외래 키를 만족하는 테스트 회원을 현재 트랜잭션에 등록 */
	private int createMemberId() {
		String token = createToken();
		MemberVO member = new MemberVO();
		member.setEmail("collection-like-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("좋아요" + token.substring(0, 8));
		member.setIntroduction("컬렉션 좋아요 통합 테스트 회원");
		member.setRole("USER");

		assertEquals(1, memberMapper.insertMember(member));
		assertNotNull(member.getMemberId());

		return member.getMemberId().intValue();
	}

	/** 외래 키를 만족하는 테스트 컬렉션을 현재 트랜잭션에 등록 */
	private CollectionVO createCollection(int memberId) {
		return createCollection(memberId, "Y");
	}

	/** 공개 여부를 지정해 테스트 컬렉션을 현재 트랜잭션에 등록 */
	private CollectionVO createCollection(int memberId, String isPublic) {
		CollectionVO collection = new CollectionVO(
				0,
				memberId,
				"좋아요 통합 테스트 컬렉션 " + createToken(),
				"컬렉션 좋아요 Service 통합 테스트",
				isPublic,
				null,
				null);
		assertEquals(1, collectionMapper.doSave(collection));

		return collection;
	}

	/** DB 고유 제약조건 충돌을 피할 테스트 식별자 생성 */
	private String createToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
