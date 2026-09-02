package com.endit.service;

import static com.endit.support.CollectionRequestFixtures.createRequest;
import static com.endit.support.CollectionRequestFixtures.updateRequest;
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
import com.endit.mapper.ContentMapper;
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
 * 2026. 8. 29. jinyoung    요청 DTO·공개 여부·전체 공개 목록·U-05·소유권 정책 검증 추가
 * 2026. 8. 31. jinyoung    입력 정규화·작품 distinct·수정 diff 정책 검증 추가
 * 2026. 8. 31. jinyoung    요청 DTO 생성 코드를 공통 테스트 픽스처로 분리
 * 2026. 9. 01. jinyoung    목록 카드 대표 포스터 조회 검증 추가
 * 2026. 9. 02. jinyoung    현재 회원 소유 비공개 컬렉션 목록 조회 검증
 * 2026. 9. 02. jinyoung    현재 회원의 목록 좋아요 여부 검증
 * 2026. 9. 02. jinyoung    전체 목록의 빈 컬렉션 제외 정책 검증
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

	@Autowired
	private CollectionMapper collectionMapper;

	@Autowired
	private CollectionItemMapper collectionItemMapper;

	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	@Autowired
	private ContentMapper contentMapper;

	/** 실제 DB 목록 조회와 기본 페이징값 및 전체 건수 설정 검증 */
	@Test
	@DisplayName("목록 조회 시 기본 페이징과 전체 건수 설정")
	void retrieve() {
		int memberId = createMemberId();
		ContentVO content = createContent("통합 목록");
		CollectionVO saved = collectionService.create(memberId,
				createRequest(
						"통합 목록 컬렉션",
						"컬렉션 설명",
						List.of(content.getContentId())));

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

	/** 컬렉션 목록에 내부 작품의 대표 포스터가 함께 조회되는지 검증 */
	@Test
	@DisplayName("목록 조회 시 내부 작품 대표 포스터 포함")
	void retrieveWithPreviewPoster() {
		int memberId = createMemberId();
		ContentVO content = createContent("목록 대표 포스터");
		CollectionVO saved = collectionService.create(
				memberId,
				createRequest(
						"대표 포스터 컬렉션",
						"컬렉션 설명",
						List.of(content.getContentId())));

		DTO param = searchByTitle(saved.getTitle());
		List<CollectionVO> result = collectionService.retrieve(param);

		assertEquals(1, result.size());
		assertEquals(content.getPosterUrl(), result.get(0).getPreviewPosterUrl1());
	}

	/** 목록 조회에 현재 회원의 컬렉션 좋아요 여부가 포함되는지 검증 */
	@Test
	@DisplayName("목록 조회 시 현재 회원 좋아요 여부 포함")
	void retrieveWithCurrentMemberLike() {
		int ownerId = createMemberId();
		int currentMemberId = createMemberId();
		ContentVO content = createContent("좋아요 상태");
		CollectionVO saved = collectionService.create(
				ownerId,
				createRequest(
						"좋아요 상태 컬렉션-" + UUID.randomUUID(),
						"컬렉션 설명",
						List.of(content.getContentId())));
		CollectionLikeVO like = new CollectionLikeVO(
				currentMemberId, saved.getCollectionId(), null);
		assertEquals(1, collectionLikeMapper.insertCollectionLike(like));

		DTO param = searchByTitle(saved.getTitle());
		List<CollectionVO> result = collectionService.retrieve(
				param, OptionalLong.of(currentMemberId));

		assertEquals(1, result.size());
		assertTrue(result.get(0).isLikedByCurrentMember());
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

	/** 전체 목록은 로그인한 작성자의 비공개 컬렉션을 포함하는지 검증 */
	@Test
	@DisplayName("전체 목록은 작성자 본인의 비공개 컬렉션 포함")
	void retrievePrivateCollection() {
		int ownerId = createMemberId();
		String title = "비공개목록-" + UUID.randomUUID();
		ContentVO content = createContent("비공개 목록");
		CollectionVO privateCollection = collectionService.create(
				ownerId,
				createRequest(
						title,
						"컬렉션 설명",
						"N",
						List.of(content.getContentId())));

		DTO param = searchByTitle(title);
		List<CollectionVO> result = collectionService.retrieve(
				param, OptionalLong.of(ownerId));

		assertEquals(1, result.size());
		assertEquals(privateCollection.getCollectionId(),
				result.get(0).getCollectionId());
		assertEquals(1, param.getTotalCnt());
	}

	/** 빈 컬렉션은 전체 목록에서 제외하고 회원별 목록에는 유지하는지 검증 */
	@Test
	@DisplayName("빈 컬렉션은 전체 목록에서 제외하고 회원별 목록에는 포함")
	void retrieveEmptyCollectionVisibility() {
		int ownerId = createMemberId();
		CollectionVO emptyCollection = collectionService.create(
				ownerId,
				createRequest(
						"빈 컬렉션-" + UUID.randomUUID(),
						"컬렉션 설명",
						List.of()));

		DTO publicParam = searchByTitle(emptyCollection.getTitle());
		List<CollectionVO> publicResult = collectionService.retrieve(
				publicParam, OptionalLong.of(ownerId));

		assertTrue(publicResult.isEmpty());
		assertEquals(0, publicParam.getTotalCnt());

		DTO memberParam = searchByTitle(emptyCollection.getTitle());
		List<CollectionVO> memberResult = collectionService.retrieveByMember(
				ownerId, memberParam, OptionalLong.of(ownerId));

		assertEquals(1, memberResult.size());
		assertEquals(emptyCollection.getCollectionId(),
				memberResult.get(0).getCollectionId());
		assertEquals(1, memberParam.getTotalCnt());
	}

	/** U-05를 작성자 본인이 조회하면 공개와 비공개를 모두 반환하는지 검증 */
	@Test
	@DisplayName("본인 U-05는 공개와 비공개 컬렉션 모두 조회")
	void retrieveByMemberAsOwner() {
		int ownerId = createMemberId();
		String title = "본인U05-" + UUID.randomUUID();
		saveCollection(ownerId, title, "Y");
		saveCollection(ownerId, title, "N");

		DTO param = searchByTitle(title);
		List<CollectionVO> result = collectionService.retrieveByMember(
				ownerId, param, OptionalLong.of(ownerId));

		assertEquals(2, result.size());
		assertEquals(2, param.getTotalCnt());
		assertTrue(result.stream().anyMatch(
				collection -> "Y".equals(collection.getIsPublic())));
		assertTrue(result.stream().anyMatch(
				collection -> "N".equals(collection.getIsPublic())));
	}

	/** U-05를 타인이 조회하면 대상 회원의 공개 컬렉션만 반환하는지 검증 */
	@Test
	@DisplayName("타인 U-05는 공개 컬렉션만 조회")
	void retrieveByMemberAsNonOwner() {
		int ownerId = createMemberId();
		int otherMemberId = createMemberId();
		String title = "타인U05-" + UUID.randomUUID();
		CollectionVO publicCollection = saveCollection(ownerId, title, "Y");
		saveCollection(ownerId, title, "N");

		DTO param = searchByTitle(title);
		List<CollectionVO> result = collectionService.retrieveByMember(
				ownerId, param, OptionalLong.of(otherMemberId));

		assertEquals(1, result.size());
		assertEquals(publicCollection.getCollectionId(),
				result.get(0).getCollectionId());
		assertEquals(1, param.getTotalCnt());
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
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId,
				createRequest("통합 단건 컬렉션", "컬렉션 설명", List.of()));

		CollectionVO result = collectionService.get(
				saved.getCollectionId(), OptionalLong.of(memberId));

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
				() -> collectionService.get(
						MISSING_COLLECTION_ID, OptionalLong.empty()));
	}

	/** 비공개 컬렉션을 비소유자가 조회하면 미조회와 같게 처리하는지 검증 */
	@Test
	@DisplayName("비공개 컬렉션 비소유자 조회는 404 예외")
	void getPrivateCollectionByNonOwner() {
		int ownerId = createMemberId();
		int otherMemberId = createMemberId();
		CollectionVO saved = saveCollection(
				ownerId, "비공개 단건 컬렉션", "N");

		assertThrows(
				NoSuchElementException.class,
				() -> collectionService.get(
						saved.getCollectionId(), OptionalLong.of(otherMemberId)));
	}

	/** 공개 컬렉션도 비소유자는 변경할 수 없음을 검증 */
	@Test
	@DisplayName("공개 컬렉션 비소유자 변경은 403 예외")
	void getOwnedByNonOwner() {
		int ownerId = createMemberId();
		int otherMemberId = createMemberId();
		CollectionVO saved = saveCollection(
				ownerId, "비소유자 변경 컬렉션", "Y");

		assertThrows(
				ForbiddenOperationException.class,
				() -> collectionService.getOwned(
						saved.getCollectionId(), otherMemberId));
	}

	/** 공개 여부를 생략한 기존 요청은 기본 공개 상태로 등록되는지 검증 */
	@Test
	@DisplayName("공개 여부를 생략한 컬렉션은 공개 상태 Y로 등록")
	void create() {
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest(
				"  통합 등록 컬렉션  ", "   ", null);

		CollectionVO result = collectionService.create(memberId, request);

		assertTrue(result.getCollectionId() > 0);
		assertEquals("Y", result.getIsPublic());
		assertEquals("통합 등록 컬렉션", result.getTitle());
		assertNull(result.getDescription());
		assertEquals(memberId, result.getMemberId());
		assertNotNull(result.getCreatedDt());
	}

	/** 공개 여부 N을 요청한 컬렉션이 비공개로 등록되는지 검증 */
	@Test
	@DisplayName("컬렉션은 비공개 상태 N으로 등록")
	void createPrivateCollection() {
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest(
				"통합 비공개 컬렉션", "컬렉션 설명", "N", List.of());

		CollectionVO result = collectionService.create(memberId, request);

		assertEquals("N", result.getIsPublic());
	}

	/** 여러 작품과 중복 작품 번호가 순서 보존 distinct로 저장되는지 검증 */
	@Test
	@DisplayName("여러 작품과 중복 contentId는 중복 없이 등록")
	void createWithDistinctContentIds() {
		int memberId = createMemberId();
		ContentVO first = createContent("첫 번째");
		ContentVO second = createContent("두 번째");
		CollectionCreateRequest request = createRequest(
				"작품 distinct 컬렉션",
				"컬렉션 설명",
				List.of(first.getContentId(), second.getContentId(), first.getContentId()));

		CollectionVO result = collectionService.create(memberId, request);
		List<Integer> contentIds = collectionItemMapper
				.selectContentIdsByCollectionId(result.getCollectionId());

		assertEquals(2, contentIds.size());
		assertTrue(contentIds.contains(first.getContentId()));
		assertTrue(contentIds.contains(second.getContentId()));
	}

	/** 존재하지 않는 작품 번호가 FK 오류로 유실되지 않고 상위 400 변환 대상이 되는지 검증 */
	@Test
	@DisplayName("존재하지 않는 contentId로 컬렉션을 등록하지 않음")
	void createWithMissingContentId() {
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest(
				"존재하지 않는 작품 컬렉션",
				"컬렉션 설명",
				List.of(Integer.MAX_VALUE));

		assertThrows(
				DataIntegrityViolationException.class,
				() -> collectionService.create(memberId, request));
	}

	/** Y/N 이외의 공개 여부가 저장되지 않도록 검증 */
	@Test
	@DisplayName("잘못된 공개 여부로 컬렉션을 등록하지 않음")
	void createWithInvalidPublicState() {
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest(
				"통합 컬렉션", "컬렉션 설명", "INVALID", List.of());

		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(memberId, request));
	}

	/** 필수 제목이 없는 컬렉션 등록 방지 검증 */
	@Test
	@DisplayName("제목이 없는 컬렉션은 등록하지 않음")
	void createWithoutTitle() {
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest(
				" ", "컬렉션 설명", List.of());

		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(memberId, request));
	}

	/** 최대 길이를 초과한 제목의 컬렉션 등록 방지 검증 */
	@Test
	@DisplayName("제목이 100자를 초과한 컬렉션은 등록하지 않음")
	void createWithLongTitle() {
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest(
				"가".repeat(101), "컬렉션 설명", List.of());

		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(memberId, request));
	}

	/** 최대 길이를 초과한 설명의 컬렉션 등록 방지 검증 */
	@Test
	@DisplayName("설명이 1000자를 초과한 컬렉션은 등록하지 않음")
	void createWithLongDescription() {
		int memberId = createMemberId();
		CollectionCreateRequest request = createRequest(
				"통합 컬렉션", "가".repeat(1001), List.of());

		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(memberId, request));
	}

	/** 수정 시 작성자를 유지하면서 공개 컬렉션을 비공개로 변경하는지 검증 */
	@Test
	@DisplayName("기존 작성자를 유지하고 공개 컬렉션을 비공개로 수정")
	void update() {
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId,
				createRequest("수정 전 컬렉션", "컬렉션 설명", List.of()));
		CollectionUpdateRequest request = updateRequest(
				"수정 후 컬렉션", "수정 후 설명", "N", List.of());

		CollectionVO result = collectionService.update(
				memberId, saved.getCollectionId(), request);

		assertEquals(saved.getCollectionId(), result.getCollectionId());
		assertEquals(memberId, result.getMemberId());
		assertEquals("수정 후 컬렉션", result.getTitle());
		assertEquals("N", result.getIsPublic());
		assertNotNull(result.getUpdatedDt());
	}

	/** 공개 여부를 생략한 기존 수정 요청은 현재 상태를 유지하는지 검증 */
	@Test
	@DisplayName("공개 여부를 생략한 수정은 기존 상태 유지")
	void updateWithoutPublicState() {
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId,
				createRequest(
						"비공개 수정 전 컬렉션", "컬렉션 설명", "N", List.of()));
		CollectionUpdateRequest request = updateRequest(
				"비공개 수정 후 컬렉션", "수정 후 설명", List.of());

		CollectionVO result = collectionService.update(
				memberId, saved.getCollectionId(), request);

		assertEquals("N", result.getIsPublic());
	}

	/** 비공개 컬렉션을 다시 공개 상태로 수정하는지 검증 */
	@Test
	@DisplayName("비공개 컬렉션을 공개 상태로 수정")
	void updatePrivateToPublic() {
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId,
				createRequest("재공개 전 컬렉션", "컬렉션 설명", "N", List.of()));
		CollectionUpdateRequest request = updateRequest(
				"재공개 후 컬렉션", "수정 후 설명", "Y", List.of());

		CollectionVO result = collectionService.update(
				memberId, saved.getCollectionId(), request);

		assertEquals("Y", result.getIsPublic());
	}

	/** 수정 요청의 null 작품 목록을 빈 목록으로 처리하는지 검증 */
	@Test
	@DisplayName("수정 시 contentIds가 없으면 작품을 모두 제거")
	void updateWithoutContentIds() {
		int memberId = createMemberId();
		ContentVO content = createContent("전체 제거");
		CollectionVO saved = collectionService.create(memberId,
				createRequest(
						"작품 목록 수정 전",
						"컬렉션 설명",
						List.of(content.getContentId())));
		CollectionUpdateRequest request = updateRequest(
				"작품 목록 수정 후", "컬렉션 설명", null);

		collectionService.update(memberId, saved.getCollectionId(), request);

		assertTrue(collectionItemMapper
				.selectContentIdsByCollectionId(saved.getCollectionId())
				.isEmpty());
	}

	/** 작품 추가와 제거를 동시에 처리하면서 유지 작품의 ADDED_DT를 보존하는지 검증 */
	@Test
	@DisplayName("수정 시 작품 diff와 유지 작품 추가 일시 보존")
	void updateContentDiff() {
		int memberId = createMemberId();
		ContentVO retained = createContent("유지");
		ContentVO removed = createContent("제거");
		ContentVO added = createContent("추가");
		CollectionVO saved = collectionService.create(memberId,
				createRequest(
						"작품 diff 수정 전",
						"컬렉션 설명",
						List.of(retained.getContentId(), removed.getContentId())));
		CollectionItemVO retainedKey = createItemKey(
				saved.getCollectionId(), retained.getContentId());
		String retainedAddedDt = collectionItemMapper
				.doSelectOne(retainedKey).getAddedDt();

		collectionService.update(
				memberId,
				saved.getCollectionId(),
				updateRequest(
						"작품 diff 수정 후",
						"수정 후 설명",
						List.of(retained.getContentId(), added.getContentId())));

		assertEquals(retainedAddedDt,
				collectionItemMapper.doSelectOne(retainedKey).getAddedDt());
		assertNull(collectionItemMapper.doSelectOne(createItemKey(
				saved.getCollectionId(), removed.getContentId())));
		assertNotNull(collectionItemMapper.doSelectOne(createItemKey(
				saved.getCollectionId(), added.getContentId())));
	}

	/** 실제 DB에서 컬렉션 삭제 결과 검증 */
	@Test
	@DisplayName("컬렉션 삭제")
	void delete() {
		int memberId = createMemberId();
		CollectionVO saved = collectionService.create(memberId,
				createRequest("삭제 컬렉션", "컬렉션 설명", List.of()));

		collectionService.delete(memberId, saved.getCollectionId());

		assertThrows(
				NoSuchElementException.class,
				() -> collectionService.get(
						saved.getCollectionId(), OptionalLong.of(memberId)));
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

	/** 작품 스냅샷 테스트에 사용할 콘텐츠 등록 */
	private ContentVO createContent(String titleSuffix) {
		String token = UUID.randomUUID().toString().replace("-", "");
		ContentVO content = new ContentVO(
				0,
				"COLLECTION_" + token,
				"컬렉션 작품 " + titleSuffix,
				"Collection Content " + titleSuffix,
				"컬렉션 작품 스냅샷 테스트",
				"2026-08-31",
				120,
				"KR",
				"https://example.com/poster.jpg",
				"https://example.com/backdrop.jpg",
				null);
		assertEquals(1, contentMapper.doSave(content));
		return content;
	}

	/** 컬렉션 작품 복합 키 생성 */
	private CollectionItemVO createItemKey(int collectionId, int contentId) {
		return new CollectionItemVO(collectionId, contentId, null);
	}

	/** Mapper로 공개 여부를 지정해 접근 정책용 컬렉션 등록 */
	private CollectionVO saveCollection(
			int memberId,
			String title,
			String isPublic) {

		CollectionVO collection = new CollectionVO(
				0,
				memberId,
				title,
				"접근 정책 통합 테스트",
				isPublic,
				null,
				null);
		assertEquals(1, collectionMapper.doSave(collection));
		return collection;
	}

	/** 제목 검색 조건 생성 */
	private DTO searchByTitle(String title) {
		DTO param = new DTO();
		param.setSearchDiv("10");
		param.setSearchWord(title);
		return param;
	}

}
