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
import com.endit.domain.PersonVO;

@SpringBootTest
@Transactional
@Disabled("deleteAll() 전체 삭제를 제거하고 테스트 데이터를 격리할 때까지 비활성화")
class PersonMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private PersonMapper mapper;

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
		// 1. 전체삭제
		// 2. 3건 입력
		// 3. 페이징 조회

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		mapper.doSave(person01);
		mapper.doSave(person02);
		mapper.doSave(person03);
		assertEquals(3, mapper.totalCnt());

		dto.setPageNo(1);
		dto.setPageSize(10);

		List<PersonVO> list = mapper.doRetrieve(dto);
		for (PersonVO vo : list) {
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

		int flag = mapper.doSave(person01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		PersonVO updateVO = mapper.doSelectOne(person01);
		assertNotNull(updateVO);

		String updateStr = "_U";
		updateVO.setNameKo(updateVO.getNameKo() + updateStr);
		updateVO.setNameOrg(updateVO.getNameOrg() + updateStr);
		updateVO.setProfileImageUrl(updateVO.getProfileImageUrl() + updateStr);

		flag = mapper.doUpdate(updateVO);
		assertEquals(1, flag);

		PersonVO outVO = mapper.doSelectOne(updateVO);
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

		int flag = mapper.doSave(person01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		flag = mapper.doDelete(person01);
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

		int flag = mapper.doSave(person01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());
		assertEquals(true, person01.getPersonId() > 0);
		log.debug("saved personId(person01)={}", person01.getPersonId());

		flag = mapper.doSave(person02);
		assertEquals(1, flag);
		assertEquals(2, mapper.totalCnt());

		flag = mapper.doSave(person03);
		assertEquals(1, flag);
		assertEquals(3, mapper.totalCnt());

		Integer foundId = mapper.findPersonIdByExternal(person01.getExternalId());
		assertEquals(person01.getPersonId(), foundId.intValue());
		assertNull(mapper.findPersonIdByExternal("NOT_EXISTS"));
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

		mapper.doSave(person01);
		mapper.doSave(person02);
		mapper.doSave(person03);
		assertEquals(3, mapper.totalCnt());

		PersonVO outVO01 = mapper.doSelectOne(person01);
		assertNotNull(outVO01);

		PersonVO outVO02 = mapper.doSelectOne(person02);
		assertNotNull(outVO02);

		PersonVO outVO03 = mapper.doSelectOne(person03);
		assertNotNull(outVO03);

		isSameData(person01, outVO01);
		isSameData(person02, outVO02);
		isSameData(person03, outVO03);
	}

	private void isSameData(PersonVO expected, PersonVO actual) {
		assertEquals(expected.getPersonId(), actual.getPersonId());
		assertEquals(expected.getExternalId(), actual.getExternalId());
		assertEquals(expected.getNameKo(), actual.getNameKo());
		assertEquals(expected.getNameOrg(), actual.getNameOrg());
		assertEquals(expected.getProfileImageUrl(), actual.getProfileImageUrl());
	}

	@Test
	@DisplayName("bean테스트")
	void beans() {
		assertNotNull(mapper);
		log.debug("mapper: {}", mapper);
	}

}
