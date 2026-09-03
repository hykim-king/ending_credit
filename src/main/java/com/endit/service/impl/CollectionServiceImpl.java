package com.endit.service.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.auth.ForbiddenOperationException;
import com.endit.cmn.DTO;
import com.endit.domain.CollectionCreateRequest;
import com.endit.domain.CollectionItemVO;
import com.endit.domain.CollectionQueryParam;
import com.endit.domain.CollectionUpdateRequest;
import com.endit.domain.CollectionVO;
import com.endit.mapper.CollectionItemMapper;
import com.endit.mapper.CollectionMapper;
import com.endit.service.CollectionService;

/**
 * <pre>
 * Class Name  : CollectionServiceImpl
 * Description : 컬렉션의 입력값 검증, 페이징 처리 및 Mapper 호출을 담당하는 Service 구현체
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 21. jinyoung    최초 생성
 * 2026. 8. 29. jinyoung    요청 DTO·작품 스냅샷·공개 여부·전체 목록·U-05 접근 정책 적용
 * 2026. 8. 31. jinyoung    제목·설명 정규화와 contentIds null·중복 권장 정책 적용
 * 2026. 9. 02. jinyoung    전체 목록 조회에 현재 회원 공개 범위 반영
 * 2026. 9. 03. jinyoung    회원별 컬렉션 조회 정렬 및 공개 범위 건수 조회 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 21.
 */
@Service
@Transactional(readOnly = true)
public class CollectionServiceImpl implements CollectionService {

	private static final String PUBLIC_YES = "Y";            // 공개 컬렉션
	private static final String PUBLIC_NO = "N";             // 비공개 컬렉션
	private static final String SEARCH_MEMBER = "20";        // 회원 번호 검색 구분
	private static final String SORT_LATEST = "latest";      // 최신순
	private static final String SORT_OLDEST = "oldest";      // 오래된순
	private static final String SORT_LIKES = "likes";        // 좋아요 많은순

	private static final int DEFAULT_PAGE_SIZE = 10;         // 기본 페이지 크기
	private static final int MAX_PAGE_SIZE = 100;            // 최대 페이지 크기
	private static final int MAX_TITLE_LENGTH = 100;         // 제목 최대 길이
	private static final int MAX_DESCRIPTION_LENGTH = 1000;  // 설명 최대 길이

	private final CollectionMapper collectionMapper;
	private final CollectionItemMapper collectionItemMapper;

	/**
	 * Service 구현체 생성
	 *
	 * @param collectionMapper     컬렉션 Mapper
	 * @param collectionItemMapper 컬렉션 작품 Mapper
	 */
	public CollectionServiceImpl(
			CollectionMapper collectionMapper,
			CollectionItemMapper collectionItemMapper) {

		this.collectionMapper = collectionMapper;
		this.collectionItemMapper = collectionItemMapper;
	}

	/**
	 * 비회원 기준 공개 컬렉션 목록 조회
	 *
	 * @param param 검색 및 페이징 조건
	 * @return 공개 컬렉션 목록
	 */
	@Override
	public List<CollectionVO> retrieve(DTO param) {

		return retrieve(param, OptionalLong.empty());
	}

	/**
	 * 현재 회원의 접근 범위를 반영한 컬렉션 목록 조회
	 *
	 * @param param           검색 및 페이징 조건
	 * @param currentMemberId 현재 로그인 회원 번호
	 * @return 접근 가능한 컬렉션 목록
	 */
	@Override
	public List<CollectionVO> retrieve(DTO param, OptionalLong currentMemberId) {

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		if (currentMemberId == null) {
			throw new IllegalArgumentException("현재 회원 조회 결과가 필요합니다.");
		}

		normalizePaging(param);
		normalizeSort(param);
		CollectionQueryParam queryParam = createQueryParam(
				param, currentMemberId);

		return retrieveVisible(param, queryParam);
	}

