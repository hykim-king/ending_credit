package com.endit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;
import com.endit.service.CollectionItemService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * Class Name  : CollectionItemControllerTest
 * Description : 컬렉션 작품 REST API의 응답과 예외 변환을 검증하는 단위 테스트
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
@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionItemController 테스트")
class CollectionItemControllerTest {

	// 이 테스트의 대상은 HTTP 계층이므로 실제 Service 대신 Mock을 사용한다.
	// Service 내부 로직은 CollectionItemServiceTest에서 별도로 검증한다.
	@Mock
	private CollectionItemService collectionItemService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	/** Mock CollectionItemService를 사용하는 독립형 MockMvc 환경 준비 */
	@BeforeEach
	void setUp() {
		// standaloneSetup은 전체 Spring Boot Context와 DB를 띄우지 않고
		// 지정한 Controller의 매핑과 @ExceptionHandler만 빠르게 테스트한다.
		CollectionItemController controller =
				new CollectionItemController(collectionItemService);

		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
		// Java 요청 객체를 실제 HTTP 요청에 넣을 JSON 문자열로 바꿀 때 사용한다.
		objectMapper = new ObjectMapper();
	}

	/** 컬렉션 작품 목록과 페이징 정보 응답 검증 */
	@Test
	@DisplayName("컬렉션 작품 목록과 페이징 정보 반환")
	void retrieve() throws Exception {
		// Given: Service가 반환할 작품 한 건을 준비한다.
		CollectionItemVO item = createItem(1, 100);

		// thenAnswer를 사용하면 Controller가 만든 DTO 인스턴스를 직접 꺼내 수정할 수 있다.
		// 실제 Service도 동일한 DTO에 totalCnt를 기록하므로 운영 흐름을 흉내 낸다.
		when(collectionItemService.retrieve(anyInt(), any(DTO.class)))
				.thenAnswer(invocation -> {
					DTO param = invocation.getArgument(1);
					param.setTotalCnt(1);
					return List.of(item);
				});

		// When, Then: GET 요청 후 상태 코드, 목록 크기, JSON 필드를 확인한다.
		mockMvc.perform(get("/api/collections/1/items")
					.param("pageNo", "1")
					.param("pageSize", "12"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].collectionId").value(1))
				.andExpect(jsonPath("$.items[0].contentId").value(100))
				.andExpect(jsonPath("$.page.totalCnt").value(1));
	}

	/** 컬렉션 작품 단건 응답 검증 */
	@Test
	@DisplayName("컬렉션 작품 단건 반환")
	void getItem() throws Exception {
		// Given: 두 경로 변수로 조회하면 준비한 작품을 반환하도록 설정한다.
		when(collectionItemService.get(1, 100))
				.thenReturn(createItem(1, 100));

		// When, Then: Service 결과가 JSON 응답으로 직렬화되는지 확인한다.
		mockMvc.perform(get("/api/collections/1/items/100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.collectionId").value(1))
				.andExpect(jsonPath("$.contentId").value(100));
	}

	/** 컬렉션 작품 추가 성공 상태와 접근 URI 검증 */
	@Test
	@DisplayName("컬렉션 작품 추가 후 201과 Location 반환")
	void create() throws Exception {
		// Given: 클라이언트 요청 객체와 저장 후 반환 객체를 나누어 준비한다.
		CollectionItemVO request = createItem(0, 100);
		CollectionItemVO created = createItem(1, 100);

		when(collectionItemService.create(
				anyInt(), any(CollectionItemVO.class)))
				.thenReturn(created);

		// When, Then: JSON POST 결과가 201이며 Location에 복합 키 주소가 있는지 확인한다.
		mockMvc.perform(post("/api/collections/1/items")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string(
						"Location", "/api/collections/1/items/100"))
				.andExpect(jsonPath("$.contentId").value(100));
	}

	/** 컬렉션 작품 삭제 성공 상태 검증 */
	@Test
	@DisplayName("컬렉션 작품 삭제 후 204 반환")
	void deleteItem() throws Exception {
		// When, Then: 삭제 성공은 응답 본문 없이 204를 반환해야 한다.
		mockMvc.perform(delete("/api/collections/1/items/100"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		// 경로의 collectionId/contentId가 Service 인자로 정확히 전달됐는지도 확인한다.
		verify(collectionItemService).delete(1, 100);
	}

	/** 잘못된 요청값 예외의 HTTP 400 변환 검증 */
	@Test
	@DisplayName("잘못된 요청은 400으로 변환")
	void badRequest() throws Exception {
		// Given: Service 입력 검증 실패 상황을 의도적으로 만든다.
		when(collectionItemService.get(0, 100)).thenThrow(
				new IllegalArgumentException("올바른 컬렉션 번호가 필요합니다."));

		// When, Then: IllegalArgumentException이 400 상태의 MessageVO JSON으로 변환된다.
		mockMvc.perform(get("/api/collections/0/items/100"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"));
	}

	/** 컬렉션 작품 미존재 예외의 HTTP 404 변환 검증 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 작품은 404로 변환")
	void notFound() throws Exception {
		// Given: Service의 미존재 예외를 준비해 404 Handler만 분리해서 확인한다.
		when(collectionItemService.get(1, 999)).thenThrow(
				new NoSuchElementException("컬렉션에 포함되지 않은 작품입니다."));

		mockMvc.perform(get("/api/collections/1/items/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.id").value("404"));
	}

	/** 중복 작품 예외의 HTTP 409 변환 검증 */
	@Test
	@DisplayName("중복 작품 추가는 409로 변환")
	void conflict() throws Exception {
		// Given: 중복 추가 상황을 나타내는 IllegalStateException을 준비한다.
		when(collectionItemService.create(
				anyInt(), any(CollectionItemVO.class)))
				.thenThrow(new IllegalStateException("이미 컬렉션에 추가된 작품입니다."));

		mockMvc.perform(post("/api/collections/1/items")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(createItem(0, 100))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.id").value("409"));
	}

	/** 데이터 무결성 예외의 HTTP 400 변환 검증 */
	@Test
	@DisplayName("존재하지 않는 참조 번호는 400으로 변환")
	void dataIntegrityViolation() throws Exception {
		// Given: COLLECTION 또는 CONTENT 외래 키 위반 상황을 Spring 예외로 흉내 낸다.
		when(collectionItemService.create(
				anyInt(), any(CollectionItemVO.class)))
				.thenThrow(new DataIntegrityViolationException("FK 오류"));

		mockMvc.perform(post("/api/collections/1/items")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(createItem(0, 999))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.id").value("400"));
	}

	/** 테스트에 사용할 컬렉션 작품 정보 생성 */
	private CollectionItemVO createItem(int collectionId, int contentId) {
		// 모든 테스트가 동일한 최소 객체 생성 규칙을 사용하도록 도우미로 분리한다.
		return new CollectionItemVO(collectionId, contentId, null);
	}
}
