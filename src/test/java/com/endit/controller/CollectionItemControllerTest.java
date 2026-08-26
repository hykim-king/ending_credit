package com.endit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.endit.domain.CollectionItemVO;
import com.endit.domain.CollectionVO;
import com.endit.domain.ContentVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.CollectionItemMapper;
import com.endit.mapper.CollectionMapper;
import com.endit.mapper.ContentMapper;
import com.endit.mapper.MemberMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * Class Name  : CollectionItemControllerTest
 * Description : 실제 Controller, Service, Mapper와 DB를 사용해 컬렉션 작품 REST API를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * 2026. 8. 26. jinyoung    실제 Spring Bean과 DB 기반 통합 테스트로 변경
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("CollectionItemController 통합 테스트")
class CollectionItemControllerTest {

	private static final int MISSING_CONTENT_ID = Integer.MAX_VALUE;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CollectionItemMapper collectionItemMapper;

	@Autowired
	private CollectionMapper collectionMapper;

	@Autowired
	private ContentMapper contentMapper;

	@Autowired
	private MemberMapper memberMapper;

	/** 실제 DB 컬렉션 작품 목록과 페이징 정보의 HTTP 응답 검증 */
	@Test
	@DisplayName("컬렉션 작품 목록과 페이징 정보 반환")
	void retrieve() throws Exception {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		CollectionItemVO item = createItem(
				collection.getCollectionId(), content.getContentId());
		assertEquals(1, collectionItemMapper.doSave(item));

		mockMvc.perform(get("/api/collections/{collectionId}/items",
					collection.getCollectionId())
					.param("pageNo", "1")
					.param("pageSize", "12"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].collectionId")
						.value(collection.getCollectionId()))
				.andExpect(jsonPath("$.items[0].contentId")
						.value(content.getContentId()))
				.andExpect(jsonPath("$.items[0].titleKo")
						.value(content.getTitleKo()))
				.andExpect(jsonPath("$.page.totalCnt").value(1));
	}

	/** 실제 DB 컬렉션 작품 단건의 HTTP 응답 검증 */
	@Test
	@DisplayName("컬렉션 작품 단건 반환")
	void getItem() throws Exception {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		CollectionItemVO item = createItem(
				collection.getCollectionId(), content.getContentId());
		assertEquals(1, collectionItemMapper.doSave(item));

		mockMvc.perform(get(
					"/api/collections/{collectionId}/items/{contentId}",
					collection.getCollectionId(), content.getContentId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.collectionId")
						.value(collection.getCollectionId()))
				.andExpect(jsonPath("$.contentId")
						.value(content.getContentId()))
				.andExpect(jsonPath("$.addedDt").isNotEmpty());
	}

	/** JSON 요청부터 실제 DB 추가까지 성공 상태와 접근 URI 검증 */
	@Test
	@DisplayName("컬렉션 작품 추가 후 201과 Location 반환")
	void create() throws Exception {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		CollectionItemVO request = createItem(0, content.getContentId());

		mockMvc.perform(post("/api/collections/{collectionId}/items",
					collection.getCollectionId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string(
						"Location",
						"/api/collections/" + collection.getCollectionId()
								+ "/items/" + content.getContentId()))
				.andExpect(jsonPath("$.collectionId")
						.value(collection.getCollectionId()))
				.andExpect(jsonPath("$.contentId")
						.value(content.getContentId()))
				.andExpect(jsonPath("$.addedDt").isNotEmpty());
	}

	/** HTTP 삭제 요청의 실제 DB 반영 검증 */
	@Test
	@DisplayName("컬렉션 작품 삭제 후 204 반환")
	void deleteItem() throws Exception {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		CollectionItemVO item = createItem(
				collection.getCollectionId(), content.getContentId());
		assertEquals(1, collectionItemMapper.doSave(item));

		mockMvc.perform(delete(
					"/api/collections/{collectionId}/items/{contentId}",
					collection.getCollectionId(), content.getContentId()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertNull(collectionItemMapper.doSelectOne(item));
	}

	/** 실제 Service 입력 검증 예외의 HTTP 400 변환 검증 */
	@Test
	@DisplayName("잘못된 요청은 400으로 변환")
	void badRequest() throws Exception {
		mockMvc.perform(get("/api/collections/0/items/1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"));
	}

	/** 실제 DB 미조회 결과의 HTTP 404 변환 검증 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 작품은 404로 변환")
	void notFound() throws Exception {
		CollectionVO collection = createCollection();

		mockMvc.perform(get(
					"/api/collections/{collectionId}/items/{contentId}",
					collection.getCollectionId(), MISSING_CONTENT_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.id").value("404"));
	}

	/** 실제 DB 중복 데이터에 대한 HTTP 409 변환 검증 */
	@Test
	@DisplayName("중복 작품 추가는 409로 변환")
	void conflict() throws Exception {
		CollectionVO collection = createCollection();
		ContentVO content = createContent();
		CollectionItemVO item = createItem(
				collection.getCollectionId(), content.getContentId());
		assertEquals(1, collectionItemMapper.doSave(item));

		mockMvc.perform(post("/api/collections/{collectionId}/items",
					collection.getCollectionId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(item)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.id").value("409"));
	}

	/** 실제 외래 키 위반에 대한 HTTP 400 변환 검증 */
	@Test
	@DisplayName("존재하지 않는 참조 번호는 400으로 변환")
	void dataIntegrityViolation() throws Exception {
		CollectionVO collection = createCollection();
		CollectionItemVO request = createItem(0, MISSING_CONTENT_ID);

		mockMvc.perform(post("/api/collections/{collectionId}/items",
					collection.getCollectionId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"));
	}

	/** 외래 키를 만족하는 회원과 컬렉션을 현재 트랜잭션에 등록 */
	private CollectionVO createCollection() {
		String token = createToken();
		MemberVO member = new MemberVO();
		member.setEmail("item-api-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("작품API" + token.substring(0, 8));
		member.setIntroduction("컬렉션 작품 API 통합 테스트 회원");
		member.setRole("USER");
		assertEquals(1, memberMapper.insertMember(member));

		CollectionVO collection = new CollectionVO(
				0,
				member.getMemberId().intValue(),
				"작품 API 통합 테스트 컬렉션",
				"컬렉션 작품 Controller 통합 테스트",
				"Y",
				null,
				null);
		assertEquals(1, collectionMapper.doSave(collection));

		return collection;
	}

	/** 외래 키를 만족하는 콘텐츠를 현재 트랜잭션에 등록 */
	private ContentVO createContent() {
		String token = createToken();
		ContentVO content = new ContentVO(
				0,
				"API_INTEGRATION_" + token,
				"API 통합 테스트 콘텐츠",
				"API Integration Test Content",
				"컬렉션 작품 Controller 통합 테스트 콘텐츠",
				"2026-08-26",
				120,
				"Korea",
				"https://example.com/poster.jpg",
				"https://example.com/backdrop.jpg",
				null);
		assertEquals(1, contentMapper.doSave(content));

		return content;
	}

	/** 컬렉션 작품 데이터 생성 */
	private CollectionItemVO createItem(int collectionId, int contentId) {
		return new CollectionItemVO(collectionId, contentId, null);
	}

	/** DB 고유 제약조건 충돌을 피할 테스트 식별자 생성 */
	private String createToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
