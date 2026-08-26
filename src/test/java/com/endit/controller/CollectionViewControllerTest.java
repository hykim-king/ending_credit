package com.endit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * <pre>
 * Class Name  : CollectionViewControllerTest
 * Description : 컬렉션 화면 경로와 View 이름을 검증하는 단위 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@DisplayName("CollectionViewController 테스트")
class CollectionViewControllerTest {

	private MockMvc mockMvc;

	/** 독립형 MockMvc 환경 준비 */
	@BeforeEach
	void setUp() {
		// View Controller는 Service가 없으므로 실제 객체를 바로 생성한다.
		// 서버를 띄우지 않고 URL, View 이름, Model 값만 확인하는 테스트다.
		mockMvc = MockMvcBuilders
				.standaloneSetup(new CollectionViewController())
				.build();
	}

	/** 컬렉션 목록 View 경로 검증 */
	@Test
	@DisplayName("컬렉션 목록 화면 반환")
	void list() throws Exception {
		// GET /collections가 templates/collection/list.html을 선택하는지 검증한다.
		mockMvc.perform(get("/collections"))
				.andExpect(status().isOk())
				.andExpect(view().name("collection/list"));
	}

	/** 컬렉션 등록 View 경로와 모델 검증 */
	@Test
	@DisplayName("컬렉션 등록 화면 반환")
	void createForm() throws Exception {
		// 등록·수정 공용 form.html이 등록 모드로 열리는지 Model까지 확인한다.
		mockMvc.perform(get("/collections/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("collection/form"))
				.andExpect(model().attribute("formMode", "create"))
				.andExpect(model().attribute("collectionId", 0));
	}

	/** 컬렉션 상세 View 경로와 모델 검증 */
	@Test
	@DisplayName("컬렉션 상세 화면 반환")
	void detail() throws Exception {
		// 경로 변수 collectionId가 상세 View의 Model로 전달되는지 확인한다.
		mockMvc.perform(get("/collections/1"))
				.andExpect(status().isOk())
				.andExpect(view().name("collection/detail"))
				.andExpect(model().attribute("collectionId", 1));
	}

	/** 컬렉션 수정 View 경로와 모델 검증 */
	@Test
	@DisplayName("컬렉션 수정 화면 반환")
	void updateForm() throws Exception {
		// 수정 모드에서는 formMode=update와 실제 collectionId가 모두 필요하다.
		mockMvc.perform(get("/collections/1/edit"))
				.andExpect(status().isOk())
				.andExpect(view().name("collection/form"))
				.andExpect(model().attribute("formMode", "update"))
				.andExpect(model().attribute("collectionId", 1));
	}
}
