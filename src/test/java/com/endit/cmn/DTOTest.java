package com.endit.cmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("공용 DTO 테스트")
class DTOTest {

	@Test
	@DisplayName("생성자로 전달한 값에 따라 페이지 정보를 계산한다")
	void constructorCalculatesPagination() {
		// Given & When: 전체 105건을 10건씩 보여주는 11페이지 DTO를 생성
		DTO dto = new DTO(11, 10, 105);

		// Then: 마지막 페이지인 11페이지와 이전/다음 페이지 여부를 확인
		assertEquals(11, dto.getStartNo());
		assertEquals(11, dto.getEndNo());
		assertTrue(dto.isPre());
		assertFalse(dto.isNext());
	}

	@Test
	@DisplayName("페이징 값이 바뀌면 페이지 정보를 다시 계산한다")
	void settersRecalculatePagination() {
		// Given: 2페이지, 페이지당 10건, 전체 120건으로 설정
		DTO dto = new DTO();
		dto.setPageNo(2);
		dto.setPageSize(10);
		dto.setTotalCnt(120);

		// Then: 현재 페이지 묶음은 1~10이고 다음 묶음이 존재
		assertEquals(1, dto.getStartNo());
		assertEquals(10, dto.getEndNo());
		assertFalse(dto.isPre());
		assertTrue(dto.isNext());

		// When: 전체 건수를 15건으로 변경
		dto.setTotalCnt(15);

		// Then: 마지막 페이지가 2페이지로 다시 계산됨
		assertEquals(1, dto.getStartNo());
		assertEquals(2, dto.getEndNo());
		assertFalse(dto.isNext());
	}

	@Test
	@DisplayName("잘못된 페이징 값은 계산 결과를 초기화한다")
	void invalidPagingValuesResetCalculatedState() {
		// Given: 정상적인 페이지 정보가 계산된 DTO
		DTO dto = new DTO(1, 10, 100);

		// When: 페이지 크기를 0으로 변경
		dto.setPageSize(0);

		// Then: 계산된 페이지 정보를 기본값으로 초기화
		assertEquals(0, dto.getStartNo());
		assertEquals(0, dto.getEndNo());
		assertFalse(dto.isPre());
		assertFalse(dto.isNext());
	}

	@Test
	@DisplayName("숫자가 아닌 검색어는 숫자로 변환하지 않는다")
	void nonNumericSearchWordReturnsNull() {
		// Given: 숫자가 아닌 검색어
		DTO dto = new DTO();
		dto.setSearchWord("not-a-number");

		// Then: 숫자 검색값은 null
		assertNull(dto.getSearchNumber());
	}

	@Test
	@DisplayName("공백이 포함된 숫자 검색어를 Long으로 변환한다")
	void numericSearchWordReturnsLong() {
		// Given: 앞뒤에 공백이 있는 숫자 검색어
		DTO dto = new DTO();
		dto.setSearchWord(" 42 ");

		// Then: 공백을 제거한 Long 값으로 변환
		assertEquals(42L, dto.getSearchNumber());
	}
}
