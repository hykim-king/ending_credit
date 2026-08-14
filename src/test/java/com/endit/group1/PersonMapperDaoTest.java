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
import com.endit.domain.PersonVO;
import com.endit.mapper.PersonMapper;

@SpringBootTest
class PersonMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private PersonMapper personMapper;

	private PersonVO person01;
	private PersonVO person02;
	private PersonVO person03;

	private DTO dto;

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");

		int seq = 0;
		dto = new DTO();

		person01 = new PersonVO(seq, "TMDB_PERSON_1001", "배우1", "Actor One", "http://profile1", "사용않함", "사용않함");
		person02 = new PersonVO(seq, "TMDB_PERSON_1002", "배우2", "Actor Two", "http://profile2", "사용않함", "사용않함");
		person03 = new PersonVO(seq, "TMDB_PERSON_1003", "감독1", "Director One", "http://profile3", "사용않함", "사용않함");
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

		personMapper.deleteAll();
		assertEquals(0, personMapper.selectAllCount());

		personMapper.doSave(person01);
		personMapper.doSave(person02);
		personMapper.doSave(person03);
		assertEquals(3, personMapper.selectAllCount());

		dto.setPageNo(1);
		dto.setPageSize(10);

		List<PersonVO> list = personMapper.doRetrieve(dto);
		for (PersonVO vo : list) {
			log.debug("{}", vo);
		}
		assertEquals(3, list.size());
	}

	@Test
	void doDelete() {
		log.debug("---------------------------");
		log.debug("*doDelete()*");
		log.debug("---------------------------");

		personMapper.deleteAll();
		assertEquals(0, personMapper.selectAllCount());

		personMapper.doSave(person01);
		assertEquals(1, personMapper.selectAllCount());

		personMapper.doSave(person02);
		assertEquals(2, personMapper.selectAllCount());

		personMapper.doDelete(person01);
		assertEquals(1, personMapper.selectAllCount());
	}

	@Test
	void doUpdate() {
		log.debug("---------------------------");
		log.debug("*doUpdate()*");
		log.debug("---------------------------");

		personMapper.deleteAll();
		assertEquals(0, personMapper.selectAllCount());

		int flag = personMapper.doSave(person01);
		assertEquals(1, flag);
		assertEquals(1, personMapper.selectAllCount());

		PersonVO outVO01 = personMapper.doSelectOne(person01);
		assertNotNull(outVO01);

		String upString = "_U";
		outVO01.setNameKo(outVO01.getNameKo() + upString);
		outVO01.setNameOrg(outVO01.getNameOrg() + upString);
		outVO01.setProfileImageUrl(outVO01.getProfileImageUrl() + upString);

		flag = personMapper.doUpdate(outVO01);
		assertEquals(1, flag);

		PersonVO resultVO01 = personMapper.doSelectOne(outVO01);
		assertNotNull(resultVO01);
		isSameData(resultVO01, outVO01);
	}

	@Test
	void doSave() {
		log.debug("---------------------------");
		log.debug("*doSave()*");
		log.debug("---------------------------");

		personMapper.deleteAll();
		assertEquals(0, personMapper.selectAllCount());

		int flag = personMapper.doSave(person01);
		assertEquals(1, flag);
		assertEquals(1, personMapper.selectAllCount());
		log.debug("saved personId(person01)={}", person01.getPersonId());

		flag = personMapper.doSave(person02);
		assertEquals(1, flag);
		assertEquals(2, personMapper.selectAllCount());

		flag = personMapper.doSave(person03);
		assertEquals(1, flag);
		assertEquals(3, personMapper.selectAllCount());

		Integer foundId = personMapper.findPersonIdByExternal(person01.getExternalId());
		assertEquals(person01.getPersonId(), foundId.intValue());
		assertNull(personMapper.findPersonIdByExternal("NOT_EXISTS"));
	}

	@Test
	void doSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSelectOne()*");
		log.debug("---------------------------");

		personMapper.deleteAll();
		assertEquals(0, personMapper.selectAllCount());

		personMapper.doSave(person01);
		personMapper.doSave(person02);
		personMapper.doSave(person03);
		assertEquals(3, personMapper.selectAllCount());

		PersonVO outVO01 = personMapper.doSelectOne(person01);
		assertNotNull(outVO01);

		PersonVO outVO02 = personMapper.doSelectOne(person02);
		PersonVO outVO03 = personMapper.doSelectOne(person03);

		isSameData(outVO01, person01);
		isSameData(outVO02, person02);
		isSameData(outVO03, person03);
	}

	private void isSameData(PersonVO outVO, PersonVO person) {
		assertEquals(outVO.getPersonId(), person.getPersonId());
		assertEquals(outVO.getExternalId(), person.getExternalId());
		assertEquals(outVO.getNameKo(), person.getNameKo());
		assertEquals(outVO.getNameOrg(), person.getNameOrg());
		assertEquals(outVO.getProfileImageUrl(), person.getProfileImageUrl());
	}

	@Test
	void beans() {
		assertNotNull(personMapper);
		log.debug("personMapper: {}", personMapper);
	}

}
