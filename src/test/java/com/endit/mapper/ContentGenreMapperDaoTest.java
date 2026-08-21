package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import com.endit.domain.ContentGenreVO;

@SpringBootTest
@Transactional
@Disabled("deleteAll() 전체 삭제를 제거하고 테스트 데이터를 격리할 때까지 비활성화")
class ContentGenreMapperDaoTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ContentGenreMapper mapper;

	// 더미 CONTENT.CONTENT_ID = 1 - 인셉션
	private static final int PARENT_CONTENT_ID = 1;
	// 더미 GENRE.GENRE_ID = 1 - 액션, 2 - SF, 3 - 드라마
	private static final int PARENT_GENRE_ID_01 = 1;
	private static final int PARENT_GENRE_ID_02 = 2;
	private static final int PARENT_GENRE_ID_03 = 3;

	private ContentGenreVO link01;
	private ContentGenreVO link02;
	private ContentGenreVO link03;

	private DTO dto;

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");

		dto = new DTO();

		link01 = new ContentGenreVO(PARENT_CONTENT_ID, PARENT_GENRE_ID_01);
		link02 = new ContentGenreVO(PARENT_CONTENT_ID, PARENT_GENRE_ID_02);
		link03 = new ContentGenreVO(PARENT_CONTENT_ID, PARENT_GENRE_ID_03);
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
		// 2. 3건 입력 (부모 CONTENT/GENRE PK 하드코딩)
		// 3. 페이징 조회

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		mapper.doSave(link01);
		mapper.doSave(link02);
		mapper.doSave(link03);
		assertEquals(3, mapper.totalCnt());

		dto.setPageNo(1);
		dto.setPageSize(10);
		dto.setSearchDiv("10");
		dto.setSearchWord(String.valueOf(PARENT_CONTENT_ID));

		List<ContentGenreVO> list = mapper.doRetrieve(dto);
		for (ContentGenreVO vo : list) {
			log.debug("{}", vo);
		}
		assertEquals(3, list.size());
	}

	@Test
	void doUpdate() {
		log.debug("---------------------------");
		log.debug("*doUpdate()*");
		log.debug("---------------------------");
		// 복합 PK 테이블: 동일 키 유지(연결 존재) 확인

		mapper.deleteAll();
		assertEquals(0, mapper.totalCnt());

		int flag = mapper.doSave(link01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		ContentGenreVO updateVO = mapper.doSelectOne(link01);
		assertNotNull(updateVO);

		flag = mapper.doUpdate(updateVO);
		assertEquals(1, flag);

		ContentGenreVO outVO = mapper.doSelectOne(updateVO);
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

		int flag = mapper.doSave(link01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		flag = mapper.doDelete(link01);
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

		int flag = mapper.doSave(link01);
		assertEquals(1, flag);
		assertEquals(1, mapper.totalCnt());

		flag = mapper.doSave(link02);
		assertEquals(1, flag);
		assertEquals(2, mapper.totalCnt());

		flag = mapper.doSave(link03);
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

		mapper.doSave(link01);
		mapper.doSave(link02);
		mapper.doSave(link03);
		assertEquals(3, mapper.totalCnt());

		ContentGenreVO outVO01 = mapper.doSelectOne(link01);
		assertNotNull(outVO01);

		ContentGenreVO outVO02 = mapper.doSelectOne(link02);
		assertNotNull(outVO02);

		ContentGenreVO outVO03 = mapper.doSelectOne(link03);
		assertNotNull(outVO03);

		isSameData(link01, outVO01);
		isSameData(link02, outVO02);
		isSameData(link03, outVO03);
	}

	private void isSameData(ContentGenreVO expected, ContentGenreVO actual) {
		assertEquals(expected.getContentId(), actual.getContentId());
		assertEquals(expected.getGenreId(), actual.getGenreId());
	}

	@Test
	@DisplayName("bean테스트")
	void beans() {
		assertNotNull(mapper);
		log.debug("mapper: {}", mapper);
	}

}
