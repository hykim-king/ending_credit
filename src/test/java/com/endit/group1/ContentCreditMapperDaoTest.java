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
import com.endit.domain.ContentCreditVO;
import com.endit.domain.ContentVO;
import com.endit.domain.PersonVO;
import com.endit.mapper.ContentCreditMapper;
import com.endit.mapper.ContentMapper;
import com.endit.mapper.PersonMapper;

@SpringBootTest
class ContentCreditMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ContentCreditMapper contentCreditMapper;

	@Autowired
	private ContentMapper contentMapper;

	@Autowired
	private PersonMapper personMapper;

	private ContentVO content01;
	private PersonVO person01;
	private PersonVO person02;

	private ContentCreditVO credit01;
	private ContentCreditVO credit02;
	private ContentCreditVO credit03;

	private DTO dto;

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");

		int seq = 0;
		dto = new DTO();

		content01 = new ContentVO(seq, "TMDB_CREDIT_C_1001", "크레딧영화", "Credit Movie", "줄거리",
				"2024-01-01", 120, "Korea", "http://poster", "http://backdrop", "사용않함", "사용않함");
		person01 = new PersonVO(seq, "TMDB_CREDIT_P_1001", "배우A", "Actor A", "http://p1", "사용않함", "사용않함");
		person02 = new PersonVO(seq, "TMDB_CREDIT_P_1002", "감독A", "Director A", "http://p2", "사용않함", "사용않함");

		// contentId/personId/creditId 는 저장 후 시퀀스 값으로 채움
		credit01 = new ContentCreditVO(seq, seq, seq, "ACTOR", "주인공", 0);
		credit02 = new ContentCreditVO(seq, seq, seq, "ACTOR", "조연", 1);
		credit03 = new ContentCreditVO(seq, seq, seq, "DIRECTOR", null, 0);
	}

	@AfterEach
	void tearDown() throws Exception {
		log.debug("*****************************");
		log.debug("*@AfterEach*");
		log.debug("*****************************");
	}

	private void prepareParents() {
		contentCreditMapper.deleteAll();
		contentMapper.deleteAll();
		personMapper.deleteAll();

		contentMapper.doSave(content01);
		personMapper.doSave(person01);
		personMapper.doSave(person02);

		credit01.setContentId(content01.getContentId());
		credit01.setPersonId(person01.getPersonId());

		credit02.setContentId(content01.getContentId());
		credit02.setPersonId(person01.getPersonId());
		credit02.setRole("WRITER");
		credit02.setCharacter(null);

		credit03.setContentId(content01.getContentId());
		credit03.setPersonId(person02.getPersonId());
	}

	@Test
	void doRetrieve() {
		log.debug("---------------------------");
		log.debug("*doRetrieve()*");
		log.debug("---------------------------");

		prepareParents();
		assertEquals(0, contentCreditMapper.selectAllCount());

		contentCreditMapper.doSave(credit01);
		contentCreditMapper.doSave(credit02);
		contentCreditMapper.doSave(credit03);
		assertEquals(3, contentCreditMapper.selectAllCount());

		dto.setPageNo(1);
		dto.setPageSize(10);
		dto.setSearchDiv("10");
		dto.setSearchWord(String.valueOf(content01.getContentId()));

		List<ContentCreditVO> list = contentCreditMapper.doRetrieve(dto);
		for (ContentCreditVO vo : list) {
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

		contentCreditMapper.doSave(credit01);
		assertEquals(1, contentCreditMapper.selectAllCount());

		contentCreditMapper.doSave(credit03);
		assertEquals(2, contentCreditMapper.selectAllCount());

		contentCreditMapper.doDelete(credit01);
		assertEquals(1, contentCreditMapper.selectAllCount());
	}

	@Test
	void doUpdate() {
		log.debug("---------------------------");
		log.debug("*doUpdate()*");
		log.debug("---------------------------");

		prepareParents();

		int flag = contentCreditMapper.doSave(credit01);
		assertEquals(1, flag);

		ContentCreditVO outVO01 = contentCreditMapper.doSelectOne(credit01);
		assertNotNull(outVO01);

		outVO01.setCharacter(outVO01.getCharacter() + "_U");
		outVO01.setDisplayOrder(9);

		flag = contentCreditMapper.doUpdate(outVO01);
		assertEquals(1, flag);

		ContentCreditVO resultVO01 = contentCreditMapper.doSelectOne(outVO01);
		assertNotNull(resultVO01);
		isSameData(resultVO01, outVO01);
	}

	@Test
	void doSave() {
		log.debug("---------------------------");
		log.debug("*doSave()*");
		log.debug("---------------------------");

		prepareParents();

		int flag = contentCreditMapper.doSave(credit01);
		assertEquals(1, flag);
		log.debug("saved creditId(credit01)={}", credit01.getCreditId());
		assertEquals(1, contentCreditMapper.selectAllCount());

		flag = contentCreditMapper.doSave(credit02);
		assertEquals(1, flag);
		assertEquals(2, contentCreditMapper.selectAllCount());

		flag = contentCreditMapper.doSave(credit03);
		assertEquals(1, flag);
		assertEquals(3, contentCreditMapper.selectAllCount());
	}

	@Test
	void doSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSelectOne()*");
		log.debug("---------------------------");

		prepareParents();

		contentCreditMapper.doSave(credit01);
		contentCreditMapper.doSave(credit02);
		contentCreditMapper.doSave(credit03);
		assertEquals(3, contentCreditMapper.selectAllCount());

		ContentCreditVO outVO01 = contentCreditMapper.doSelectOne(credit01);
		assertNotNull(outVO01);

		ContentCreditVO outVO02 = contentCreditMapper.doSelectOne(credit02);
		ContentCreditVO outVO03 = contentCreditMapper.doSelectOne(credit03);

		isSameData(outVO01, credit01);
		isSameData(outVO02, credit02);
		isSameData(outVO03, credit03);
	}

	private void isSameData(ContentCreditVO outVO, ContentCreditVO credit) {
		assertEquals(outVO.getCreditId(), credit.getCreditId());
		assertEquals(outVO.getContentId(), credit.getContentId());
		assertEquals(outVO.getPersonId(), credit.getPersonId());
		assertEquals(outVO.getRole(), credit.getRole());
		assertEquals(outVO.getCharacter(), credit.getCharacter());
		assertEquals(outVO.getDisplayOrder(), credit.getDisplayOrder());
	}

	@Test
	void beans() {
		assertNotNull(contentCreditMapper);
		assertNotNull(contentMapper);
		assertNotNull(personMapper);
		log.debug("contentCreditMapper: {}", contentCreditMapper);
	}

}
