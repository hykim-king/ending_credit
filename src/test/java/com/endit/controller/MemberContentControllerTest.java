package com.endit.controller;

import static com.endit.support.DatabaseTestFixtures.insertContent;
import static com.endit.support.DatabaseTestFixtures.insertMember;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.endit.domain.ContentVO;
import com.endit.domain.MemberContentVO;
import com.endit.domain.MemberVO;
import com.endit.domain.RatingRequest;
import com.endit.mapper.MemberContentMapper;
import com.endit.support.SecurityTestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * Class Name  : MemberContentControllerTest
 * Description : 실제 Controller, Service, Mapper와 DB를 사용해 회원 콘텐츠 REST API를 검증하는 통합 테스트
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
@DisplayName("MemberContentController 통합 테스트")
class MemberContentControllerTest {

	private static final int MISSING_CONTENT_ID = Integer.MAX_VALUE;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberContentMapper memberContentMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private int memberId;
	private int contentId;

	@BeforeEach
	void setUp() {
		MemberVO member = createMember();
		memberId = member.getMemberId().intValue();
		SecurityTestContext.login(member);
		contentId = createContentId();
	}

	@AfterEach
	void clearAuthentication() {
		// SecurityContext는 ThreadLocal이므로 테스트마다 명시적으로 비운다.
		SecurityTestContext.clear();
	}

	@Test
	@DisplayName("회원 평가 목록 반환")
	void retrieveRatings() throws Exception {
		saveMemberContent(4, "N");

		// URL에 회원 번호가 없는 본인 전용 API가 로그인 회원의 기록만 반환해야 한다.
		mockMvc.perform(get("/api/members/ratings")
					.param("page", "1")
					.param("size", "12")
					.param("sort", "latest"))
				.andExpect(status().isOk())
				.andExpect(content()
						.contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].contentId").value(contentId))
				.andExpect(jsonPath("$.items[0].ratingScore").value(4))
				.andExpect(jsonPath("$.items[0].titleKo").isNotEmpty())
				.andExpect(jsonPath("$.page.pageNo").value(1))
				.andExpect(jsonPath("$.page.pageSize").value(12))
				.andExpect(jsonPath("$.page.totalCnt").value(1));
	}

	@Test
	@DisplayName("회원 보고싶어요 목록 반환")
	void retrieveWatchlist() throws Exception {
		saveMemberContent(null, "Y");

		// 보고싶어요 상태가 Y인 콘텐츠와 페이징 정보를 반환한다.
		mockMvc.perform(get("/api/members/watchlist")
					.param("page", "1")
					.param("size", "12")
					.param("sort", "latest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].contentId").value(contentId))
				.andExpect(jsonPath("$.items[0].watchlist").value("Y"))
				.andExpect(jsonPath("$.items[0].titleKo").isNotEmpty())
				.andExpect(jsonPath("$.page.totalCnt").value(1));
	}

