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
import com.endit.domain.CollectionVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.MemberMapper;

/**
 * <pre>
 * Class Name  : CollectionServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 컬렉션 Service를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 21. jinyoung    최초 생성
 * 2026. 8. 26. jinyoung    실제 Spring Bean과 DB 기반 통합 테스트로 변경
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 21.
 */
@SpringBootTest
@Transactional
@DisplayName("CollectionService 통합 테스트")
class CollectionServiceTest {

	private static final int MISSING_COLLECTION_ID = Integer.MAX_VALUE;

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private MemberMapper memberMapper;

	/** 실제 DB 목록 조회와 기본 페이징값 및 전체 건수 설정 검증 */
	@Test
	@DisplayName("목록 조회 시 기본 페이징과 전체 건수 설정")
	void retrieve() {
		CollectionVO saved = collectionService.create(
				createCollection(createMemberId(), "통합 목록 컬렉션", "Y"));

		DTO param = new DTO();
		param.setSearchDiv("10");
		param.setSearchWord(saved.getTitle());

		List<CollectionVO> result = collectionService.retrieve(param);

		assertEquals(1, result.size());
		assertEquals(saved.getCollectionId(), result.get(0).getCollectionId());
		assertEquals(1, param.getPageNo());
		assertEquals(10, param.getPageSize());
		assertEquals(1, param.getTotalCnt());
	}

	/** 검색 결과가 없을 때 실제 DB 조회 결과 검증 */
	@Test
	@DisplayName("조회 결과가 없으면 빈 목록 반환")
	void retrieveEmpty() {
		DTO param = new DTO();
		param.setSearchDiv("10");
		param.setSearchWord("존재하지않는컬렉션-" + UUID.randomUUID());

		List<CollectionVO> result = collectionService.retrieve(param);

		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	/** null 조회 조건에 대한 입력값 검증 */
	@Test
	@DisplayName("조회 조건이 null이면 예외 발생")
	void retrieveNull() {
		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.retrieve(null));
	}

	/** 실제 DB에 등록한 컬렉션의 단건 조회 검증 */
	@Test
	@DisplayName("컬렉션 번호로 단건 조회")
	void get() {
		CollectionVO saved = collectionService.create(
				createCollection(createMemberId(), "통합 단건 컬렉션", "Y"));

		CollectionVO result = collectionService.get(saved.getCollectionId());

		assertEquals(saved.getCollectionId(), result.getCollectionId());
		assertEquals(saved.getTitle(), result.getTitle());
		assertNotNull(result.getCreatedDt());
	}

	/** 존재하지 않는 컬렉션 단건 조회 결과 검증 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 조회 시 예외 발생")
	void getNotFound() {
		assertThrows(
				NoSuchElementException.class,
				() -> collectionService.get(MISSING_COLLECTION_ID));
	}

	/** 공개 여부 기본값과 실제 DB 등록 결과 검증 */
	@Test
	@DisplayName("공개 여부가 없으면 Y로 등록")
	void create() {
		CollectionVO param = createCollection(
				createMemberId(), "통합 등록 컬렉션", null);

		CollectionVO result = collectionService.create(param);

		assertTrue(result.getCollectionId() > 0);
		assertEquals("Y", result.getIsPublic());
		assertEquals("Y", param.getIsPublic());
		assertNotNull(result.getCreatedDt());
	}

	/** 필수 제목이 없는 컬렉션 등록 방지 검증 */
	@Test
	@DisplayName("제목이 없는 컬렉션은 등록하지 않음")
	void createWithoutTitle() {
		CollectionVO param = createCollection(createMemberId(), " ", "Y");

		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(param));
	}

	/** 최대 길이를 초과한 제목의 컬렉션 등록 방지 검증 */
	@Test
	@DisplayName("제목이 100자를 초과한 컬렉션은 등록하지 않음")
	void createWithLongTitle() {
		CollectionVO param = createCollection(
				createMemberId(), "가".repeat(101), "Y");

		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(param));
	}

	/** 최대 길이를 초과한 설명의 컬렉션 등록 방지 검증 */
	@Test
	@DisplayName("설명이 1000자를 초과한 컬렉션은 등록하지 않음")
	void createWithLongDescription() {
		CollectionVO param = createCollection(
				createMemberId(), "통합 컬렉션", "Y");
		param.setDescription("가".repeat(1001));

		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(param));
	}

	/** 수정 시 기존 작성자와 공개 여부 유지 및 DB 반영 검증 */
	@Test
	@DisplayName("기존 작성자와 공개 여부를 유지하여 수정")
	void update() {
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(
				createCollection(memberId, "수정 전 컬렉션", "N"));
		CollectionVO param = createCollection(0, "수정 후 컬렉션", null);

		CollectionVO result = collectionService.update(
				saved.getCollectionId(), param);

		assertEquals(saved.getCollectionId(), result.getCollectionId());
		assertEquals(memberId, result.getMemberId());
		assertEquals("수정 후 컬렉션", result.getTitle());
		assertEquals("N", result.getIsPublic());
		assertNotNull(result.getUpdatedDt());
	}

	/** 실제 DB에서 컬렉션 삭제 결과 검증 */
	@Test
	@DisplayName("컬렉션 삭제")
	void delete() {
		CollectionVO saved = collectionService.create(
				createCollection(createMemberId(), "삭제 컬렉션", "Y"));

		collectionService.delete(saved.getCollectionId());

		assertThrows(
				NoSuchElementException.class,
				() -> collectionService.get(saved.getCollectionId()));
	}

	/** 외래 키를 만족하는 테스트 회원을 현재 트랜잭션에 등록 */
	private int createMemberId() {
		String token = UUID.randomUUID().toString().replace("-", "");
		MemberVO member = new MemberVO();
		member.setEmail("collection-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("컬렉션" + token.substring(0, 8));
		member.setIntroduction("컬렉션 통합 테스트 회원");
		member.setRole("USER");

		assertEquals(1, memberMapper.insertMember(member));
		assertNotNull(member.getMemberId());

		return member.getMemberId().intValue();
	}

	/** 테스트에 사용할 컬렉션 정보 생성 */
	private CollectionVO createCollection(
			int memberId,
			String title,
			String isPublic) {

		return new CollectionVO(
				0,
				memberId,
				title,
				"컬렉션 설명",
				isPublic,
				null,
				null);
	}
}
