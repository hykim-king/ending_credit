package com.endit.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

	// 외부 ID 중복 검사 - POL-034의 UK_PERSON_EXTERNAL을 저장 전에 확인한다
	@Override
	public boolean hasExternalId(String externalId) {
		if (!StringUtils.hasText(externalId)) {
			throw new IllegalArgumentException("외부 ID가 필요합니다.");
		}

		return personMapper.findPersonIdByExternal(externalId.trim()) != null;
	}

	// 인물 등록
	@Override
	@Transactional
	public PersonVO create(PersonVO param) {
		if (param == null) {
			throw new IllegalArgumentException("등록할 인물 정보가 필요합니다.");
		}

		validateName(param.getNameKo(), param.getNameOrg());
		normalizeExternalId(param);

		// 외부 ID는 선택이다. TMDB에 없는 인물도 AD-06에서 수기로 등록할 수 있어야 한다
		if (StringUtils.hasText(param.getExternalId()) && hasExternalId(param.getExternalId())) {
			throw new IllegalStateException("이미 등록된 외부 ID입니다. externalId=" + param.getExternalId());
		}

		// 화면이 돌려보낸 완성 URL을 그대로 저장하면 크기 구간까지 DB에 박힌다
		param.setProfileImageUrl(contentImageService.toStoredPath(param.getProfileImageUrl()));

		int result = personMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("인물 등록에 실패했습니다.");
		}

		// doSave의 selectKey가 채번한 personId를 param이 그대로 들고 있다
		return get(param.getPersonId());
	}

	// 인물 수정 - doUpdate가 모든 컬럼을 무조건 SET하므로 비워 둔 필드는 기존 값으로 메운다.
	// 그러지 않으면 이름 하나만 고쳐도 사진과 외부 ID가 NULL로 덮인다
	@Override
	@Transactional
	public PersonVO update(int personId, PersonVO param) {
		PersonVO stored = getStored(personId);

		if (param == null) {
			throw new IllegalArgumentException("수정할 인물 정보가 필요합니다.");
		}

		param.setPersonId(personId);

		if (!StringUtils.hasText(param.getNameKo())) {
			param.setNameKo(stored.getNameKo());
		}

		if (!StringUtils.hasText(param.getNameOrg())) {
			param.setNameOrg(stored.getNameOrg());
		}

		// 기존 값을 메운 뒤에 판정한다. 이름 한쪽만 보내는 수정이 정상이기 때문이다
		validateName(param.getNameKo(), param.getNameOrg());
		normalizeExternalId(param);

		if (!StringUtils.hasText(param.getExternalId())) {
			param.setExternalId(stored.getExternalId());
		} else {
			validateExternalIdOwner(param.getExternalId(), personId);
		}

		if (!StringUtils.hasText(param.getProfileImageUrl())) {
			param.setProfileImageUrl(stored.getProfileImageUrl());
		} else {
			param.setProfileImageUrl(contentImageService.toStoredPath(param.getProfileImageUrl()));
		}

		int result = personMapper.doUpdate(param);

		if (result != 1) {
			throw new IllegalStateException("인물 수정에 실패했습니다.");
		}

		return get(personId);
	}

	// DB 원본 그대로 읽는다. get()은 풀 URL을 돌려주므로 저장 값을 읽는 데 쓰면 안 된다
	private PersonVO getStored(int personId) {
		PersonVO key = new PersonVO();
		key.setPersonId(personId);

		PersonVO person = personMapper.doSelectOne(key);

		if (person == null) {
			throw new NoSuchElementException("존재하지 않는 인물입니다. personId=" + personId);
		}

		return person;
	}

	// POL-034 - 국문명과 원문명 중 하나 이상이면 된다. 둘 다 요구하지 않는다
	private void validateName(String nameKo, String nameOrg) {
		if (!StringUtils.hasText(nameKo) && !StringUtils.hasText(nameOrg)) {
			throw new IllegalArgumentException("국문명과 원문명 중 하나는 필요합니다.");
		}
	}

	// 조회 키와 저장 값이 어긋나지 않도록 공백을 먼저 턴다
	private void normalizeExternalId(PersonVO param) {
		if (StringUtils.hasText(param.getExternalId())) {
			param.setExternalId(param.getExternalId().trim());
		}
	}

	// 수정에서는 hasExternalId를 쓸 수 없다. 바꾸지 않은 자기 외부 ID까지 중복으로 잡는다
	private void validateExternalIdOwner(String externalId, int personId) {
		Integer owner = personMapper.findPersonIdByExternal(externalId);

		if (owner != null && owner != personId) {
			throw new IllegalStateException("이미 등록된 외부 ID입니다. externalId=" + externalId);
		}
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
