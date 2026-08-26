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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;
import com.endit.mapper.CollectionItemMapper;
import com.endit.service.impl.CollectionItemServiceImpl;

/**
 * <pre>
 * Class Name  : CollectionItemServiceTest
 * Description : 컬렉션 작품 Service의 검증, 페이징 및 Mapper 호출을 검증하는 단위 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionItemService 테스트")
class CollectionItemServiceTest {

	// 실제 DB Mapper 대신 반환값과 호출 여부를 제어할 수 있는 가짜 객체를 사용한다.
	@Mock
	private CollectionItemMapper collectionItemMapper;

	private CollectionItemService collectionItemService;

	/** Mock CollectionItemMapper를 사용하는 Service 구현체 준비 */
	@BeforeEach
	void setUp() {
		collectionItemService = new CollectionItemServiceImpl(collectionItemMapper);
	}

	/** 컬렉션 번호 조건과 기본 페이징값을 적용한 목록 조회 검증 */
	@Test
	@DisplayName("컬렉션 작품 목록 조회 시 컬렉션 조건과 페이징 설정")
	void retrieve() {
		// Given: 기본값 DTO와 Mapper가 반환할 작품 한 건을 준비한다.
		DTO param = new DTO();
		CollectionItemVO item = createItem(1, 100);

		// Mock 메서드가 반환할 값을 미리 지정한다.
		when(collectionItemMapper.count(param)).thenReturn(1);
		when(collectionItemMapper.doRetrieve(param)).thenReturn(List.of(item));

		// When: 테스트 대상인 Service 메서드를 실제로 호출한다.
		List<CollectionItemVO> result = collectionItemService.retrieve(1, param);

		// Then: 목록 결과와 Service가 DTO에 계산해 넣은 값들을 함께 확인한다.
		assertEquals(1, result.size());
		assertEquals(item, result.get(0));
		assertEquals(1, param.getPageNo());
		assertEquals(12, param.getPageSize());
		assertEquals(1, param.getTotalCnt());
		assertEquals("10", param.getSearchDiv());
		assertEquals("1", param.getSearchWord());
	}

	/** 컬렉션 작품이 없을 때 빈 목록 반환 검증 */
	@Test
	@DisplayName("컬렉션 작품이 없으면 목록 쿼리를 실행하지 않음")
	void retrieveEmpty() {
		// Given: 전체 건수가 0인 상황을 만든다.
		DTO param = new DTO();

		when(collectionItemMapper.count(param)).thenReturn(0);

		// When: 작품 목록을 조회한다.
		List<CollectionItemVO> result = collectionItemService.retrieve(1, param);

		// Then: 빈 목록을 반환하고 무거운 목록 SQL은 호출하지 않아야 한다.
		assertTrue(result.isEmpty());
		// 해당 Mock 메서드가 한 번도 호출되지 않았음을 검증한다.
		verify(collectionItemMapper, never()).doRetrieve(any(DTO.class));
	}

	/** 복합 키를 이용한 컬렉션 작품 단건 조회 검증 */
	@Test
	@DisplayName("컬렉션 작품 단건 조회")
	void get() {
		// Given: Service 내부에서 어떤 조회 키 객체를 만들더라도 준비한 작품을 반환한다.
		CollectionItemVO item = createItem(1, 100);

		when(collectionItemMapper.doSelectOne(any(CollectionItemVO.class)))
				.thenReturn(item);

		// When: 복합 키인 컬렉션 번호와 콘텐츠 번호로 조회한다.
		CollectionItemVO result = collectionItemService.get(1, 100);

		// Then: Mapper 조회 결과가 Service 결과로 반환되는지 확인한다.
		assertEquals(item, result);
	}

	/** 존재하지 않는 컬렉션 작품 조회 결과 검증 */
	@Test
	@DisplayName("존재하지 않는 컬렉션 작품 조회 시 예외 발생")
	void getNotFound() {
		// Given: Mapper의 null은 복합 키에 해당하는 행이 없다는 의미다.
		when(collectionItemMapper.doSelectOne(any(CollectionItemVO.class)))
				.thenReturn(null);

		// When, Then: Service가 null을 404 처리용 NoSuchElementException으로 변환한다.
		assertThrows(
				NoSuchElementException.class,
				() -> collectionItemService.get(1, 999));
	}

	/** 중복 확인 후 컬렉션 작품 추가 결과 검증 */
	@Test
	@DisplayName("중복 확인 후 컬렉션 작품 추가")
	void create() {
		// Given: 요청 객체와 DB 저장 후 조회될 결과 객체를 준비한다.
		CollectionItemVO param = createItem(0, 100);
		CollectionItemVO created = createItem(1, 100);

		// 같은 메서드의 첫 호출은 중복 확인용 null, 두 번째는 저장 결과를 반환한다.
		when(collectionItemMapper.doSelectOne(param)).thenReturn(null, created);
		when(collectionItemMapper.doSave(param)).thenReturn(1);

		// When: URL에서 전달됐다고 가정한 컬렉션 번호 1에 작품을 추가한다.
		CollectionItemVO result = collectionItemService.create(1, param);

		// Then: 저장 결과와 URL의 collectionId가 요청 객체에 반영됐는지 확인한다.
		assertEquals(created, result);
		assertEquals(1, param.getCollectionId());
		verify(collectionItemMapper).doSave(param);
	}

	/** 이미 포함된 작품의 중복 추가 방지 검증 */
	@Test
	@DisplayName("이미 포함된 작품은 추가하지 않음")
	void createDuplicate() {
		// Given: 중복 확인 조회에서 기존 행을 반환한다.
		CollectionItemVO param = createItem(0, 100);
		when(collectionItemMapper.doSelectOne(param)).thenReturn(createItem(1, 100));

		// When, Then: 중복 예외가 발생하고 INSERT Mapper는 호출되지 않아야 한다.
		assertThrows(
				IllegalStateException.class,
				() -> collectionItemService.create(1, param));

		verify(collectionItemMapper, never())
				.doSave(any(CollectionItemVO.class));
	}

	/** 존재하는 컬렉션 작품 조회 후 삭제 검증 */
	@Test
	@DisplayName("컬렉션 작품을 조회한 후 삭제")
	void delete() {
		// Given: 삭제 대상 조회 결과와 삭제 성공 행 수 1을 준비한다.
		CollectionItemVO existing = createItem(1, 100);

		when(collectionItemMapper.doSelectOne(any(CollectionItemVO.class)))
				.thenReturn(existing);
		when(collectionItemMapper.doDelete(existing)).thenReturn(1);

		// When: 복합 키로 작품 삭제를 요청한다.
		collectionItemService.delete(1, 100);

		// Then: 실제로 조회된 객체가 삭제 Mapper에 전달됐는지 확인한다.
		verify(collectionItemMapper).doDelete(existing);
	}

	/** 잘못된 컬렉션 번호에 대한 입력값 검증 */
	@Test
	@DisplayName("잘못된 컬렉션 번호이면 Mapper를 호출하지 않음")
	void invalidCollectionId() {
		// When, Then: 입력값 검증이 Mapper 호출보다 먼저 실행되는지 확인한다.
		assertThrows(
				IllegalArgumentException.class,
				() -> collectionItemService.retrieve(0, new DTO()));

		// Mock의 어떤 메서드도 호출되지 않아야 입력 검증의 위치가 올바른 것이다.
		verifyNoInteractions(collectionItemMapper);
	}

	/** 테스트에 사용할 컬렉션 작품 정보 생성 */
	private CollectionItemVO createItem(int collectionId, int contentId) {
		return new CollectionItemVO(collectionId, contentId, null);
	}
}
