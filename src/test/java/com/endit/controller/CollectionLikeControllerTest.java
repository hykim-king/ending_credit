package com.endit.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.CollectionLikeVO;
import com.endit.domain.CollectionVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.CollectionLikeMapper;
import com.endit.mapper.CollectionMapper;
import com.endit.mapper.MemberMapper;

/**
 * <pre>
 * Class Name  : CollectionLikeControllerTest
 * Description : 컬렉션 좋아요 등록, 취소 및 회원별 좋아요 컬렉션 목록 API 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. gunwoo      최초 생성
 * 2026. 8. 28. jinyoung    조회 API 규격 변경 반영
 * 2026. 8. 29. jinyoung    인증·공개 범위·본인 제한 및 상태 조회 검증 추가
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

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	@Autowired
	private CollectionMapper collectionMapper;

	@Autowired
	private MemberMapper memberMapper;

	@Value("${endit.dev-auth.member-id}")
	private long authenticatedMemberId;

	/** 인증 회원의 컬렉션 좋아요 등록 API 검증 */
	@Test
	@DisplayName("컬렉션 좋아요 등록 API")
	void like() throws Exception {
		CollectionVO collection = createCollection(createMemberId(), "Y");

		mockMvc.perform(post("/api/collections/{collectionId}/likes",
					collection.getCollectionId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberId")
						.value(Math.toIntExact(authenticatedMemberId)))
				.andExpect(jsonPath("$.collectionId")
						.value(collection.getCollectionId()));
	}

	/** 이미 좋아요한 컬렉션의 멱등 등록 검증 */
	@Test
	@DisplayName("이미 좋아요한 컬렉션 재등록은 멱등 응답")
	void likeIdempotent() throws Exception {
		CollectionVO collection = createCollection(createMemberId(), "Y");
		CollectionLikeVO existing = likeKey(collection.getCollectionId());
		assertEquals(1, collectionLikeMapper.insertCollectionLike(existing));

		mockMvc.perform(post("/api/collections/{collectionId}/likes",
					collection.getCollectionId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberId")
						.value(Math.toIntExact(authenticatedMemberId)))
				.andExpect(jsonPath("$.collectionId")
						.value(collection.getCollectionId()));
	}

	/** 인증 회원의 컬렉션 좋아요 취소 API 검증 */
	@Test
	@DisplayName("컬렉션 좋아요 취소 API")
	void unlike() throws Exception {
		CollectionVO collection = createCollection(createMemberId(), "Y");
		CollectionLikeVO existing = likeKey(collection.getCollectionId());
		assertEquals(1, collectionLikeMapper.insertCollectionLike(existing));

		mockMvc.perform(delete("/api/collections/{collectionId}/likes",
					collection.getCollectionId()))
				.andExpect(status().isNoContent());

		assertNull(collectionLikeMapper.selectCollectionLike(existing));
	}

	/** 좋아요가 없어도 취소 요청을 멱등 처리하는지 검증 */
	@Test
	@DisplayName("좋아요가 없어도 취소 요청은 204")
	void unlikeIdempotent() throws Exception {
		CollectionVO collection = createCollection(createMemberId(), "Y");

		mockMvc.perform(delete("/api/collections/{collectionId}/likes",
					collection.getCollectionId()))
				.andExpect(status().isNoContent());
	}

	/** 현재 인증 회원이 좋아요한 컬렉션의 상태 조회 검증 */
	@Test
	@DisplayName("좋아요한 컬렉션 상태는 true")
	void likeStatusTrue() throws Exception {
		CollectionVO collection = createCollection(createMemberId(), "Y");
		assertEquals(1, collectionLikeMapper.insertCollectionLike(
				likeKey(collection.getCollectionId())));

		mockMvc.perform(get("/api/collections/{collectionId}/likes",
					collection.getCollectionId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(true));
	}

	/** 현재 인증 회원이 좋아요하지 않은 컬렉션의 상태 조회 검증 */
	@Test
	@DisplayName("좋아요하지 않은 컬렉션 상태는 false")
	void likeStatusFalse() throws Exception {
		CollectionVO collection = createCollection(createMemberId(), "Y");

		mockMvc.perform(get("/api/collections/{collectionId}/likes",
					collection.getCollectionId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false));
	}

	/** 특정 회원의 공개 좋아요 컬렉션 목록 조회 검증 */
	@Test
	@DisplayName("회원별 좋아요 컬렉션 목록 조회 API")
	void retrieveByMember() throws Exception {
		int targetMemberId = createMemberId();
		CollectionVO first = createCollection(createMemberId(), "Y");
		CollectionVO second = createCollection(createMemberId(), "Y");
		assertEquals(1, collectionLikeMapper.insertCollectionLike(
				new CollectionLikeVO(
						targetMemberId, first.getCollectionId(), null)));
		assertEquals(1, collectionLikeMapper.insertCollectionLike(
				new CollectionLikeVO(
						targetMemberId, second.getCollectionId(), null)));

		mockMvc.perform(get("/api/users/{memberId}/likes", targetMemberId)
					.param("type", "collection")
					.param("page", "1")
					.param("size", "12"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.page.totalCnt").value(2));
	}

	/** 좋아요 컬렉션이 없는 회원의 빈 목록 검증 */
	@Test
	@DisplayName("좋아요한 컬렉션이 없으면 빈 목록 응답")
	void retrieveByMemberEmpty() throws Exception {
		int targetMemberId = createMemberId();

		mockMvc.perform(get("/api/users/{memberId}/likes", targetMemberId)
					.param("type", "collection"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(0));
	}

	/** 컬렉션 이외의 좋아요 타입 거부 검증 */
	@Test
	@DisplayName("type=collection이 아니면 400 응답")
	void retrieveByMemberInvalidType() throws Exception {
		mockMvc.perform(get("/api/users/{memberId}/likes", createMemberId())
					.param("type", "people"))
				.andExpect(status().isBadRequest());
	}

	/** 인증 회원 본인 컬렉션 좋아요 금지 검증 */
	@Test
	@DisplayName("본인 컬렉션 좋아요는 403")
	void likeOwnCollection() throws Exception {
		CollectionVO collection = createCollection(
				Math.toIntExact(authenticatedMemberId), "Y");

		mockMvc.perform(post("/api/collections/{collectionId}/likes",
					collection.getCollectionId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.id").value("403"));
	}

	/** 비공개 컬렉션 비소유자의 좋아요 요청 은닉 검증 */
	@Test
	@DisplayName("비공개 컬렉션 비소유자 좋아요는 404")
	void likePrivateCollection() throws Exception {
		CollectionVO collection = createCollection(createMemberId(), "N");

		mockMvc.perform(post("/api/collections/{collectionId}/likes",
					collection.getCollectionId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.id").value("404"));
	}

	/** 테스트 회원 등록 */
	private int createMemberId() {
		String token = UUID.randomUUID().toString().replace("-", "");
		MemberVO member = new MemberVO();
		member.setEmail("collection-like-api-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("좋아요API" + token.substring(0, 8));
		member.setIntroduction("컬렉션 좋아요 API 테스트 회원");
		member.setRole("USER");
		assertEquals(1, memberMapper.insertMember(member));
		return member.getMemberId().intValue();
	}

	/** 지정한 회원 소유 테스트 컬렉션 등록 */
	private CollectionVO createCollection(int memberId, String isPublic) {
		CollectionVO collection = new CollectionVO(
				0,
				memberId,
				"좋아요 API 컬렉션 " + UUID.randomUUID(),
				"컬렉션 좋아요 Controller 테스트",
				isPublic,
				null,
				null);
		assertEquals(1, collectionMapper.doSave(collection));
		return collection;
	}

	/** 인증 회원의 좋아요 복합키 생성 */
	private CollectionLikeVO likeKey(int collectionId) {
		return new CollectionLikeVO(
				Math.toIntExact(authenticatedMemberId), collectionId, null);
	}
}