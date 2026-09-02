package com.endit.service.impl;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.PersonVO;
import com.endit.mapper.PersonMapper;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentImageService;
import com.endit.service.PersonService;

/**
 * 인물 서비스 구현
 * 담당: 이기준
 */
@Service
@Transactional(readOnly = true)
public class PersonServiceImpl implements PersonService {

	private static final Logger log = LoggerFactory.getLogger(PersonServiceImpl.class);
	private static final int DEFAULT_PAGE_NO = 1;
	private static final int DEFAULT_PAGE_SIZE = 12;
	private static final int MAX_PAGE_SIZE = 100;
	private static final int FILMOGRAPHY_SIZE = 50;

	private final PersonMapper personMapper;
	private final ContentCreditService contentCreditService;
	private final ContentImageService contentImageService;

	public PersonServiceImpl(PersonMapper personMapper, ContentCreditService contentCreditService,
			ContentImageService contentImageService) {
		this.personMapper = personMapper;
		this.contentCreditService = contentCreditService;
		this.contentImageService = contentImageService;
	}

	@Override
	public PersonVO get(int personId) {
		PersonVO param = new PersonVO();
		param.setPersonId(personId);
		PersonVO person = personMapper.doSelectOne(param);
		log.debug("get personId={} found={}", personId, person != null);

		if (person == null) {
			return null;
		}

		applyFullImageUrl(person);
		return person;
	}

	@Override
	public List<PersonVO> retrieve(DTO param) {
		DTO search = normalize(param);
		List<PersonVO> list = personMapper.doRetrieve(search);
		log.debug("retrieve size={}", list == null ? 0 : list.size());

		if (list == null || list.isEmpty()) {
			search.setTotalCnt(0);
			return Collections.emptyList();
		}

		// doRetrieve가 CROSS JOIN으로 검색조건까지 걸러낸 총건수를 각 행에 실어 준다
		search.setTotalCnt(list.get(0).getTotalCnt());

		for (PersonVO person : list) {
			applyFullImageUrl(person);
		}

		return list;
	}

	@Override
	public List<ContentCreditVO> getFilmography(int personId) {
		// 이미지 URL 완성까지 ContentCreditService가 책임진다. 매퍼를 직접 부르면 그 변환을 건너뛴다
		DTO param = new DTO();
		param.setPageNo(DEFAULT_PAGE_NO);
		param.setPageSize(FILMOGRAPHY_SIZE);

		List<ContentCreditVO> list = contentCreditService.retrieveByPerson(personId, param);
		log.debug("getFilmography personId={} size={}", personId, list.size());
		return list;
	}

	// TMDB 원본 경로를 화면에 바로 쓸 수 있는 풀 URL로 완성한다.
	// 어떤 크기를 쓸지는 ContentImageServiceImpl이 정하므로 여기서는 용도만 말한다
	private void applyFullImageUrl(PersonVO person) {
		person.setProfileImageUrl(contentImageService.toPersonProfileUrl(person.getProfileImageUrl()));
	}

	private DTO normalize(DTO search) {
		DTO param = search != null ? search : new DTO();
		if (param.getPageNo() <= 0) {
			param.setPageNo(DEFAULT_PAGE_NO);
		}
		if (param.getPageSize() <= 0) {
			param.setPageSize(DEFAULT_PAGE_SIZE);
		} else if (param.getPageSize() > MAX_PAGE_SIZE) {
			param.setPageSize(MAX_PAGE_SIZE);
		}
		if (param.getSearchWord() != null) {
			param.setSearchWord(param.getSearchWord().trim());
		}
		return param;
	}
}
