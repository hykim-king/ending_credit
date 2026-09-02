package com.endit.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.PersonLikeVO;
import com.endit.mapper.PersonLikeMapper;
import com.endit.service.PersonLikeService;

/**
 * <pre>
 * Class Name  : PersonLikeServiceImpl
 * Description : 인물 좋아요 등록·해제, 상태 확인, 건수 및 회원별 목록 조회 처리
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@Service
@Transactional(readOnly = true)
public class PersonLikeServiceImpl implements PersonLikeService {

	private static final String SEARCH_MEMBER = "10";
	private static final String SEARCH_PERSON = "20";
	private static final String SORT_LATEST = "latest";

	private final PersonLikeMapper personLikeMapper;

	/**
	 * PersonLikeMapper를 주입받아 Service 구현체 생성
	 *
	 * @param personLikeMapper 인물 좋아요 Mapper
	 */
	public PersonLikeServiceImpl(PersonLikeMapper personLikeMapper) {
		this.personLikeMapper = personLikeMapper;
	}

	@Override
	public List<PersonLikeVO> retrieveLikes(int memberId, DTO param, String sort) {
		validateMemberId(memberId);
		validateRetrieveParam(param, sort);
		normalizePaging(param);

		param.setSearchDiv(SEARCH_MEMBER);
		param.setSearchWord(String.valueOf(memberId));

		int totalCount = personLikeMapper.count(param);
		param.setTotalCnt(totalCount);

		if (totalCount == 0) {
			return Collections.emptyList();
		}

		return personLikeMapper.doRetrieve(param);
	}

	@Override
	public int countLikes(int personId) {
		validatePersonId(personId);

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_PERSON);
		param.setSearchWord(String.valueOf(personId));

		return personLikeMapper.count(param);
	}

	@Override
	public boolean isLiked(int memberId, int personId) {
		return find(memberId, personId) != null;
	}

	@Override
	@Transactional
	public PersonLikeVO addLike(int memberId, int personId) {
		PersonLikeVO personLike = find(memberId, personId);

		// 이미 등록된 좋아요는 중복 INSERT 없이 기존 결과를 반환한다.
		if (personLike != null) {
			return personLike;
		}

		PersonLikeVO newPersonLike = new PersonLikeVO(memberId, personId, null);
		requireOneRow(personLikeMapper.doSave(newPersonLike), "인물 좋아요 등록에 실패했습니다.");

		return findRequired(memberId, personId);
	}

	@Override
	@Transactional
	public void deleteLike(int memberId, int personId) {
		PersonLikeVO personLike = find(memberId, personId);

		// 행이 없거나 이미 해제된 요청은 최종 상태가 같으므로 성공으로 처리한다.
		if (personLike == null) {
			return;
		}

		requireOneRow(personLikeMapper.doDelete(personLike), "인물 좋아요 해제에 실패했습니다.");
	}

	/**
	 * 회원 번호와 인물 번호로 좋아요 정보 조회
	 */
	private PersonLikeVO find(int memberId, int personId) {
		validateMemberId(memberId);
		validatePersonId(personId);

		return personLikeMapper.doSelectOne(new PersonLikeVO(memberId, personId, null));
	}

	/**
	 * 등록 이후 좋아요 정보를 다시 조회, 조회되지 않으면 저장 실패 처리
	 */
	private PersonLikeVO findRequired(int memberId, int personId) {
		PersonLikeVO personLike = find(memberId, personId);

		if (personLike == null) {
			throw new IllegalStateException("저장된 인물 좋아요 정보를 조회할 수 없습니다.");
		}

		return personLike;
	}

	/**
	 * 목록 조회 객체와 정렬 조건 검증
	 */
	private void validateRetrieveParam(DTO param, String sort) {
		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		if (sort == null || !SORT_LATEST.equalsIgnoreCase(sort.trim())) {
			throw new IllegalArgumentException("정렬 조건은 latest만 사용할 수 있습니다.");
		}
	}

	/**
	 * 페이지 번호와 페이지 크기에 기본값 및 최댓값 적용
	 */
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

	/**
	 * 회원 번호가 실제 PK로 사용할 수 있는 양수인지 검증
	 */
	private void validateMemberId(int memberId) {
		if (memberId <= 0) {
			throw new IllegalArgumentException("올바른 회원 번호가 필요합니다.");
		}
	}

	/**
	 * 인물 번호가 실제 PK로 사용할 수 있는 양수인지 검증
	 */
	private void validatePersonId(int personId) {
		if (personId <= 0) {
			throw new IllegalArgumentException("올바른 인물 번호가 필요합니다.");
		}
	}

	/**
	 * Mapper의 등록·삭제 결과가 정확히 한 행인지 검증
	 */
	private void requireOneRow(int result, String message) {
		if (result != 1) {
			throw new IllegalStateException(message);
		}
	}
}