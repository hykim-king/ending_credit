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
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 21.
 */
@Service
@Transactional(readOnly = true)
public class CollectionServiceImpl implements CollectionService {

	private static final String PUBLIC_YES = "Y";
	private static final String PUBLIC_NO = "N";

	private final CollectionMapper collectionMapper;
	private final CollectionItemMapper collectionItemMapper;

	/**
	 * CollectionMapper를 주입받아 Service 구현체 생성
	 *
	 * @param collectionMapper 컬렉션 Mapper
	 * @param collectionItemMapper 컬렉션 작품 Mapper
	 */
	public CollectionServiceImpl(
			CollectionMapper collectionMapper,
			CollectionItemMapper collectionItemMapper) {

		this.collectionMapper = collectionMapper;
		this.collectionItemMapper = collectionItemMapper;
	}

	@Override
	public List<CollectionVO> retrieve(DTO param) {
		return retrieve(param, OptionalLong.empty());
	}

	@Override
	public List<CollectionVO> retrieve(
			DTO param,
			OptionalLong currentMemberId) {

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}
		if (currentMemberId == null) {
			throw new IllegalArgumentException("현재 회원 조회 결과가 필요합니다.");
		}

		normalizePaging(param);
		CollectionQueryParam queryParam = createQueryParam(
				param, currentMemberId);

