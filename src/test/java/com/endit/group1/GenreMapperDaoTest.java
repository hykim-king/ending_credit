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
import com.endit.domain.GenreVO;
import com.endit.mapper.GenreMapper;

@SpringBootTest
class GenreMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private GenreMapper genreMapper;

	private GenreVO genre01;
	private GenreVO genre02;
	private GenreVO genre03;

	private DTO dto;

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");

		int seq = 0;
		dto = new DTO();

		genre01 = new GenreVO(seq, "28", "액션");
		genre02 = new GenreVO(seq, "35", "코미디");
		genre03 = new GenreVO(seq, "18", "드라마");
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

		genreMapper.deleteAll();
		assertEquals(0, genreMapper.selectAllCount());

		genreMapper.doSave(genre01);
		genreMapper.doSave(genre02);
		genreMapper.doSave(genre03);
		assertEquals(3, genreMapper.selectAllCount());

		dto.setPageNo(1);
		dto.setPageSize(10);

		List<GenreVO> list = genreMapper.doRetrieve(dto);
		for (GenreVO vo : list) {
			log.debug("{}", vo);
		}
		assertEquals(3, list.size());
	}

	@Test
	void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");

		genreMapper.deleteAll();
		assertEquals(0, genreMapper.selectAllCount());

		genreMapper.doSave(genre01);
		assertEquals(1, genreMapper.selectAllCount());

		genreMapper.doSave(genre02);
		assertEquals(2, genreMapper.selectAllCount());

		genreMapper.doDelete(genre01);
		assertEquals(1, genreMapper.selectAllCount());
	}

	@Test
	void doUpdate() {
		log.debug("---------------------------");
		log.debug("*doUpdate()*");
		log.debug("---------------------------");

		genreMapper.deleteAll();
		assertEquals(0, genreMapper.selectAllCount());

		int flag = genreMapper.doSave(genre01);
		assertEquals(1, flag);
		assertEquals(1, genreMapper.selectAllCount());

		GenreVO outVO01 = genreMapper.doSelectOne(genre01);
		assertNotNull(outVO01);

		outVO01.setName(outVO01.getName() + "_U");

		flag = genreMapper.doUpdate(outVO01);
		assertEquals(1, flag);

		GenreVO resultVO01 = genreMapper.doSelectOne(outVO01);
		assertNotNull(resultVO01);
		isSameData(resultVO01, outVO01);
	}

	@Test
	void doSave() {
		log.debug("---------------------------");
		log.debug("*doSave()*");
		log.debug("---------------------------");

		genreMapper.deleteAll();
		assertEquals(0, genreMapper.selectAllCount());

		int flag = genreMapper.doSave(genre01);
		assertEquals(1, flag);
		assertEquals(1, genreMapper.selectAllCount());
		log.debug("saved genreId(genre01)={}", genre01.getGenreId());

		flag = genreMapper.doSave(genre02);
		assertEquals(1, flag);
		assertEquals(2, genreMapper.selectAllCount());

		flag = genreMapper.doSave(genre03);
		assertEquals(1, flag);
		assertEquals(3, genreMapper.selectAllCount());

		Integer foundId = genreMapper.findGenreIdByExternal(genre01.getExternalGenreId());
		assertEquals(genre01.getGenreId(), foundId.intValue());
		assertNull(genreMapper.findGenreIdByExternal("NOT_EXISTS"));
	}

	@Test
	void doSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSelectOne()*");
		log.debug("---------------------------");

		genreMapper.deleteAll();
		assertEquals(0, genreMapper.selectAllCount());

		genreMapper.doSave(genre01);
		genreMapper.doSave(genre02);
		genreMapper.doSave(genre03);
		assertEquals(3, genreMapper.selectAllCount());

		GenreVO outVO01 = genreMapper.doSelectOne(genre01);
		assertNotNull(outVO01);

		GenreVO outVO02 = genreMapper.doSelectOne(genre02);
		GenreVO outVO03 = genreMapper.doSelectOne(genre03);

		isSameData(outVO01, genre01);
		isSameData(outVO02, genre02);
		isSameData(outVO03, genre03);
	}

	private void isSameData(GenreVO outVO, GenreVO genre) {
		assertEquals(outVO.getGenreId(), genre.getGenreId());
		assertEquals(outVO.getExternalGenreId(), genre.getExternalGenreId());
		assertEquals(outVO.getName(), genre.getName());
	}

	@Test
	void beans() {
		assertNotNull(genreMapper);
		log.debug("genreMapper: {}", genreMapper);
	}

}
