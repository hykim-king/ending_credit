package com.endit.support;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.jdbc.core.JdbcTemplate;

import com.endit.domain.ContentVO;
import com.endit.domain.MemberVO;
import com.endit.domain.PersonVO;

/**
 * 통합 테스트가 대상 기능과 무관한 부모 데이터를 독립적으로 준비하도록 돕는 픽스처.
 *
 * <p>회원 콘텐츠, 인물 좋아요, 컬렉션 작품 테스트에서 MEMBER·CONTENT·PERSON은
 * 테스트 대상이 아니라 외래 키를 만족시키기 위한 선행 데이터다. 이런 데이터까지 운영
 * Mapper의 {@code selectKey}를 사용하면, 외부에서 적재한 데이터의 최대 PK와 DB 시퀀스가
 * 잠시 어긋난 것만으로도 실제 검증 로직에 도달하기 전에 테스트가 실패한다.</p>
 *
 * <p>따라서 이 클래스는 일반 서비스 데이터와 겹치기 어려운 양수 상위 구간에서 테스트
 * 전용 PK를 만들고 JDBC로 부모 행을 직접 등록한다. 양수를 사용하는 이유는 서비스의
 * 정상 ID 검증 조건({@code id > 0})도 그대로 만족시키기 위해서다. 모든 호출부는
 * {@code @Transactional} 테스트 안에서 사용하므로 행은 테스트 종료 시 롤백된다.</p>
 */
public final class DatabaseTestFixtures {

	private static final AtomicInteger MEMBER_IDS =
			new AtomicInteger(1_900_000_000);
	private static final AtomicInteger CONTENT_IDS =
			new AtomicInteger(1_800_000_000);
	private static final AtomicInteger PERSON_IDS =
			new AtomicInteger(1_700_000_000);

	private DatabaseTestFixtures() {
		// static 테스트 지원 메서드만 제공하므로 인스턴스 생성을 막는다.
	}

	/**
	 * 테스트 회원을 명시적인 PK로 등록한다.
	 *
	 * @param jdbcTemplate 현재 테스트 트랜잭션에 참여하는 JdbcTemplate
	 * @param member 등록할 회원 정보
	 * @return PK가 채워진 동일 회원 객체
	 */
	public static MemberVO insertMember(
			JdbcTemplate jdbcTemplate,
			MemberVO member) {

		int memberId = nextUnusedId(
				jdbcTemplate,
				MEMBER_IDS,
				"SELECT COUNT(*) FROM MEMBER WHERE MEMBER_ID = ?");
		member.setMemberId(Long.valueOf(memberId));

		int affected = jdbcTemplate.update("""
				INSERT INTO MEMBER (
					MEMBER_ID, EMAIL, PASSWORD, NICKNAME, INTRODUCTION,
					PROFILE_IMG_URL, ROLE, CREATED_DT
				) VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE)
				""",
				memberId,
				member.getEmail(),
				member.getPassword(),
				member.getNickname(),
				member.getIntroduction(),
				member.getProfileImgUrl(),
				member.getRole());

		assertSingleInsert("MEMBER", affected);
		return member;
	}

	/**
	 * 대상 테스트의 외래 키와 콘텐츠 JOIN을 만족하는 콘텐츠를 등록한다.
	 *
	 * @param jdbcTemplate 현재 테스트 트랜잭션에 참여하는 JdbcTemplate
	 * @param content 등록할 콘텐츠 정보
	 * @return PK가 채워진 동일 콘텐츠 객체
	 */
	public static ContentVO insertContent(
			JdbcTemplate jdbcTemplate,
			ContentVO content) {

		int contentId = nextUnusedId(
				jdbcTemplate,
				CONTENT_IDS,
				"SELECT COUNT(*) FROM CONTENT WHERE CONTENT_ID = ?");
		content.setContentId(contentId);

		int affected = jdbcTemplate.update("""
				INSERT INTO CONTENT (
					CONTENT_ID, EXTERNAL_ID, TITLE_KO, TITLE_ORG, OVERVIEW,
					RELEASE_YEAR, RUNTIME_MIN, COUNTRY, POSTER_URL,
					BACKDROP_URL, CREATED_DT
				) VALUES (?, ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, SYSDATE)
				""",
				contentId,
				content.getExternalId(),
				content.getTitleKo(),
				content.getTitleOrg(),
				content.getOverview(),
				content.getReleaseYear(),
				content.getRuntimeMin(),
				content.getCountry(),
				content.getPosterUrl(),
				content.getBackdropUrl());

		assertSingleInsert("CONTENT", affected);
		return content;
	}

	/**
	 * 대상 테스트의 외래 키와 인물 JOIN을 만족하는 인물을 등록한다.
	 *
	 * @param jdbcTemplate 현재 테스트 트랜잭션에 참여하는 JdbcTemplate
	 * @param person 등록할 인물 정보
	 * @return PK가 채워진 동일 인물 객체
	 */
	public static PersonVO insertPerson(
			JdbcTemplate jdbcTemplate,
			PersonVO person) {

		int personId = nextUnusedId(
				jdbcTemplate,
				PERSON_IDS,
				"SELECT COUNT(*) FROM PERSON WHERE PERSON_ID = ?");
		person.setPersonId(personId);

		int affected = jdbcTemplate.update("""
				INSERT INTO PERSON (
					PERSON_ID, EXTERNAL_ID, NAME_KO, NAME_ORG,
					PROFILE_IMAGE_URL, CREATED_DT, UPDATED_DT
				) VALUES (?, ?, ?, ?, ?, SYSDATE, NULL)
				""",
				personId,
				person.getExternalId(),
				person.getNameKo(),
				person.getNameOrg(),
				person.getProfileImageUrl());

		assertSingleInsert("PERSON", affected);
		return person;
	}

	/**
	 * JVM 안에서는 AtomicInteger로 중복을 막고, 이미 적재된 외부 데이터와도 충돌하지
	 * 않도록 DB 존재 여부를 마지막으로 확인한다. 후보가 사용 중이면 다음 값을 사용한다.
	 */
	private static int nextUnusedId(
			JdbcTemplate jdbcTemplate,
			AtomicInteger candidates,
			String countSql) {

		for (int attempt = 0; attempt < 10_000; attempt++) {
			int candidate = candidates.getAndDecrement();
			Integer count = jdbcTemplate.queryForObject(
					countSql, Integer.class, candidate);

			if (count != null && count.intValue() == 0) {
				return candidate;
			}
		}

		throw new IllegalStateException(
				"테스트 전용 PK 후보를 확보하지 못했습니다.");
	}

	/** 부모 픽스처가 정확히 한 행 등록됐는지 즉시 확인한다. */
	private static void assertSingleInsert(String tableName, int affected) {
		if (affected != 1) {
			throw new IllegalStateException(
					tableName + " 테스트 픽스처 등록 건수가 1이 아닙니다: "
							+ affected);
		}
	}
}
