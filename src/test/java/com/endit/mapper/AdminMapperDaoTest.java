/**
 * 관리자 임시 조회 Mapper JUnit (읽기 전용 — 팀 규칙: @Transactional)
 */
package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("AdminMapper 테스트")
class AdminMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	AdminMapper mapper;

	@Test
	public void getSummary() {
		log.debug("---------------------------");
		log.debug("*getSummary()*");
		log.debug("---------------------------");
		// 더미(테이블당 10건 전제) 위에서 집계가 0보다 큰지만 확인 — 개수 단정은 안 한다
		Map<String, Object> summary = mapper.getSummary();
		assertNotNull(summary);
		log.debug("summary: {}", summary);

		assertTrue(((Number) summary.get("CONTENT_CNT")).intValue() > 0);
		assertTrue(((Number) summary.get("MEMBER_CNT")).intValue() > 0);
		assertTrue(((Number) summary.get("REPORT_CNT")).intValue() > 0);
	}

	@Test
	public void getLists() {
		log.debug("---------------------------");
		log.debug("*getLists()*");
		log.debug("---------------------------");
		// 네 목록이 전부 조회되는지 확인
		List<Map<String, Object>> contents = mapper.getContentList();
		List<Map<String, Object>> persons = mapper.getPersonList();
		List<Map<String, Object>> members = mapper.getMemberList();
		List<Map<String, Object>> notices = mapper.getNoticeList();

		assertTrue(contents.size() > 0);
		assertTrue(persons.size() > 0);
		assertTrue(members.size() > 0);
		assertTrue(notices.size() > 0);

		// 화면이 쓰는 키가 실려 오는지 표본 확인
		assertNotNull(contents.get(0).get("TITLE"));
		assertNotNull(members.get(0).get("NICKNAME"));
	}

	@Test
	void beans() {
		log.debug("---------------------------");
		log.debug("*beans()*");
		log.debug("---------------------------");
		assertNotNull(mapper);
		log.debug("mapper: {}", mapper);
	}

}
