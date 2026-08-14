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
import com.endit.domain.ContentImageVO;
import com.endit.domain.ContentVO;
import com.endit.mapper.ContentImageMapper;
import com.endit.mapper.ContentMapper;

@SpringBootTest
class ContentImageMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ContentImageMapper contentImageMapper;

	@Autowired
	private ContentMapper contentMapper;

	private ContentVO content01;

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

		content01 = new ContentVO(seq, "TMDB_IMG_C_1001", "이미지영화", "Image Movie", "줄거리",
				"2024-01-01", 120, "Korea", "http://poster", "http://backdrop", "사용않함", "사용않함");

		image01 = new ContentImageVO(seq, seq, "http://image1.jpg", "사용않함");
		image02 = new ContentImageVO(seq, seq, "http://image2.jpg", "사용않함");
		image03 = new ContentImageVO(seq, seq, "http://image3.jpg", "사용않함");
	}

	@AfterEach
	void tearDown() throws Exception {
		log.debug("*****************************");
		log.debug("*@AfterEach*");
		log.debug("*****************************");
	}

	private void prepareParents() {
		contentImageMapper.deleteAll();
		contentMapper.deleteAll();

		contentMapper.doSave(content01);

		image01.setContentId(content01.getContentId());
		image02.setContentId(content01.getContentId());
		image03.setContentId(content01.getContentId());
	}

	@Test
	void doRetrieve() {
		log.debug("---------------------------");
		log.debug("*doRetrieve()*");
		log.debug("---------------------------");

		prepareParents();
		assertEquals(0, contentImageMapper.selectAllCount());

		contentImageMapper.doSave(image01);
		contentImageMapper.doSave(image02);
		contentImageMapper.doSave(image03);
		assertEquals(3, contentImageMapper.selectAllCount());

		dto.setPageNo(1);
		dto.setPageSize(10);
		dto.setSearchDiv("10");
		dto.setSearchWord(String.valueOf(content01.getContentId()));

		List<ContentImageVO> list = contentImageMapper.doRetrieve(dto);
		for (ContentImageVO vo : list) {
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

		contentImageMapper.doSave(image01);
		assertEquals(1, contentImageMapper.selectAllCount());

		contentImageMapper.doSave(image02);
		assertEquals(2, contentImageMapper.selectAllCount());

		contentImageMapper.doDelete(image01);
		assertEquals(1, contentImageMapper.selectAllCount());
	}

	@Test
	void doUpdate() {
		log.debug("---------------------------");
		log.debug("*doUpdate()*");
		log.debug("---------------------------");

		prepareParents();

		int flag = contentImageMapper.doSave(image01);
		assertEquals(1, flag);

		ContentImageVO outVO01 = contentImageMapper.doSelectOne(image01);
		assertNotNull(outVO01);

		outVO01.setImageUrl(outVO01.getImageUrl() + "_U");

		flag = contentImageMapper.doUpdate(outVO01);
		assertEquals(1, flag);

		ContentImageVO resultVO01 = contentImageMapper.doSelectOne(outVO01);
		assertNotNull(resultVO01);
		isSameData(resultVO01, outVO01);
	}

	@Test
	void doSave() {
		log.debug("---------------------------");
		log.debug("*doSave()*");
		log.debug("---------------------------");

		prepareParents();

		int flag = contentImageMapper.doSave(image01);
		assertEquals(1, flag);
		log.debug("saved imageId(image01)={}", image01.getImageId());
		assertEquals(1, contentImageMapper.selectAllCount());

		flag = contentImageMapper.doSave(image02);
		assertEquals(1, flag);
		assertEquals(2, contentImageMapper.selectAllCount());

		flag = contentImageMapper.doSave(image03);
		assertEquals(1, flag);
		assertEquals(3, contentImageMapper.selectAllCount());
	}

	@Test
	void doSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSelectOne()*");
		log.debug("---------------------------");

		prepareParents();

		contentImageMapper.doSave(image01);
		contentImageMapper.doSave(image02);
		contentImageMapper.doSave(image03);
		assertEquals(3, contentImageMapper.selectAllCount());

		ContentImageVO outVO01 = contentImageMapper.doSelectOne(image01);
		assertNotNull(outVO01);

		ContentImageVO outVO02 = contentImageMapper.doSelectOne(image02);
		ContentImageVO outVO03 = contentImageMapper.doSelectOne(image03);

		isSameData(outVO01, image01);
		isSameData(outVO02, image02);
		isSameData(outVO03, image03);
	}

	private void isSameData(ContentImageVO outVO, ContentImageVO image) {
		assertEquals(outVO.getImageId(), image.getImageId());
		assertEquals(outVO.getContentId(), image.getContentId());
		assertEquals(outVO.getImageUrl(), image.getImageUrl());
	}

	@Test
	void beans() {
		assertNotNull(contentImageMapper);
		assertNotNull(contentMapper);
		log.debug("contentImageMapper: {}", contentImageMapper);
	}

}
