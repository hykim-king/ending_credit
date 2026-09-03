package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * 2026. 9. 2.  eunhu       PersonService.getFilmography 삭제에 따라 참여작 빈 목록 검증 이관
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

	// POL-033이 정한 크레딧 역할 4종
	private static final String ROLE_ACTOR = "ACTOR";
	private static final String ROLE_DIRECTOR = "DIRECTOR";
	private static final String ROLE_WRITER = "WRITER";
	private static final String ROLE_PRODUCER = "PRODUCER";
	private static final String UNKNOWN_ROLE = "STUNT";

	// 역할 필터를 싣는 searchMap 키
	private static final String SEARCH_KEY_ROLE = "role";

	private static final int DEFAULT_DISPLAY_ORDER = 0;

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

	/** 매퍼의 역할 우선순위 정렬로 감독이 앞에 오는지 검증 */
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

	/** 역할 4종을 역순으로 심어도 매퍼의 CASE 정렬이 우선순위대로 돌려주는지 검증 - POL-033 */
	@Test
	@DisplayName("전체 목록은 감독 > 배우 > 각본 > 제작 순")
	void retrieveAllOrdersByRolePriority() {
		int contentId = createContentId();
		createCredit(contentId, ROLE_PRODUCER);
		createCredit(contentId, ROLE_WRITER);
		createCredit(contentId, ROLE_ACTOR);
		createCredit(contentId, ROLE_DIRECTOR);

		List<ContentCreditVO> result = contentCreditService.retrieveAll(contentId);

		assertEquals(4, result.size());
		assertEquals(ROLE_DIRECTOR, result.get(0).getRole());
		assertEquals(ROLE_ACTOR, result.get(1).getRole());
		assertEquals(ROLE_WRITER, result.get(2).getRole());
		assertEquals(ROLE_PRODUCER, result.get(3).getRole());
	}

	/** 같은 역할 안에서는 DISPLAY_ORDER가 작은 쪽이 먼저 오는지 검증 */
	@Test
	@DisplayName("같은 역할 안에서는 표시순서가 앞선다")
	void retrieveAllOrdersByDisplayOrderWithinRole() {
		int contentId = createContentId();
		ContentCreditVO second = createCredit(contentId, ROLE_ACTOR, 1);
		ContentCreditVO first = createCredit(contentId, ROLE_ACTOR, 0);

		List<ContentCreditVO> result = contentCreditService.retrieveAll(contentId);

		assertEquals(2, result.size());
		assertEquals(first.getCreditId(), result.get(0).getCreditId());
		assertEquals(second.getCreditId(), result.get(1).getCreditId());
	}

	/** C-03 역할 필터(API-007) - searchMap의 role이 목록과 전체 건수에 모두 걸리는지 검증 */
	@Test
	@DisplayName("역할 필터는 목록과 전체 건수에 함께 걸린다")
	void retrieveFiltersByRole() {
		int contentId = createContentId();
		createCredit(contentId, ROLE_ACTOR);
		createCredit(contentId, ROLE_ACTOR);
		createCredit(contentId, ROLE_DIRECTOR);

		DTO param = new DTO();
		param.getSearchMap().put(SEARCH_KEY_ROLE, ROLE_ACTOR);

		List<ContentCreditVO> result = contentCreditService.retrieve(contentId, param);

		assertEquals(2, result.size());
		assertEquals(ROLE_ACTOR, result.get(0).getRole());
		assertEquals(ROLE_ACTOR, result.get(1).getRole());
		// CROSS JOIN 카운트도 같은 WHERE를 타므로 필터가 반영돼야 한다
		assertEquals(2, param.getTotalCnt());
	}

	/** 모르는 역할이 조용히 0건으로 나가지 않고 막히는지 검증 - 두 조회 축 모두 */
	@Test
	@DisplayName("허용값 밖 역할은 두 축 모두에서 거부")
	void retrieveRejectsUnknownRole() {
		int contentId = createContentId();
		ContentCreditVO created = createCredit(contentId, ROLE_ACTOR);

		DTO byContent = new DTO();
		byContent.getSearchMap().put(SEARCH_KEY_ROLE, UNKNOWN_ROLE);
		assertThrows(IllegalArgumentException.class,
				() -> contentCreditService.retrieve(contentId, byContent));

		DTO byPerson = new DTO();
		byPerson.getSearchMap().put(SEARCH_KEY_ROLE, UNKNOWN_ROLE);
		assertThrows(IllegalArgumentException.class,
				() -> contentCreditService.retrieveByPerson(created.getPersonId(), byPerson));
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

	/** 참여작이 없는 인물의 반환값 검증 - P-01이 빈 목록에서 더보기를 띄우지 않아야 한다 */
	@Test
	@DisplayName("참여작이 없으면 빈 목록에 총건수 0")
	void retrieveByPersonEmpty() {
		int personId = createPersonId();

		DTO param = new DTO();
		List<ContentCreditVO> result = contentCreditService.retrieveByPerson(personId, param);

		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
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
		return createCredit(contentId, role, DEFAULT_DISPLAY_ORDER);
	}

	/** 테스트용 인물을 등록하고 번호를 돌려준다. DB에는 TMDB 원본 경로만 저장된다 */
	private int createPersonId() {
		PersonVO person = new PersonVO();
		person.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		person.setNameKo("크레딧 테스트 인물");
		person.setNameOrg("Credit Test Person");
		person.setProfileImageUrl(SAMPLE_PATH);
		personMapper.doSave(person);

		return person.getPersonId();
	}

	/** 표시순서까지 지정해 크레딧을 만든다. 같은 역할 안의 정렬을 검증할 때 쓴다 */
	private ContentCreditVO createCredit(int contentId, String role, int displayOrder) {
		ContentCreditVO credit = new ContentCreditVO();
		credit.setPersonId(createPersonId());
		credit.setRole(role);
		credit.setCharacter(ROLE_ACTOR.equals(role) ? "테스트 배역" : null);
		credit.setDisplayOrder(displayOrder);

		return contentCreditService.create(contentId, credit);
	}

}
