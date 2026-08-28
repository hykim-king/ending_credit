package com.endit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.CollectionVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.CollectionMapper;
import com.endit.mapper.MemberMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * Class Name  : CollectionControllerTest
 * Description : 실제 Controller, Service, Mapper와 DB를 사용해 컬렉션 REST API를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 21. jinyoung    최초 생성
 * 2026. 8. 26. jinyoung    실제 Spring Bean과 DB 기반 통합 테스트로 변경
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 21.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("CollectionController 통합 테스트")
class CollectionControllerTest {

	private static final int MISSING_COLLECTION_ID = Integer.MAX_VALUE;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CollectionMapper collectionMapper;

	@Autowired
	private MemberMapper memberMapper;

	/** 실제 DB 목록과 페이징 정보의 HTTP 응답 검증 */
	@Test
	@DisplayName("컬렉션 목록과 페이징 정보 반환")
	void retrieve() throws Exception {
		CollectionVO collection = createSavedCollection("HTTP 목록 컬렉션");

		mockMvc.perform(get("/api/collections")
					.param("pageNo", "1")
					.param("pageSize", "10")
					.param("searchDiv", "10")
					.param("searchWord", collection.getTitle()))
				.andExpect(status().isOk())
				.andExpect(content()
						.contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].collectionId")
						.value(collection.getCollectionId()))
				.andExpect(jsonPath("$.items[0].title")
						.value(collection.getTitle()))
				.andExpect(jsonPath("$.page.pageNo").value(1))
				.andExpect(jsonPath("$.page.pageSize").value(10))
				.andExpect(jsonPath("$.page.totalCnt").value(1));
	}

	/** 실제 DB 컬렉션 단건의 HTTP 응답 검증 */
	@Test
	@DisplayName("컬렉션 단건 반환")
	void getCollection() throws Exception {
		CollectionVO collection = createSavedCollection("HTTP 단건 컬렉션");

		mockMvc.perform(get("/api/collections/{collectionId}",
					collection.getCollectionId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.collectionId")
						.value(collection.getCollectionId()))
				.andExpect(jsonPath("$.memberId")
						.value(collection.getMemberId()))
				.andExpect(jsonPath("$.title")
						.value(collection.getTitle()));
	}

	/** JSON 요청부터 실제 DB 등록까지 성공 상태와 접근 URI 검증 */
	@Test
	@DisplayName("컬렉션 등록 후 201과 Location 반환")
	void create() throws Exception {
		CollectionVO request = createCollection(
				createMemberId(), "HTTP 등록 컬렉션", null);

		mockMvc.perform(post("/api/collections")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesPattern(
								"/api/collections/[1-9][0-9]*")))
				.andExpect(jsonPath("$.collectionId").isNumber())
				.andExpect(jsonPath("$.title").value("HTTP 등록 컬렉션"))
				.andExpect(jsonPath("$.isPublic").value("Y"));
	}

	/** 실제 DB 컬렉션 수정 결과의 HTTP 응답 검증 */
	@Test
	@DisplayName("컬렉션 수정 결과 반환")
	void update() throws Exception {
		CollectionVO saved = createSavedCollection("HTTP 수정 전 컬렉션");
		CollectionVO request = createCollection(
				0, "HTTP 수정 후 컬렉션", "N");

		mockMvc.perform(put("/api/collections/{collectionId}",
					saved.getCollectionId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.collectionId")
						.value(saved.getCollectionId()))
				.andExpect(jsonPath("$.memberId")
						.value(saved.getMemberId()))
				.andExpect(jsonPath("$.title")
						.value("HTTP 수정 후 컬렉션"))
				.andExpect(jsonPath("$.isPublic").value("N"));
	}

	/** HTTP 삭제 요청의 실제 DB 반영 검증 */
	@Test
	@DisplayName("컬렉션 삭제 후 204 반환")
	void deleteCollection() throws Exception {
		CollectionVO saved = createSavedCollection("HTTP 삭제 컬렉션");

		mockMvc.perform(delete("/api/collections/{collectionId}",
					saved.getCollectionId()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertNull(collectionMapper.doSelectOne(saved));
	}

	/** 실제 Service 입력 검증 예외의 HTTP 400 변환 검증 */
	@Test
	@DisplayName("잘못된 요청은 400으로 변환")
	void badRequest() throws Exception {
		mockMvc.perform(get("/api/collections/0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"))
				.andExpect(jsonPath("$.message")
						.value("올바른 컬렉션 번호가 필요합니다."));
	}

	/** 실제 DB 미조회 결과의 HTTP 404 변환 검증 */
	@Test
	@DisplayName("존재하지 않는 컬렉션은 404로 변환")
	void notFound() throws Exception {
		mockMvc.perform(get("/api/collections/{collectionId}",
					MISSING_COLLECTION_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.id").value("404"));
	}

	/** 외래 키를 만족하는 회원과 컬렉션을 현재 트랜잭션에 등록 */
	private CollectionVO createSavedCollection(String title) {
		CollectionVO collection = createCollection(createMemberId(), title, "Y");
		assertEquals(1, collectionMapper.doSave(collection));

		return collection;
	}

	/** 외래 키를 만족하는 테스트 회원을 현재 트랜잭션에 등록 */
	private int createMemberId() {
		String token = UUID.randomUUID().toString().replace("-", "");
		MemberVO member = new MemberVO();
		member.setEmail("collection-api-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("컬렉션API" + token.substring(0, 8));
		member.setIntroduction("컬렉션 API 통합 테스트 회원");
		member.setRole("USER");
		assertEquals(1, memberMapper.insertMember(member));

		return member.getMemberId().intValue();
	}

	/** 테스트에 사용할 컬렉션 정보 생성 */
	private CollectionVO createCollection(
			int memberId,
			String title,
			String isPublic) {

		return new CollectionVO(
				0,
				memberId,
				title,
				"컬렉션 설명",
				isPublic,
				null,
				null);
	}
}
