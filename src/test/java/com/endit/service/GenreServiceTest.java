package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.GenreVO;
import com.endit.mapper.GenreMapper;

/**
 * <pre>
 * Class Name  : GenreServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 장르 Service를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 31. eunhu       최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author eunhu
 * @since 2026. 8. 31.
 */
@SpringBootTest
@Transactional
@DisplayName("GenreService 통합 테스트")
class GenreServiceTest {

	private static final int MISSING_GENRE_ID = Integer.MAX_VALUE;
	private static final int INVALID_GENRE_ID = 0;
	private static final int NEGATIVE_GENRE_ID = -1;

	@Autowired
	private GenreService genreService;

	/** GenreService에는 등록 기능이 없으므로 테스트 데이터는 매퍼로 직접 심는다 */
	@Autowired
	private GenreMapper genreMapper;

	/** 실제 DB 전체 목록 조회와 genre_id 오름차순 정렬 검증 */
	@Test
	@DisplayName("전체 목록 조회 시 등록한 장르가 포함되고 genre_id 오름차순으로 정렬")
	void retrieveAll() {
		GenreVO saved = createGenre("통합 장르");

		List<GenreVO> result = genreService.retrieveAll();

		assertTrue(result.stream()
				.anyMatch(genre -> genre.getGenreId() == saved.getGenreId()));

		for (int i = 1; i < result.size(); i++) {
			assertTrue(
					result.get(i - 1).getGenreId() < result.get(i).getGenreId(),
					"genre_id 오름차순이어야 합니다.");
		}
	}

	/** 검색 조건 없이 전체가 조회되는지 건수 증가로 검증 */
	@Test
	@DisplayName("전체 목록 조회에는 필터가 걸리지 않아 건수가 정확히 1 증가")
	void retrieveAllReturnsAll() {
		int before = genreService.retrieveAll().size();

		createGenre("건수 검증 장르");

		assertEquals(before + 1, genreService.retrieveAll().size());
	}

	/** 실제 DB에 등록한 장르의 단건 조회 검증 */
	@Test
	@DisplayName("장르 번호로 단건 조회")
	void get() {
		GenreVO saved = createGenre("단건 장르");

		GenreVO result = genreService.get(saved.getGenreId());

		assertEquals(saved.getGenreId(), result.getGenreId());
		assertEquals(saved.getName(), result.getName());
		assertEquals(saved.getExternalGenreId(), result.getExternalGenreId());
		assertNotNull(result.getName());
	}

	/** 존재하지 않는 장르 단건 조회 결과 검증 */
	@Test
	@DisplayName("존재하지 않는 장르 조회 시 예외 발생")
	void getNotFound() {
		assertThrows(
				NoSuchElementException.class,
				() -> genreService.get(MISSING_GENRE_ID));
	}

	/** 잘못된 장르 번호에 대한 입력값 검증 */
	@Test
	@DisplayName("장르 번호가 0 이하이면 예외 발생")
	void getInvalidId() {
		assertThrows(
				IllegalArgumentException.class,
				() -> genreService.get(INVALID_GENRE_ID));

		assertThrows(
				IllegalArgumentException.class,
				() -> genreService.get(NEGATIVE_GENRE_ID));
	}

	/** 외부 장르 ID 유니크 제약을 피하려고 UUID 기반으로 만든다 */
	private GenreVO createGenre(String name) {
		GenreVO param = new GenreVO();
		param.setExternalGenreId(UUID.randomUUID().toString().substring(0, 8));
		param.setName(name);

		genreMapper.doSave(param);

		return param;
	}

}