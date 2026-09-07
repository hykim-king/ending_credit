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
@DisplayName("PersonLikeViewController 통합 테스트")
class PersonLikeViewControllerTest {

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
		member.setEmail("member-like-view-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("좋아요화면" + token.substring(0, 6));
		member.setIntroduction("회원 좋아요 View 통합 테스트 회원");
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
	 * 기본 인물 좋아요 화면 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("기본 인물 좋아요 화면 반환")
	void personLikes() throws Exception {
		// URL에 회원 번호가 없어도 로그인 회원 번호가 Thymeleaf 모델에 전달되어야 한다.
		mockMvc.perform(get("/members/likes"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/likes"))
				.andExpect(model().attribute("memberId", memberId))
				.andExpect(model().attribute("type", "person"))
				.andExpect(model().attribute("currentMemberId", Long.valueOf(memberId)))
				.andExpect(content().contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(containsString("data-current-member-id=\"" + memberId + "\"")))
				.andExpect(content().string(containsString("data-initial-type=\"person\"")));
	}

	/**
	 * 인물 유형 좋아요 화면 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("인물 유형 좋아요 화면 반환")
	void personType() throws Exception {
		// 명시적인 type=person 요청도 동일한 인물 좋아요 화면을 반환한다.
		mockMvc.perform(get("/members/likes")
					.param("type", "person"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/likes"))
				.andExpect(model().attribute("memberId", memberId))
				.andExpect(model().attribute("type", "person"))
				.andExpect(content().string(containsString("data-initial-type=\"person\"")));
	}

	/**
	 * 컬렉션 유형 좋아요 화면 반환 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("컬렉션 유형 좋아요 화면 반환")
	void collectionType() throws Exception {
		mockMvc.perform(get("/members/likes")
					.param("type", "collection"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/likes"))
				.andExpect(model().attribute("memberId", memberId))
				.andExpect(model().attribute("type", "collection"))
				.andExpect(content().string(containsString("data-initial-type=\"collection\"")));
	}

	/**
	 * 지원하지 않는 유형은 인물로 보정 검증
	 *
	 * @throws Exception HTTP 요청 또는 응답 검증 실패
	 */
	@Test
	@DisplayName("지원하지 않는 유형은 인물로 보정")
	void invalidType() throws Exception {
		mockMvc.perform(get("/members/likes")
					.param("type", "unknown"))
				.andExpect(status().isOk())
				.andExpect(view().name("member/likes"))
				.andExpect(model().attribute("type", "person"))
				.andExpect(content().string(containsString("data-initial-type=\"person\"")));
	}
}
