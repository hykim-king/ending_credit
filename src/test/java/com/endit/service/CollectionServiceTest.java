package com.endit.service;

import static com.endit.support.CollectionRequestFixtures.createRequest;
import static com.endit.support.CollectionRequestFixtures.updateRequest;
import static com.endit.support.DatabaseTestFixtures.insertContent;
import static com.endit.support.DatabaseTestFixtures.insertMember;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.endit.auth.ForbiddenOperationException;
import com.endit.cmn.DTO;
import com.endit.domain.CollectionCreateRequest;
import com.endit.domain.CollectionItemVO;
import com.endit.domain.CollectionLikeVO;
import com.endit.domain.CollectionUpdateRequest;
import com.endit.domain.CollectionVO;
import com.endit.domain.ContentVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.CollectionItemMapper;
import com.endit.mapper.CollectionLikeMapper;
import com.endit.mapper.CollectionMapper;

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
 * 2026. 8. 29. jinyoung    요청 DTO·공개 여부·전체 공개 목록·U-05·소유권 정책 검증 추가
 * 2026. 8. 31. jinyoung    입력·작품 중복·수정 차이 검증 및 요청 픽스처 공통화
 * 2026. 9. 01. jinyoung    목록 카드 대표 포스터 조회 검증 추가
 * 2026. 9. 02. jinyoung    본인 비공개·좋아요 여부·빈 컬렉션 제외 정책 검증
 * 2026. 9. 03. jinyoung    회원별 컬렉션 및 공개 범위 건수 조회 검증 추가
 * 2026. 9. 05. jinyoung    대상 외 부모 데이터를 운영 시퀀스와 분리
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
	private CollectionMapper collectionMapper;

	@Autowired
	private CollectionItemMapper collectionItemMapper;

	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("목록 조회 시 기본 페이징과 전체 건수 설정")
	void retrieve() {
		// Given: 작품이 포함된 공개 컬렉션과 제목 검색 조건을 준비한다.
		int memberId = createMemberId();
		ContentVO content = createContent("통합 목록");
		CollectionVO saved = collectionService.create(memberId,
				createRequest("통합 목록 컬렉션", "컬렉션 설명", List.of(content.getContentId())));

		DTO param = new DTO();
		param.setSearchDiv("10");
		param.setSearchWord(saved.getTitle());

		// When: 전체 컬렉션 목록을 조회한다.
		List<CollectionVO> result = collectionService.retrieve(param);

		// Then: 대상 컬렉션과 기본 페이징 및 전체 건수가 반환되어야 한다.
		assertEquals(1, result.size());
		assertEquals(saved.getCollectionId(), result.get(0).getCollectionId());
		assertEquals(1, param.getPageNo());
		assertEquals(10, param.getPageSize());
		assertEquals(1, param.getTotalCnt());
	}

	@Test
	@DisplayName("목록 조회 시 내부 작품 대표 포스터 포함")
	void retrieveWithPreviewPoster() {
		// Given: 포스터가 있는 작품을 포함한 컬렉션을 등록한다.
		int memberId = createMemberId();
		ContentVO content = createContent("목록 대표 포스터");
		CollectionVO saved = collectionService.create(memberId,
				createRequest("대표 포스터 컬렉션", "컬렉션 설명", List.of(content.getContentId())));

		DTO param = searchByTitle(saved.getTitle());

		// When: 해당 컬렉션을 목록에서 조회한다.
		List<CollectionVO> result = collectionService.retrieve(param);

		// Then: 첫 번째 작품의 포스터가 대표 포스터로 반환되어야 한다.
		assertEquals(1, result.size());
		assertEquals(content.getPosterUrl(), result.get(0).getPreviewPosterUrl1());
	}

	@Test
	@DisplayName("목록 조회 시 현재 회원 좋아요 여부 포함")
	void retrieveWithCurrentMemberLike() {
		// Given: 현재 회원이 다른 회원의 컬렉션에 좋아요한 상태를 준비한다.
		int ownerId = createMemberId();
		int currentMemberId = createMemberId();
		ContentVO content = createContent("좋아요 상태");
		CollectionVO saved = collectionService.create(ownerId,
				createRequest("좋아요 상태 컬렉션-" + UUID.randomUUID(), "컬렉션 설명", List.of(content.getContentId())));
		CollectionLikeVO like = new CollectionLikeVO(currentMemberId, saved.getCollectionId(), null);
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like));

		DTO param = searchByTitle(saved.getTitle());

		// When: 현재 회원 정보를 포함해 컬렉션 목록을 조회한다.
		List<CollectionVO> result = collectionService.retrieve(param, OptionalLong.of(currentMemberId));

		// Then: 조회 결과에 현재 회원의 좋아요 상태가 포함되어야 한다.
		assertEquals(1, result.size());
		assertTrue(result.get(0).isLikedByCurrentMember());
	}

	@Test
	@DisplayName("조회 결과가 없으면 빈 목록 반환")
	void retrieveEmpty() {
		// Given: 존재하지 않는 제목의 검색 조건을 준비한다.
		DTO param = new DTO();
		param.setSearchDiv("10");
		param.setSearchWord("존재하지않는컬렉션-" + UUID.randomUUID());

		// When: 전체 컬렉션 목록을 조회한다.
		List<CollectionVO> result = collectionService.retrieve(param);

		// Then: 빈 목록을 반환하고 전체 건수는 0이어야 한다.
		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	@Test
	@DisplayName("전체 목록은 작성자 본인의 비공개 컬렉션 포함")
	void retrievePrivateCollection() {
		// Given: 소유자가 작품이 포함된 비공개 컬렉션을 등록한다.
		int ownerId = createMemberId();
		String title = "비공개목록-" + UUID.randomUUID();
		ContentVO content = createContent("비공개 목록");
		CollectionVO privateCollection = collectionService.create(ownerId,
				createRequest(title, "컬렉션 설명", "N", List.of(content.getContentId())));

		DTO param = searchByTitle(title);

		// When: 소유자 관점에서 전체 컬렉션 목록을 조회한다.
		List<CollectionVO> result = collectionService.retrieve(param, OptionalLong.of(ownerId));

		// Then: 소유자의 비공개 컬렉션이 목록에 포함되어야 한다.
		assertEquals(1, result.size());
		assertEquals(privateCollection.getCollectionId(), result.get(0).getCollectionId());
		assertEquals(1, param.getTotalCnt());
	}

	@Test
	@DisplayName("빈 컬렉션은 전체 목록에서 제외하고 회원별 목록에는 포함")
	void retrieveEmptyCollectionVisibility() {
		// Given: 작품이 없는 빈 컬렉션을 등록한다.
		int ownerId = createMemberId();
		CollectionVO emptyCollection = collectionService.create(ownerId,
				createRequest("빈 컬렉션-" + UUID.randomUUID(), "컬렉션 설명", List.of()));

		// When: 전체 컬렉션 목록에서 빈 컬렉션을 조회한다.
		DTO publicParam = searchByTitle(emptyCollection.getTitle());
		List<CollectionVO> publicResult = collectionService.retrieve(publicParam, OptionalLong.of(ownerId));

		// Then: 작품이 없는 컬렉션은 전체 목록에서 제외되어야 한다.
		assertTrue(publicResult.isEmpty());
		assertEquals(0, publicParam.getTotalCnt());

		// When: 소유자의 회원별 컬렉션 목록을 조회한다.
		DTO memberParam = searchByTitle(emptyCollection.getTitle());
		List<CollectionVO> memberResult = collectionService.retrieveByMember(ownerId, memberParam,
				OptionalLong.of(ownerId));

		// Then: 빈 컬렉션도 회원별 목록에는 포함되어야 한다.
		assertEquals(1, memberResult.size());
		assertEquals(emptyCollection.getCollectionId(), memberResult.get(0).getCollectionId());
		assertEquals(1, memberParam.getTotalCnt());
	}

	@Test
	@DisplayName("본인 U-05는 공개와 비공개 컬렉션 모두 조회")
	void retrieveByMemberAsOwner() {
		// Given: 한 회원이 같은 검색어를 가진 공개·비공개 컬렉션을 등록한다.
		int ownerId = createMemberId();
		String title = "본인U05-" + UUID.randomUUID();
		saveCollection(ownerId, title, "Y");
		saveCollection(ownerId, title, "N");

		DTO param = searchByTitle(title);

		// When: 소유자 본인이 회원별 컬렉션 목록을 조회한다.
		List<CollectionVO> result = collectionService.retrieveByMember(ownerId, param, OptionalLong.of(ownerId));

		// Then: 공개·비공개 컬렉션이 모두 반환되어야 한다.
		assertEquals(2, result.size());
		assertEquals(2, param.getTotalCnt());
		assertTrue(result.stream().anyMatch(collection -> "Y".equals(collection.getIsPublic())));
		assertTrue(result.stream().anyMatch(collection -> "N".equals(collection.getIsPublic())));
	}

	@Test
	@DisplayName("타인 U-05는 공개 컬렉션만 조회")
	void retrieveByMemberAsNonOwner() {
		// Given: 대상 회원에게 같은 검색어를 가진 공개·비공개 컬렉션을 등록한다.
		int ownerId = createMemberId();
		int otherMemberId = createMemberId();
		String title = "타인U05-" + UUID.randomUUID();
		CollectionVO publicCollection = saveCollection(ownerId, title, "Y");
		saveCollection(ownerId, title, "N");

		DTO param = searchByTitle(title);

		// When: 다른 회원이 대상 회원의 컬렉션 목록을 조회한다.
		List<CollectionVO> result = collectionService.retrieveByMember(ownerId, param, OptionalLong.of(otherMemberId));

		// Then: 공개 컬렉션만 반환되어야 한다.
		assertEquals(1, result.size());
		assertEquals(publicCollection.getCollectionId(), result.get(0).getCollectionId());
		assertEquals(1, param.getTotalCnt());
	}

	@Test
	@DisplayName("본인 프로필 컬렉션 건수는 공개와 비공개 전체 포함")
	void countByMember() {
		// Given: 대상 회원에게 공개·비공개 컬렉션을 하나씩 등록한다.
		int targetMemberId = createMemberId();
		String title = "본인집계-" + UUID.randomUUID();

		saveCollection(targetMemberId, title + "-공개", "Y");
		saveCollection(targetMemberId, title + "-비공개", "N");

		// 다른 회원의 컬렉션은 대상 회원의 집계에 포함되지 않아야 한다.
		int otherMemberId = createMemberId();
		saveCollection(otherMemberId, title + "-다른회원", "Y");

		// When: 대상 회원이 작성한 전체 컬렉션 건수를 조회한다.
		int result = collectionService.countByMember(targetMemberId);

		// Then: 대상 회원의 공개·비공개 컬렉션 두 건만 집계되어야 한다.
		assertEquals(2, result);
	}

	@Test
	@DisplayName("프로필 조회자의 접근 범위에 따른 컬렉션 건수 조회")
	void countVisibleByMember() {
		// Given: 대상 회원에게 공개·비공개 컬렉션을 하나씩 등록한다.
		int targetMemberId = createMemberId();
		int otherMemberId = createMemberId();
		String title = "공개범위집계-" + UUID.randomUUID();

		saveCollection(targetMemberId, title + "-공개", "Y");
		saveCollection(targetMemberId, title + "-비공개", "N");

		// When, Then: 다른 로그인 회원은 공개 컬렉션만 볼 수 있어야 한다.
		assertEquals(1, collectionService.countVisibleByMember(targetMemberId, Long.valueOf(otherMemberId)));

		// When, Then: 비회원도 공개 컬렉션만 볼 수 있어야 한다.
		assertEquals(1, collectionService.countVisibleByMember(targetMemberId, null));

		// When, Then: 대상 회원 본인은 공개·비공개 컬렉션을 모두 볼 수 있어야 한다.
		assertEquals(2, collectionService.countVisibleByMember(targetMemberId, Long.valueOf(targetMemberId)));
	}

	@Test
	@DisplayName("회원별 컬렉션 건수 조회 시 회원 번호 검증")
	void validateCountMemberId() {
		// Given: 유효한 대상 회원을 준비한다.
		int targetMemberId = createMemberId();

		// Then: 대상 회원 번호가 유효하지 않으면 Mapper 호출 전에 거부해야 한다.
		assertThrows(IllegalArgumentException.class, () -> collectionService.countByMember(0));

		assertThrows(IllegalArgumentException.class, () -> collectionService.countVisibleByMember(-1, null));

		// 현재 조회 회원은 null이면 비회원으로 허용하지만, 0 이하는 허용하지 않는다.
		assertThrows(IllegalArgumentException.class, () -> collectionService.countVisibleByMember(targetMemberId, 0L));
	}

	@Test
	@DisplayName("조회 조건이 null이면 예외 발생")
	void retrieveNull() {
		// When, Then: 조회 조건이 null이면 예외가 발생해야 한다.
		assertThrows(IllegalArgumentException.class, () -> collectionService.retrieve(null));
	}

	@Test
	@DisplayName("컬렉션 번호로 단건 조회")
	void get() {
		// Given: 회원이 컬렉션을 하나 등록한다.
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId, createRequest("통합 단건 컬렉션", "컬렉션 설명", List.of()));

		// When: 소유자 관점에서 컬렉션을 단건 조회한다.
		CollectionVO result = collectionService.get(saved.getCollectionId(), OptionalLong.of(memberId));

		// Then: 등록한 컬렉션 정보와 생성 일시가 반환되어야 한다.
		assertEquals(saved.getCollectionId(), result.getCollectionId());
		assertEquals(saved.getTitle(), result.getTitle());
		assertNotNull(result.getCreatedDt());
	}

	@Test
	@DisplayName("존재하지 않는 컬렉션 조회 시 예외 발생")
	void getNotFound() {
		// When, Then: 존재하지 않는 컬렉션 번호로 조회하면 예외가 발생해야 한다.
		assertThrows(NoSuchElementException.class,
				() -> collectionService.get(MISSING_COLLECTION_ID, OptionalLong.empty()));
	}

	@Test
	@DisplayName("비공개 컬렉션 비소유자 조회는 404 예외")
	void getPrivateCollectionByNonOwner() {
		// Given: 소유자와 다른 회원 및 비공개 컬렉션을 준비한다.
		int ownerId = createMemberId();
		int otherMemberId = createMemberId();
		CollectionVO saved = saveCollection(ownerId, "비공개 단건 컬렉션", "N");

		// When, Then: 비소유자가 조회하면 존재하지 않는 컬렉션처럼 처리되어야 한다.
		assertThrows(NoSuchElementException.class,
				() -> collectionService.get(saved.getCollectionId(), OptionalLong.of(otherMemberId)));
	}

	@Test
	@DisplayName("공개 컬렉션 비소유자 변경은 403 예외")
	void getOwnedByNonOwner() {
		// Given: 소유자와 다른 회원 및 공개 컬렉션을 준비한다.
		int ownerId = createMemberId();
		int otherMemberId = createMemberId();
		CollectionVO saved = saveCollection(ownerId, "비소유자 변경 컬렉션", "Y");

		// When, Then: 비소유자가 변경용 조회를 요청하면 권한 예외가 발생해야 한다.
		assertThrows(ForbiddenOperationException.class,
				() -> collectionService.getOwned(saved.getCollectionId(), otherMemberId));
	}

	@Test
	@DisplayName("공개 여부를 생략한 컬렉션은 공개 상태 Y로 등록")
	void create() {
		// Given: 앞뒤 공백이 있는 제목과 빈 설명, 공개 여부가 없는 요청을 준비한다.
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest("  통합 등록 컬렉션  ", "   ", null);

		// When: 컬렉션을 등록한다.
		CollectionVO result = collectionService.create(memberId, request);

		// Then: 입력은 정규화되고 공개 여부는 기본값 Y로 저장되어야 한다.
		assertTrue(result.getCollectionId() > 0);
		assertEquals("Y", result.getIsPublic());
		assertEquals("통합 등록 컬렉션", result.getTitle());
		assertNull(result.getDescription());
		assertEquals(memberId, result.getMemberId());
		assertNotNull(result.getCreatedDt());
	}

	@Test
	@DisplayName("컬렉션은 비공개 상태 N으로 등록")
	void createPrivateCollection() {
		// Given: 공개 여부가 N인 컬렉션 등록 요청을 준비한다.
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest("통합 비공개 컬렉션", "컬렉션 설명", "N", List.of());

		// When: 컬렉션을 등록한다.
		CollectionVO result = collectionService.create(memberId, request);

		// Then: 비공개 상태 N으로 저장되어야 한다.
		assertEquals("N", result.getIsPublic());
	}

	@Test
	@DisplayName("여러 작품과 중복 contentId는 중복 없이 등록")
	void createWithDistinctContentIds() {
		// Given: 서로 다른 두 작품과 중복된 콘텐츠 번호가 포함된 요청을 준비한다.
		int memberId = createMemberId();
		ContentVO first = createContent("첫 번째");
		ContentVO second = createContent("두 번째");
		CollectionCreateRequest request = createRequest("작품 distinct 컬렉션", "컬렉션 설명",
				List.of(first.getContentId(), second.getContentId(), first.getContentId()));

		// When: 컬렉션을 등록하고 저장된 콘텐츠 번호를 조회한다.
		CollectionVO result = collectionService.create(memberId, request);
		List<Integer> contentIds = collectionItemMapper.selectContentIdsByCollectionId(result.getCollectionId());

		// Then: 중복 콘텐츠 번호는 제거되고 두 작품만 저장되어야 한다.
		assertEquals(2, contentIds.size());
		assertTrue(contentIds.contains(first.getContentId()));
		assertTrue(contentIds.contains(second.getContentId()));
	}

	@Test
	@DisplayName("존재하지 않는 contentId로 컬렉션을 등록하지 않음")
	void createWithMissingContentId() {
		// Given: 존재하지 않는 콘텐츠 번호가 포함된 요청을 준비한다.
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest("존재하지 않는 작품 컬렉션", "컬렉션 설명", List.of(Integer.MAX_VALUE));

		// When, Then: 컬렉션 등록 시 데이터 무결성 예외가 발생해야 한다.
		assertThrows(DataIntegrityViolationException.class, () -> collectionService.create(memberId, request));
	}

	@Test
	@DisplayName("잘못된 공개 여부로 컬렉션을 등록하지 않음")
	void createWithInvalidPublicState() {
		// Given: 허용되지 않은 공개 여부가 포함된 요청을 준비한다.
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest("통합 컬렉션", "컬렉션 설명", "INVALID", List.of());

		// When, Then: 컬렉션 등록 시 입력값 예외가 발생해야 한다.
		assertThrows(IllegalArgumentException.class, () -> collectionService.create(memberId, request));
	}

	@Test
	@DisplayName("제목이 없는 컬렉션은 등록하지 않음")
	void createWithoutTitle() {
		// Given: 공백만 있는 제목이 포함된 요청을 준비한다.
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest(" ", "컬렉션 설명", List.of());

		// When, Then: 컬렉션 등록 시 입력값 예외가 발생해야 한다.
		assertThrows(IllegalArgumentException.class, () -> collectionService.create(memberId, request));
	}

	@Test
	@DisplayName("제목이 100자를 초과한 컬렉션은 등록하지 않음")
	void createWithLongTitle() {
		// Given: 100자를 초과한 제목이 포함된 요청을 준비한다.
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest("가".repeat(101), "컬렉션 설명", List.of());

		// When, Then: 컬렉션 등록 시 입력값 예외가 발생해야 한다.
		assertThrows(IllegalArgumentException.class, () -> collectionService.create(memberId, request));
	}

	@Test
	@DisplayName("설명이 1000자를 초과한 컬렉션은 등록하지 않음")
	void createWithLongDescription() {
		// Given: 1000자를 초과한 설명이 포함된 요청을 준비한다.
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest("통합 컬렉션", "가".repeat(1001), List.of());

		// When, Then: 컬렉션 등록 시 입력값 예외가 발생해야 한다.
		assertThrows(IllegalArgumentException.class, () -> collectionService.create(memberId, request));
	}

	@Test
	@DisplayName("기존 작성자를 유지하고 공개 컬렉션을 비공개로 수정")
	void update() {
		// Given: 공개 컬렉션과 제목·설명·공개 여부 수정 요청을 준비한다.
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId, createRequest("수정 전 컬렉션", "컬렉션 설명", List.of()));
		CollectionUpdateRequest request = updateRequest("수정 후 컬렉션", "수정 후 설명", "N", List.of());

		// When: 소유자가 컬렉션을 수정한다.
		CollectionVO result = collectionService.update(memberId, saved.getCollectionId(), request);

		// Then: 작성자는 유지되고 요청한 내용과 수정 일시가 반영되어야 한다.
		assertEquals(saved.getCollectionId(), result.getCollectionId());
		assertEquals(memberId, result.getMemberId());
		assertEquals("수정 후 컬렉션", result.getTitle());
		assertEquals("N", result.getIsPublic());
		assertNotNull(result.getUpdatedDt());
	}

	@Test
	@DisplayName("공개 여부를 생략한 수정은 기존 상태 유지")
	void updateWithoutPublicState() {
		// Given: 비공개 컬렉션과 공개 여부가 없는 수정 요청을 준비한다.
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId,
				createRequest("비공개 수정 전 컬렉션", "컬렉션 설명", "N", List.of()));
		CollectionUpdateRequest request = updateRequest("비공개 수정 후 컬렉션", "수정 후 설명", List.of());

		// When: 소유자가 컬렉션을 수정한다.
		CollectionVO result = collectionService.update(memberId, saved.getCollectionId(), request);

		// Then: 기존 비공개 상태 N이 유지되어야 한다.
		assertEquals("N", result.getIsPublic());
	}

	@Test
	@DisplayName("비공개 컬렉션을 공개 상태로 수정")
	void updatePrivateToPublic() {
		// Given: 비공개 컬렉션과 공개 상태 Y로 변경하는 요청을 준비한다.
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId, createRequest("재공개 전 컬렉션", "컬렉션 설명", "N", List.of()));
		CollectionUpdateRequest request = updateRequest("재공개 후 컬렉션", "수정 후 설명", "Y", List.of());

		// When: 소유자가 컬렉션을 수정한다.
		CollectionVO result = collectionService.update(memberId, saved.getCollectionId(), request);

		// Then: 공개 상태 Y로 변경되어야 한다.
		assertEquals("Y", result.getIsPublic());
	}

	@Test
	@DisplayName("수정 시 contentIds가 없으면 작품을 모두 제거")
	void updateWithoutContentIds() {
		// Given: 작품이 포함된 컬렉션과 contentIds가 null인 수정 요청을 준비한다.
		int memberId = createMemberId();
		ContentVO content = createContent("전체 제거");
		CollectionVO saved = collectionService.create(memberId,
				createRequest("작품 목록 수정 전", "컬렉션 설명", List.of(content.getContentId())));
		CollectionUpdateRequest request = updateRequest("작품 목록 수정 후", "컬렉션 설명", null);

		// When: 소유자가 컬렉션을 수정한다.
		collectionService.update(memberId, saved.getCollectionId(), request);

		// Then: 기존 컬렉션 작품이 모두 제거되어야 한다.
		assertTrue(collectionItemMapper.selectContentIdsByCollectionId(saved.getCollectionId()).isEmpty());
	}

	@Test
	@DisplayName("수정 시 작품 diff와 유지 작품 추가 일시 보존")
	void updateContentDiff() {
		// Given: 유지·제거할 작품이 있는 컬렉션과 새로 추가할 작품을 준비한다.
		int memberId = createMemberId();
		ContentVO retained = createContent("유지");
		ContentVO removed = createContent("제거");
		ContentVO added = createContent("추가");
		CollectionVO saved = collectionService.create(memberId,
				createRequest("작품 diff 수정 전", "컬렉션 설명", List.of(retained.getContentId(), removed.getContentId())));
		CollectionItemVO retainedKey = createItemKey(saved.getCollectionId(), retained.getContentId());
		String retainedAddedDt = collectionItemMapper.doSelectOne(retainedKey).getAddedDt();

		// When: 유지 작품과 추가 작품만 포함하도록 컬렉션을 수정한다.
		collectionService.update(memberId, saved.getCollectionId(),
				updateRequest("작품 diff 수정 후", "수정 후 설명", List.of(retained.getContentId(), added.getContentId())));

		// Then: 유지 작품 일시는 보존되고 제거·추가 작품의 diff가 반영되어야 한다.
		assertEquals(retainedAddedDt, collectionItemMapper.doSelectOne(retainedKey).getAddedDt());
		assertNull(collectionItemMapper.doSelectOne(createItemKey(saved.getCollectionId(), removed.getContentId())));
		assertNotNull(collectionItemMapper.doSelectOne(createItemKey(saved.getCollectionId(), added.getContentId())));
	}

	@Test
	@DisplayName("컬렉션 삭제")
	void delete() {
		// Given: 삭제할 컬렉션을 등록한다.
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId, createRequest("삭제 컬렉션", "컬렉션 설명", List.of()));

		// When: 소유자가 컬렉션을 삭제한다.
		collectionService.delete(memberId, saved.getCollectionId());

		// Then: 삭제한 컬렉션을 다시 조회하면 예외가 발생해야 한다.
		assertThrows(NoSuchElementException.class,
				() -> collectionService.get(saved.getCollectionId(), OptionalLong.of(memberId)));
	}

	/**
	 * 외래 키를 만족하는 테스트 회원을 현재 트랜잭션에 등록
	 *
	 * @return 등록된 테스트 회원 번호
	 */
	private int createMemberId() {
		String token = UUID.randomUUID().toString().replace("-", "");
		MemberVO member = new MemberVO();

		member.setEmail("collection-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("컬렉션" + token.substring(0, 8));
		member.setIntroduction("컬렉션 통합 테스트 회원");
		member.setRole("USER");

		return insertMember(jdbcTemplate, member).getMemberId().intValue();
	}

	/**
	 * 작품 스냅샷 테스트에 사용할 콘텐츠 등록
	 *
	 * @param titleSuffix 테스트 작품 제목의 접미사
	 * @return 콘텐츠 정보
	 */
	private ContentVO createContent(String titleSuffix) {
		String token = UUID.randomUUID().toString().replace("-", "");
		ContentVO content = new ContentVO(0, "COLLECTION_" + token, "컬렉션 작품 " + titleSuffix,
				"Collection Content " + titleSuffix, "컬렉션 작품 스냅샷 테스트", "2026-08-31", 120, "KR",
				"https://example.com/poster.jpg", "https://example.com/backdrop.jpg", null);
		// CONTENT 생성 자체는 테스트 대상이 아니므로 CONTENT 시퀀스 상태에 의존하지 않는다.
		return insertContent(jdbcTemplate, content);
	}

	/**
	 * 컬렉션 작품 복합 키 생성
	 *
	 * @param collectionId 컬렉션 번호
	 * @param contentId    콘텐츠 번호
	 * @return 컬렉션과 콘텐츠의 복합키
	 */
	private CollectionItemVO createItemKey(int collectionId, int contentId) {
		return new CollectionItemVO(collectionId, contentId, null);
	}

	/**
	 * Mapper로 공개 여부를 지정해 접근 정책용 컬렉션 등록
	 *
	 * @param memberId 회원 번호
	 * @param title    컬렉션 제목
	 * @param isPublic 공개 여부 (Y/N)
	 * @return 컬렉션 정보
	 */
	private CollectionVO saveCollection(int memberId, String title, String isPublic) {
		CollectionVO collection = new CollectionVO(0, memberId, title, "접근 정책 통합 테스트", isPublic, null, null);
		assertEquals(1, collectionMapper.doSave(collection));

		return collection;
	}

	/**
	 * 제목 검색 조건 생성
	 *
	 * @param title 컬렉션 제목
	 * @return 제목 검색 조건
	 */
	private DTO searchByTitle(String title) {
		DTO param = new DTO();
		param.setSearchDiv("10");
		param.setSearchWord(title);

		return param;
	}

}
