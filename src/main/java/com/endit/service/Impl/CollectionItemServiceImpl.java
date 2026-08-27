package com.endit.service.Impl;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;
import com.endit.mapper.CollectionItemMapper;
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

	private final CollectionItemMapper collectionItemMapper;

	/**
	 * CollectionItemMapper를 주입받아 Service 구현체 생성
	 *
	 * @param collectionItemMapper 컬렉션 작품 Mapper
	 */
	public CollectionItemServiceImpl(CollectionItemMapper collectionItemMapper) {
		this.collectionItemMapper = collectionItemMapper;
	}

	@Override
	public List<CollectionItemVO> retrieve(int collectionId, DTO param) {
		validateCollectionId(collectionId);

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		// 기존 Mapper의 공통 검색 규칙(searchDiv=10)을 Service 내부에서 구성해
		// 다른 컬렉션의 작품이 함께 조회되지 않도록 고정한다.
		normalizePaging(param);
		param.setSearchDiv(SEARCH_BY_COLLECTION);
		param.setSearchWord(String.valueOf(collectionId));

		// 목록과 같은 조건으로 count를 먼저 조회해 페이지 계산 정보에 사용한다.
		int totalCount = collectionItemMapper.count(param);
		param.setTotalCnt(totalCount);

		if (totalCount == 0) {
			return Collections.emptyList();
		}

		return collectionItemMapper.doRetrieve(param);
	}

	@Override
	public CollectionItemVO get(int collectionId, int contentId) {
		// COLLECTION_ITEM은 collectionId와 contentId가 함께 PK이므로 두 값 모두 필요하다.
		CollectionItemVO key = createKey(collectionId, contentId);
		CollectionItemVO item = collectionItemMapper.doSelectOne(key);

		if (item == null) {
			throw new NoSuchElementException(
					"컬렉션에 포함되지 않은 작품입니다. collectionId="
					+ collectionId + ", contentId=" + contentId);
		}

		return item;
	}

	@Override
	@Transactional
	public CollectionItemVO create(
			int collectionId,
			CollectionItemVO param) {

		validateCollectionId(collectionId);

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

	@Override
	@Transactional
	public void delete(int collectionId, int contentId) {
		CollectionItemVO existing = get(collectionId, contentId);
		int result = collectionItemMapper.doDelete(existing);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 작품 삭제에 실패했습니다.");
		}
	}

	/** 페이지 번호와 페이지 크기를 허용 범위의 기본값으로 보정 */
	private void normalizePaging(DTO param) {
		if (param.getPageNo() <= 0) {
			param.setPageNo(1);
		}

		if (param.getPageSize() <= 0) {
			param.setPageSize(12);
		} else if (param.getPageSize() > 100) {
			param.setPageSize(100);
		}
	}

	/** 컬렉션 번호와 콘텐츠 번호를 담은 조회 키 생성 */
	private CollectionItemVO createKey(int collectionId, int contentId) {
		validateCollectionId(collectionId);
		validateContentId(contentId);

		CollectionItemVO key = new CollectionItemVO();
		key.setCollectionId(collectionId);
		key.setContentId(contentId);

		return key;
	}

	/** 컬렉션 번호가 유효한 양수인지 검증 */
	private void validateCollectionId(int collectionId) {
		if (collectionId <= 0) {
			throw new IllegalArgumentException("올바른 컬렉션 번호가 필요합니다.");
		}
	}

	/** 콘텐츠 번호가 유효한 양수인지 검증 */
	private void validateContentId(int contentId) {
		if (contentId <= 0) {
			throw new IllegalArgumentException("올바른 콘텐츠 번호가 필요합니다.");
		}
	}

}
