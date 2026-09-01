package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.GenreVO;

@SpringBootTest
@Transactional
class GenreMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private GenreMapper mapper;

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

		genre01 = new GenreVO(seq, "T_28", "테스트액션");
		genre02 = new GenreVO(seq, "T_35", "테스트코미디");
		genre03 = new GenreVO(seq, "T_18", "테스트드라마");
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

		mapper.doSave(genre01);
		mapper.doSave(genre02);
		mapper.doSave(genre03);
		assertEquals(3, mapper.totalCnt());

		dto.setPageNo(1);
		dto.setPageSize(10);

		List<GenreVO> list = mapper.doRetrieve(dto);
		for (GenreVO vo : list) {
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
		// 2. 단건등록
		// 3. 단건조회 후 수정
		// 4. update
		// 5. 재조회 후 비교

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		int flag = mapper.doSave(genre01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		GenreVO updateVO = mapper.doSelectOne(genre01);
		assertNotNull(updateVO);

		updateVO.setName(updateVO.getName() + "_U");

		flag = mapper.doUpdate(updateVO);
		assertEquals(1, flag);

		GenreVO outVO = mapper.doSelectOne(updateVO);
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

		int flag = mapper.doSave(genre01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		flag = mapper.doDelete(genre01);
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

		int flag = mapper.doSave(genre01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());
		assertEquals(true, genre01.getGenreId() > 0);
		log.debug("saved genreId(genre01)={}", genre01.getGenreId());

		flag = mapper.doSave(genre02);
		assertEquals(1, flag);
		assertEquals(2, mapper.totalCnt());

		flag = mapper.doSave(genre03);
		assertEquals(1, flag);
		assertEquals(3, mapper.totalCnt());

		Integer foundId = mapper.findGenreIdByExternal(genre01.getExternalGenreId());
		assertEquals(genre01.getGenreId(), foundId.intValue());
		assertNull(mapper.findGenreIdByExternal("NOT_EXISTS"));
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

		mapper.doSave(genre01);
		mapper.doSave(genre02);
		mapper.doSave(genre03);
		assertEquals(3, mapper.totalCnt());

		GenreVO outVO01 = mapper.doSelectOne(genre01);
		assertNotNull(outVO01);

		GenreVO outVO02 = mapper.doSelectOne(genre02);
		assertNotNull(outVO02);

		GenreVO outVO03 = mapper.doSelectOne(genre03);
		assertNotNull(outVO03);

		isSameData(genre01, outVO01);
		isSameData(genre02, outVO02);
		isSameData(genre03, outVO03);
	}

	private void isSameData(GenreVO expected, GenreVO actual) {
		assertEquals(expected.getGenreId(), actual.getGenreId());
		assertEquals(expected.getExternalGenreId(), actual.getExternalGenreId());
		assertEquals(expected.getName(), actual.getName());
	}

	@Test
	@DisplayName("bean테스트")
	void beans() {
		assertNotNull(mapper);
		log.debug("mapper: {}", mapper);
	}

}
