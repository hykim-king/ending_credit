package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;
import com.endit.mapper.ContentMapper;

@SpringBootTest
@Transactional
@Disabled("deleteAll() 전체 삭제를 제거하고 테스트 데이터를 격리할 때까지 비활성화")
class ContentMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ContentMapper mapper;

	private ContentVO content01;
	private ContentVO content02;
	private ContentVO content03;

	private DTO dto;

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");

		int seq = 0;
		dto = new DTO();

		content01 = new ContentVO(seq, "TMDB_TEST_1001", "테스트영화1", "Test Movie 1", "줄거리1",
				"2024-01-01", 120, "Korea", "http://poster1", "http://backdrop1", "사용않함");
		content02 = new ContentVO(seq, "TMDB_TEST_1002", "테스트영화2", "Test Movie 2", "줄거리2",
				"2024-02-01", 110, "USA", "http://poster2", "http://backdrop2", "사용않함");
		content03 = new ContentVO(seq, "TMDB_TEST_1003", "테스트영화3", "Test Movie 3", "줄거리3",
				"2024-03-01", 100, "Japan", "http://poster3", "http://backdrop3", "사용않함");
	}

	@AfterEach
	void tearDown() throws Exception {
		log.debug("*****************************");
		log.debug("*@AfterEach*");
		log.debug("*****************************");
	}

	@Test
	void doRetrieve() {
		log.debug("---------------------------");
		log.debug("*doRetrieve()*");
		log.debug("---------------------------");
		// 1. 전체삭제
		// 2. 3건 입력
		// 3. 페이징 조회

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		mapper.doSave(content01);
		mapper.doSave(content02);
		mapper.doSave(content03);
		assertEquals(3, mapper.totalCnt());

		dto.setPageNo(1);
		dto.setPageSize(10);

		List<ContentVO> list = mapper.doRetrieve(dto);
		for (ContentVO vo : list) {
			log.debug("{}", vo);
		}
		assertEquals(3, list.size());
	}

	@Test
	void doUpdate() {
		log.debug("---------------------------");
		log.debug("*doUpdate()*");
		log.debug("---------------------------");
		// 1. 전체삭제
		// 2. 단건등록(content01)
		// 3. 단건조회
		// 4. 조회 결과 수정
		// 5. update
		// 6. 재조회 후 비교

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		int flag = mapper.doSave(content01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		ContentVO updateVO = mapper.doSelectOne(content01);
		assertNotNull(updateVO);

		String updateStr = "_U";
		updateVO.setTitleKo(updateVO.getTitleKo() + updateStr);
		updateVO.setTitleOrg(updateVO.getTitleOrg() + updateStr);
		updateVO.setOverview(updateVO.getOverview() + updateStr);
		updateVO.setCountry(updateVO.getCountry() + updateStr);

		flag = mapper.doUpdate(updateVO);
		assertEquals(1, flag);

		ContentVO outVO = mapper.doSelectOne(updateVO);
		assertNotNull(outVO);
		isSameData(updateVO, outVO);
	}

	@Test
	void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");
		// 1. 전체삭제
		// 2. 단건등록
		// 3. 단건삭제
		// 4. 건수비교

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		int flag = mapper.doSave(content01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		flag = mapper.doDelete(content01);
		assertEquals(1, flag);
		assertEquals(0, mapper.totalCnt());
	}

	@Test
	void doSave() {
		log.debug("---------------------------");
		log.debug("*doSave()*");
		log.debug("---------------------------");
		// 1. 전체삭제
		// 2. 3건 등록
		// 3. 건수비교 / 외부ID 조회

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		int flag = mapper.doSave(content01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());
		assertEquals(true, content01.getContentId() > 0);
		log.debug("saved contentId(content01)={}", content01.getContentId());

		flag = mapper.doSave(content02);
		assertEquals(1, flag);
		assertEquals(2, mapper.totalCnt());

		flag = mapper.doSave(content03);
		assertEquals(1, flag);
		assertEquals(3, mapper.totalCnt());

		Integer foundId = mapper.findContentIdByExternal(content01.getExternalId());
		assertEquals(content01.getContentId(), foundId.intValue());
		assertNull(mapper.findContentIdByExternal("NOT_EXISTS"));
	}

	@Test
	void doSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSelectOne()*");
		log.debug("---------------------------");
		// 1. 전체삭제
		// 2. 3건 등록
		// 3. 단건조회 후 비교

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		mapper.doSave(content01);
		mapper.doSave(content02);
		mapper.doSave(content03);
		assertEquals(3, mapper.totalCnt());

		ContentVO outVO01 = mapper.doSelectOne(content01);
		assertNotNull(outVO01);

		ContentVO outVO02 = mapper.doSelectOne(content02);
		assertNotNull(outVO02);

		ContentVO outVO03 = mapper.doSelectOne(content03);
		assertNotNull(outVO03);

		isSameData(content01, outVO01);
		isSameData(content02, outVO02);
		isSameData(content03, outVO03);
	}

	private void isSameData(ContentVO expected, ContentVO actual) {
		assertEquals(expected.getContentId(), actual.getContentId());
		assertEquals(expected.getExternalId(), actual.getExternalId());
		assertEquals(expected.getTitleKo(), actual.getTitleKo());
		assertEquals(expected.getTitleOrg(), actual.getTitleOrg());
		assertEquals(expected.getOverview(), actual.getOverview());
		assertEquals(expected.getReleaseYear(), actual.getReleaseYear());
		assertEquals(expected.getRuntimeMin(), actual.getRuntimeMin());
		assertEquals(expected.getCountry(), actual.getCountry());
		assertEquals(expected.getPosterUrl(), actual.getPosterUrl());
		assertEquals(expected.getBackdropUrl(), actual.getBackdropUrl());
	}

	@Test
	@DisplayName("bean테스트")
	void beans() {
		assertNotNull(mapper);
		log.debug("mapper: {}", mapper);
	}

}
