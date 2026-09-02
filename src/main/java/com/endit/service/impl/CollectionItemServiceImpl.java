package com.endit.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;
import com.endit.mapper.CollectionItemMapper;
import com.endit.service.CollectionService;
import com.endit.service.CollectionItemService;

/**
 * <pre>
 * Class Name  : CollectionItemServiceImpl
 * Description : 컬렉션 작품의 입력값 검증, 페이징 처리 및 Mapper 호출을 담당하는 Service 구현체
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * 2026. 8. 29. jinyoung    부모 컬렉션 조회 권한 및 변경 소유권 검증 추가
 * 2026. 9. 02. jinyoung    목록 조회 시 현재 회원 평가 조건 전달
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@Service
@Transactional(readOnly = true)
public class CollectionItemServiceImpl implements CollectionItemService {

	private static final String SEARCH_BY_COLLECTION = "10";
	private static final int DEFAULT_PAGE_SIZE = 12;
	private static final int MAX_PAGE_SIZE = 100;

	private final CollectionItemMapper collectionItemMapper;
	private final CollectionService collectionService;

	/**
	 * CollectionItemMapper를 주입받아 Service 구현체 생성
	 *
	 * @param collectionItemMapper 컬렉션 작품 Mapper
	 * @param collectionService 부모 컬렉션 접근 정책 Service
	 */
	public CollectionItemServiceImpl(
			CollectionItemMapper collectionItemMapper,
			CollectionService collectionService) {

		this.collectionItemMapper = collectionItemMapper;
		this.collectionService = collectionService;
	}

	/** 접근 권한과 페이징 조건을 확인한 뒤 컬렉션 작품을 조회한다. */
	@Override
	public List<CollectionItemVO> retrieve(
			int collectionId, DTO param, OptionalLong currentMemberId) {

		validateCollectionId(collectionId);
		collectionService.get(collectionId, currentMemberId);

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		// 기존 Mapper의 공통 검색 규칙(searchDiv=10)을 Service 내부에서 구성해
		// 다른 컬렉션의 작품이 함께 조회되지 않도록 고정한다.
		normalizePaging(param);
		param.setSearchDiv(SEARCH_BY_COLLECTION);
		param.setSearchWord(String.valueOf(collectionId));
		param.getSearchMap().remove("currentMemberId");
		currentMemberId.ifPresent(memberId ->
				param.getSearchMap().put("currentMemberId", String.valueOf(memberId)));

		// 목록과 같은 조건으로 count를 먼저 조회해 페이지 계산 정보에 사용한다.
		int totalCount = collectionItemMapper.count(param);
		param.setTotalCnt(totalCount);

		if (totalCount == 0) {
			return Collections.emptyList();
		}

		return collectionItemMapper.doRetrieve(param);
	}

	/** 접근 가능한 컬렉션에서 작품 한 건을 조회한다. */
	@Override
	public CollectionItemVO get(
			int collectionId,
			int contentId,
			OptionalLong currentMemberId) {

		collectionService.get(collectionId, currentMemberId);
		return findItem(collectionId, contentId);
	}

	/** 소유 컬렉션에 중복되지 않은 작품 한 건을 추가한다. */
	@Override
	@Transactional
	public CollectionItemVO create(
			long memberId,
			int collectionId,
			CollectionItemVO param) {

		validateCollectionId(collectionId);
		collectionService.getOwned(collectionId, memberId);

		if (param == null) {
			throw new IllegalArgumentException("추가할 작품 정보가 필요합니다.");
		}

		validateContentId(param.getContentId());

		param.setCollectionId(collectionId);

		if (collectionItemMapper.doSelectOne(param) != null) {
			throw new IllegalStateException("이미 컬렉션에 추가된 작품입니다.");
		}

		int result = collectionItemMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 작품 추가에 실패했습니다.");
		}

		CollectionItemVO created = collectionItemMapper.doSelectOne(param);

		if (created == null) {
			throw new IllegalStateException("추가한 컬렉션 작품을 조회할 수 없습니다.");
		}

		return created;
	}

	/** 소유 컬렉션에 포함된 작품 한 건을 삭제한다. */
	@Override
	@Transactional
	public void delete(long memberId, int collectionId, int contentId) {
		collectionService.getOwned(collectionId, memberId);
		CollectionItemVO existing = findItem(collectionId, contentId);
		int result = collectionItemMapper.doDelete(existing);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 작품 삭제에 실패했습니다.");
		}
	}

	/** 부모 컬렉션 접근 확인이 끝난 작품을 복합 키로 조회한다. */
	private CollectionItemVO findItem(int collectionId, int contentId) {
		CollectionItemVO key = createKey(collectionId, contentId);
		CollectionItemVO item = collectionItemMapper.doSelectOne(key);

		if (item == null) {
			throw new NoSuchElementException(
					"컬렉션에 포함되지 않은 작품입니다. collectionId="
					+ collectionId + ", contentId=" + contentId);
		}
		return item;
	}

	/** 페이지 번호와 크기를 허용 범위로 보정한다. */
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

	/** 컬렉션 번호와 콘텐츠 번호를 담은 복합 키를 만든다. */
	private CollectionItemVO createKey(int collectionId, int contentId) {
		validateCollectionId(collectionId);
		validateContentId(contentId);

		CollectionItemVO key = new CollectionItemVO();
		key.setCollectionId(collectionId);
		key.setContentId(contentId);

		return key;
	}

	/** 컬렉션 번호가 양수인지 확인한다. */
	private void validateCollectionId(int collectionId) {
		if (collectionId <= 0) {
			throw new IllegalArgumentException("올바른 컬렉션 번호가 필요합니다.");
		}
	}

	/** 콘텐츠 번호가 양수인지 확인한다. */
	private void validateContentId(int contentId) {
		if (contentId <= 0) {
			throw new IllegalArgumentException("올바른 콘텐츠 번호가 필요합니다.");
		}
	}

}
