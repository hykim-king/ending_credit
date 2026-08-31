package com.endit.controller;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.MemberVO;
import com.endit.domain.PersonLikeVO;
import com.endit.domain.PersonVO;
import com.endit.mapper.MemberMapper;
import com.endit.mapper.PersonLikeMapper;
import com.endit.mapper.PersonMapper;

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

	private static final String MEMBER_HEADER = "X-Member-Id";
	private static final int MISSING_PERSON_ID = Integer.MAX_VALUE;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PersonLikeMapper personLikeMapper;

	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private PersonMapper personMapper;

	private int memberId;
	private int personId;

	@BeforeEach
	void setUp() {
		memberId = createMemberId();
		personId = createPersonId();
	}

	@Test
	@DisplayName("회원 인물 좋아요 목록 반환")
	void retrieveLikes() throws Exception {
		savePersonLike();

		// type=person인 공개 프로필 요청에 인물 정보와 페이징 정보를 반환한다.
		mockMvc.perform(get("/api/users/{memberId}/likes", memberId)
					.param("type", "person")
					.param("page", "1")
					.param("size", "12")
					.param("sort", "latest"))
				.andExpect(status().isOk())
				.andExpect(content()
						.contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
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

	@Test
	@DisplayName("좋아요가 없으면 빈 목록 반환")
	void retrieveEmpty() throws Exception {
		// 좋아요가 없는 회원도 null이 아닌 빈 items와 전체 건수 0을 반환한다.
		mockMvc.perform(get("/api/users/{memberId}/likes", memberId)
					.param("type", "person")
					.param("page", "1")
					.param("size", "12")
					.param("sort", "latest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.page.totalCnt").value(0));
	}

	@Test
	@DisplayName("인물 좋아요 등록 결과 반환")
	void addLike() throws Exception {
		// 임시 회원 헤더와 인물 번호를 전달하면 좋아요를 등록한다.
		mockMvc.perform(post("/api/people/{personId}/likes", personId)
					.header(MEMBER_HEADER, memberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(memberId))
				.andExpect(jsonPath("$.personId").value(personId))
				.andExpect(jsonPath("$.createdDt").isNotEmpty());

		PersonLikeVO saved = selectPersonLike();
		assertNotNull(saved);
		assertNotNull(saved.getCreatedDt());
	}

	@Test
	@DisplayName("좋아요 중복 등록 허용")
	void addLikeAgain() throws Exception {
		// 최초 요청으로 좋아요를 등록하고 DB에서 생성된 등록 일시를 확인한다.
		mockMvc.perform(post("/api/people/{personId}/likes", personId)
					.header(MEMBER_HEADER, memberId))
				.andExpect(status().isOk());

		PersonLikeVO first = selectPersonLike();
		assertNotNull(first);

		// 같은 요청을 반복하면 중복 INSERT 없이 기존 좋아요 정보를 반환한다.
		mockMvc.perform(post("/api/people/{personId}/likes", personId)
					.header(MEMBER_HEADER, memberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(memberId))
				.andExpect(jsonPath("$.personId").value(personId))
				.andExpect(jsonPath("$.createdDt")
						.value(first.getCreatedDt()));

		assertEquals(
				first.getCreatedDt(),
				selectPersonLike().getCreatedDt());
	}

	@Test
	@DisplayName("인물 좋아요 해제 후 204 반환")
	void deleteLike() throws Exception {
		savePersonLike();

		// 등록된 인물 좋아요를 해제하면 복합 PK 행이 삭제된다.
		mockMvc.perform(delete("/api/people/{personId}/likes", personId)
					.header(MEMBER_HEADER, memberId))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertNull(selectPersonLike());
	}

	@Test
	@DisplayName("좋아요 반복 해제 허용")
	void deleteLikeAgain() throws Exception {
		savePersonLike();

		// 최초 요청으로 좋아요를 해제한다.
		mockMvc.perform(delete("/api/people/{personId}/likes", personId)
					.header(MEMBER_HEADER, memberId))
				.andExpect(status().isNoContent());

		// 이미 해제된 요청을 반복해도 동일한 최종 상태이므로 204를 반환한다.
		mockMvc.perform(delete("/api/people/{personId}/likes", personId)
					.header(MEMBER_HEADER, memberId))
				.andExpect(status().isNoContent());

		assertNull(selectPersonLike());
	}

	@Test
	@DisplayName("잘못된 인물 번호는 400으로 변환")
	void invalidPerson() throws Exception {
		// Service의 인물 번호 검증 예외를 인물 좋아요용 오류 응답으로 변환한다.
		mockMvc.perform(post("/api/people/0/likes")
					.header(MEMBER_HEADER, memberId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"))
				.andExpect(jsonPath("$.message")
						.value("올바른 인물 번호가 필요합니다."));
	}

	@Test
	@DisplayName("회원 헤더가 없으면 400 반환")
	void missingMember() throws Exception {
		// 임시 로그인 단계에서도 좋아요 등록에는 회원 식별 헤더가 필요하다.
		mockMvc.perform(post("/api/people/{personId}/likes", personId))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("존재하지 않는 인물은 400으로 변환")
	void missingPerson() throws Exception {
		// 존재하지 않는 인물 등록으로 발생한 외래 키 예외를 400으로 변환한다.
		mockMvc.perform(post("/api/people/{personId}/likes",
					MISSING_PERSON_ID)
					.header(MEMBER_HEADER, memberId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"));
	}

	/** 현재 테스트 회원과 인물의 복합 PK 좋아요 조회 */
	private PersonLikeVO selectPersonLike() {
		return personLikeMapper.doSelectOne(
				new PersonLikeVO(memberId, personId, null));
	}

	/** 목록 및 해제 테스트에 사용할 인물 좋아요 생성 */
	private PersonLikeVO savePersonLike() {
		PersonLikeVO personLike =
				new PersonLikeVO(memberId, personId, null);

		assertEquals(1, personLikeMapper.doSave(personLike));

		return personLike;
	}

	/** PERSON_LIKE 외래 키를 만족하는 테스트 회원 생성 */
	private int createMemberId() {
		String token = createToken();

		MemberVO member = new MemberVO();
		member.setEmail("person-like-api-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("인물좋아요API" + token.substring(0, 8));
		member.setIntroduction("인물 좋아요 Controller 통합 테스트 회원");
		member.setRole("USER");

		assertEquals(1, memberMapper.insertMember(member));
		assertNotNull(member.getMemberId());

		return member.getMemberId().intValue();
	}

	/** PERSON_LIKE 외래 키와 목록 JOIN을 만족하는 테스트 인물 생성 */
	private int createPersonId() {
		String token = createToken();

		// EXTERNAL_ID 컬럼 최대 길이를 넘지 않도록 짧은 접두사를 사용한다.
		PersonVO person = new PersonVO(
				0,
				"PL_API_" + token,
				"인물좋아요API" + token.substring(0, 6),
				"Person Like API " + token.substring(0, 6),
				"https://example.com/person.jpg",
				null,
				null);

		assertEquals(1, personMapper.doSave(person));

		return person.getPersonId();
	}

	/** 회원과 인물의 고유 제약조건 충돌 방지용 문자열 생성 */
	private String createToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}