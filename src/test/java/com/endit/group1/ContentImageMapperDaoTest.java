package com.endit.group1;

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
import com.endit.domain.ContentImageVO;
import com.endit.mapper.ContentImageMapper;

@SpringBootTest
@Transactional
class ContentImageMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ContentImageMapper mapper;

	// 더미 CONTENT.CONTENT_ID = 1 - 인셉션
	private static final int PARENT_CONTENT_ID = 1;

	private ContentImageVO image01;
	private ContentImageVO image02;
	private ContentImageVO image03;

	private DTO dto;

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");

		int seq = 0;
		dto = new DTO();

		image01 = new ContentImageVO(seq, PARENT_CONTENT_ID, "http://image1.jpg", "사용않함");
		image02 = new ContentImageVO(seq, PARENT_CONTENT_ID, "http://image2.jpg", "사용않함");
		image03 = new ContentImageVO(seq, PARENT_CONTENT_ID, "http://image3.jpg", "사용않함");
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
		// 2. 3건 입력 (부모 CONTENT_ID=1 하드코딩)
		// 3. 페이징 조회

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		mapper.doSave(image01);
		mapper.doSave(image02);
		mapper.doSave(image03);
		assertEquals(3, mapper.totalCnt());

		dto.setPageNo(1);
		dto.setPageSize(10);
		dto.setSearchDiv("10");
		dto.setSearchWord(String.valueOf(PARENT_CONTENT_ID));

		List<ContentImageVO> list = mapper.doRetrieve(dto);
		for (ContentImageVO vo : list) {
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

		int flag = mapper.doSave(image01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		ContentImageVO updateVO = mapper.doSelectOne(image01);
		assertNotNull(updateVO);

		updateVO.setImageUrl(updateVO.getImageUrl() + "_U");

		flag = mapper.doUpdate(updateVO);
		assertEquals(1, flag);

		ContentImageVO outVO = mapper.doSelectOne(updateVO);
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

		int flag = mapper.doSave(image01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		flag = mapper.doDelete(image01);
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

		int flag = mapper.doSave(image01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());
		assertEquals(true, image01.getImageId() > 0);
		log.debug("saved imageId(image01)={}", image01.getImageId());

		flag = mapper.doSave(image02);
		assertEquals(1, flag);
		assertEquals(2, mapper.totalCnt());

		flag = mapper.doSave(image03);
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

		mapper.doSave(image01);
		mapper.doSave(image02);
		mapper.doSave(image03);
		assertEquals(3, mapper.totalCnt());

		ContentImageVO outVO01 = mapper.doSelectOne(image01);
		assertNotNull(outVO01);

		ContentImageVO outVO02 = mapper.doSelectOne(image02);
		assertNotNull(outVO02);

		ContentImageVO outVO03 = mapper.doSelectOne(image03);
		assertNotNull(outVO03);

		isSameData(image01, outVO01);
		isSameData(image02, outVO02);
		isSameData(image03, outVO03);
	}

	private void isSameData(ContentImageVO expected, ContentImageVO actual) {
		assertEquals(expected.getImageId(), actual.getImageId());
		assertEquals(expected.getContentId(), actual.getContentId());
		assertEquals(expected.getImageUrl(), actual.getImageUrl());
	}

	@Test
	@DisplayName("bean테스트")
	void beans() {
		assertNotNull(mapper);
		log.debug("mapper: {}", mapper);
	}

}
