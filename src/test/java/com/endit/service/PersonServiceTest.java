package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.PersonVO;
import com.endit.mapper.PersonMapper;

/**
 * <pre>
 * Class Name  : PersonServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 인물 Service를 검증하는 통합 테스트
 *               인물 프로필은 참여작 카드(w185)와 크기가 다르므로(w300),
 *               URL이 어느 크기로 완성되는지가 이 테스트의 핵심 검증 대상이다.
 *               참여작 조회는 ContentCreditServiceTest.retrieveByPerson이 맡는다.
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 31. eunhu       최초 생성
 * 2026. 9. 1.  eunhu       등록·수정·외부 ID 중복 검사 테스트 추가
 * 2026. 9. 2.  eunhu       getFilmography 삭제에 따라 참여작 테스트 이관
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

	private static final String SAMPLE_PATH = "/sample-profile.jpg";
	private static final String SEARCH_BY_NAME_KO = "10";

	private static final int OVER_MAX_PAGE_SIZE = 500;
	private static final int MAX_PAGE_SIZE = 100;
	private static final int NOT_FOUND_PERSON_ID = 0;

	@Autowired
	private PersonService personService;

	@Autowired
	private PersonMapper personMapper;

	@Autowired
	private SqlSession sqlSession;

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

	/** 등록 결과가 재조회 계약(풀 URL)으로 돌아오는지 검증 */
	@Test
	@DisplayName("등록하면 채번된 인물이 w300 풀 URL로 반환")
	void create() {
		PersonVO param = new PersonVO();
		param.setNameKo("등록 인물");
		param.setNameOrg("Created Person");
		param.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		param.setProfileImageUrl(SAMPLE_PATH);

		PersonVO result = personService.create(param);

		assertTrue(result.getPersonId() > 0);
		assertEquals("등록 인물", result.getNameKo());
		assertEquals(PERSON_PROFILE_PREFIX + "sample-profile.jpg", result.getProfileImageUrl());
	}

	/** POL-034 - 국문명과 원문명이 모두 비면 등록을 막는다 */
	@Test
	@DisplayName("이름이 둘 다 없으면 등록 실패")
	void createWithoutName() {
		PersonVO param = new PersonVO();
		param.setExternalId(UUID.randomUUID().toString().substring(0, 12));

		assertThrows(IllegalArgumentException.class, () -> personService.create(param));
	}

	// PERSON.EXTERNAL_ID가 NOT NULL이라 서비스가 먼저 막는다.
	// 여기서 안 막으면 INSERT에서 ORA-01400이 나 500이 된다 (docs/known-issues.md)
	@Test
	@DisplayName("외부 ID가 없으면 등록 실패")
	void createWithoutExternalId() {
		PersonVO param = new PersonVO();
		param.setNameKo("외부ID 없는 인물");
		param.setNameOrg("No External Person");

		assertThrows(IllegalArgumentException.class, () -> personService.create(param));
	}

	// PERSON.NAME_ORG도 NOT NULL이라 같은 이유로 막는다
	@Test
	@DisplayName("원문명이 없으면 등록 실패")
	void createWithoutNameOrg() {
		PersonVO param = new PersonVO();
		param.setNameKo("원문명 없는 인물");
		param.setExternalId(UUID.randomUUID().toString().substring(0, 12));

		assertThrows(IllegalArgumentException.class, () -> personService.create(param));
	}

	/** POL-034 UK_PERSON_EXTERNAL - 이미 쓰인 외부 ID는 막는다 */
	@Test
	@DisplayName("외부 ID가 중복이면 등록 실패")
	void createDuplicateExternalId() {
		String externalId = UUID.randomUUID().toString().substring(0, 12);

		PersonVO first = new PersonVO();
		first.setNameKo("선점 인물");
		first.setNameOrg("First Person");
		first.setExternalId(externalId);
		personService.create(first);

		PersonVO second = new PersonVO();
		second.setNameKo("중복 인물");
		second.setNameOrg("Second Person");
		second.setExternalId(externalId);

		assertThrows(IllegalStateException.class, () -> personService.create(second));
	}

	/**
	 * 화면이 돌려보낸 완성 URL이 그대로 저장되지 않는지 검증.
	 * 저장돼 버리면 다음 조회에서 접두사가 겹친다
	 */
	@Test
	@DisplayName("완성 URL로 등록해도 DB에는 원본 경로만 저장")
	void createReversesFullImageUrl() {
		PersonVO param = new PersonVO();
		param.setNameKo("URL 역변환 인물");
		param.setNameOrg("Url Person");
		param.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		param.setProfileImageUrl(PERSON_PROFILE_PREFIX + "sample-profile.jpg");

		int personId = personService.create(param).getPersonId();

		assertEquals(SAMPLE_PATH, findStoredProfilePath(personId));
	}

	/**
	 * doUpdate가 모든 컬럼을 무조건 SET하므로, 안 채운 필드가 NULL로 덮이지 않는지 검증.
	 * ContentImage·ContentCredit에서 이미 두 번 났던 결함이다
	 */
	@Test
	@DisplayName("이름만 수정해도 프로필과 외부 ID가 유지")
	void updateKeepsUnsetFields() {
		String externalId = UUID.randomUUID().toString().substring(0, 12);

		PersonVO created = new PersonVO();
		created.setNameKo("수정 전 이름");
		created.setNameOrg("Before Update");
		created.setExternalId(externalId);
		created.setProfileImageUrl(SAMPLE_PATH);

		int personId = personService.create(created).getPersonId();

		PersonVO param = new PersonVO();
		param.setNameKo("수정 후 이름");

		PersonVO result = personService.update(personId, param);

		assertEquals("수정 후 이름", result.getNameKo());
		assertEquals("Before Update", result.getNameOrg());
		assertEquals(externalId, result.getExternalId());
		assertEquals(PERSON_PROFILE_PREFIX + "sample-profile.jpg", result.getProfileImageUrl());
	}

	/**
	 * 자기 외부 ID를 그대로 되보내는 수정이 중복으로 걸리지 않는지 검증.
	 * hasExternalId로 판정하면 자기 자신에게 걸린다
	 */
	@Test
	@DisplayName("자기 외부 ID를 그대로 보내는 수정은 통과")
	void updateWithOwnExternalId() {
		String externalId = UUID.randomUUID().toString().substring(0, 12);

		PersonVO created = new PersonVO();
		created.setNameKo("자기 외부ID 인물");
		created.setNameOrg("Own External Person");
		created.setExternalId(externalId);

		int personId = personService.create(created).getPersonId();

		PersonVO param = new PersonVO();
		param.setNameKo("이름 변경");
		param.setExternalId(externalId);

		assertEquals(externalId, personService.update(personId, param).getExternalId());
	}

	/** 다른 인물이 이미 쓰는 외부 ID로는 수정할 수 없다 */
	@Test
	@DisplayName("남의 외부 ID로 수정하면 실패")
	void updateDuplicateExternalId() {
		String takenId = UUID.randomUUID().toString().substring(0, 12);

		PersonVO owner = new PersonVO();
		owner.setNameKo("외부ID 선점 인물");
		owner.setNameOrg("Owner Person");
		owner.setExternalId(takenId);
		personService.create(owner);

		PersonVO other = new PersonVO();
		other.setNameKo("수정 대상 인물");
		other.setNameOrg("Other Person");
		other.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		int personId = personService.create(other).getPersonId();

		PersonVO param = new PersonVO();
		param.setExternalId(takenId);

		assertThrows(IllegalStateException.class, () -> personService.update(personId, param));
	}

	/** 없는 인물의 수정은 조회 단계에서 막힌다 */
	@Test
	@DisplayName("존재하지 않는 인물 수정은 실패")
	void updateNotFound() {
		PersonVO param = new PersonVO();
		param.setNameKo("이름");

		assertThrows(NoSuchElementException.class,
				() -> personService.update(NOT_FOUND_PERSON_ID, param));
	}

	/** 외부 ID 중복 검사(POL-034)의 참·거짓 검증 */
	@Test
	@DisplayName("등록된 외부 ID만 true 반환")
	void hasExternalId() {
		String externalId = UUID.randomUUID().toString().substring(0, 12);

		PersonVO param = new PersonVO();
		param.setNameKo("외부ID 검사 인물");
		param.setNameOrg("Has External Person");
		param.setExternalId(externalId);
		personService.create(param);

		assertTrue(personService.hasExternalId(externalId));
		assertFalse(personService.hasExternalId("NOT_EXISTS_" + UUID.randomUUID()));
	}

	/** 빈 외부 ID는 검사 대상이 아니다 */
	@Test
	@DisplayName("외부 ID가 비면 검사 실패")
	void hasExternalIdBlank() {
		assertThrows(IllegalArgumentException.class, () -> personService.hasExternalId(" "));
	}

	// DB 원본 경로를 읽는다 - 캐시를 비우지 않으면 create가 제자리에서 바꾼 VO가 그대로 돌아온다
	private String findStoredProfilePath(int personId) {
		// MyBatis 1차 캐시는 같은 트랜잭션에서 같은 인스턴스를 돌려주는데,
		// PersonServiceImpl.get이 그 인스턴스의 URL을 풀 URL로 바꿔 놓는다
		sqlSession.clearCache();

		PersonVO key = new PersonVO();
		key.setPersonId(personId);

		return personMapper.doSelectOne(key).getProfileImageUrl();
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

}
