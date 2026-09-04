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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * <pre>
 * Class Name  : PersonLikeViewControllerTest
 * Description : 실제 Spring MVC와 Thymeleaf로 회원 좋아요 화면 경로를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * 2026. 8. 28. jinyoung    컬렉션 좋아요 유형 검증 추가
 * 2026. 9. 01. jinyoung    본인 좋아요 취소용 인증 회원 모델 검증
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PersonLikeViewController 통합 테스트")
class PersonLikeViewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Value("${endit.dev-auth.member-id}")
	private long authenticatedMemberId;

	@Test
	@DisplayName("기본 인물 좋아요 화면 반환")
	void personLikes() throws Exception {
		// type을 생략하면 인물 유형과 회원 번호가 Thymeleaf 모델에 전달되어야 한다.
		mockMvc.perform(get("/users/{memberId}/likes", 10))
				.andExpect(status().isOk())
				.andExpect(view().name("user/likes"))
				.andExpect(model().attribute("memberId", 10))
				.andExpect(model().attribute("type", "person"))
				.andExpect(model().attribute(
						"currentMemberId",
						authenticatedMemberId))
				.andExpect(content()
						.contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(
						containsString("data-member-id=\"10\"")))
				.andExpect(content().string(
						containsString("data-initial-type=\"person\"")));
	}

	@Test
	@DisplayName("인물 유형 좋아요 화면 반환")
	void personType() throws Exception {
		// 명시적인 type=person 요청도 동일한 인물 좋아요 화면을 반환한다.
		mockMvc.perform(get("/users/{memberId}/likes", 10)
					.param("type", "person"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/likes"))
				.andExpect(model().attribute("memberId", 10))
				.andExpect(model().attribute("type", "person"))
				.andExpect(content().string(
						containsString(
								"data-initial-type=\"person\"")));
	}

	@Test
	@DisplayName("컬렉션 유형 좋아요 화면 반환")
	void collectionType() throws Exception {
		mockMvc.perform(get("/users/{memberId}/likes", 10)
					.param("type", "collection"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/likes"))
				.andExpect(model().attribute("memberId", 10))
				.andExpect(model().attribute("type", "collection"))
				.andExpect(content().string(
						containsString(
								"data-initial-type=\"collection\"")));
	}

	@Test
	@DisplayName("지원하지 않는 유형은 인물로 보정")
	void invalidType() throws Exception {
		mockMvc.perform(get("/users/{memberId}/likes", 10)
					.param("type", "unknown"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/likes"))
				.andExpect(model().attribute("type", "person"))
				.andExpect(content().string(
						containsString(
								"data-initial-type=\"person\"")));
	}
}
