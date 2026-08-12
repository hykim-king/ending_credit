/**
 * <pre>
 * Class Name : MapperTestFixture
 * Description : 4조 매퍼 JUnit 공통 픽스처
 *               코멘트·좋아요·신고는 FK 때문에 부모(회원·영화·컬렉션)가 먼저 있어야 한다.
 *               테스트가 항상 같은 상태에서 시작하도록(Test Isolation)
 *               이전 픽스처를 지우고 매번 다시 심는다.
 *               ⚠️ 로컬 개발 DB(scott_test) 전용. 팀 공용 DB에 물리면 안 된다.
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 12.  홍선기   최초 생성
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 12.
 */
package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.jdbc.core.JdbcTemplate;

public class MapperTestFixture {

	// 테스트를 허용하는 유일한 DB 계정.
	// 이 픽스처는 테이블을 통째로 비우므로, 공용 DB(ENDITPCWK)에 붙은 채 돌면 팀 데이터가 전멸한다.
	// DataSourceConfig는 개인/공용을 주석 토글로 바꾸는 구조라 사람이 실수하기 쉬워
	// 경고 문구가 아니라 실제 잠금장치로 막는다.
	public static final String ALLOWED_DB_USER = "SCOTT_TEST";

	// 실데이터(시퀀스 채번)와 절대 안 겹치게 테스트 전용 ID 대역(9000번대)을 쓴다
	public static final long MEMBER_A = 9001L;     // 일반 회원A (코멘트 작성자)
	public static final long MEMBER_B = 9002L;     // 일반 회원B (신고자)
	public static final long ADMIN = 9009L;        // 관리자 (신고 처리자)
	public static final long CONTENT_A = 9101L;    // 영화A
	public static final long CONTENT_B = 9102L;    // 영화B
	public static final long COLLECTION_A = 9201L; // 컬렉션A (소유: 회원A)

	private MapperTestFixture() {
	}

	/**
	 * 부모 데이터(회원 3, 영화 2, 컬렉션 1)를 매번 새로 심는다.
	 *
	 * @param jdbc
	 */
	public static void seed(JdbcTemplate jdbc) {
		// 0. 접속한 DB가 로컬 개발 계정인지 먼저 확인한다(공용 DB면 여기서 테스트를 중단)
		checkLocalDatabase(jdbc);

		// 1. 이전 픽스처 삭제 — 자식(컬렉션·코멘트·좋아요·신고)은 FK ON DELETE CASCADE로 함께 지워진다
		jdbc.update("DELETE FROM member  WHERE member_id  IN (?, ?, ?)", MEMBER_A, MEMBER_B, ADMIN);
		jdbc.update("DELETE FROM content WHERE content_id IN (?, ?)", CONTENT_A, CONTENT_B);

		// 2. 회원
		jdbc.update("INSERT INTO member (member_id, email, nickname, role) VALUES (?, ?, ?, ?)",
				MEMBER_A, "member_a@test.com", "테스트회원A", "USER");
		jdbc.update("INSERT INTO member (member_id, email, nickname, role) VALUES (?, ?, ?, ?)",
				MEMBER_B, "member_b@test.com", "테스트회원B", "USER");
		jdbc.update("INSERT INTO member (member_id, email, nickname, role) VALUES (?, ?, ?, ?)",
				ADMIN, "admin@test.com", "테스트관리자", "ADMIN");

		// 3. 영화
		jdbc.update("INSERT INTO content (content_id, external_id, title_org) VALUES (?, ?, ?)",
				CONTENT_A, "TEST-MOVIE-A", "Test Movie A");
		jdbc.update("INSERT INTO content (content_id, external_id, title_org) VALUES (?, ?, ?)",
				CONTENT_B, "TEST-MOVIE-B", "Test Movie B");

		// 4. 컬렉션 (회원A 소유)
		jdbc.update("INSERT INTO collection (collection_id, member_id, title) VALUES (?, ?, ?)",
				COLLECTION_A, MEMBER_A, "테스트 컬렉션A");
	}

	/**
	 * 접속한 DB가 로컬 개발 계정인지 확인한다.
	 * 공용 DB에 붙어 있으면 테스트를 실패시켜 데이터 삭제를 막는다.
	 *
	 * @param jdbc
	 */
	private static void checkLocalDatabase(JdbcTemplate jdbc) {
		String dbUser = jdbc.queryForObject("SELECT USER FROM dual", String.class);

		if (false == ALLOWED_DB_USER.equals(dbUser)) {
			fail("테스트 중단: 로컬 개발 DB(" + ALLOWED_DB_USER + ")가 아닙니다. 현재 접속 계정=" + dbUser
					+ " / 이 테스트는 테이블을 전부 비우므로 공용 DB에서 실행하면 안 됩니다."
					+ " DataSourceConfig의 DB 설정을 개인용으로 되돌리십시오.");
		}
	}

}
