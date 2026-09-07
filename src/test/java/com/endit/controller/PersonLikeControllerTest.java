package com.endit.controller;

import static com.endit.support.DatabaseTestFixtures.insertMember;
import static com.endit.support.DatabaseTestFixtures.insertPerson;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.MemberVO;
import com.endit.domain.PersonLikeVO;
import com.endit.domain.PersonVO;
import com.endit.mapper.PersonLikeMapper;
import com.endit.support.SecurityTestContext;

/**
 * <pre>
 * Class Name  : PersonLikeControllerTest
 * Description : 실제 Controller, Service, Mapper와 DB를 사용해 인물 좋아요 REST API를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * 2026. 9. 05. jinyoung    로그인 principal 및 시퀀스 독립 부모 픽스처 적용
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("PersonLikeController 통합 테스트")
class PersonLikeControllerTest {

	private static final int MISSING_PERSON_ID = Integer.MAX_VALUE;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PersonLikeMapper personLikeMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private int memberId;
	private int personId;

	/** 테스트 데이터와 로그인 인증 설정 */
	@BeforeEach
	void setUp() {
		MemberVO member = createMember();
		memberId = member.getMemberId().intValue();
		SecurityTestContext.login(member);
		personId = createPersonId();
	}

	/** 테스트 종료 후 인증 정보 제거 */
	@AfterEach
	void clearAuthentication() {
		SecurityTestContext.clear();
	}

	/**
	 * 회원 인물 좋아요 목록 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("회원 인물 좋아요 목록 반환")
	void retrieveLikes() throws Exception {
		savePersonLike();

		// URL에 회원 번호가 없는 본인 전용 API가 로그인 회원의 좋아요만 반환해야 한다.
		mockMvc.perform(get("/api/members/likes")
					.param("type", "person")
					.param("page", "1")
					.param("size", "12")
					.param("sort", "latest"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].memberId").value(memberId))
				.andExpect(jsonPath("$.items[0].personId").value(personId))
				.andExpect(jsonPath("$.items[0].externalId").isNotEmpty())
				.andExpect(jsonPath("$.items[0].nameKo").isNotEmpty())
				.andExpect(jsonPath("$.items[0].profileImageUrl").isNotEmpty())
				.andExpect(jsonPath("$.page.pageNo").value(1))
				.andExpect(jsonPath("$.page.pageSize").value(12))
				.andExpect(jsonPath("$.page.totalCnt").value(1));
	}

	/**
	 * 좋아요가 없으면 빈 목록 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("좋아요가 없으면 빈 목록 반환")
	void retrieveEmpty() throws Exception {
		// 좋아요가 없는 회원도 null이 아닌 빈 items와 전체 건수 0을 반환한다.
		mockMvc.perform(get("/api/members/likes")
					.param("type", "person")
					.param("page", "1")
					.param("size", "12")
					.param("sort", "latest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.page.totalCnt").value(0));
	}

	/**
	 * 인물 좋아요 등록 결과 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("인물 좋아요 등록 결과 반환")
	void addLike() throws Exception {
		// 요청 헤더가 아니라 SecurityContext에 로그인한 회원으로 좋아요를 등록한다.
		mockMvc.perform(post("/api/people/{personId}/likes", personId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(memberId))
				.andExpect(jsonPath("$.personId").value(personId))
				.andExpect(jsonPath("$.createdDt").isNotEmpty());

		PersonLikeVO saved = selectPersonLike();
		assertNotNull(saved);
		assertNotNull(saved.getCreatedDt());
	}

	/**
	 * 좋아요 중복 등록 허용 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("좋아요 중복 등록 허용")
	void addLikeAgain() throws Exception {
		// 최초 요청으로 좋아요를 등록하고 DB에서 생성된 등록 일시를 확인한다.
		mockMvc.perform(post("/api/people/{personId}/likes", personId))
				.andExpect(status().isOk());

		PersonLikeVO first = selectPersonLike();
		assertNotNull(first);

		// 같은 요청을 반복하면 중복 INSERT 없이 기존 좋아요 정보를 반환한다.
		mockMvc.perform(post("/api/people/{personId}/likes", personId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(memberId))
				.andExpect(jsonPath("$.personId").value(personId))
				.andExpect(jsonPath("$.createdDt").value(first.getCreatedDt()));

		assertEquals(first.getCreatedDt(), selectPersonLike().getCreatedDt());
	}

	/**
	 * 인물 좋아요 해제 후 204 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("인물 좋아요 해제 후 204 반환")
	void deleteLike() throws Exception {
		savePersonLike();

		// 등록된 인물 좋아요를 해제하면 복합 PK 행이 삭제된다.
		mockMvc.perform(delete("/api/people/{personId}/likes", personId))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertNull(selectPersonLike());
	}

	/**
	 * 좋아요 반복 해제 허용 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("좋아요 반복 해제 허용")
	void deleteLikeAgain() throws Exception {
		savePersonLike();

		// 최초 요청으로 좋아요를 해제한다.
		mockMvc.perform(delete("/api/people/{personId}/likes", personId))
				.andExpect(status().isNoContent());

		// 이미 해제된 요청을 반복해도 동일한 최종 상태이므로 204를 반환한다.
		mockMvc.perform(delete("/api/people/{personId}/likes", personId))
				.andExpect(status().isNoContent());

		assertNull(selectPersonLike());
	}

	/**
	 * 잘못된 인물 번호는 400으로 변환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("잘못된 인물 번호는 400으로 변환")
	void invalidPerson() throws Exception {
		// Service의 인물 번호 검증 예외를 인물 좋아요용 오류 응답으로 변환한다.
		mockMvc.perform(post("/api/people/0/likes"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"))
				.andExpect(jsonPath("$.message").value("올바른 인물 번호가 필요합니다."));
	}

	/**
	 * Controller 직접 호출에서 로그인 정보가 없으면 409 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("Controller 직접 호출에서 로그인 정보가 없으면 409 반환")
	void missingMember() throws Exception {
		// 필터를 제외한 이 테스트에서는 LoginMemberHelper가 발생시키는 예외 응답을 확인한다.
		SecurityTestContext.clear();
		mockMvc.perform(post("/api/people/{personId}/likes", personId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.id").value("409"))
				.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	/**
	 * 존재하지 않는 인물은 400으로 변환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("존재하지 않는 인물은 400으로 변환")
	void missingPerson() throws Exception {
		// 존재하지 않는 인물 등록으로 발생한 외래 키 예외를 400으로 변환한다.
		mockMvc.perform(post("/api/people/{personId}/likes", MISSING_PERSON_ID))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"));
	}

	/**
	 * 현재 테스트 회원과 인물의 복합 PK 좋아요 조회
	 *
	 * @return 인물 좋아요 정보
	 */
	private PersonLikeVO selectPersonLike() {
		return personLikeMapper.doSelectOne(new PersonLikeVO(memberId, personId, null));
	}

	/**
	 * 목록 및 해제 테스트에 사용할 인물 좋아요 생성
	 *
	 * @return 인물 좋아요 정보
	 */
	private PersonLikeVO savePersonLike() {
		PersonLikeVO personLike = new PersonLikeVO(memberId, personId, null);

		assertEquals(1, personLikeMapper.doSave(personLike));

		return personLike;
	}

	/**
	 * PERSON_LIKE 외래 키를 만족하는 테스트 회원 생성
	 *
	 * @return 등록된 테스트 회원 번호
	 */
	private int createMemberId() {
		return createMember().getMemberId().intValue();
	}

	/**
	 * 좋아요 소유자 또는 비교 대상 테스트 회원 생성
	 *
	 * @return 회원 정보
	 */
	private MemberVO createMember() {
		String token = createToken();

		MemberVO member = new MemberVO();
		member.setEmail("person-like-api-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("인물좋아요API" + token.substring(0, 8));
		member.setIntroduction("인물 좋아요 Controller 통합 테스트 회원");
		member.setRole("USER");

		return insertMember(jdbcTemplate, member);
	}

	/**
	 * PERSON_LIKE 외래 키와 목록 JOIN을 만족하는 테스트 인물 생성
	 *
	 * @return 등록된 테스트 인물 번호
	 */
	private int createPersonId() {
		String token = createToken();

		// EXTERNAL_ID 컬럼 최대 길이를 넘지 않도록 짧은 접두사를 사용한다.
		PersonVO person = new PersonVO(0, "PL_API_" + token, "인물좋아요API" + token.substring(0, 6), "Person Like API " + token.substring(0, 6),
				"https://example.com/person.jpg",
				null, null);

		// PERSON는 좋아요 테스트의 부모 데이터이므로 PERSON 시퀀스에 의존하지 않는다.
		return insertPerson(jdbcTemplate, person).getPersonId();
	}

	/**
	 * 회원과 인물의 고유 제약조건 충돌 방지용 문자열 생성
	 *
	 * @return 하이픈을 제외한 UUID 문자열
	 */
	private String createToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