		return retrieveVisible(param, queryParam);
	}

	@Override
	public List<CollectionVO> retrieveByMember(
			long memberId,
			DTO param,
			OptionalLong currentMemberId) {

		validateMemberId(memberId);
		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}
		if (currentMemberId == null) {
			throw new IllegalArgumentException("현재 회원 조회 결과가 필요합니다.");
		}

		normalizePaging(param);
		CollectionQueryParam queryParam = createQueryParam(
				param, currentMemberId);
		queryParam.setTargetMemberId(memberId);

		return retrieveVisible(param, queryParam);
	}

	/** 공개 범위 조회와 응답용 페이징 전체 건수 설정을 공통 처리한다. */
	private List<CollectionVO> retrieveVisible(
			DTO param,
			CollectionQueryParam queryParam) {

		int totalCount = collectionMapper.countVisible(queryParam);
		param.setTotalCnt(totalCount);

		if (totalCount == 0) {
			return Collections.emptyList();
		}

		return collectionMapper.retrieveVisible(queryParam);
	}

	@Override
	public CollectionVO get(
			int collectionId,
			OptionalLong currentMemberId) {

		if (currentMemberId == null) {
			throw new IllegalArgumentException("현재 회원 조회 결과가 필요합니다.");
		}

		CollectionVO collection = findExisting(collectionId);
		if (!PUBLIC_YES.equals(collection.getIsPublic())
				&& (!currentMemberId.isPresent()
						|| collection.getMemberId() != currentMemberId.getAsLong())) {
			throw collectionNotFound(collectionId);
		}
		return collection;
	}

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

		throw new ForbiddenOperationException(
				"다른 회원의 컬렉션은 변경할 수 없습니다.");
	}

	@Override
	@Transactional
	public CollectionVO create(
			long memberId,
			CollectionCreateRequest request) {

		validateMemberId(memberId);
		validateCreateRequest(request);

		Set<Integer> contentIds = normalizeContentIds(request.getContentIds());

		CollectionVO param = new CollectionVO();
		param.setMemberId(Math.toIntExact(memberId));
		param.setTitle(normalizeTitle(request.getTitle()));
		param.setDescription(normalizeDescription(request.getDescription()));
		param.setIsPublic(normalizeIsPublic(
				request.getIsPublic(), PUBLIC_YES));

		int result = collectionMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 등록에 실패했습니다.");
		}

		for (int contentId : contentIds) {
			insertCollectionItem(param.getCollectionId(), contentId);
		}

		return get(param.getCollectionId(), OptionalLong.of(memberId));
	}

	@Override
	@Transactional
	public CollectionVO update(
			long memberId,
			int collectionId,
			CollectionUpdateRequest request) {

		validateMemberId(memberId);
		validateCollectionId(collectionId);

		if (request == null) {
			throw new IllegalArgumentException("수정할 컬렉션 정보가 필요합니다.");
		}

		CollectionVO existing = getOwned(collectionId, memberId);

		Set<Integer> requestedContentIds = normalizeContentIds(
				request.getContentIds());

		CollectionVO param = new CollectionVO();
		param.setTitle(normalizeTitle(request.getTitle()));
		param.setDescription(normalizeDescription(request.getDescription()));
		param.setIsPublic(normalizeIsPublic(
				request.getIsPublic(), existing.getIsPublic()));

		param.setCollectionId(collectionId);
		param.setMemberId(existing.getMemberId());

		int result = collectionMapper.doUpdate(param);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 수정에 실패했습니다.");
		}

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
	 * 페이지 번호와 페이지 크기를 허용 범위의 기본값으로 보정
	 *
	 * @param param 검색 및 페이징 조건
	 */
	private void normalizePaging(DTO param) {
		if (param.getPageNo() <= 0) {
			param.setPageNo(1);
		}

		if (param.getPageSize() <= 0) {
			param.setPageSize(10);
		} else if (param.getPageSize() > 100) {
			param.setPageSize(100);
		}
	}

	/**
	 * 컬렉션 생성 요청 객체 필수값 검증
	 *
	 * @param request 등록할 컬렉션 정보
	 */
	private void validateCreateRequest(CollectionCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("등록할 컬렉션 정보가 필요합니다.");
		}

	}

	/** 외부 응답용 DTO와 인증 정보를 내부 컬렉션 조회 조건으로 복사 */
	private CollectionQueryParam createQueryParam(
			DTO param,
			OptionalLong currentMemberId) {

		CollectionQueryParam queryParam = new CollectionQueryParam();
		queryParam.setPageNo(param.getPageNo());
		queryParam.setPageSize(param.getPageSize());
		queryParam.setSearchDiv(param.getSearchDiv());
		queryParam.setSearchWord(param.getSearchWord());
		if (currentMemberId.isPresent()) {
			validateMemberId(currentMemberId.getAsLong());
			queryParam.setCurrentMemberId(currentMemberId.getAsLong());
		}
		return queryParam;
	}

	/** 접근 판정 전에 실제 컬렉션 존재 여부 조회 */
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

	/** 비공개 컬렉션의 존재 여부가 노출되지 않도록 미조회와 같은 예외 생성 */
	private NoSuchElementException collectionNotFound(int collectionId) {
		return new NoSuchElementException(
				"존재하지 않는 컬렉션입니다. collectionId=" + collectionId);
	}

	/** 요청 회원 번호가 CollectionVO가 사용하는 정수 범위의 양수인지 검증 */
	private void validateMemberId(long memberId) {
		if (memberId <= 0 || memberId > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("올바른 회원 번호가 필요합니다.");
		}
	}

	/**
	 * 컬렉션 번호가 유효한 양수인지 검증
	 *
	 * @param collectionId 컬렉션 번호
	 */
	private void validateCollectionId(int collectionId) {
		if (collectionId <= 0) {
			throw new IllegalArgumentException("올바른 컬렉션 번호가 필요합니다.");
		}
	}

	/**
	 * 컬렉션 제목 필수값 및 최대 길이 검증
	 *
	 * @param title 컬렉션 제목
	 */
	private String normalizeTitle(String title) {
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("컬렉션 제목은 필수입니다.");
		}

		String normalized = title.trim();

		if (normalized.length() > 100) {
			throw new IllegalArgumentException(
					"컬렉션 제목은 100자 이하여야 합니다.");
		}

		return normalized;
	}

	/**
	 * 컬렉션 설명 최대 길이 검증
	 *
	 * @param description 컬렉션 설명
	 */
	private String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}

		String normalized = description.trim();

		if (normalized.length() > 1000) {
			throw new IllegalArgumentException(
					"컬렉션 설명은 1000자 이하여야 합니다.");
		}

		return normalized;
	}

	/** null은 빈 목록, 중복은 한 건으로 정규화하고 요청 순서를 유지한다. */
	private Set<Integer> normalizeContentIds(List<Integer> contentIds) {

		if (contentIds == null) {
			return Collections.emptySet();
		}

		Set<Integer> uniqueContentIds = new LinkedHashSet<>();

		for (Integer contentId : contentIds) {
			if (contentId == null || contentId <= 0) {
				throw new IllegalArgumentException(
						"작품 번호(contentIds)는 양수여야 합니다.");
			}


			uniqueContentIds.add(contentId);
		}

		return uniqueContentIds;
	}

	/** 공개 여부를 Y/N으로 정규화하고 값이 없으면 지정한 기본값을 사용한다. */
	private String normalizeIsPublic(
			String isPublic,
			String defaultValue) {

		if (isPublic == null || isPublic.isBlank()) {
			return defaultValue;
		}

		String normalized = isPublic.trim().toUpperCase(Locale.ROOT);

		if (!PUBLIC_YES.equals(normalized)
				&& !PUBLIC_NO.equals(normalized)) {
			throw new IllegalArgumentException(
					"공개 여부(isPublic)는 Y 또는 N이어야 합니다.");
		}

		return normalized;
	}

	/** 컬렉션 작품 한 건을 저장하고 결과를 검증 */
	private void insertCollectionItem(int collectionId, int contentId) {
		CollectionItemVO item = new CollectionItemVO();
		item.setCollectionId(collectionId);
		item.setContentId(contentId);

		if (collectionItemMapper.doSave(item) != 1) {
			throw new IllegalStateException(
					"컬렉션 작품 추가에 실패했습니다. contentId=" + contentId);
		}
	}

	/** 컬렉션 작품 한 건을 삭제하고 결과를 검증 */
	private void deleteCollectionItem(int collectionId, int contentId) {
		CollectionItemVO item = new CollectionItemVO();
		item.setCollectionId(collectionId);
		item.setContentId(contentId);

		if (collectionItemMapper.doDelete(item) != 1) {
			throw new IllegalStateException(
					"컬렉션 작품 삭제에 실패했습니다. contentId=" + contentId);
		}
	}
}
