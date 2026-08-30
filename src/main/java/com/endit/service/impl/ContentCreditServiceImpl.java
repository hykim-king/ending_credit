package com.endit.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;
import com.endit.mapper.ContentCreditMapper;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentImageService;

@Service
@Transactional(readOnly = true)
public class ContentCreditServiceImpl implements ContentCreditService {

	private static final String SEARCH_BY_CONTENT = "10";
	private static final String ROLE_DIRECTOR = "DIRECTOR";
	// 페이징 없이 "전체 조회"를 흉내낼 때 쓰는 페이지 크기 - 콘텐츠 하나가 가질 수 있는 크레딧 수보다 넉넉하게 잡음
	private static final int RETRIEVE_ALL_PAGE_SIZE = 100;
	// 출연/제작 프로필 이미지 크기 (C-02/C-03)
	private static final String CREDIT_PROFILE_IMAGE_SIZE = "w185";

	private final ContentCreditMapper contentCreditMapper;
	private final ContentImageService contentImageService;

	public ContentCreditServiceImpl(ContentCreditMapper contentCreditMapper, ContentImageService contentImageService) {
		this.contentCreditMapper = contentCreditMapper;
		this.contentImageService = contentImageService;
	}

	// 콘텐츠 하나의 출연/제작진 목록 조회 - CONTENT_CREDIT + PERSON + CONTENT 조인, role/이름/프로필/배역/순서 반환
	@Override
	public List<ContentCreditVO> retrieve(int contentId, DTO param) {
		validateContentId(contentId);

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		normalizePaging(param);
		param.setSearchDiv(SEARCH_BY_CONTENT);
		param.setSearchWord(String.valueOf(contentId));

		List<ContentCreditVO> credits = contentCreditMapper.doRetrieve(param);

		// doRetrieve가 CROSS JOIN으로 content_id까지 걸러낸 총건수를 각 행에 실어 준다.
		param.setTotalCnt(credits.isEmpty() ? 0 : credits.get(0).getTotalCnt());

		for (ContentCreditVO credit : credits) {
			credit.setProfileImageUrl(
					contentImageService.toFullImageUrl(credit.getProfileImageUrl(), CREDIT_PROFILE_IMAGE_SIZE));
		}

		return credits;
	}

	// 콘텐츠 하나의 출연/제작진 전체 목록 조회
	@Override
	public List<ContentCreditVO> retrieveAll(int contentId) {
		DTO param = new DTO();
		param.setPageNo(1);
		param.setPageSize(RETRIEVE_ALL_PAGE_SIZE);
		List<ContentCreditVO> credits = retrieve(contentId, param);

		List<ContentCreditVO> directors = credits.stream()
				.filter(credit -> ROLE_DIRECTOR.equals(credit.getRole()))
				.collect(Collectors.toList());
		List<ContentCreditVO> others = credits.stream()
				.filter(credit -> !ROLE_DIRECTOR.equals(credit.getRole()))
				.collect(Collectors.toList());

		List<ContentCreditVO> result = new ArrayList<>(directors);
		result.addAll(others);
		return result;
	}

	// 크레딧 단건 조회
	@Override
	public ContentCreditVO get(int creditId) {
		ContentCreditVO item = contentCreditMapper.doSelectOne(createKey(creditId));

		if (item == null) {
			throw new NoSuchElementException("존재하지 않는 크레딧입니다. creditId=" + creditId);
		}

		return item;
	}

	// 콘텐츠에 크레딧(배우 또는 감독) 등록
	@Override
	@Transactional
	public ContentCreditVO create(int contentId, ContentCreditVO param) {
		validateContentId(contentId);

		if (param == null) {
			throw new IllegalArgumentException("등록할 크레딧 정보가 필요합니다.");
		}

		param.setContentId(contentId);

		int result = contentCreditMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("크레딧 등록에 실패했습니다.");
		}

		// doSave가 시퀀스로 채번한 creditId를 param이 그대로 들고 있으므로 재조회에 사용한다
		return get(param.getCreditId());
	}

	// 크레딧의 배역명/표시순서 등 수정
	@Override
	@Transactional
	public ContentCreditVO update(int creditId, ContentCreditVO param) {
		get(creditId);

		if (param == null) {
			throw new IllegalArgumentException("수정할 크레딧 정보가 필요합니다.");
		}

		param.setCreditId(creditId);

		int result = contentCreditMapper.doUpdate(param);

		if (result != 1) {
			throw new IllegalStateException("크레딧 수정에 실패했습니다.");
		}

		return get(creditId);
	}

	// 크레딧 삭제
	@Override
	@Transactional
	public void delete(int creditId) {
		ContentCreditVO existing = get(creditId);
		int result = contentCreditMapper.doDelete(existing);

		if (result != 1) {
			throw new IllegalStateException("크레딧 삭제에 실패했습니다.");
		}
	}

	// 페이지 번호와 페이지 크기를 허용 범위의 기본값으로 보정
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

	// 크레딧 번호를 담은 조회 키 생성
	private ContentCreditVO createKey(int creditId) {
		validateCreditId(creditId);

		ContentCreditVO key = new ContentCreditVO();
		key.setCreditId(creditId);

		return key;
	}

	// 콘텐츠 번호가 유효한 양수인지 검증
	private void validateContentId(int contentId) {
		if (contentId <= 0) {
			throw new IllegalArgumentException("올바른 콘텐츠 번호가 필요합니다.");
		}
	}

	// 크레딧 번호가 유효한 양수인지 검증
	private void validateCreditId(int creditId) {
		if (creditId <= 0) {
			throw new IllegalArgumentException("올바른 크레딧 번호가 필요합니다.");
		}
	}

}
