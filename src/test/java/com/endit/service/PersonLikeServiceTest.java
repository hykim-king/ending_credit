package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.MemberVO;
import com.endit.domain.PersonLikeVO;
import com.endit.domain.PersonVO;
import com.endit.mapper.MemberMapper;
import com.endit.mapper.PersonMapper;

/**
 * <pre>
 * Class Name  : PersonLikeServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 인물 좋아요 Service를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@SpringBootTest
@Transactional
@DisplayName("PersonLikeService 통합 테스트")
class PersonLikeServiceTest {

	@Autowired
	private PersonLikeService personLikeService;

	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private PersonMapper personMapper;

	private int memberId;
	private int personId;

	@BeforeEach
	void setUp() {
		// 공용 데이터에 의존하지 않도록 테스트마다 회원과 인물을 생성한다.
		memberId = createMemberId();
		personId = createPersonId("DEFAULT");
	}

	@Test
	@DisplayName("좋아요 목록 조회")
	void retrieveLikes() {
		// Given: 한 회원이 두 인물을 좋아요하고, 다른 회원도 별도 좋아요를 등록한다.
		personLikeService.addLike(memberId, personId);

		int secondPersonId = createPersonId("SECOND");
		personLikeService.addLike(memberId, secondPersonId);

		int otherMemberId = createMemberId();
		int otherPersonId = createPersonId("OTHER");
		personLikeService.addLike(otherMemberId, otherPersonId);

		DTO param = new DTO();

		// When: 조회 대상 회원의 최신 좋아요 목록을 조회한다.
		List<PersonLikeVO> result = personLikeService.retrieveLikes(memberId, param, "latest");

		// Then: 해당 회원의 좋아요와 JOIN된 인물 정보만 반환해야 한다.
		assertEquals(2, result.size());
		assertTrue(result.stream().allMatch(item -> item.getMemberId() == memberId));
		assertTrue(result.stream().anyMatch(item -> item.getPersonId() == personId));
		assertTrue(result.stream().anyMatch(item -> item.getPersonId() == secondPersonId));
		assertTrue(result.stream().allMatch(item -> item.getExternalId() != null));
		assertTrue(result.stream().allMatch(item -> item.getNameKo() != null));
		assertTrue(result.stream().allMatch(item -> item.getProfileImageUrl() != null));

		assertEquals(1, param.getPageNo());
		assertEquals(12, param.getPageSize());
		assertEquals(2, param.getTotalCnt());
		assertEquals("10", param.getSearchDiv());
		assertEquals(String.valueOf(memberId), param.getSearchWord());
	}

	@Test
	@DisplayName("빈 목록 조회")
	void retrieveEmpty() {
		DTO param = new DTO();

		// When: 좋아요가 없는 회원의 목록을 조회한다.
		List<PersonLikeVO> result = personLikeService.retrieveLikes(memberId, param, "latest");

		// Then: null 대신 빈 목록을 반환하고 전체 건수는 0이어야 한다.
		assertNotNull(result);
		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	@Test
	@DisplayName("페이징 보정")
	void normalizePaging() {
		DTO param = new DTO();
		param.setPageNo(0);
		param.setPageSize(101);

		// When: 잘못된 페이지 번호와 최댓값을 초과한 크기로 조회한다.
		personLikeService.retrieveLikes(memberId, param, "latest");

		// Then: 페이지 번호는 1, 페이지 크기는 최대 100으로 보정되어야 한다.
		assertEquals(1, param.getPageNo());
		assertEquals(100, param.getPageSize());
	}

	@Test
	@DisplayName("좋아요 등록")
	void addLike() {
		// When: 좋아요가 없는 회원과 인물 조합을 등록한다.
		PersonLikeVO result = personLikeService.addLike(memberId, personId);

		// Then: 복합 PK와 DB에서 생성한 등록 일시가 반환되어야 한다.
		assertEquals(memberId, result.getMemberId());
		assertEquals(personId, result.getPersonId());
		assertNotNull(result.getCreatedDt());
		assertTrue(personLikeService.isLiked(memberId, personId));
	}

	@Test
	@DisplayName("중복 등록")
	void addLikeAgain() {
		// Given: 같은 회원과 인물의 좋아요를 먼저 등록한다.
		PersonLikeVO first = personLikeService.addLike(memberId, personId);

		// When: 동일한 등록 요청을 다시 수행한다.
		PersonLikeVO second = personLikeService.addLike(memberId, personId);

		// Then: 새 행을 만들지 않고 기존 좋아요 정보를 반환해야 한다.
		assertEquals(first.getMemberId(), second.getMemberId());
		assertEquals(first.getPersonId(), second.getPersonId());
		assertEquals(first.getCreatedDt(), second.getCreatedDt());
		assertEquals(1, personLikeService.countLikes(personId));
	}

	@Test
	@DisplayName("좋아요 여부 조회")
	void checkLiked() {
		// Then: 등록 전에는 false여야 한다.
		assertFalse(personLikeService.isLiked(memberId, personId));

		// When: 인물 좋아요를 등록한다.
		personLikeService.addLike(memberId, personId);

		// Then: 등록 후에는 true여야 한다.
		assertTrue(personLikeService.isLiked(memberId, personId));
	}

	@Test
	@DisplayName("좋아요 수 조회")
	void countLikes() {
		// Given: 서로 다른 두 회원이 같은 인물을 좋아요한다.
		int secondMemberId = createMemberId();
		personLikeService.addLike(memberId, personId);
		personLikeService.addLike(secondMemberId, personId);

		// When, Then: 해당 인물의 좋아요 수는 2여야 한다.
		assertEquals(2, personLikeService.countLikes(personId));
	}

	@Test
	@DisplayName("좋아요 해제")
	void deleteLike() {
		// Given: 삭제할 인물 좋아요를 등록한다.
		personLikeService.addLike(memberId, personId);

		// When: 좋아요를 해제한다.
		personLikeService.deleteLike(memberId, personId);

		// Then: 좋아요 상태와 건수가 모두 해제되어야 한다.
		assertFalse(personLikeService.isLiked(memberId, personId));
		assertEquals(0, personLikeService.countLikes(personId));
	}

	@Test
	@DisplayName("반복 해제")
	void deleteLikeAgain() {
		// Then: 등록되지 않은 좋아요 해제 요청도 예외 없이 처리되어야 한다.
		assertDoesNotThrow(() -> personLikeService.deleteLike(memberId, personId));

		personLikeService.addLike(memberId, personId);
		personLikeService.deleteLike(memberId, personId);

		// Then: 이미 해제된 좋아요를 다시 해제해도 최종 상태는 같아야 한다.
		assertDoesNotThrow(() -> personLikeService.deleteLike(memberId, personId));
		assertFalse(personLikeService.isLiked(memberId, personId));
	}

	@Test
	@DisplayName("정렬 조건 검증")
	void validateSort() {
		assertThrows(IllegalArgumentException.class,
				() -> personLikeService.retrieveLikes(memberId, new DTO(), "popular"));

		assertThrows(IllegalArgumentException.class,
				() -> personLikeService.retrieveLikes(memberId, new DTO(), null));
	}

	@Test
	@DisplayName("조회 조건 검증")
	void validateParam() {
		assertThrows(IllegalArgumentException.class,
				() -> personLikeService.retrieveLikes(memberId, null, "latest"));
	}

	@Test
	@DisplayName("번호 검증")
	void validateIds() {
		assertThrows(IllegalArgumentException.class,
				() -> personLikeService.retrieveLikes(0, new DTO(), "latest"));

		assertThrows(IllegalArgumentException.class,
				() -> personLikeService.countLikes(0));

		assertThrows(IllegalArgumentException.class,
				() -> personLikeService.isLiked(0, personId));

		assertThrows(IllegalArgumentException.class,
				() -> personLikeService.isLiked(memberId, 0));

		assertThrows(IllegalArgumentException.class,
				() -> personLikeService.addLike(memberId, -1));

		assertThrows(IllegalArgumentException.class,
				() -> personLikeService.deleteLike(-1, personId));
	}

	/**
	 * PERSON_LIKE 외래 키를 만족하는 고유 테스트 회원 생성
	 */
	private int createMemberId() {
		String token = createToken();

		MemberVO member = new MemberVO();
		member.setEmail("person-like-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("인물좋아요" + token.substring(0, 6));
		member.setIntroduction("인물 좋아요 Service 통합 테스트 회원");
		member.setRole("USER");

		assertEquals(1, memberMapper.insertMember(member));
		assertNotNull(member.getMemberId());

		return member.getMemberId().intValue();
	}

	/**
	 * PERSON_LIKE 외래 키와 목록 JOIN을 만족하는 고유 테스트 인물 생성
	 */
	private int createPersonId(String prefix) {
		String token = createToken();

		PersonVO person = new PersonVO(
				0,
				"PERSON_LIKE_" + prefix + "_" + token.substring(0, 20),
				"인물좋아요" + token.substring(0, 6),
				"Person Like " + token.substring(0, 6),
				"https://example.com/person.jpg",
				null,
				null);

		assertEquals(1, personMapper.doSave(person));
		assertTrue(person.getPersonId() > 0);

		return person.getPersonId();
	}

	/**
	 * 회원과 인물의 고유 제약조건 충돌 방지용 문자열 생성
	 */
	private String createToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
