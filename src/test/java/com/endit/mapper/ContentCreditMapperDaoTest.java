package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import com.endit.domain.ContentCreditVO;

@SpringBootTest
@Transactional
class ContentCreditMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ContentCreditMapper mapper;

	// 더미 CONTENT.CONTENT_ID = 1 - 인셉션
	private static final int PARENT_CONTENT_ID = 1;
	// 더미 PERSON.PERSON_ID = 1 - 디카프리오, 2 - 놀란, 6 - 톰 하디
	private static final int PARENT_PERSON_ID_01 = 1;
	private static final int PARENT_PERSON_ID_02 = 2;
	private static final int PARENT_PERSON_ID_03 = 6;

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

		credit01 = new ContentCreditVO(seq, PARENT_CONTENT_ID, PARENT_PERSON_ID_01, "ACTOR", "코브", 1);
		credit02 = new ContentCreditVO(seq, PARENT_CONTENT_ID, PARENT_PERSON_ID_02, "DIRECTOR", null, 1);
		credit03 = new ContentCreditVO(seq, PARENT_CONTENT_ID, PARENT_PERSON_ID_03, "ACTOR", "임스", 2);
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
		// 2. 3건 입력 (부모 CONTENT/PERSON PK 하드코딩)
		// 3. 페이징 조회

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		mapper.doSave(credit01);
		mapper.doSave(credit02);
		mapper.doSave(credit03);
		assertEquals(3, mapper.totalCnt());

		dto.setPageNo(1);
		dto.setPageSize(10);
		dto.setSearchDiv("10");
		dto.setSearchWord(String.valueOf(PARENT_CONTENT_ID));

		List<ContentCreditVO> list = mapper.doRetrieve(dto);
		for (ContentCreditVO vo : list) {
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

		int flag = mapper.doSave(credit01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		ContentCreditVO updateVO = mapper.doSelectOne(credit01);
		assertNotNull(updateVO);

		updateVO.setCharacter(updateVO.getCharacter() + "_U");
		updateVO.setDisplayOrder(9);

		flag = mapper.doUpdate(updateVO);
		assertEquals(1, flag);

		ContentCreditVO outVO = mapper.doSelectOne(updateVO);
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

		int flag = mapper.doSave(credit01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		flag = mapper.doDelete(credit01);
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
		// 3. 건수비교

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		int flag = mapper.doSave(credit01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());
		assertEquals(true, credit01.getCreditId() > 0);
		log.debug("saved creditId(credit01)={}", credit01.getCreditId());

		flag = mapper.doSave(credit02);
		assertEquals(1, flag);
		assertEquals(2, mapper.totalCnt());

		flag = mapper.doSave(credit03);
		assertEquals(1, flag);
		assertEquals(3, mapper.totalCnt());
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

		mapper.doSave(credit01);
		mapper.doSave(credit02);
		mapper.doSave(credit03);
		assertEquals(3, mapper.totalCnt());

		ContentCreditVO outVO01 = mapper.doSelectOne(credit01);
		assertNotNull(outVO01);

		ContentCreditVO outVO02 = mapper.doSelectOne(credit02);
		assertNotNull(outVO02);

		ContentCreditVO outVO03 = mapper.doSelectOne(credit03);
		assertNotNull(outVO03);

		isSameData(credit01, outVO01);
		isSameData(credit02, outVO02);
		isSameData(credit03, outVO03);
	}

	private void isSameData(ContentCreditVO expected, ContentCreditVO actual) {
		assertEquals(expected.getCreditId(), actual.getCreditId());
		assertEquals(expected.getContentId(), actual.getContentId());
		assertEquals(expected.getPersonId(), actual.getPersonId());
		assertEquals(expected.getRole(), actual.getRole());
		assertEquals(expected.getCharacter(), actual.getCharacter());
		assertEquals(expected.getDisplayOrder(), actual.getDisplayOrder());
	}

	@Test
	@DisplayName("bean테스트")
	void beans() {
		assertNotNull(mapper);
		log.debug("mapper: {}", mapper);
	}

}
