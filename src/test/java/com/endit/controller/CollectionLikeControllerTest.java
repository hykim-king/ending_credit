package com.endit.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.CollectionLikeVO;
import com.endit.mapper.CollectionLikeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * Class Name  : CollectionLikeControllerTest
 * Description : 컬렉션 좋아요 등록, 취소 및 회원별 좋아요 컬렉션 목록 조회 API를 검증하는 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. gunwoo      최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 27.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("CollectionLikeController 테스트")
class CollectionLikeControllerTest {

	private static final Logger log = LoggerFactory.getLogger(CollectionLikeControllerTest.class);

	// MEMBER_ID 1, COLLECTION_ID 1·2는 공용 DB에 존재하는 부모 더미 데이터
	private static final int MEMBER_ID = 1;
	private static final int COLLECTION_ID = 1;
	private static final int OTHER_COLLECTION_ID = 2;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	/**매 테스트 전 COLLECTION_LIKE 데이터를 비운 상태로 초기화*/
	@BeforeEach
	void setUp() {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ setUp()                      │");
		log.debug("└──────────────────────────────┘");

		collectionLikeMapper.deleteAll();
		assertEquals(0, collectionLikeMapper.totalCnt());

		log.debug("* initializedData: totalCnt-{}건", collectionLikeMapper.totalCnt());
	}

	/**컬렉션 좋아요 등록 API 검증*/
	@Test
	@DisplayName("컬렉션 좋아요 등록 API")
	void testLike() throws Exception {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ like()                       │");
		log.debug("└──────────────────────────────┘");

		CollectionLikeVO param = new CollectionLikeVO();
		param.setMemberId(MEMBER_ID);

		mockMvc.perform(post("/api/collections/{collectionId}/likes", COLLECTION_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(param)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberId").value(MEMBER_ID))
				.andExpect(jsonPath("$.collectionId").value(COLLECTION_ID));

		assertEquals(1, collectionLikeMapper.totalCnt());

		log.debug("* likedData: memberId-{}, collectionId-{}", MEMBER_ID, COLLECTION_ID);
	}

	/**정책(POL-010) · 이미 좋아요를 누른 상태에서 재요청 시 멱등 응답 검증*/
	@Test
	@DisplayName("이미 좋아요를 누른 상태에서 재등록 요청 시 멱등 응답")
	void testLike_alreadyLiked_idempotent() throws Exception {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ like_alreadyLiked()          │");
		log.debug("└──────────────────────────────┘");

		CollectionLikeVO existing = new CollectionLikeVO(MEMBER_ID, COLLECTION_ID, null);
		assertEquals(1, collectionLikeMapper.insertCollectionLike(existing));

		CollectionLikeVO param = new CollectionLikeVO();
		param.setMemberId(MEMBER_ID);

		mockMvc.perform(post("/api/collections/{collectionId}/likes", COLLECTION_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(param)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberId").value(MEMBER_ID))
				.andExpect(jsonPath("$.collectionId").value(COLLECTION_ID));

		assertEquals(1, collectionLikeMapper.totalCnt());

		log.debug("* totalCnt: {}건", collectionLikeMapper.totalCnt());
	}

	/**컬렉션 좋아요 취소 API 검증*/
	@Test
	@DisplayName("컬렉션 좋아요 취소 API")
	void testUnlike() throws Exception {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ unlike()                     │");
		log.debug("└──────────────────────────────┘");

		CollectionLikeVO existing = new CollectionLikeVO(MEMBER_ID, COLLECTION_ID, null);
		assertEquals(1, collectionLikeMapper.insertCollectionLike(existing));

		CollectionLikeVO param = new CollectionLikeVO();
		param.setMemberId(MEMBER_ID);

		mockMvc.perform(delete("/api/collections/{collectionId}/likes", COLLECTION_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(param)))
				.andExpect(status().isNoContent());

		assertNull(collectionLikeMapper.selectCollectionLike(existing));

		log.debug("* remainingCnt: {}건", collectionLikeMapper.totalCnt());
	}

	/** 정책(POL-011) · 좋아요가 없는 상태에서 취소 요청 시 멱등 응답 검증*/
	@Test
	@DisplayName("좋아요가 없는 상태에서 취소 요청해도 멱등 응답")
	void testUnlike_notLiked_idempotent() throws Exception {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ unlike_notLiked()            │");
		log.debug("└──────────────────────────────┘");

		CollectionLikeVO param = new CollectionLikeVO();
		param.setMemberId(MEMBER_ID);

		mockMvc.perform(delete("/api/collections/{collectionId}/likes", COLLECTION_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(param)))
				.andExpect(status().isNoContent());

		log.debug("* result: 멱등 처리로 204 응답 확인");
	}

	/**회원별 좋아요 컬렉션 목록 조회 API 검증*/
	@Test
	@DisplayName("회원별 좋아요 컬렉션 목록 조회 API")
	void testRetrieveByMember() throws Exception {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ retrieveByMember()           │");
		log.debug("└──────────────────────────────┘");

		assertEquals(1, collectionLikeMapper.insertCollectionLike(
				new CollectionLikeVO(MEMBER_ID, COLLECTION_ID, null)));
		assertEquals(1, collectionLikeMapper.insertCollectionLike(
				new CollectionLikeVO(MEMBER_ID, OTHER_COLLECTION_ID, null)));

		mockMvc.perform(get("/api/users/{memberId}/likes", MEMBER_ID)
				.param("type", "collections")
				.param("pageNo", "1")
				.param("pageSize", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.page.totalCnt").value(2));

		log.debug("* totalCnt: {}건", collectionLikeMapper.selectLikedCollectionCountByMember(MEMBER_ID));
	}

	/**좋아요한 컬렉션이 없는 회원을 조회하면 빈 목록이 응답되는지 검증*/
	@Test
	@DisplayName("좋아요한 컬렉션이 없으면 빈 목록 응답")
	void testRetrieveByMember_empty() throws Exception {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ retrieveByMember_empty()     │");
		log.debug("└──────────────────────────────┘");

		mockMvc.perform(get("/api/users/{memberId}/likes", MEMBER_ID)
				.param("type", "collections"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(0));

		log.debug("* emptyResult 확인");
	}

	/**type=collections가 아닌 요청은 400으로 응답되는지 검증*/
	@Test
	@DisplayName("type=collections가 아니면 400 응답")
	void testRetrieveByMember_invalidType() throws Exception {
		log.debug("┌──────────────────────────────┐");
		log.debug("│ retrieveByMember_invalidType()│");
		log.debug("└──────────────────────────────┘");

		mockMvc.perform(get("/api/users/{memberId}/likes", MEMBER_ID)
				.param("type", "people"))
				.andExpect(status().isBadRequest());

		log.debug("* result: 400 응답 확인");
	}

}