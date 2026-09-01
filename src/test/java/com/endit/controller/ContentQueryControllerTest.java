package com.endit.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.ContentVO;
import com.endit.mapper.ContentMapper;

/**
 * <pre>
 * Class Name  : ContentQueryControllerTest
 * Description : 저장된 콘텐츠 제목 검색 API 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    컬렉션 작품 선택용 콘텐츠 검색 API 검증 최초 생성
 * 2026. 8. 31. jinyoung    영화 검색 최종 경로 및 query/page/size 계약 검증
 * ------------------------------------------------------------
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("ContentQueryController 통합 테스트")
class ContentQueryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ContentMapper contentMapper;

	/** 한글 제목 일부로 저장된 콘텐츠를 검색하는지 검증 */
	@Test
	@DisplayName("영화 제목 검색 API")
	void retrieveByTitle() throws Exception {
		String token = UUID.randomUUID().toString().replace("-", "");
		ContentVO content = new ContentVO(
				0,
				"QUERY_" + token,
				"컬렉션검색영화" + token,
				"Collection Search Movie " + token,
				"콘텐츠 검색 API 테스트",
				"2026-08-29",
				120,
				"US",
				"https://image.test/poster.jpg",
				null,
				null);
		assertEquals(1, contentMapper.doSave(content));

		mockMvc.perform(get("/api/search/movies")
					.param("query", token)
					.param("page", "1")
					.param("size", "8"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].contentId").value(content.getContentId()))
				.andExpect(jsonPath("$.items[0].titleKo").value(content.getTitleKo()))
				.andExpect(jsonPath("$.page.totalCnt").value(1));
	}
}
