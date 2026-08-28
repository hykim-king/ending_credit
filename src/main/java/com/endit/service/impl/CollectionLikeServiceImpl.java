package com.endit.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionLikeItemVO;
import com.endit.domain.CollectionLikeVO;
import com.endit.mapper.CollectionLikeMapper;
import com.endit.service.CollectionLikeService;

/**
 * <pre>
 * Class Name  : CollectionLikeServiceImpl
 * Description : 컬렉션 좋아요의 입력값 검증, 페이징 처리 및 Mapper 호출을 담당하는 Service 구현체
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. gunwoo      최초 생성
 * 2026. 8. 28. jinyoung    중복 등록 및 페이징 처리 보완
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 26.
 */
@Service
@Transactional(readOnly = true)
public class CollectionLikeServiceImpl implements CollectionLikeService {

	private final CollectionLikeMapper collectionLikeMapper;

	/**
	 * CollectionLikeMapper를 주입받아 Service 구현체 생성
	 *
	 * @param collectionLikeMapper 컬렉션 좋아요 Mapper
	 */
	public CollectionLikeServiceImpl(CollectionLikeMapper collectionLikeMapper) {
		this.collectionLikeMapper = collectionLikeMapper;
	}

	@Override
	@Transactional
	public CollectionLikeVO create(int memberId, int collectionId) {
		CollectionLikeVO key = createKey(memberId, collectionId);
		CollectionLikeVO existing = collectionLikeMapper.selectCollectionLike(key);

		if (existing != null) {
			return existing;
		}

		int result;

		try {
			result = collectionLikeMapper.insertCollectionLike(key);
		} catch (DuplicateKeyException duplicate) {
			existing = collectionLikeMapper.selectCollectionLike(key);

			if (existing != null) {
				return existing;
			}

			throw duplicate;
		}

		if (result != 1) {
			throw new IllegalStateException("컬렉션 좋아요 등록에 실패했습니다.");
		}

		CollectionLikeVO created = collectionLikeMapper.selectCollectionLike(key);

		if (created == null) {
			throw new IllegalStateException("등록한 컬렉션 좋아요를 조회할 수 없습니다.");
		}

		return created;
	}

	@Override
	@Transactional
	public void delete(int memberId, int collectionId) {
		CollectionLikeVO existing = get(memberId, collectionId);

		int result = collectionLikeMapper.deleteCollectionLike(existing);

		if (result != 1) {
			throw new IllegalStateException("컬렉션 좋아요 취소에 실패했습니다.");
		}
	}

	@Override
	public CollectionLikeVO get(int memberId, int collectionId) {
		CollectionLikeVO like = collectionLikeMapper.selectCollectionLike(
				createKey(memberId, collectionId));

		if (like == null) {
			throw new NoSuchElementException(
					"좋아요를 누르지 않은 컬렉션입니다. memberId="
					+ memberId + ", collectionId=" + collectionId);
		}

		return like;
	}

	@Override
	public List<CollectionLikeItemVO> retrieveByMember(int memberId, DTO param) {
		validateMemberId(memberId);

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		normalizePaging(param);

		int totalCount = collectionLikeMapper.selectLikedCollectionCountByMember(memberId);
		param.setTotalCnt(totalCount);

		if (totalCount == 0) {
			return Collections.emptyList();
		}

		return collectionLikeMapper.selectLikedCollectionListByMember(
				memberId,
				param.getPageNo(),
				param.getPageSize());
	}

	@Override
	public List<CollectionLikeVO> retrieveByCollection(int collectionId) {
		validateCollectionId(collectionId);

		return collectionLikeMapper.selectCollectionLikeListByCollection(collectionId);
	}

	@Override
	public int countByCollection(int collectionId) {
		validateCollectionId(collectionId);

		return collectionLikeMapper.selectCollectionLikeCount(collectionId);
	}

	/**페이지 번호와 페이지 크기를 허용 범위의 기본값으로 보정*/
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

	/**회원 번호와 컬렉션 번호를 담은 조회 키 생성*/
	private CollectionLikeVO createKey(int memberId, int collectionId) {
		validateMemberId(memberId);
		validateCollectionId(collectionId);

		CollectionLikeVO key = new CollectionLikeVO();
		key.setMemberId(memberId);
		key.setCollectionId(collectionId);

		return key;
	}

	/**회원 번호가 유효한 양수인지 검증*/
	private void validateMemberId(int memberId) {
		if (memberId <= 0) {
			throw new IllegalArgumentException("올바른 회원 번호가 필요합니다.");
		}
	}

	/**컬렉션 번호가 유효한 양수인지 검증*/
	private void validateCollectionId(int collectionId) {
		if (collectionId <= 0) {
			throw new IllegalArgumentException("올바른 컬렉션 번호가 필요합니다.");
		}
	}
}
