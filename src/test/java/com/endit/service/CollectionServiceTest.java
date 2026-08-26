package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionVO;
import com.endit.mapper.CollectionMapper;
import com.endit.service.impl.CollectionServiceImpl;

/**
 * <pre>
 * Class Name  : CollectionServiceTest
 * Description : 컬렉션 Service의 입력값 검증, 페이징 처리 및 Mapper 호출을 검증하는 단위 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 21. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 21.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionService 테스트")
class CollectionServiceTest {

	// Service의 비즈니스 로직만 확인하기 위해 실제 DB Mapper를 Mockito Mock으로 대체한다.
	@Mock
	private CollectionMapper collectionMapper;

	private CollectionService collectionService;

	/** Mock CollectionMapper를 사용하는 Service 구현체 준비 */
	@BeforeEach
	void setUp() {
		collectionService = new CollectionServiceImpl(collectionMapper);
	}

	/** 목록 조회 시 기본 페이징값과 검색 결과 전체 건수 설정 검증 */
	@Test
	@DisplayName("목록 조회 시 기본 페이징과 전체 건수 설정")
	void retrieve() {
		
		// Given: 기본값 상태의 조회 조건과 Mapper 조회 결과를 준비
		DTO param = new DTO();
		CollectionVO collection = createCollection(1, 10, "테스트 컬렉션", "Y");

		when(collectionMapper.count(param)).thenReturn(1);
		when(collectionMapper.doRetrieve(param)).thenReturn(List.of(collection));

		// When: 컬렉션 목록을 조회
		List<CollectionVO> result = collectionService.retrieve(param);

		// Then: 기본 페이징값, 전체 건수 및 목록 결과가 일치해야 함
		assertEquals(1, result.size());
		assertEquals(collection, result.get(0));
		assertEquals(1, param.getPageNo());
		assertEquals(10, param.getPageSize());
		assertEquals(1, param.getTotalCnt());
	}

	/** 검색 결과가 없을 때 빈 목록 반환 및 불필요한 목록 쿼리 미호출 검증 */
	@Test
	@DisplayName("조회 결과가 없으면 목록 조회 실행하지 않음")
	void retrieveEmpty() {
		
		// Given: 검색 결과 건수가 0인 조회 조건을 준비
		DTO param = new DTO();

		when(collectionMapper.count(param)).thenReturn(0);

		// When: 컬렉션 목록을 조회
		List<CollectionVO> result = collectionService.retrieve(param);

		// Then: 빈 목록을 반환하고 목록 조회 쿼리를 실행하지 않아야 함
		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());

		verify(collectionMapper, never()).doRetrieve(any(DTO.class));
	}

	/** null 조회 조건에 대한 입력값 검증 */
	@Test
	@DisplayName("조회 조건이 null이면 예외 발생")
	void retrieveNull() {
		
		// When, Then: null 조회 조건을 전달하면 예외가 발생하고 Mapper를 호출하지 않아야 함
		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.retrieve(null));

		verifyNoInteractions(collectionMapper);
	}

	/** 컬렉션 번호를 이용한 단건 조회 및 Mapper 전달값 검증 */
	@Test
	@DisplayName("컬렉션 번호로 단건 조회")
	void get() {
		
		// Given: 컬렉션 단건 조회 결과를 준비
		CollectionVO collection = createCollection(1, 10, "테스트 컬렉션", "Y");

		when(collectionMapper.doSelectOne(any(CollectionVO.class)))
				.thenReturn(collection);

		// When: 컬렉션 번호로 단건 조회
		CollectionVO result = collectionService.get(1);

		// Then: 조회 결과와 Mapper에 전달한 컬렉션 번호가 일치해야 함
		assertEquals(collection, result);

		ArgumentCaptor<CollectionVO> captor = ArgumentCaptor.forClass(CollectionVO.class);

		verify(collectionMapper).doSelectOne(captor.capture());

		assertEquals(1, captor.getValue().getCollectionId());
	}

	/** 존재하지 않는 컬렉션 단건 조회 결과 검증 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 조회 시 예외 발생")
	void getNotFound() {
		
		// Given: Mapper가 조회 결과를 반환하지 않도록 준비
		when(collectionMapper.doSelectOne(any(CollectionVO.class)))
				.thenReturn(null);

		// When, Then: 존재하지 않는 컬렉션을 조회하면 예외가 발생해야 함
		assertThrows(
				NoSuchElementException.class,
				() -> collectionService.get(999));
	}

	/** 공개 여부 기본값 설정과 컬렉션 등록 결과 검증 */
	@Test
	@DisplayName("공개 여부가 없으면 Y로 등록")
	void create() {
		
		// Given: 공개 여부가 없는 등록 정보와 Mapper 등록 결과를 준비
		CollectionVO param = createCollection(1, 10, "신규 컬렉션", null);
		CollectionVO saved = createCollection(1, 10, "신규 컬렉션", "Y");

		when(collectionMapper.doSave(param)).thenReturn(1);

		when(collectionMapper.doSelectOne(any(CollectionVO.class)))
				.thenReturn(saved);

		// When: 컬렉션을 등록
		CollectionVO result = collectionService.create(param);

		// Then: 공개 여부가 Y로 설정되고 등록 결과가 반환되어야 함
		assertEquals(saved, result);
		assertEquals("Y", param.getIsPublic());

		verify(collectionMapper).doSave(param);
	}

	/** 필수 제목이 없는 컬렉션 등록 방지 검증 */
	@Test
	@DisplayName("제목이 없는 컬렉션은 등록하지 않음")
	void createWithoutTitle() {
		
		// Given: 제목이 공백인 컬렉션 정보를 준비
		CollectionVO param = createCollection(0, 10, " ", "Y");

		// When, Then: 등록 시 예외가 발생하고 Mapper를 호출하지 않아야 함
		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(param));

		verifyNoInteractions(collectionMapper);
	}

	/** 최대 길이를 초과한 제목의 컬렉션 등록 방지 검증 */
	@Test
	@DisplayName("제목이 100자를 초과한 컬렉션은 등록하지 않음")
	void createWithLongTitle() {
		
		// Given: 최대 길이를 초과한 제목의 컬렉션 정보를 준비
		CollectionVO param = createCollection(
				0,
				10,
				"가".repeat(101),
				"Y");

		// When, Then: 등록 시 예외가 발생하고 Mapper를 호출하지 않아야 함
		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(param));

		verifyNoInteractions(collectionMapper);
	}

	/** 최대 길이를 초과한 설명의 컬렉션 등록 방지 검증 */
	@Test
	@DisplayName("설명이 1000자를 초과한 컬렉션은 등록하지 않음")
	void createWithLongDescription() {
		
		// Given: 최대 길이를 초과한 설명의 컬렉션 정보를 준비
		CollectionVO param = createCollection(0, 10, "컬렉션", "Y");
		param.setDescription("가".repeat(1001));

		// When, Then: 등록 시 예외가 발생하고 Mapper를 호출하지 않아야 함
		assertThrows(
				IllegalArgumentException.class,
				() -> collectionService.create(param));

		verifyNoInteractions(collectionMapper);
	}

	/** 수정 시 기존 작성자와 공개 여부 유지 검증 */
	@Test
	@DisplayName("기존 작성자와 공개 여부를 유지하여 수정")
	void update() {
		
		// Given: 기존 컬렉션, 수정 요청 및 수정 결과를 준비
		CollectionVO existing = createCollection(1, 10, "기존 컬렉션", "N");
		CollectionVO param = createCollection(0, 999, "수정 컬렉션", null);
		CollectionVO updated = createCollection(1, 10, "수정 컬렉션", "N");

		when(collectionMapper.doSelectOne(any(CollectionVO.class)))
				.thenReturn(existing, updated);

		when(collectionMapper.doUpdate(param)).thenReturn(1);

		// When: 컬렉션을 수정
		CollectionVO result = collectionService.update(1, param);

		// Then: 기존 작성자와 공개 여부를 유지한 수정 결과가 반환되어야 함
		assertEquals(updated, result);
		assertEquals(1, param.getCollectionId());
		assertEquals(10, param.getMemberId());
		assertEquals("N", param.getIsPublic());

		verify(collectionMapper).doUpdate(param);
	}

	/** 존재하는 컬렉션 조회 후 삭제 처리 검증 */
	@Test
	@DisplayName("컬렉션을 조회한 후 삭제")
	void delete() {
		
		// Given: 삭제할 컬렉션과 Mapper 삭제 결과를 준비
		CollectionVO existing = createCollection(1, 10, "삭제 컬렉션", "Y");

		when(collectionMapper.doSelectOne(any(CollectionVO.class)))
				.thenReturn(existing);
		when(collectionMapper.doDelete(existing)).thenReturn(1);

		// When: 컬렉션을 삭제
		collectionService.delete(1);

		// Then: 조회된 컬렉션 정보로 삭제 쿼리를 실행해야 함
		verify(collectionMapper).doDelete(existing);
	}

	/** 테스트에 사용할 컬렉션 정보 생성 */
	private CollectionVO createCollection(
			int collectionId,
			int memberId,
			String title,
			String isPublic) {

		return new CollectionVO(
				collectionId,
				memberId,
				title,
				"컬렉션 설명",
				isPublic,
				null,
				null);
	}
}
