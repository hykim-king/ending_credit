package com.endit.support;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.jdbc.core.JdbcTemplate;

import com.endit.domain.CollectionVO;
import com.endit.domain.ContentVO;
import com.endit.domain.MemberVO;
import com.endit.domain.PersonVO;

/**
 * 통합 테스트의 외래 키를 위한 회원·컬렉션·콘텐츠·인물 픽스처
 *
 * <p>운영 시퀀스와 적재 데이터의 PK 불일치 영향을 피하도록 테스트 전용 양수 PK로 직접 등록.</p>
 *
 * <p>서비스의 {@code id > 0} 조건을 만족하며, {@code @Transactional} 테스트에서 사용해 등록 행을 롤백.</p>
 */
public final class DatabaseTestFixtures {

	private static final AtomicInteger MEMBER_IDS = new AtomicInteger(1_900_000_000);
	private static final AtomicInteger COLLECTION_IDS = new AtomicInteger(1_600_000_000);
	private static final AtomicInteger CONTENT_IDS = new AtomicInteger(1_800_000_000);
	private static final AtomicInteger PERSON_IDS = new AtomicInteger(1_700_000_000);

	/** 유틸리티 클래스의 인스턴스 생성 방지 */
	private DatabaseTestFixtures() {
	}

	/**
	 * 테스트 회원을 명시적인 PK로 등록
	 *
	 * @param jdbcTemplate 현재 테스트 트랜잭션에 참여하는 JdbcTemplate
	 * @param member 등록할 회원 정보
	 * @return PK가 채워진 동일 회원 객체
	 */
	public static MemberVO insertMember(JdbcTemplate jdbcTemplate, MemberVO member) {
		int memberId = nextUnusedId(jdbcTemplate, MEMBER_IDS, "SELECT COUNT(*) FROM MEMBER WHERE MEMBER_ID = ?");
		member.setMemberId(Long.valueOf(memberId));

		int affected = jdbcTemplate.update("""
				INSERT INTO MEMBER (
					MEMBER_ID, EMAIL, PASSWORD, NICKNAME, INTRODUCTION,
					PROFILE_IMG_URL, ROLE, CREATED_DT
				) VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE)
				""", memberId, member.getEmail(), member.getPassword(), member.getNickname(), member.getIntroduction(),
				member.getProfileImgUrl(), member.getRole());

		assertSingleInsert("MEMBER", affected);
		return member;
	}

	/**
	 * 대상 테스트의 외래 키와 컬렉션 JOIN을 만족하는 컬렉션을 등록
	 *
	 * @param jdbcTemplate 현재 테스트 트랜잭션에 참여하는 JdbcTemplate
	 * @param collection 등록할 컬렉션 정보
	 * @return PK가 채워진 동일 컬렉션 객체
	 */
	public static CollectionVO insertCollection(JdbcTemplate jdbcTemplate, CollectionVO collection) {
		int collectionId = nextUnusedId(
				jdbcTemplate,
				COLLECTION_IDS,
				"SELECT COUNT(*) FROM COLLECTION WHERE COLLECTION_ID = ?");
		collection.setCollectionId(collectionId);

		int affected = jdbcTemplate.update("""
				INSERT INTO COLLECTION (
					COLLECTION_ID, MEMBER_ID, TITLE, DESCRIPTION,
					IS_PUBLIC, CREATED_DT
				) VALUES (?, ?, ?, ?, ?, SYSDATE)
				""", collectionId, collection.getMemberId(), collection.getTitle(), collection.getDescription(),
				collection.getIsPublic());

		assertSingleInsert("COLLECTION", affected);
		return collection;
	}

	/**
	 * 대상 테스트의 외래 키와 콘텐츠 JOIN을 만족하는 콘텐츠를 등록
	 *
	 * @param jdbcTemplate 현재 테스트 트랜잭션에 참여하는 JdbcTemplate
	 * @param content 등록할 콘텐츠 정보
	 * @return PK가 채워진 동일 콘텐츠 객체
	 */
	public static ContentVO insertContent(JdbcTemplate jdbcTemplate, ContentVO content) {
		int contentId = nextUnusedId(jdbcTemplate, CONTENT_IDS, "SELECT COUNT(*) FROM CONTENT WHERE CONTENT_ID = ?");
		content.setContentId(contentId);

		int affected = jdbcTemplate.update("""
				INSERT INTO CONTENT (
					CONTENT_ID, EXTERNAL_ID, TITLE_KO, TITLE_ORG, OVERVIEW,
					RELEASE_YEAR, RUNTIME_MIN, COUNTRY, POSTER_URL,
					BACKDROP_URL, CREATED_DT
				) VALUES (?, ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, SYSDATE)
				""", contentId, content.getExternalId(), content.getTitleKo(), content.getTitleOrg(), content.getOverview(),
				content.getReleaseYear(), content.getRuntimeMin(), content.getCountry(), content.getPosterUrl(), content.getBackdropUrl());

		assertSingleInsert("CONTENT", affected);
		return content;
	}

	/**
	 * 대상 테스트의 외래 키와 인물 JOIN을 만족하는 인물을 등록
	 *
	 * @param jdbcTemplate 현재 테스트 트랜잭션에 참여하는 JdbcTemplate
	 * @param person 등록할 인물 정보
	 * @return PK가 채워진 동일 인물 객체
	 */
	public static PersonVO insertPerson(JdbcTemplate jdbcTemplate, PersonVO person) {
		int personId = nextUnusedId(jdbcTemplate, PERSON_IDS, "SELECT COUNT(*) FROM PERSON WHERE PERSON_ID = ?");
		person.setPersonId(personId);

		int affected = jdbcTemplate.update("""
				INSERT INTO PERSON (
					PERSON_ID, EXTERNAL_ID, NAME_KO, NAME_ORG,
					PROFILE_IMAGE_URL, CREATED_DT, UPDATED_DT
				) VALUES (?, ?, ?, ?, ?, SYSDATE, NULL)
				""", personId, person.getExternalId(), person.getNameKo(), person.getNameOrg(), person.getProfileImageUrl());

		assertSingleInsert("PERSON", affected);
		return person;
	}

	/**
	 * DB에서 사용하지 않는 테스트 PK 선택
	 * JVM 내 후보 중복 방지, 사용 중인 번호는 건너뜀
	 *
	 * @param jdbcTemplate 현재 테스트 트랜잭션의 JdbcTemplate
	 * @param candidates 테스트 PK 후보
	 * @param countSql 후보 PK의 존재 여부 조회 SQL
	 * @return 사용하지 않는 양수 PK
	 */
	private static int nextUnusedId(JdbcTemplate jdbcTemplate, AtomicInteger candidates, String countSql) {
		for (int attempt = 0; attempt < 10_000; attempt++) {
			int candidate = candidates.getAndDecrement();
			Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, candidate);

			if (count != null && count.intValue() == 0) {
				return candidate;
			}
		}

		throw new IllegalStateException("테스트 전용 PK 후보를 확보하지 못했습니다.");
	}

	/**
	 * 부모 픽스처가 정확히 한 행 등록됐는지 즉시 확인
	 *
	 * @param tableName 등록 대상 테이블명
	 * @param affected 등록된 행 수
	 */
	private static void assertSingleInsert(String tableName, int affected) {
		if (affected != 1) {
			throw new IllegalStateException(tableName + " 테스트 픽스처 등록 건수가 1이 아닙니다: " + affected);
		}
	}
}
