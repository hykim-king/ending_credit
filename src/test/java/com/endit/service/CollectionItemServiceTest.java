package com.endit.service;

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
import org.springframework.transaction.annotation.Transactional;

import com.endit.auth.ForbiddenOperationException;
import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;
import com.endit.domain.CollectionVO;
import com.endit.domain.ContentVO;
import com.endit.domain.MemberContentVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.CollectionMapper;
import com.endit.mapper.ContentMapper;
import com.endit.mapper.MemberContentMapper;
import com.endit.mapper.MemberMapper;

/**
 * <pre>
 * Class Name  : CollectionItemServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 컬렉션 작품 Service를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * 2026. 8. 26. jinyoung    실제 Spring Bean과 DB 기반 통합 테스트로 변경
 * 2026. 8. 29. jinyoung    인증 회원 및 컬렉션 작품 소유권 검증 추가
 * 2026. 8. 31. jinyoung    컬렉션 작품 평균 별점 조회 검증 추가
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
	private ContentMapper contentMapper;

	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private MemberContentMapper memberContentMapper;

	/** 실제 DB 목록 조회와 컬렉션 조건 및 기본 페이징값 검증 */
	@Test
	@DisplayName("컬렉션 작품 목록 조회 시 컬렉션 조건과 페이징 설정")
	void retrieve() {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(),
				createItem(content.getContentId()));
		saveRating(collection.getMemberId(), content.getContentId(), 3);
		saveRating(createMemberId(), content.getContentId(), 5);
		DTO param = new DTO();

		List<CollectionItemVO> result = collectionItemService.retrieve(
				collection.getCollectionId(), param, viewer(collection));

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

	/** 작품이 없는 컬렉션의 실제 DB 목록 조회 결과 검증 */
	@Test
	@DisplayName("컬렉션 작품이 없으면 빈 목록 반환")
	void retrieveEmpty() {
		CollectionVO collection = createCollection();
		DTO param = new DTO();

		List<CollectionItemVO> result = collectionItemService.retrieve(
				collection.getCollectionId(), param, viewer(collection));

		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	/** 실제 DB에 추가한 컬렉션 작품 단건 조회 검증 */
	@Test
	@DisplayName("컬렉션 작품 단건 조회")
	void get() {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(),
				createItem(content.getContentId()));

		CollectionItemVO result = collectionItemService.get(
				collection.getCollectionId(), content.getContentId(),
				viewer(collection));

		assertEquals(collection.getCollectionId(), result.getCollectionId());
		assertEquals(content.getContentId(), result.getContentId());
		assertNotNull(result.getAddedDt());
	}

	/** 존재하지 않는 컬렉션 작품 조회 결과 검증 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 작품 조회 시 예외 발생")
	void getNotFound() {
		CollectionVO collection = createCollection();

		assertThrows(
				NoSuchElementException.class,
				() -> collectionItemService.get(
						collection.getCollectionId(), MISSING_CONTENT_ID,
						viewer(collection)));
	}

	/** 실제 DB 중복 확인과 컬렉션 작품 추가 결과 검증 */
	@Test
	@DisplayName("중복 확인 후 컬렉션 작품 추가")
	void create() {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();

		CollectionItemVO result = collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(),
				createItem(content.getContentId()));

		assertEquals(collection.getCollectionId(), result.getCollectionId());
		assertEquals(content.getContentId(), result.getContentId());
		assertNotNull(result.getAddedDt());
	}

	/** 실제 DB에 이미 포함된 작품의 중복 추가 방지 검증 */
	@Test
	@DisplayName("이미 포함된 작품은 추가하지 않음")
	void createDuplicate() {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(),
				createItem(content.getContentId()));

		assertThrows(
				IllegalStateException.class,
				() -> collectionItemService.create(
						collection.getMemberId(), collection.getCollectionId(),
						createItem(content.getContentId())));
	}

	/** 비소유자는 공개 컬렉션에도 작품을 추가할 수 없음을 검증 */
	@Test
	@DisplayName("비소유자의 컬렉션 작품 추가는 403 예외")
	void createByNonOwner() {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		int otherMemberId = createMemberId();

		assertThrows(
				ForbiddenOperationException.class,
				() -> collectionItemService.create(
						otherMemberId,
						collection.getCollectionId(),
						createItem(content.getContentId())));
	}

	/** 실제 DB에서 컬렉션 작품 삭제 결과 검증 */
	@Test
	@DisplayName("컬렉션 작품 삭제")
	void delete() {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		collectionItemService.create(
				collection.getMemberId(), collection.getCollectionId(),
				createItem(content.getContentId()));

		collectionItemService.delete(
				collection.getMemberId(), collection.getCollectionId(),
				content.getContentId());

		assertThrows(
				NoSuchElementException.class,
				() -> collectionItemService.get(
						collection.getCollectionId(), content.getContentId(),
						viewer(collection)));
	}

	/** 잘못된 컬렉션 번호에 대한 입력값 검증 */
	@Test
	@DisplayName("잘못된 컬렉션 번호이면 예외 발생")
	void invalidCollectionId() {
		assertThrows(
				IllegalArgumentException.class,
				() -> collectionItemService.retrieve(
						0, new DTO(), OptionalLong.empty()));
	}

	/** 외래 키를 만족하는 회원과 컬렉션을 현재 트랜잭션에 등록 */
	private CollectionVO createCollection() {
		int memberId = createMemberId();

		CollectionVO collection = new CollectionVO(
				0,
				memberId,
				"작품 통합 테스트 컬렉션",
				"컬렉션 작품 Service 통합 테스트",
				"Y",
				null,
				null);
		assertEquals(1, collectionMapper.doSave(collection));

		return collection;
	}

	/** 외래 키를 만족하는 테스트 회원을 현재 트랜잭션에 등록 */
	private int createMemberId() {
		String token = createToken();
		MemberVO member = new MemberVO();
		member.setEmail("item-service-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("작품서비스" + token.substring(0, 8));
		member.setIntroduction("컬렉션 작품 통합 테스트 회원");
		member.setRole("USER");
		assertEquals(1, memberMapper.insertMember(member));
		return member.getMemberId().intValue();
	}

	/** 외래 키를 만족하는 콘텐츠를 현재 트랜잭션에 등록 */
	private ContentVO createContent() {
		String token = createToken();
		ContentVO content = new ContentVO(
				0,
				"INTEGRATION_" + token,
				"통합 테스트 콘텐츠",
				"Integration Test Content",
				"컬렉션 작품 Service 통합 테스트 콘텐츠",
				"2026-08-26",
				120,
				"Korea",
				"https://example.com/poster.jpg",
				"https://example.com/backdrop.jpg",
				null);
		assertEquals(1, contentMapper.doSave(content));

		return content;
	}

	/** 컬렉션 작품 등록 요청 생성 */
	private CollectionItemVO createItem(int contentId) {
		return new CollectionItemVO(0, contentId, null);
	}

	/** 콘텐츠 평균 별점 검증에 사용할 회원 평가 등록 */
	private void saveRating(int memberId, int contentId, int ratingScore) {
		MemberContentVO rating = new MemberContentVO(
				memberId,
				contentId,
				ratingScore,
				"N",
				null,
				null,
				null);
		assertEquals(1, memberContentMapper.doSave(rating));
	}

	/** 테스트 컬렉션 소유자를 현재 조회 회원으로 사용 */
	private OptionalLong viewer(CollectionVO collection) {
		return OptionalLong.of(collection.getMemberId());
	}

	/** DB 고유 제약조건 충돌을 피할 테스트 식별자 생성 */
	private String createToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
