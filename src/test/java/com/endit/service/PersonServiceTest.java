package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.ContentVO;
import com.endit.domain.PersonVO;
import com.endit.mapper.PersonMapper;

/**
 * <pre>
 * Class Name  : PersonServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 인물 Service를 검증하는 통합 테스트
 *               인물 프로필과 참여작 이미지는 축마다 크기가 다르므로(w300 / w185 / w500),
 *               각 URL이 어느 크기로 완성되는지가 이 테스트의 핵심 검증 대상이다.
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 31. eunhu       최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author eunhu
 * @since 2026. 8. 31.
 */
@SpringBootTest
@Transactional
@DisplayName("PersonService 통합 테스트")
class PersonServiceTest {

	private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";
	private static final String PERSON_PROFILE_PREFIX = TMDB_IMAGE_BASE_URL + "w300/";
	private static final String CREDIT_PROFILE_PREFIX = TMDB_IMAGE_BASE_URL + "w185/";
	private static final String POSTER_PREFIX = TMDB_IMAGE_BASE_URL + "w500/";

	private static final String SAMPLE_PATH = "/sample-profile.jpg";
	private static final String SEARCH_BY_NAME_KO = "10";
	private static final String ROLE_ACTOR = "ACTOR";

	private static final int OVER_MAX_PAGE_SIZE = 500;
	private static final int MAX_PAGE_SIZE = 100;
	private static final int NOT_FOUND_PERSON_ID = 0;

	@Autowired
	private PersonService personService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private ContentCreditService contentCreditService;

	@Autowired
	private PersonMapper personMapper;

	/** 단건 조회 결과의 프로필 경로가 인물 상세용 크기로 완성되는지 검증 */
	@Test
	@DisplayName("단건 조회의 프로필 경로가 w300 풀 URL로 완성")
	void get() {
		int personId = createPersonId("프로필 변환 인물");

		PersonVO result = personService.get(personId);

		assertEquals(PERSON_PROFILE_PREFIX + "sample-profile.jpg", result.getProfileImageUrl());
	}

	/** 없는 인물 번호에 대한 반환값 검증 - 컨트롤러가 404로 바꾸므로 null 계약을 유지한다 */
	@Test
	@DisplayName("존재하지 않는 인물이면 null 반환")
	void getNotFound() {
		assertNull(personService.get(NOT_FOUND_PERSON_ID));
	}

	/** 목록 조회 결과의 프로필 변환과 전체 건수 설정 검증 */
	@Test
	@DisplayName("목록 조회 시 프로필 변환과 전체 건수 설정")
	void retrieve() {
		String nameKo = "목록검색-" + UUID.randomUUID();
		createPersonId(nameKo);

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_BY_NAME_KO);
		param.setSearchWord(nameKo);

		List<PersonVO> result = personService.retrieve(param);

		assertEquals(1, result.size());
		assertTrue(result.get(0).getProfileImageUrl().startsWith(PERSON_PROFILE_PREFIX));
		assertEquals(1, param.getTotalCnt());
	}

	/** 허용 범위를 넘는 페이지 크기의 상한 보정 검증 */
	@Test
	@DisplayName("페이지 크기가 상한을 넘으면 100으로 보정")
	void retrieveOverMaxPageSize() {
		DTO param = new DTO();
		param.setPageSize(OVER_MAX_PAGE_SIZE);

		personService.retrieve(param);

		assertEquals(MAX_PAGE_SIZE, param.getPageSize());
	}

	/** 검색 결과가 없을 때의 반환값과 전체 건수 검증 */
	@Test
	@DisplayName("조회 결과가 없으면 빈 목록 반환")
	void retrieveEmpty() {
		DTO param = new DTO();
		param.setSearchDiv(SEARCH_BY_NAME_KO);
		param.setSearchWord("존재하지않는인물-" + UUID.randomUUID());

		List<PersonVO> result = personService.retrieve(param);

		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	/**
	 * 참여작 목록의 이미지가 크레딧 축 크기로 완성되는지 검증.
	 * 인물 상세(w300)와 달리 참여작 카드의 프로필은 w185, 포스터는 w500이다
	 */
	@Test
	@DisplayName("참여작 목록의 프로필은 w185, 포스터는 w500으로 완성")
	void getFilmography() {
		int personId = createPersonId("참여작 인물");
		int contentId = createContentId();
		createCredit(contentId, personId);

		List<ContentCreditVO> result = personService.getFilmography(personId);

		assertFalse(result.isEmpty());
		assertTrue(result.get(0).getProfileImageUrl().startsWith(CREDIT_PROFILE_PREFIX));
		assertTrue(result.get(0).getPosterUrl().startsWith(POSTER_PREFIX));
	}

	/** 참여작이 없는 인물에 대한 반환값 검증 */
	@Test
	@DisplayName("참여작이 없으면 빈 목록 반환")
	void getFilmographyEmpty() {
		int personId = createPersonId("참여작 없는 인물");

		assertTrue(personService.getFilmography(personId).isEmpty());
	}

	/** 테스트용 인물을 등록하고 번호를 돌려준다. DB에는 TMDB 원본 경로만 저장된다 */
	private int createPersonId(String nameKo) {
		PersonVO person = new PersonVO();
		person.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		person.setNameKo(nameKo);
		person.setNameOrg("Test Person");
		person.setProfileImageUrl(SAMPLE_PATH);

		personMapper.doSave(person);

		return person.getPersonId();
	}

	/** 참여작 검증에 쓸 콘텐츠를 등록하고 번호를 돌려준다 */
	private int createContentId() {
		ContentVO content = new ContentVO();
		content.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		content.setTitleKo("인물 테스트 영화");
		content.setTitleOrg("Person Test Movie");
		content.setOverview("통합 테스트용 줄거리");
		content.setReleaseYear("2026-01-01");
		content.setRuntimeMin(120);
		content.setCountry("US");
		content.setPosterUrl(SAMPLE_PATH);
		content.setBackdropUrl(SAMPLE_PATH);

		return contentService.create(content).getContentId();
	}

	/** 인물을 콘텐츠에 배우로 연결한다 */
	private void createCredit(int contentId, int personId) {
		ContentCreditVO credit = new ContentCreditVO();
		credit.setPersonId(personId);
		credit.setRole(ROLE_ACTOR);
		credit.setCharacter("테스트 배역");
		credit.setDisplayOrder(0);

		contentCreditService.create(contentId, credit);
	}

}
