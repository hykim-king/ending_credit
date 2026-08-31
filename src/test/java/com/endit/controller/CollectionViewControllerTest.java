package com.endit.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
 * Class Name  : CollectionViewControllerTest
 * Description : 실제 Spring MVC와 Thymeleaf로 컬렉션 화면 경로를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * 2026. 8. 29. jinyoung    상세 인증 회원·조회 전용 및 공개 여부·작품 선택 영역 검증 추가
 * 2026. 8. 31. jinyoung    D-04 작품 추가 모달 렌더링 검증 추가
 * 2026. 8. 31. jinyoung    D-01 링크 복사·코멘트·삭제 모달 렌더링 검증 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CollectionViewController 통합 테스트")
class CollectionViewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Value("${endit.dev-auth.member-id}")
	private long authenticatedMemberId;

	/** 컬렉션 목록 View 경로와 실제 HTML 렌더링 검증 */
	@Test
	@DisplayName("컬렉션 목록 화면 반환")
	void list() throws Exception {
		mockMvc.perform(get("/collections"))
				.andExpect(status().isOk())
				.andExpect(view().name("collection/list"))
				.andExpect(content().contentTypeCompatibleWith("text/html"));
	}

	/** 컬렉션 등록 View 경로와 모델 및 실제 HTML 렌더링 검증 */
	@Test
	@DisplayName("컬렉션 등록 화면 반환")
	void createForm() throws Exception {
		mockMvc.perform(get("/collections/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("collection/form"))
				.andExpect(model().attribute("formMode", "create"))
				.andExpect(model().attribute("collectionId", 0))
				.andExpect(content().contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(containsString("id=\"isPublic\"")))
				.andExpect(content().string(containsString(
						"id=\"openContentSearchButton\"")))
				.andExpect(content().string(containsString(
						"id=\"contentSearchModal\"")))
				.andExpect(content().string(containsString("id=\"contentSearchInput\"")))
				.andExpect(content().string(containsString("id=\"selectedContentList\"")));
	}

	/** 컬렉션 상세 View 경로와 모델 및 실제 HTML 렌더링 검증 */
	@Test
	@DisplayName("컬렉션 상세 화면 반환")
	void detail() throws Exception {
		mockMvc.perform(get("/collections/1"))
				.andExpect(status().isOk())
				.andExpect(view().name("collection/detail"))
				.andExpect(model().attribute("collectionId", 1))
				.andExpect(model().attribute("currentMemberId", authenticatedMemberId))
				.andExpect(content().contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(containsString("id=\"copyLinkButton\"")))
				.andExpect(content().string(containsString("id=\"commentsLink\"")))
				.andExpect(content().string(containsString(
						"id=\"deleteCollectionModal\"")))
				.andExpect(content().string(not(
						containsString("detailContentSearchInput"))));
	}

	/** 컬렉션 수정 View 경로와 모델 및 실제 HTML 렌더링 검증 */
	@Test
	@DisplayName("컬렉션 수정 화면 반환")
	void updateForm() throws Exception {
		mockMvc.perform(get("/collections/1/edit"))
				.andExpect(status().isOk())
				.andExpect(view().name("collection/form"))
				.andExpect(model().attribute("formMode", "update"))
				.andExpect(model().attribute("collectionId", 1))
				.andExpect(content().contentTypeCompatibleWith("text/html"));
	}
}
