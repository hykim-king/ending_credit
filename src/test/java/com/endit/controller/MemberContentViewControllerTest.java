package com.endit.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

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
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MemberContentViewController 통합 테스트")
class MemberContentViewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("기본 평가 기록 화면 반환")
	void ratings() throws Exception {
		// tab을 생략하면 평가 탭과 회원 번호가 Thymeleaf 모델에 전달되어야 한다.
		mockMvc.perform(get("/users/{memberId}/records", 10))
				.andExpect(status().isOk())
				.andExpect(view().name("user/records"))
				.andExpect(model().attribute("memberId", 10))
				.andExpect(model().attribute("tab", "ratings"))
				.andExpect(content()
						.contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(
						containsString("data-member-id=\"10\"")))
				.andExpect(content().string(
						containsString("data-initial-tab=\"ratings\"")));
	}

	@Test
	@DisplayName("보고싶어요 기록 화면 반환")
	void watchlist() throws Exception {
		// watchlist 요청은 같은 HTML을 사용하되 최초 활성 탭만 보고싶어요로 전달한다.
		mockMvc.perform(get("/users/{memberId}/records", 10)
					.param("tab", "watchlist"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/records"))
				.andExpect(model().attribute("memberId", 10))
				.andExpect(model().attribute("tab", "watchlist"))
				.andExpect(content().string(
						containsString(
								"data-initial-tab=\"watchlist\"")));
	}

	@Test
	@DisplayName("지원하지 않는 탭은 평가로 보정")
	void invalidTab() throws Exception {
		// 아직 연결되지 않은 탭이나 잘못된 값은 오류 대신 기본 평가 탭을 사용한다.
		mockMvc.perform(get("/users/{memberId}/records", 10)
					.param("tab", "unknown"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/records"))
				.andExpect(model().attribute("tab", "ratings"))
				.andExpect(content().string(
						containsString(
								"data-initial-tab=\"ratings\"")));
	}
}