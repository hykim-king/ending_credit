package com.endit.cmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DTOTest {

	@Test
	void constructorCalculatesPagination() {
		DTO dto = new DTO(11, 10, 105);

		assertEquals(11, dto.getStartNo());
		assertEquals(11, dto.getEndNo());
		assertTrue(dto.isPre());
		assertFalse(dto.isNext());
	}

	@Test
	void settersRecalculatePagination() {
		DTO dto = new DTO();
		dto.setPageNo(2);
		dto.setPageSize(10);
		dto.setTotalCnt(120);

		assertEquals(1, dto.getStartNo());
		assertEquals(10, dto.getEndNo());
		assertFalse(dto.isPre());
		assertTrue(dto.isNext());

		dto.setTotalCnt(15);

		assertEquals(1, dto.getStartNo());
		assertEquals(2, dto.getEndNo());
		assertFalse(dto.isNext());
	}

	@Test
	void invalidPagingValuesResetCalculatedState() {
		DTO dto = new DTO(1, 10, 100);

		dto.setPageSize(0);

		assertEquals(0, dto.getStartNo());
		assertEquals(0, dto.getEndNo());
		assertFalse(dto.isPre());
		assertFalse(dto.isNext());
	}

	@Test
	void searchNumberReturnsNullForNonNumericSearchWord() {
		DTO dto = new DTO();
		dto.setSearchWord("not-a-number");

		assertNull(dto.getSearchNumber());

		dto.setSearchWord(" 42 ");
		assertEquals(42L, dto.getSearchNumber());
	}
}
