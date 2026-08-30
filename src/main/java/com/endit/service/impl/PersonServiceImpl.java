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
import com.endit.mapper.ContentCreditMapper;
import com.endit.mapper.PersonMapper;
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
	private static final int FILMOGRAPHY_SIZE = 50;

	private final PersonMapper personMapper;
	private final ContentCreditMapper contentCreditMapper;

	public PersonServiceImpl(PersonMapper personMapper, ContentCreditMapper contentCreditMapper) {
		this.personMapper = personMapper;
		this.contentCreditMapper = contentCreditMapper;
	}

	@Override
	public PersonVO get(int personId) {
		PersonVO param = new PersonVO();
		param.setPersonId(personId);
		PersonVO person = personMapper.doSelectOne(param);
		log.debug("get personId={} found={}", personId, person != null);
		return person;
	}

	@Override
	public List<PersonVO> retrieve(DTO param) {
		DTO search = normalize(param);
		List<PersonVO> list = personMapper.doRetrieve(search);
		log.debug("retrieve size={}", list == null ? 0 : list.size());
		return list != null ? list : Collections.emptyList();
	}

	@Override
	public List<ContentCreditVO> getFilmography(int personId) {
		// ContentCreditMapper: searchDiv 20 = person_id
		DTO param = new DTO();
		param.setSearchDiv("20");
		param.setSearchWord(String.valueOf(personId));
		param.setPageNo(DEFAULT_PAGE_NO);
		param.setPageSize(FILMOGRAPHY_SIZE);

		List<ContentCreditVO> list = contentCreditMapper.doRetrieve(param);
		log.debug("getFilmography personId={} size={}", personId, list == null ? 0 : list.size());
		return list != null ? list : Collections.emptyList();
	}

	private DTO normalize(DTO search) {
		DTO param = search != null ? search : new DTO();
		if (param.getPageNo() <= 0) {
			param.setPageNo(DEFAULT_PAGE_NO);
		}
		if (param.getPageSize() <= 0) {
			param.setPageSize(DEFAULT_PAGE_SIZE);
		}
		if (param.getSearchWord() != null) {
			param.setSearchWord(param.getSearchWord().trim());
		}
		return param;
	}
}
