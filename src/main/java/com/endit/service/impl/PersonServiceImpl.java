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
import com.endit.domain.PersonVO;
import com.endit.mapper.PersonMapper;
import com.endit.service.ContentImageService;
import com.endit.service.PersonService;

// PERSON 단건·목록·등록·수정 - 참여작은 ContentCreditService.retrieveByPerson 담당
@Service
@Transactional(readOnly = true)
public class PersonServiceImpl implements PersonService {

	private static final Logger log = LoggerFactory.getLogger(PersonServiceImpl.class);
	private static final int DEFAULT_PAGE_NO = 1;
	private static final int DEFAULT_PAGE_SIZE = 12;
	private static final int MAX_PAGE_SIZE = 100;

	// LIKE 이스케이프 - PersonMapper의 ESCAPE '\'와 짝이다
	private static final String LIKE_ESCAPE_CHAR = "\\";
	private static final String SEARCH_BY_EXTERNAL_ID = "30";

	private final PersonMapper personMapper;
	private final ContentImageService contentImageService;

	public PersonServiceImpl(PersonMapper personMapper, ContentImageService contentImageService) {
		this.personMapper = personMapper;
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

		// 이스케이프한 검색어는 매퍼에만 넘기고 호출부 DTO는 원래 값으로 되돌린다
		String rawSearchWord = search.getSearchWord();
		search.setSearchWord(toEscapedSearchWord(search));

		List<PersonVO> list;

		try {
			list = personMapper.doRetrieve(search);
		} finally {
			search.setSearchWord(rawSearchWord);
		}

		log.debug("retrieve size={}", list == null ? 0 : list.size());

		if (list == null || list.isEmpty()) {
			search.setTotalCnt(0);
			return Collections.emptyList();
		}

		// doRetrieve가 필터된 총건수를 각 행에 실어 준다
		search.setTotalCnt(list.get(0).getTotalCnt());

		for (PersonVO person : list) {
			applyFullImageUrl(person);
		}

		return list;
	}

	// AD-06 외부ID 중복 검사 - UK_PERSON_EXTERNAL을 저장 전에 확인한다
	@Override
	public boolean hasExternalId(String externalId) {
		if (!StringUtils.hasText(externalId)) {
			throw new IllegalArgumentException("외부 ID가 필요합니다.");
		}

		return personMapper.findPersonIdByExternal(externalId.trim()) != null;
	}

	// AD-06 인물 등록
	@Override
	@Transactional
	public PersonVO create(PersonVO param) {
		if (param == null) {
			throw new IllegalArgumentException("등록할 인물 정보가 필요합니다.");
		}

		validateName(param.getNameKo(), param.getNameOrg());
		normalizeExternalId(param);
		validateRequiredForInsert(param);

		if (hasExternalId(param.getExternalId())) {
			throw new IllegalStateException("이미 등록된 외부 ID입니다. externalId=" + param.getExternalId());
		}

		// 완성 URL을 그대로 저장하면 크기 구간까지 DB에 박힌다
		param.setProfileImageUrl(contentImageService.toStoredPath(param.getProfileImageUrl()));

		int result = personMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("인물 등록에 실패했습니다.");
		}

		// selectKey가 채번한 personId를 param이 들고 있다
		return get(param.getPersonId());
	}

	// AD-06 인물 수정 - doUpdate가 전 컬럼을 SET하므로 빈 필드를 기존 값으로 메운다(안 그러면 NULL로 덮인다)
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

		// 이름 한쪽만 보내는 수정이 정상이라 기존 값을 메운 뒤에 판정한다
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

	// DB 원본 그대로 - get()은 풀 URL을 주므로 저장 값 읽기에 쓰면 안 된다
	private PersonVO getStored(int personId) {
		PersonVO key = new PersonVO();
		key.setPersonId(personId);

		PersonVO person = personMapper.doSelectOne(key);

		if (person == null) {
			throw new NoSuchElementException("존재하지 않는 인물입니다. personId=" + personId);
		}

		return person;
	}

	// AD-06 등록 필수값 - PERSON.NAME_ORG·EXTERNAL_ID가 NOT NULL이라 막는다(docs/known-issues.md).
	// 여기서 안 막으면 INSERT에서 ORA-01400이 나 500으로 새어 나간다
	private void validateRequiredForInsert(PersonVO param) {
		if (!StringUtils.hasText(param.getNameOrg())) {
			throw new IllegalArgumentException("원문명은 필수입니다.");
		}

		if (!StringUtils.hasText(param.getExternalId())) {
			throw new IllegalArgumentException("외부 ID는 필수입니다.");
		}
	}

	// POL-034 - 국문명·원문명 중 하나만 있으면 된다
	private void validateName(String nameKo, String nameOrg) {
		if (!StringUtils.hasText(nameKo) && !StringUtils.hasText(nameOrg)) {
			throw new IllegalArgumentException("국문명과 원문명 중 하나는 필요합니다.");
		}
	}

	// 조회 키와 저장 값이 어긋나지 않게 공백을 턴다
	private void normalizeExternalId(PersonVO param) {
		if (StringUtils.hasText(param.getExternalId())) {
			param.setExternalId(param.getExternalId().trim());
		}
	}

	// 수정은 hasExternalId를 못 쓴다 - 자기 외부ID까지 중복으로 잡힌다
	private void validateExternalIdOwner(String externalId, int personId) {
		Integer owner = personMapper.findPersonIdByExternal(externalId);

		if (owner != null && owner != personId) {
			throw new IllegalStateException("이미 등록된 외부 ID입니다. externalId=" + externalId);
		}
	}

	// AD-05·S-03 검색어의 %·_를 글자로 되돌린다 - 안 그러면 "%" 한 글자에 전 건이 걸린다
	private String toEscapedSearchWord(DTO param) {
		String searchWord = param.getSearchWord();

		// 외부 ID 축은 등호 비교라 이스케이프가 오히려 검색어를 망친다
		if (!StringUtils.hasText(searchWord) || SEARCH_BY_EXTERNAL_ID.equals(param.getSearchDiv())) {
			return searchWord;
		}

		// 역슬래시를 먼저 바꾼다. 나중에 바꾸면 앞서 붙인 이스케이프까지 다시 이스케이프된다
		return searchWord
				.replace(LIKE_ESCAPE_CHAR, LIKE_ESCAPE_CHAR + LIKE_ESCAPE_CHAR)
				.replace("%", LIKE_ESCAPE_CHAR + "%")
				.replace("_", LIKE_ESCAPE_CHAR + "_");
	}

	// TMDB 원본 경로 → 화면용 풀 URL (크기는 ContentImageServiceImpl이 정한다)
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
