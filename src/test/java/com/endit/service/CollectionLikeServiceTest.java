package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionLikeItemVO;
import com.endit.domain.CollectionLikeVO;
import com.endit.domain.CollectionVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.CollectionMapper;
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

	/** 실제 DB 좋아요 등록 결과 검증 */
	@Test
	@DisplayName("좋아요 등록")
	void create() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(memberId);

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
		CollectionVO collection = createCollection(memberId);
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
		CollectionVO collection = createCollection(memberId);
		collectionLikeService.create(memberId, collection.getCollectionId());

		collectionLikeService.delete(memberId, collection.getCollectionId());

		assertThrows(
				NoSuchElementException.class,
				() -> collectionLikeService.get(memberId, collection.getCollectionId()));
	}

	/** 좋아요가 없는 컬렉션 취소 결과 검증 */
	@Test
	@DisplayName("좋아요가 없는 컬렉션 취소 시 예외 발생")
	void deleteNotFound() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(memberId);

		assertThrows(
				NoSuchElementException.class,
				() -> collectionLikeService.delete(memberId, collection.getCollectionId()));
	}

	/** 실제 DB에 등록한 좋아요의 단건 조회 검증 */
	@Test
	@DisplayName("좋아요 여부 단건 조회")
	void get() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(memberId);
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

	/** 실제 DB 목록 조회와 기본 페이징 및 전체 건수 설정 검증 */
	@Test
	@DisplayName("회원별 좋아요 컬렉션 목록 조회 시 기본 페이징과 전체 건수 설정")
	void retrieveByMember() {
		int memberId = createMemberId();
		CollectionVO first = createCollection(memberId);
		CollectionVO second = createCollection(memberId);
		collectionLikeService.create(memberId, first.getCollectionId());
		collectionLikeService.create(memberId, second.getCollectionId());

		DTO param = new DTO();

		List<CollectionLikeItemVO> result =
				collectionLikeService.retrieveByMember(memberId, param);

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
				collectionLikeService.retrieveByMember(memberId, param);

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
				() -> collectionLikeService.retrieveByMember(memberId, null));
	}

	/** 실제 DB 컬렉션별 좋아요 회원 목록 조회 검증 */
	@Test
	@DisplayName("컬렉션별 좋아요 회원 목록 조회")
	void retrieveByCollection() {
		int memberId = createMemberId();
		CollectionVO collection = createCollection(memberId);
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
		CollectionVO collection = createCollection(memberId);

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
		CollectionVO collection = new CollectionVO(
				0,
				memberId,
				"좋아요 통합 테스트 컬렉션 " + createToken(),
				"컬렉션 좋아요 Service 통합 테스트",
				"Y",
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
