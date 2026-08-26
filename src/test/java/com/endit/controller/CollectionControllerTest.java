package com.endit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionVO;
import com.endit.service.CollectionService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * Class Name  : CollectionControllerTest
 * Description : 컬렉션 REST API의 상태 코드, 응답 본문 및 예외 변환을 검증하는 단위 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 21. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 21.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionController 테스트")
class CollectionControllerTest {

	// HTTP 매핑과 응답 변환만 확인하기 위해 실제 Service 대신 Mock을 사용한다.
	@Mock
	private CollectionService collectionService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	/** Mock CollectionService를 사용하는 독립형 MockMvc 환경 준비 */
	@BeforeEach
	void setUp() {
		// 전체 Spring Context를 실행하지 않아 DB 없이 Controller만 빠르게 확인할 수 있다.
		CollectionController controller = new CollectionController(collectionService);

		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		// 등록·수정 요청 객체를 JSON 본문으로 직렬화하기 위한 Jackson 객체다.
		objectMapper = new ObjectMapper();
	}

	/** 컬렉션 목록과 페이징 정보 응답 검증 */
	@Test
	@DisplayName("컬렉션 목록과 페이징 정보 반환")
	void retrieve() throws Exception {
		
		// Given: Service가 반환할 컬렉션 목록과 전체 건수를 준비
		CollectionVO collection = createCollection(1, "테스트 컬렉션");

		// 실제 Service처럼 전달된 DTO에 totalCnt를 기록해야 하므로 thenAnswer를 사용한다.
		when(collectionService.retrieve(any(DTO.class)))
				.thenAnswer(invocation -> {
					DTO param = invocation.getArgument(0);
					param.setTotalCnt(1);

					return List.of(collection);
				});

		// When, Then: 목록 API가 컬렉션 목록과 페이징 정보를 반환해야 함
		mockMvc.perform(get("/api/collections")
					.param("pageNo", "1")
					.param("pageSize", "10")
					.param("searchDiv", "10")
					.param("searchWord", "테스트"))
				.andExpect(status().isOk())
				.andExpect(content()
						.contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath(
						"$.items[0].collectionId").value(1))
				.andExpect(jsonPath(
						"$.items[0].title")
						.value("테스트 컬렉션"))
				.andExpect(jsonPath(
						"$.page.pageNo").value(1))
				.andExpect(jsonPath(
						"$.page.pageSize").value(10))
				.andExpect(jsonPath(
						"$.page.totalCnt").value(1));
	}

	/** 컬렉션 단건 조회 응답 검증 */
	@Test
	@DisplayName("컬렉션 단건 반환")
	void getCollection() throws Exception {
		
		// Given: Service가 반환할 컬렉션 단건 정보를 준비
		when(collectionService.get(1))
				.thenReturn(createCollection(1, "테스트 컬렉션"));

		// When, Then: 단건 API가 조회한 컬렉션 정보를 반환해야 함
		mockMvc.perform(get("/api/collections/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath(
						"$.collectionId").value(1))
				.andExpect(jsonPath(
						"$.title").value("테스트 컬렉션"));
	}

	/** 컬렉션 등록 성공 상태와 접근 URI 응답 검증 */
	@Test
	@DisplayName("컬렉션 등록 후 201과 Location 반환")
	void create() throws Exception {
		
		// Given: 컬렉션 등록 요청과 Service 등록 결과를 준비
		CollectionVO request = createCollection(0, "신규 컬렉션");
		CollectionVO created = createCollection(7, "신규 컬렉션");

		when(collectionService.create(any(CollectionVO.class)))
				.thenReturn(created);

		// When, Then: 등록 API가 201 상태와 생성된 컬렉션 URI를 반환해야 함
		mockMvc.perform(post("/api/collections")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string(
						"Location", "/api/collections/7"))
				.andExpect(jsonPath(
						"$.collectionId").value(7))
				.andExpect(jsonPath(
						"$.title").value("신규 컬렉션"));
	}

	/** 컬렉션 수정 결과 응답 검증 */
	@Test
	@DisplayName("컬렉션 수정 결과 반환")
	void update() throws Exception {
		
		// Given: 컬렉션 수정 요청과 Service 수정 결과를 준비
		CollectionVO request = createCollection(0, "수정 컬렉션");
		CollectionVO updated = createCollection(1, "수정 컬렉션");

		when(collectionService
				.update(anyInt(), any(CollectionVO.class)))
				.thenReturn(updated);

		// When, Then: 수정 API가 수정된 컬렉션 정보를 반환해야 함
		mockMvc.perform(put("/api/collections/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath(
						"$.collectionId").value(1))
				.andExpect(jsonPath(
						"$.title").value("수정 컬렉션"));
	}

	/** 컬렉션 삭제 성공 상태와 Service 호출 검증 */
	@Test
	@DisplayName("컬렉션 삭제 후 204 반환")
	void deleteCollection() throws Exception {
		// When: 컬렉션 삭제 API를 호출
		mockMvc.perform(delete("/api/collections/1"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		// Then: 요청한 컬렉션 번호로 Service 삭제 메서드를 호출해야 함
		verify(collectionService).delete(1);
	}

	/** 잘못된 요청값 예외의 HTTP 400 변환 검증 */
	@Test
	@DisplayName("잘못된 요청은 400으로 변환한다")
	void badRequest() throws Exception {
		
		// Given: Service에서 잘못된 요청값 예외가 발생하도록 준비
		when(collectionService.get(0)).thenThrow(
				new IllegalArgumentException("올바른 컬렉션 번호가 필요합니다."));

		// When, Then: 예외 처리 결과로 400 상태와 오류 메시지를 반환해야 함
		mockMvc.perform(get("/api/collections/0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath(
						"$.id").value("400"))
				.andExpect(jsonPath(
						"$.message").value("올바른 컬렉션 번호가 필요합니다."));
	}

	/** 컬렉션 미존재 예외의 HTTP 404 변환 검증 */
	@Test
	@DisplayName("존재하지 않는 컬렉션은 404로 변환한다")
	void notFound() throws Exception {
		
		// Given: Service에서 컬렉션 미존재 예외가 발생하도록 준비
		when(collectionService.get(999)).thenThrow(
				new NoSuchElementException("존재하지 않는 컬렉션입니다."));

		// When, Then: 예외 처리 결과로 404 상태와 오류 메시지를 반환해야 함
		mockMvc.perform(get("/api/collections/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath(
						"$.id").value("404"))
				.andExpect(jsonPath(
						"$.message").value("존재하지 않는 컬렉션입니다."));
	}

	/** 테스트에 사용할 컬렉션 정보 생성 */
	private CollectionVO createCollection(
			int collectionId,
			String title) {

		return new CollectionVO(
				collectionId,
				10,
				title,
				"컬렉션 설명",
				"Y",
				null,
				null);
	}
}
