package com.endit.controller;

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
 * Class Name  : CollectionViewControllerTest
 * Description : 실제 Spring MVC와 Thymeleaf로 컬렉션 화면 경로를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * 2026. 8. 26. jinyoung    실제 Spring MVC와 View 기반 통합 테스트로 변경
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
				.andExpect(content().contentTypeCompatibleWith("text/html"));
	}

	/** 컬렉션 상세 View 경로와 모델 및 실제 HTML 렌더링 검증 */
	@Test
	@DisplayName("컬렉션 상세 화면 반환")
	void detail() throws Exception {
		mockMvc.perform(get("/collections/1"))
				.andExpect(status().isOk())
				.andExpect(view().name("collection/detail"))
				.andExpect(model().attribute("collectionId", 1))
				.andExpect(content().contentTypeCompatibleWith("text/html"));
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
