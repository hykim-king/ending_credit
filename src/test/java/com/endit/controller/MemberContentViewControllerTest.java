package com.endit.controller;

import static com.endit.support.DatabaseTestFixtures.insertMember;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.MemberVO;
import com.endit.support.SecurityTestContext;

/**
 * <pre>
 * Class Name  : MemberContentViewControllerTest
 * Description : 실제 Spring MVC와 Thymeleaf로 회원 콘텐츠 기록 화면 경로를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * 2026. 9. 01. jinyoung    코멘트·컬렉션 기록 탭 렌더링 검증
 * 2026. 9. 05. jinyoung    본인 전용 URL 및 SecurityContext 로그인 회원 검증
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("MemberContentViewController 통합 테스트")
class MemberContentViewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private int memberId;

	/** 테스트 회원 등록 및 로그인 인증 설정 */
	@BeforeEach
	void setUpAuthentication() {
		String token = UUID.randomUUID().toString().replace("-", "");
		MemberVO member = new MemberVO();
		member.setEmail("member-record-view-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("기록화면" + token.substring(0, 6));
		member.setIntroduction("회원 기록 View 통합 테스트 회원");
		member.setRole("USER");

		insertMember(jdbcTemplate, member);
		memberId = member.getMemberId().intValue();
		SecurityTestContext.login(member);
	}

	/** 테스트 종료 후 인증 정보 제거 */
	@AfterEach
	void clearAuthentication() {
		SecurityTestContext.clear();
	}

	/**
	 * 기본 평가 기록 화면 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("기본 평가 기록 화면 반환")
	void ratings() throws Exception {
		// URL에 회원 번호가 없어도 로그인 회원 번호가 Thymeleaf 모델에 전달되어야 한다.
		mockMvc.perform(get("/members/records"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/records"))
				.andExpect(model().attribute("memberId", memberId))
				.andExpect(model().attribute("tab", "ratings"))
				.andExpect(content().contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(containsString("data-initial-tab=\"ratings\"")));
	}

	/**
	 * 보고싶어요 기록 화면 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("보고싶어요 기록 화면 반환")
	void watchlist() throws Exception {
		// watchlist 요청은 같은 HTML을 사용하되 최초 활성 탭만 보고싶어요로 전달한다.
		mockMvc.perform(get("/members/records")
					.param("tab", "watchlist"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/records"))
				.andExpect(model().attribute("memberId", memberId))
				.andExpect(model().attribute("tab", "watchlist"))
				.andExpect(content().string(containsString("data-initial-tab=\"watchlist\"")));
	}

	/**
	 * 코멘트 기록 화면 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("코멘트 기록 화면 반환")
	void comments() throws Exception {
		mockMvc.perform(get("/members/records")
					.param("tab", "comments"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/records"))
				.andExpect(model().attribute("tab", "comments"))
				.andExpect(content().string(containsString("data-initial-tab=\"comments\"")));
	}

	/**
	 * 컬렉션 기록 화면 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("컬렉션 기록 화면 반환")
	void collections() throws Exception {
		mockMvc.perform(get("/members/records")
					.param("tab", "collections"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/records"))
				.andExpect(model().attribute("tab", "collections"))
				.andExpect(content().string(containsString("data-initial-tab=\"collections\"")));
	}

	/**
	 * 지원하지 않는 탭은 평가로 보정 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("지원하지 않는 탭은 평가로 보정")
	void invalidTab() throws Exception {
		// 아직 연결되지 않은 탭이나 잘못된 값은 오류 대신 기본 평가 탭을 사용한다.
		mockMvc.perform(get("/members/records")
					.param("tab", "unknown"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/records"))
				.andExpect(model().attribute("tab", "ratings"))
				.andExpect(content().string(containsString("data-initial-tab=\"ratings\"")));
	}
}