	/**
	 * 대상 회원의 공개 범위에 맞는 컬렉션 목록 조회
	 *
	 * @param memberId        대상 회원 번호
	 * @param param           검색 및 페이징 조건
	 * @param currentMemberId 현재 로그인 회원 번호
	 * @return 접근 가능한 대상 회원의 컬렉션 목록
	 */
	@Override
	public List<CollectionVO> retrieveByMember(
			long memberId, DTO param, OptionalLong currentMemberId) {

		validateMemberId(memberId);

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		if (currentMemberId == null) {
			throw new IllegalArgumentException("현재 회원 조회 결과가 필요합니다.");
		}

		normalizePaging(param);
		normalizeSort(param);
		CollectionQueryParam queryParam = createQueryParam(param, currentMemberId);
		queryParam.setTargetMemberId(memberId);

		return retrieveVisible(param, queryParam);
	}

	/**
	 * 회원 작성 컬렉션 전체 건수 조회
	 *
	 * @param memberId 작성 회원 번호
	 * @return 공개·비공개 컬렉션 전체 건수
	 */
	@Override
	public int countByMember(int memberId) {

		validateMemberId(memberId);

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_MEMBER);
		param.setSearchWord(String.valueOf(memberId));

		return collectionMapper.count(param);
	}

	/**
	 * 조회자가 접근 가능한 대상 회원의 컬렉션 건수 조회
	 *
	 * @param targetMemberId  대상 회원 번호
	 * @param currentMemberId 현재 로그인 회원 번호
	 * @return 공개 범위를 반영한 컬렉션 건수
	 */
	@Override
	public int countVisibleByMember(int targetMemberId, Long currentMemberId) {

		validateMemberId(targetMemberId);

		// 로그인하지 않은 경우에는 null을 허용하고, 로그인한 경우에만 회원 번호를 검증한다.
		if (currentMemberId != null) {
			validateMemberId(currentMemberId);
		}

		CollectionQueryParam param = new CollectionQueryParam();
		param.setTargetMemberId(Long.valueOf(targetMemberId));
		param.setCurrentMemberId(currentMemberId);

		return collectionMapper.countVisible(param);
	}

	/**
	 * 공개 조건을 공유하는 목록·전체 건수 조회
	 *
	 * @param param      전체 건수를 저장할 검색 및 페이징 조건
	 * @param queryParam 공개 범위를 포함한 Mapper 조회 조건
	 * @return 공개 범위를 반영한 컬렉션 목록
	 */
	private List<CollectionVO> retrieveVisible(
			DTO param, CollectionQueryParam queryParam) {

		int totalCount = collectionMapper.countVisible(queryParam);
		param.setTotalCnt(totalCount);

		if (totalCount == 0) {
			return Collections.emptyList();
		}

		return collectionMapper.retrieveVisible(queryParam);
	}

	/**
	 * 접근 가능한 컬렉션 단건 조회
	 *
	 * @param collectionId    컬렉션 번호
	 * @param currentMemberId 현재 로그인 회원 번호
	 * @return 컬렉션 상세 정보
	 */
	@Override
	public CollectionVO get(int collectionId, OptionalLong currentMemberId) {

		if (currentMemberId == null) {
			throw new IllegalArgumentException("현재 회원 조회 결과가 필요합니다.");
		}

		CollectionVO collection = findExisting(collectionId);

		// 비공개 컬렉션은 소유 회원이 조회할 때만 결과를 반환한다.
		if (!PUBLIC_YES.equals(collection.getIsPublic())
				&& (!currentMemberId.isPresent() || collection.getMemberId() 
						!= currentMemberId.getAsLong())) {
			throw collectionNotFound(collectionId);
		}

		return collection;
	}

	/**
	 * 소유권 검증 및 변경 가능 컬렉션 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @param memberId     요청 회원 번호
	 * @return 회원 소유 컬렉션 상세 정보
	 */
	@Override
	public CollectionVO getOwned(int collectionId, long memberId) {

		validateMemberId(memberId);
		CollectionVO collection = findExisting(collectionId);

		if (collection.getMemberId() == memberId) {
			return collection;
		}

		if (!PUBLIC_YES.equals(collection.getIsPublic())) {
			throw collectionNotFound(collectionId);
		}

		throw new ForbiddenOperationException("다른 회원의 컬렉션은 변경할 수 없습니다.");
	}

	/**
	 * 컬렉션과 선택 작품의 트랜잭션 등록
	 *
	 * @param memberId 작성 회원 번호
	 * @param request  컬렉션 등록 정보
	 * @return 등록된 컬렉션 상세 정보
	 */
	@Override
	@Transactional
	public CollectionVO create(
			long memberId, CollectionCreateRequest request) {

		validateMemberId(memberId);
		validateCreateRequest(request);

		// 작품 목록이 없으면 빈 집합을 사용하고, 중복된 작품 번호는 한 번만 등록한다.
		Set<Integer> contentIds = normalizeContentIds(request.getContentIds());

		CollectionVO param = new CollectionVO();
		param.setMemberId(Math.toIntExact(memberId));
		param.setTitle(normalizeTitle(request.getTitle()));
		param.setDescription(normalizeDescription(request.getDescription()));
		param.setIsPublic(normalizeIsPublic(request.getIsPublic(), PUBLIC_YES));

		int result = collectionMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 등록에 실패했습니다.");
		}

		for (int contentId : contentIds) {
			insertCollectionItem(param.getCollectionId(), contentId);
		}

		return get(param.getCollectionId(), OptionalLong.of(memberId));
	}

	/**
	 * 컬렉션 정보와 포함 작품의 트랜잭션 수정
	 *
	 * @param memberId     요청 회원 번호
	 * @param collectionId 컬렉션 번호
	 * @param request      컬렉션 수정 정보
	 * @return 수정된 컬렉션 상세 정보
	 */
	@Override
	@Transactional
	public CollectionVO update(
			long memberId, int collectionId, CollectionUpdateRequest request) {

		validateMemberId(memberId);
		validateCollectionId(collectionId);

		if (request == null) {
			throw new IllegalArgumentException("수정할 컬렉션 정보가 필요합니다.");
		}

		CollectionVO existing = getOwned(collectionId, memberId);

		// 수정 요청의 작품 번호를 검증하고, 중복된 번호는 하나로 합친다.
		Set<Integer> requestedContentIds = normalizeContentIds(request.getContentIds());

		CollectionVO param = new CollectionVO();
		param.setTitle(normalizeTitle(request.getTitle()));
		param.setDescription(normalizeDescription(request.getDescription()));
		param.setIsPublic(normalizeIsPublic(request.getIsPublic(), existing.getIsPublic()));

		param.setCollectionId(collectionId);
		param.setMemberId(existing.getMemberId());

		int result = collectionMapper.doUpdate(param);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 수정에 실패했습니다.");
		}

		// 현재 저장된 작품 번호를 집합으로 변환하여 추가·삭제 대상을 비교한다.
		Set<Integer> existingContentIds = new LinkedHashSet<>(
				collectionItemMapper.selectContentIdsByCollectionId(collectionId));

		for (int contentId : existingContentIds) {
			if (!requestedContentIds.contains(contentId)) {
				deleteCollectionItem(collectionId, contentId);
			}
		}

		for (int contentId : requestedContentIds) {
			if (!existingContentIds.contains(contentId)) {
				insertCollectionItem(collectionId, contentId);
			}
		}

		return get(collectionId, OptionalLong.of(memberId));
	}

	/**
	 * 소유권 확인 후 컬렉션 삭제
	 *
	 * @param memberId     요청 회원 번호
	 * @param collectionId 컬렉션 번호
	 */
	@Override
	@Transactional
	public void delete(long memberId, int collectionId) {

		CollectionVO existing = getOwned(collectionId, memberId);

		int result = collectionMapper.doDelete(existing);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 삭제에 실패했습니다.");
		}
	}

	/**
	 * 페이지 번호와 페이지 크기 정규화
	 *
	 * @param param 검색 및 페이징 조건
	 */
	private void normalizePaging(DTO param) {

		if (param.getPageNo() <= 0) {
			param.setPageNo(1);
		}

		if (param.getPageSize() <= 0) {
			param.setPageSize(DEFAULT_PAGE_SIZE);

		} else if (param.getPageSize() > MAX_PAGE_SIZE) {
			param.setPageSize(MAX_PAGE_SIZE);
		}
	}

	/**
	 * 컬렉션 목록 정렬 조건 검증 및 기본값 적용
	 *
	 * @param param 검색 및 페이징 조건
	 */
	private void normalizeSort(DTO param) {

		String sort = param.getSearchMap().get("sort");

		if (sort == null || sort.isBlank()) {
			param.getSearchMap().put("sort", SORT_LATEST);

			return;
		}

		String normalizedSort = sort.trim().toLowerCase(Locale.ROOT);

		if (!SORT_LATEST.equals(normalizedSort)
				&& !SORT_OLDEST.equals(normalizedSort)
				&& !SORT_LIKES.equals(normalizedSort)) {
			throw new IllegalArgumentException("지원하지 않는 컬렉션 정렬 조건입니다.");
		}

		param.getSearchMap().put("sort", normalizedSort);
	}

	/**
	 * 컬렉션 생성 요청 필수값 검증
	 *
	 * @param request 등록할 컬렉션 정보
	 */
	private void validateCreateRequest(CollectionCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("등록할 컬렉션 정보가 필요합니다.");
		}
	}

	/**
	 * 외부 조회 조건과 인증 정보의 Mapper 조회 조건 변환
	 *
	 * @param param           검색 및 페이징 조건
	 * @param currentMemberId 현재 로그인 회원 번호
	 * @return Mapper 전용 컬렉션 조회 조건
	 */
	private CollectionQueryParam createQueryParam(DTO param, OptionalLong currentMemberId) {

		CollectionQueryParam queryParam = new CollectionQueryParam();

		queryParam.setPageNo(param.getPageNo());
		queryParam.setPageSize(param.getPageSize());
		queryParam.setSearchDiv(param.getSearchDiv());
		queryParam.setSearchWord(param.getSearchWord());
		queryParam.setSearchMap(param.getSearchMap());

		if (currentMemberId.isPresent()) {
			validateMemberId(currentMemberId.getAsLong());
			queryParam.setCurrentMemberId(currentMemberId.getAsLong());
		}

		return queryParam;
	}

	/**
	 * 접근 권한 확인 전 컬렉션 존재 여부 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 존재하는 컬렉션 상세 정보
	 */
	private CollectionVO findExisting(int collectionId) {

		validateCollectionId(collectionId);

		CollectionVO param = new CollectionVO();
		param.setCollectionId(collectionId);

		CollectionVO collection = collectionMapper.doSelectOne(param);

		if (collection == null) {
			throw collectionNotFound(collectionId);
		}

		return collection;
	}

	/**
	 * 비공개 컬렉션 존재 여부를 숨기는 미조회 예외 생성
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 컬렉션 미조회 예외
	 */
	private NoSuchElementException collectionNotFound(int collectionId) {

		return new NoSuchElementException("존재하지 않는 컬렉션입니다. collectionId=" + collectionId);
	}

	/**
	 * CollectionVO 저장 범위의 회원 번호 검증
	 *
	 * @param memberId 회원 번호
	 */
	private void validateMemberId(long memberId) {

		if (memberId <= 0 || memberId > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("올바른 회원 번호가 필요합니다.");
		}
	}

	/**
	 * 컬렉션 번호 검증
	 *
	 * @param collectionId 컬렉션 번호
	 */
	private void validateCollectionId(int collectionId) {

		if (collectionId <= 0) {
			throw new IllegalArgumentException("올바른 컬렉션 번호가 필요합니다.");
		}
	}

	/**
	 * 컬렉션 제목 정규화 및 길이 검증
	 *
	 * @param title 컬렉션 제목
	 * @return 앞뒤 공백을 제거한 컬렉션 제목
	 */
	private String normalizeTitle(String title) {

		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("컬렉션 제목은 필수입니다.");
		}

		String normalized = title.trim();

		if (normalized.length() > MAX_TITLE_LENGTH) {
			throw new IllegalArgumentException("컬렉션 제목은 100자 이하여야 합니다.");
		}

		return normalized;
	}

	/**
	 * 컬렉션 설명 정규화 및 길이 검증
	 *
	 * @param description 컬렉션 설명
	 * @return 앞뒤 공백을 제거한 설명 또는 {@code null}
	 */
	private String normalizeDescription(String description) {

		if (description == null || description.isBlank()) {
			return null;
		}

		String normalized = description.trim();

		if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
			throw new IllegalArgumentException("컬렉션 설명은 1000자 이하여야 합니다.");
		}

		return normalized;
	}

	/**
	 * 작품 번호 목록의 null·중복값 정규화
	 *
	 * @param contentIds 작품 번호 목록
	 * @return 요청 순서를 유지한 중복 없는 작품 번호 집합
	 */
	private Set<Integer> normalizeContentIds(List<Integer> contentIds) {

		if (contentIds == null) {
			return Collections.emptySet();
		}

		Set<Integer> uniqueContentIds = new LinkedHashSet<>();

		for (Integer contentId : contentIds) {
			if (contentId == null || contentId <= 0) {
				throw new IllegalArgumentException("작품 번호(contentIds)는 양수여야 합니다.");
			}

			uniqueContentIds.add(contentId);
		}

		return uniqueContentIds;
	}

	/**
	 * 컬렉션 공개 여부 정규화
	 *
	 * @param isPublic     요청 공개 여부
	 * @param defaultValue 빈 값에 적용할 기본 공개 여부
	 * @return 정규화된 {@code Y} 또는 {@code N}
	 */
	private String normalizeIsPublic(String isPublic, String defaultValue) {

		if (isPublic == null || isPublic.isBlank()) {
			return defaultValue;
		}

		String normalized = isPublic.trim().toUpperCase(Locale.ROOT);

		if (!PUBLIC_YES.equals(normalized) && !PUBLIC_NO.equals(normalized)) {
			throw new IllegalArgumentException("공개 여부(isPublic)는 Y 또는 N이어야 합니다.");
		}

		return normalized;
	}

	/**
	 * 컬렉션 작품 단건 등록 및 처리 건수 확인
	 *
	 * @param collectionId 컬렉션 번호
	 * @param contentId    작품 번호
	 */
	private void insertCollectionItem(int collectionId, int contentId) {

		CollectionItemVO item = 
				new CollectionItemVO(collectionId, contentId, null);

		if (collectionItemMapper.doSave(item) != 1) {
			throw new IllegalStateException("컬렉션 작품 추가에 실패했습니다. contentId=" + contentId);
		}
	}

	/**
	 * 컬렉션 작품 단건 삭제 및 처리 건수 확인
	 *
	 * @param collectionId 컬렉션 번호
	 * @param contentId    작품 번호
	 */
	private void deleteCollectionItem(int collectionId, int contentId) {

		CollectionItemVO item = 
				new CollectionItemVO(collectionId, contentId, null);

		if (collectionItemMapper.doDelete(item) != 1) {
			throw new IllegalStateException("컬렉션 작품 삭제에 실패했습니다. contentId=" + contentId);
		}
	}
}
