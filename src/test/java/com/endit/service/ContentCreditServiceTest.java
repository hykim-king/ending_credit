package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Class Name  : ContentCreditServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 크레딧 Service를 검증하는 통합 테스트
 *               크레딧은 인물 프로필(w185)과 작품 포스터(w500) 두 이미지를 함께 내려주므로
 *               두 URL이 모두 완성되는지, 수정 시 소속이 유지되는지가 핵심 검증 대상이다.
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
@DisplayName("ContentCreditService 통합 테스트")
class ContentCreditServiceTest {

	private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";
	private static final String CREDIT_PROFILE_PREFIX = TMDB_IMAGE_BASE_URL + "w185/";
	private static final String POSTER_PREFIX = TMDB_IMAGE_BASE_URL + "w500/";

	private static final String SAMPLE_PATH = "/sample-credit.jpg";
	private static final String ROLE_ACTOR = "ACTOR";
	private static final String ROLE_DIRECTOR = "DIRECTOR";

	@Autowired
	private ContentCreditService contentCreditService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private PersonMapper personMapper;

	/** 목록 조회 결과에 프로필과 포스터 URL이 각각의 크기로 채워지는지 검증 */
	@Test
	@DisplayName("목록 조회 시 프로필과 포스터 URL이 모두 완성")
	void retrieve() {
		int contentId = createContentId();
		createCredit(contentId, ROLE_ACTOR);

		DTO param = new DTO();
		List<ContentCreditVO> result = contentCreditService.retrieve(contentId, param);

		assertEquals(1, result.size());
		assertTrue(result.get(0).getProfileImageUrl().startsWith(CREDIT_PROFILE_PREFIX));
		assertTrue(result.get(0).getPosterUrl().startsWith(POSTER_PREFIX));
		assertEquals(1, param.getTotalCnt());
	}

	/** 감독을 맨 앞으로 재배치하는지 검증 */
	@Test
	@DisplayName("전체 목록 조회는 감독이 앞에 온다")
	void retrieveAll() {
		int contentId = createContentId();
		createCredit(contentId, ROLE_ACTOR);
		createCredit(contentId, ROLE_DIRECTOR);

		List<ContentCreditVO> result = contentCreditService.retrieveAll(contentId);

		assertEquals(2, result.size());
		assertEquals(ROLE_DIRECTOR, result.get(0).getRole());
	}

	/** personId 축 조회가 이미지 변환까지 거치는지 검증 - P-01 참여작 */
	@Test
	@DisplayName("인물 축 조회도 두 URL이 완성")
	void retrieveByPerson() {
		int contentId = createContentId();
		ContentCreditVO created = createCredit(contentId, ROLE_ACTOR);

		DTO param = new DTO();
		List<ContentCreditVO> result = contentCreditService.retrieveByPerson(created.getPersonId(), param);

		assertEquals(1, result.size());
		assertTrue(result.get(0).getProfileImageUrl().startsWith(CREDIT_PROFILE_PREFIX));
		assertTrue(result.get(0).getPosterUrl().startsWith(POSTER_PREFIX));
		assertEquals(1, param.getTotalCnt());
	}

	/**
	 * 매퍼의 doUpdate가 content_id·person_id를 무조건 SET하므로,
	 * 배역명만 고칠 때 소속이 0으로 덮이지 않고 유지되는지 검증
	 */
	@Test
	@DisplayName("수정 시 채우지 않은 소속은 기존 값 유지")
	void updateKeepsOwnership() {
		int contentId = createContentId();
		ContentCreditVO created = createCredit(contentId, ROLE_ACTOR);

		ContentCreditVO param = new ContentCreditVO();
		param.setRole(ROLE_ACTOR);
		param.setCharacter("변경된 배역");
		param.setDisplayOrder(1);

		ContentCreditVO result = contentCreditService.update(created.getCreditId(), param);

		assertEquals(contentId, result.getContentId());
		assertEquals(created.getPersonId(), result.getPersonId());
		assertEquals("변경된 배역", result.getCharacter());
	}

	/** 단건 조회 반환값도 목록과 같은 풀 URL 계약을 따르는지 검증 */
	@Test
	@DisplayName("단건 조회의 이미지 경로가 풀 URL로 완성")
	void get() {
		int contentId = createContentId();
		ContentCreditVO created = createCredit(contentId, ROLE_ACTOR);

		ContentCreditVO result = contentCreditService.get(created.getCreditId());

		assertTrue(result.getProfileImageUrl().startsWith(CREDIT_PROFILE_PREFIX));
		assertTrue(result.getPosterUrl().startsWith(POSTER_PREFIX));
	}

	/** 테스트용 콘텐츠를 등록하고 번호를 돌려준다 */
	private int createContentId() {
		ContentVO content = new ContentVO();
		content.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		content.setTitleKo("크레딧 테스트 영화");
		content.setTitleOrg("Credit Test Movie");
		content.setOverview("통합 테스트용 줄거리");
		content.setReleaseYear("2026-01-01");
		content.setRuntimeMin(120);
		content.setCountry("US");
		content.setPosterUrl(SAMPLE_PATH);
		content.setBackdropUrl(SAMPLE_PATH);

		return contentService.create(content).getContentId();
	}

	/** 새 인물을 만들어 콘텐츠에 연결한다. DB에는 TMDB 원본 경로만 저장된다 */
	private ContentCreditVO createCredit(int contentId, String role) {
		PersonVO person = new PersonVO();
		person.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		person.setNameKo("크레딧 테스트 인물");
		person.setNameOrg("Credit Test Person");
		person.setProfileImageUrl(SAMPLE_PATH);
		personMapper.doSave(person);

		ContentCreditVO credit = new ContentCreditVO();
		credit.setPersonId(person.getPersonId());
		credit.setRole(role);
		credit.setCharacter(ROLE_DIRECTOR.equals(role) ? null : "테스트 배역");
		credit.setDisplayOrder(0);

		return contentCreditService.create(contentId, credit);
	}

}