	@Test
	@DisplayName("별점 등록 결과 반환")
	void saveRating() throws Exception {
		RatingRequest request = createRatingRequest(4);

		// 요청이 전달한 회원 번호가 아니라 SecurityContext의 로그인 회원으로 저장한다.
		mockMvc.perform(put("/api/movies/{contentId}/rating", contentId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(memberId))
				.andExpect(jsonPath("$.contentId").value(contentId))
				.andExpect(jsonPath("$.ratingScore").value(4))
				.andExpect(jsonPath("$.watchlist").value("N"))
				.andExpect(jsonPath("$.ratedDt").isNotEmpty());

		MemberContentVO saved = selectMemberContent();
		assertNotNull(saved);
		assertEquals(Integer.valueOf(4), saved.getRatingScore());
	}

	@Test
	@DisplayName("별점 변경 결과 반환")
	void updateRating() throws Exception {
		saveMemberContent(2, "N");
		RatingRequest request = createRatingRequest(5);

		// 동일 회원과 콘텐츠의 기존 별점을 새로운 점수로 변경한다.
		mockMvc.perform(put("/api/movies/{contentId}/rating", contentId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ratingScore").value(5))
				.andExpect(jsonPath("$.watchlist").value("N"));

		assertEquals(
				Integer.valueOf(5),
				selectMemberContent().getRatingScore());
	}

	@Test
	@DisplayName("별점 해제 후 204 반환")
	void deleteRating() throws Exception {
		saveMemberContent(4, "N");

		// 별점만 존재하는 기록을 해제하면 빈 MEMBER_CONTENT 행도 삭제된다.
		mockMvc.perform(delete("/api/movies/{contentId}/rating", contentId))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertNull(selectMemberContent());
	}

	@Test
	@DisplayName("보고싶어요 등록 결과 반환")
	void addWatchlist() throws Exception {
		// 본문 없이 로그인 회원과 콘텐츠 번호만으로 보고싶어요를 등록한다.
		mockMvc.perform(post("/api/watchlist/{contentId}", contentId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(memberId))
				.andExpect(jsonPath("$.contentId").value(contentId))
				.andExpect(jsonPath("$.watchlist").value("Y"))
				.andExpect(jsonPath("$.watchlistDt").isNotEmpty());

		MemberContentVO saved = selectMemberContent();
		assertNotNull(saved);
		assertEquals("Y", saved.getWatchlist());
	}

	@Test
	@DisplayName("보고싶어요 해제 후 204 반환")
	void deleteWatchlist() throws Exception {
		saveMemberContent(null, "Y");

		// 보고싶어요만 존재하는 기록을 해제하면 빈 MEMBER_CONTENT 행도 삭제된다.
		mockMvc.perform(delete("/api/watchlist/{contentId}", contentId))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertNull(selectMemberContent());
	}

	@Test
	@DisplayName("잘못된 별점은 400으로 변환")
	void invalidRating() throws Exception {
		RatingRequest request = createRatingRequest(6);

		// Service의 별점 범위 예외가 회원 콘텐츠용 오류 응답으로 변환되어야 한다.
		mockMvc.perform(put("/api/movies/{contentId}/rating", contentId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"))
				.andExpect(jsonPath("$.message")
						.value("별점은 1점부터 5점까지 입력해야 합니다."));
	}

	@Test
	@DisplayName("Controller 직접 호출에서 로그인 정보가 없으면 409 반환")
	void missingMember() throws Exception {
		// 보안 필터를 끈 Controller 테스트에서는 LoginMemberHelper 예외를 직접 검증한다.
		// 실제 애플리케이션 요청은 SecurityConfig가 이보다 먼저 비로그인 접근을 차단한다.
		SecurityTestContext.clear();
		mockMvc.perform(post("/api/watchlist/{contentId}", contentId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.id").value("409"))
				.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("존재하지 않는 콘텐츠는 400으로 변환")
	void missingContent() throws Exception {
		RatingRequest request = createRatingRequest(3);

		// 존재하지 않는 콘텐츠 등록으로 발생한 외래 키 예외를 400으로 변환한다.
		mockMvc.perform(put("/api/movies/{contentId}/rating",
					MISSING_CONTENT_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"));
	}

	/** 현재 테스트 회원과 콘텐츠의 복합 PK 기록 조회 */
	private MemberContentVO selectMemberContent() {
		MemberContentVO key = new MemberContentVO();
		key.setMemberId(memberId);
		key.setContentId(contentId);

		return memberContentMapper.doSelectOne(key);
	}

	/** 목록 및 해제 테스트에 사용할 회원 콘텐츠 기록 생성 */
	private MemberContentVO saveMemberContent(
			Integer ratingScore,
			String watchlist) {

		MemberContentVO memberContent = new MemberContentVO(
				memberId,
				contentId,
				ratingScore,
				watchlist,
				null,
				null,
				null);

		assertEquals(1, memberContentMapper.doSave(memberContent));

		return memberContent;
	}

	/** 별점 JSON 요청 객체 생성 */
	private RatingRequest createRatingRequest(int ratingScore) {
		RatingRequest request = new RatingRequest();
		request.setRatingScore(ratingScore);

		return request;
	}

	/** MEMBER_CONTENT 외래 키를 만족하는 테스트 회원 생성 */
	private MemberVO createMember() {
		String token = createToken();

		MemberVO member = new MemberVO();
		member.setEmail("member-content-api-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("회원콘텐츠API" + token.substring(0, 8));
		member.setIntroduction("회원 콘텐츠 Controller 통합 테스트 회원");
		member.setRole("USER");

		return insertMember(jdbcTemplate, member);
	}

	/** MEMBER_CONTENT 외래 키를 만족하는 테스트 콘텐츠 생성 */
	private int createContentId() {
		String token = createToken();

		// EXTERNAL_ID 컬럼 최대 길이를 넘지 않도록 짧은 접두사를 사용한다.
		ContentVO content = new ContentVO(
				0,
				"MC_API_" + token,
				"회원 콘텐츠 API 테스트 영화",
				"Member Content API Test",
				"회원 콘텐츠 Controller 통합 테스트 영화",
				"2026-08-27",
				120,
				"Korea",
				"https://example.com/poster.jpg",
				"https://example.com/backdrop.jpg",
				null);

		// CONTENT는 회원 활동 테스트의 부모 데이터이므로 CONTENT 시퀀스를 사용하지 않는다.
		return insertContent(jdbcTemplate, content).getContentId();
	}

	/** DB 고유 제약조건 충돌 방지용 문자열 생성 */
	private String createToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
