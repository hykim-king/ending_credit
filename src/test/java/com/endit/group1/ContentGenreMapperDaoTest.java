package com.endit.group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.endit.cmn.DTO;
import com.endit.domain.ContentGenreVO;
import com.endit.domain.ContentVO;
import com.endit.domain.GenreVO;
import com.endit.mapper.ContentGenreMapper;
import com.endit.mapper.ContentMapper;
import com.endit.mapper.GenreMapper;

@SpringBootTest
class ContentGenreMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ContentGenreMapper contentGenreMapper;

	@Autowired
	private ContentMapper contentMapper;

	@Autowired
	private GenreMapper genreMapper;

	private ContentVO content01;
	private GenreVO genre01;
	private GenreVO genre02;
	private GenreVO genre03;

	private ContentGenreVO link01;
	private ContentGenreVO link02;
	private ContentGenreVO link03;

	private DTO dto;

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");

		int seq = 0;
		dto = new DTO();

		content01 = new ContentVO(seq, "TMDB_CG_C_1001", "장르영화", "Genre Movie", "줄거리",
				"2024-01-01", 120, "Korea", "http://poster", "http://backdrop", "사용않함", "사용않함");
		genre01 = new GenreVO(seq, "CG_28", "액션_CG");
		genre02 = new GenreVO(seq, "CG_35", "코미디_CG");
		genre03 = new GenreVO(seq, "CG_18", "드라마_CG");

		link01 = new ContentGenreVO(seq, seq);
		link02 = new ContentGenreVO(seq, seq);
		link03 = new ContentGenreVO(seq, seq);
	}

	@AfterEach
	void tearDown() throws Exception {
		log.debug("*****************************");
		log.debug("*@AfterEach*");
		log.debug("*****************************");
	}

	private void prepareParents() {
		contentGenreMapper.deleteAll();
		contentMapper.deleteAll();
		genreMapper.deleteAll();

		contentMapper.doSave(content01);
		genreMapper.doSave(genre01);
		genreMapper.doSave(genre02);
		genreMapper.doSave(genre03);

		link01.setContentId(content01.getContentId());
		link01.setGenreId(genre01.getGenreId());

		link02.setContentId(content01.getContentId());
		link02.setGenreId(genre02.getGenreId());

		link03.setContentId(content01.getContentId());
		link03.setGenreId(genre03.getGenreId());
	}

	@Test
	void doRetrieve() {
		log.debug("---------------------------");
		log.debug("*doRetrieve()*");
		log.debug("---------------------------");

		prepareParents();
		assertEquals(0, contentGenreMapper.selectAllCount());

		contentGenreMapper.doSave(link01);
		contentGenreMapper.doSave(link02);
		contentGenreMapper.doSave(link03);
		assertEquals(3, contentGenreMapper.selectAllCount());

		dto.setPageNo(1);
		dto.setPageSize(10);
		dto.setSearchDiv("10");
		dto.setSearchWord(String.valueOf(content01.getContentId()));

		List<ContentGenreVO> list = contentGenreMapper.doRetrieve(dto);
		for (ContentGenreVO vo : list) {
			log.debug("{}", vo);
		}
		assertEquals(3, list.size());
	}

	@Test
	void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");

		prepareParents();

		contentGenreMapper.doSave(link01);
		assertEquals(1, contentGenreMapper.selectAllCount());

		contentGenreMapper.doSave(link02);
		assertEquals(2, contentGenreMapper.selectAllCount());

		contentGenreMapper.doDelete(link01);
		assertEquals(1, contentGenreMapper.selectAllCount());
	}

	@Test
	void doUpdate() {
		log.debug("---------------------------");
		log.debug("*doUpdate()*");
		log.debug("---------------------------");

		// 복합 PK 테이블: update는 동일 키 유지(연결 존재 확인) 용도로만 검증
		prepareParents();

		int flag = contentGenreMapper.doSave(link01);
		assertEquals(1, flag);

		ContentGenreVO outVO01 = contentGenreMapper.doSelectOne(link01);
		assertNotNull(outVO01);

		flag = contentGenreMapper.doUpdate(outVO01);
		assertEquals(1, flag);

		ContentGenreVO resultVO01 = contentGenreMapper.doSelectOne(outVO01);
		assertNotNull(resultVO01);
		isSameData(resultVO01, outVO01);
	}

	@Test
	void doSave() {
		log.debug("---------------------------");
		log.debug("*doSave()*");
		log.debug("---------------------------");

		prepareParents();

		int flag = contentGenreMapper.doSave(link01);
		assertEquals(1, flag);
		assertEquals(1, contentGenreMapper.selectAllCount());

		flag = contentGenreMapper.doSave(link02);
		assertEquals(1, flag);
		assertEquals(2, contentGenreMapper.selectAllCount());

		flag = contentGenreMapper.doSave(link03);
		assertEquals(1, flag);
		assertEquals(3, contentGenreMapper.selectAllCount());
	}

	@Test
	void doSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSelectOne()*");
		log.debug("---------------------------");

		prepareParents();

		contentGenreMapper.doSave(link01);
		contentGenreMapper.doSave(link02);
		contentGenreMapper.doSave(link03);
		assertEquals(3, contentGenreMapper.selectAllCount());

		ContentGenreVO outVO01 = contentGenreMapper.doSelectOne(link01);
		assertNotNull(outVO01);

		ContentGenreVO outVO02 = contentGenreMapper.doSelectOne(link02);
		ContentGenreVO outVO03 = contentGenreMapper.doSelectOne(link03);

		isSameData(outVO01, link01);
		isSameData(outVO02, link02);
		isSameData(outVO03, link03);
	}

	private void isSameData(ContentGenreVO outVO, ContentGenreVO link) {
		assertEquals(outVO.getContentId(), link.getContentId());
		assertEquals(outVO.getGenreId(), link.getGenreId());
	}

	@Test
	void beans() {
		assertNotNull(contentGenreMapper);
		assertNotNull(contentMapper);
		assertNotNull(genreMapper);
		log.debug("contentGenreMapper: {}", contentGenreMapper);
	}

}
