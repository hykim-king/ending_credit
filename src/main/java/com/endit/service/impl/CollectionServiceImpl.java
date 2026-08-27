package com.endit.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionVO;
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

	/**
	 * CollectionMapper를 주입받아 Service 구현체 생성
	 *
	 * @param collectionMapper 컬렉션 Mapper
	 */
	public CollectionServiceImpl(CollectionMapper collectionMapper) {
		this.collectionMapper = collectionMapper;
	}

	@Override
	public List<CollectionVO> retrieve(DTO param) {
		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		normalizePaging(param);

		int totalCount = collectionMapper.count(param);
		param.setTotalCnt(totalCount);

		if (totalCount == 0) {
			return Collections.emptyList();
		}

		return collectionMapper.doRetrieve(param);
	}

	@Override
	public CollectionVO get(int collectionId) {
		validateCollectionId(collectionId);

		CollectionVO param = new CollectionVO();
		param.setCollectionId(collectionId);

		CollectionVO collection = collectionMapper.doSelectOne(param);

		if (collection == null) {
			throw new NoSuchElementException(
					"존재하지 않는 컬렉션입니다. collectionId=" + collectionId);
		}

		return collection;
	}

	@Override
	@Transactional
	public CollectionVO create(CollectionVO param) {
		validateCreateParam(param);

		if (param.getIsPublic() == null || param.getIsPublic().isBlank()) {
			param.setIsPublic(PUBLIC_YES);
		}

		int result = collectionMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 등록에 실패했습니다.");
		}

		return get(param.getCollectionId());
	}

	@Override
	@Transactional
	public CollectionVO update(
			int collectionId,
			CollectionVO param) {

		validateCollectionId(collectionId);

		if (param == null) {
			throw new IllegalArgumentException("수정할 컬렉션 정보가 필요합니다.");
		}

		CollectionVO existing = get(collectionId);

		validateTitle(param.getTitle());
		validateDescription(param.getDescription());

		if (param.getIsPublic() == null || param.getIsPublic().isBlank()) {
			param.setIsPublic(existing.getIsPublic());
		}

		validateIsPublic(param.getIsPublic());

		param.setCollectionId(collectionId);
		param.setMemberId(existing.getMemberId());

		int result = collectionMapper.doUpdate(param);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 수정에 실패했습니다.");
		}

		return get(collectionId);
	}

	@Override
	@Transactional
	public void delete(int collectionId) {
		CollectionVO existing = get(collectionId);

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
	 * 컬렉션 등록에 필요한 회원 번호, 제목 및 공개 여부 검증
	 *
	 * @param param 등록할 컬렉션 정보
	 */
	private void validateCreateParam(CollectionVO param) {
		if (param == null) {
			throw new IllegalArgumentException("등록할 컬렉션 정보가 필요합니다.");
		}

		if (param.getMemberId() <= 0) {
			throw new IllegalArgumentException("회원 번호가 필요합니다.");
		}

		validateTitle(param.getTitle());
		validateDescription(param.getDescription());

		if (param.getIsPublic() != null && !param.getIsPublic().isBlank()) {
			validateIsPublic(param.getIsPublic());
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
	private void validateTitle(String title) {
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("컬렉션 제목은 필수입니다.");
		}

		if (title.length() > 100) {
			throw new IllegalArgumentException(
					"컬렉션 제목은 100자 이하여야 합니다.");
		}
	}

	/**
	 * 컬렉션 설명 최대 길이 검증
	 *
	 * @param description 컬렉션 설명
	 */
	private void validateDescription(String description) {
		if (description != null && description.length() > 1000) {
			throw new IllegalArgumentException(
					"컬렉션 설명은 1000자 이하여야 합니다.");
		}
	}

	/**
	 * 공개 여부가 Y 또는 N인지 검증
	 *
	 * @param isPublic 공개 여부
	 */
	private void validateIsPublic(String isPublic) {
		if (!PUBLIC_YES.equals(isPublic) && !PUBLIC_NO.equals(isPublic)) {
			throw new IllegalArgumentException(
					"공개 여부는 Y 또는 N이어야 합니다.");
		}
	}
}
