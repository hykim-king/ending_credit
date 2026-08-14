package com.endit.group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;
import com.endit.mapper.ContentMapper;

@SpringBootTest
class ContentMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ContentMapper contentMapper;

	private ContentVO content01;
	private ContentVO content02;
	private ContentVO content03;

	private DTO dto;

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");

		// PK는 doSave 시 시퀀스로 채워짐. 절대 고정값(1,2,3)을 기대하지 말 것.
		int seq = 0;
		dto = new DTO();

		content01 = new ContentVO(seq, "TMDB_TEST_1001", "테스트영화1", "Test Movie 1", "줄거리1",
				"2024-01-01", 120, "Korea", "http://poster1", "http://backdrop1", "사용않함", "사용않함");
		content02 = new ContentVO(seq, "TMDB_TEST_1002", "테스트영화2", "Test Movie 2", "줄거리2",
				"2024-02-01", 110, "USA", "http://poster2", "http://backdrop2", "사용않함", "사용않함");
		content03 = new ContentVO(seq, "TMDB_TEST_1003", "테스트영화3", "Test Movie 3", "줄거리3",
				"2024-03-01", 100, "Japan", "http://poster3", "http://backdrop3", "사용않함", "사용않함");
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

		contentMapper.deleteAll();
		assertEquals(0, contentMapper.selectAllCount());

		contentMapper.doSave(content01);
		contentMapper.doSave(content02);
		contentMapper.doSave(content03);
		assertEquals(3, contentMapper.selectAllCount());

		dto.setPageNo(1);
		dto.setPageSize(10);

		List<ContentVO> list = contentMapper.doRetrieve(dto);
		for (ContentVO vo : list) {
			log.debug("{}", vo);
		}
		assertEquals(3, list.size());
	}

	@Test
	void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");

		contentMapper.deleteAll();
		assertEquals(0, contentMapper.selectAllCount());

		contentMapper.doSave(content01);
		assertEquals(1, contentMapper.selectAllCount());

		contentMapper.doSave(content02);
		assertEquals(2, contentMapper.selectAllCount());

		// content01.contentId 는 doSave 시 시퀀스로 채워진 값 사용
		contentMapper.doDelete(content01);
		assertEquals(1, contentMapper.selectAllCount());
	}

	@Test
	void doUpdate() {
		log.debug("---------------------------");
		log.debug("*doUpdate()*");
		log.debug("---------------------------");

		contentMapper.deleteAll();
		assertEquals(0, contentMapper.selectAllCount());

		int flag = contentMapper.doSave(content01);
		assertEquals(1, flag);
		assertEquals(1, contentMapper.selectAllCount());

		ContentVO outVO01 = contentMapper.doSelectOne(content01);
		assertNotNull(outVO01);

		String upString = "_U";
		outVO01.setTitleKo(outVO01.getTitleKo() + upString);
		outVO01.setTitleOrg(outVO01.getTitleOrg() + upString);
		outVO01.setOverview(outVO01.getOverview() + upString);
		outVO01.setCountry(outVO01.getCountry() + upString);

		flag = contentMapper.doUpdate(outVO01);
		assertEquals(1, flag);

		ContentVO resultVO01 = contentMapper.doSelectOne(outVO01);
		assertNotNull(resultVO01);
		isSameData(resultVO01, outVO01);
	}

	@Test
	void doSave() {
		log.debug("---------------------------");
		log.debug("*doSave()*");
		log.debug("---------------------------");

		contentMapper.deleteAll();
		assertEquals(0, contentMapper.selectAllCount());

		int flag = contentMapper.doSave(content01);
		assertEquals(1, flag);
		assertEquals(1, contentMapper.selectAllCount());
		log.debug("saved contentId(content01)={}", content01.getContentId());

		flag = contentMapper.doSave(content02);
		assertEquals(1, flag);
		assertEquals(2, contentMapper.selectAllCount());
		log.debug("saved contentId(content02)={}", content02.getContentId());

		flag = contentMapper.doSave(content03);
		assertEquals(1, flag);
		assertEquals(3, contentMapper.selectAllCount());
		log.debug("saved contentId(content03)={}", content03.getContentId());

		Integer foundId = contentMapper.findContentIdByExternal(content01.getExternalId());
		assertEquals(content01.getContentId(), foundId.intValue());
		assertNull(contentMapper.findContentIdByExternal("NOT_EXISTS"));
	}

	@Test
	void doSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSelectOne()*");
		log.debug("---------------------------");

		contentMapper.deleteAll();
		assertEquals(0, contentMapper.selectAllCount());

		contentMapper.doSave(content01);
		contentMapper.doSave(content02);
		contentMapper.doSave(content03);
		assertEquals(3, contentMapper.selectAllCount());

		ContentVO outVO01 = contentMapper.doSelectOne(content01);
		assertNotNull(outVO01);

		ContentVO outVO02 = contentMapper.doSelectOne(content02);
		ContentVO outVO03 = contentMapper.doSelectOne(content03);

		isSameData(outVO01, content01);
		isSameData(outVO02, content02);
		isSameData(outVO03, content03);
	}

	private void isSameData(ContentVO outVO, ContentVO content) {
		// PK는 시퀀스 값이므로, doSave 이후 VO에 채워진 값과 조회 결과를 비교
		assertEquals(outVO.getContentId(), content.getContentId());
		assertEquals(outVO.getExternalId(), content.getExternalId());
		assertEquals(outVO.getTitleKo(), content.getTitleKo());
		assertEquals(outVO.getTitleOrg(), content.getTitleOrg());
		assertEquals(outVO.getOverview(), content.getOverview());
		assertEquals(outVO.getReleaseYear(), content.getReleaseYear());
		assertEquals(outVO.getRuntimeMin(), content.getRuntimeMin());
		assertEquals(outVO.getCountry(), content.getCountry());
		assertEquals(outVO.getPosterUrl(), content.getPosterUrl());
		assertEquals(outVO.getBackdropUrl(), content.getBackdropUrl());
	}

	@Test
	void beans() {
		assertNotNull(contentMapper);
		log.debug("contentMapper: {}", contentMapper);
	}

}
