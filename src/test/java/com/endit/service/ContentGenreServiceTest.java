package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.endit.domain.ContentGenreVO;
import com.endit.domain.ContentVO;
import com.endit.domain.GenreVO;
import com.endit.mapper.GenreMapper;

/**
 * <pre>
 * Class Name  : ContentGenreServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 콘텐츠-장르 연결 Service를 검증하는 통합 테스트
 *               복합 PK(content_id, genre_id)뿐인 연결 테이블이라 수정 기능이 없고,
 *               존재 확인(has)과 중복 연결 차단이 핵심 검증 대상이다.
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 9. 1.  eunhu       최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author eunhu
 * @since 2026. 9. 1.
 */
@SpringBootTest
@Transactional
@DisplayName("ContentGenreService 통합 테스트")
class ContentGenreServiceTest {

	private static final String SAMPLE_PATH = "/sample-content.jpg";

	private static final int MISSING_ID = Integer.MAX_VALUE;
	private static final int INVALID_ID = 0;
	private static final int NEGATIVE_ID = -1;

	@Autowired
	private ContentGenreService contentGenreService;

	@Autowired
	private ContentService contentService;

	/** GenreService에는 등록 기능이 없으므로 테스트 장르는 매퍼로 직접 심는다 */
	@Autowired
	private GenreMapper genreMapper;

	/** 연결 전체 목록이 genre_id 오름차순으로 오고 장르명까지 조인돼 오는지 검증 */
	@Test
	@DisplayName("전체 목록 조회는 genre_id 오름차순이고 장르명이 채워진다")
	void retrieveAll() {
		int contentId = createContentId();
		GenreVO first = createGenre("연결 장르 A");
		GenreVO second = createGenre("연결 장르 B");

		assertTrue(first.getGenreId() < second.getGenreId(), "SEQ_GENRE가 오름차순으로 채번해야 합니다.");

		// 나중에 만든(번호가 큰) 쪽을 먼저 연결해, 등록 순서가 아니라 genre_id로 정렬되는지 본다
		contentGenreService.create(contentId, second.getGenreId());
		contentGenreService.create(contentId, first.getGenreId());

		List<ContentGenreVO> result = contentGenreService.retrieveAll(contentId);

		assertEquals(2, result.size());
		assertEquals(first.getGenreId(), result.get(0).getGenreId());
		assertEquals(second.getGenreId(), result.get(1).getGenreId());
		assertEquals(first.getName(), result.get(0).getGenreName());
		assertNotNull(result.get(1).getGenreName());
	}

	/** 장르를 하나도 연결하지 않은 콘텐츠는 예외가 아니라 빈 목록을 받는지 검증 */
	@Test
	@DisplayName("연결이 없는 콘텐츠는 빈 목록")
	void retrieveAllEmpty() {
		int contentId = createContentId();

		assertTrue(contentGenreService.retrieveAll(contentId).isEmpty());
	}

	/** 연결 단건 조회에도 장르명이 함께 오는지 검증 */
	@Test
	@DisplayName("연결 단건 조회는 장르명까지 반환")
	void get() {
		int contentId = createContentId();
		GenreVO genre = createGenre("단건 연결 장르");
		contentGenreService.create(contentId, genre.getGenreId());

		ContentGenreVO result = contentGenreService.get(contentId, genre.getGenreId());

		assertEquals(contentId, result.getContentId());
		assertEquals(genre.getGenreId(), result.getGenreId());
		assertEquals(genre.getName(), result.getGenreName());
	}

	/** 연결되지 않은 조합을 get으로 물으면 예외가 나는지 검증 - has와 갈리는 지점 */
	@Test
	@DisplayName("연결되지 않은 조합의 단건 조회는 예외")
	void getNotFound() {
		int contentId = createContentId();
		GenreVO genre = createGenre("연결 안 한 장르");

		assertThrows(
				NoSuchElementException.class,
				() -> contentGenreService.get(contentId, genre.getGenreId()));
	}

	/** has는 없을 때 예외 대신 false를 돌려주는지 검증 - AD-03 중복 연결 방지의 근거 */
	@Test
	@DisplayName("존재 확인은 예외 없이 true/false를 돌려준다")
	void has() {
		int contentId = createContentId();
		GenreVO linked = createGenre("연결한 장르");
		GenreVO unlinked = createGenre("연결 안 한 장르");

		assertFalse(contentGenreService.has(contentId, linked.getGenreId()));

		contentGenreService.create(contentId, linked.getGenreId());

		assertTrue(contentGenreService.has(contentId, linked.getGenreId()));
		// 같은 콘텐츠라도 연결하지 않은 장르는 false
		assertFalse(contentGenreService.has(contentId, unlinked.getGenreId()));
		// 존재하지 않는 콘텐츠도 예외가 아니라 false
		assertFalse(contentGenreService.has(MISSING_ID, linked.getGenreId()));
	}

	/** 같은 조합을 두 번 연결하면 복합 PK 위반 전에 서비스가 막는지 검증 */
	@Test
	@DisplayName("이미 연결된 장르를 다시 등록하면 예외")
	void createRejectsDuplicate() {
		int contentId = createContentId();
		GenreVO genre = createGenre("중복 연결 장르");
		contentGenreService.create(contentId, genre.getGenreId());

		assertThrows(
				IllegalStateException.class,
				() -> contentGenreService.create(contentId, genre.getGenreId()));
	}

	/** 삭제 후 존재 확인이 false로 뒤집히는지 검증 */
	@Test
	@DisplayName("연결 삭제 후에는 존재 확인이 false")
	void delete() {
		int contentId = createContentId();
		GenreVO genre = createGenre("삭제할 연결 장르");
		contentGenreService.create(contentId, genre.getGenreId());

		contentGenreService.delete(contentId, genre.getGenreId());

		assertFalse(contentGenreService.has(contentId, genre.getGenreId()));
		assertTrue(contentGenreService.retrieveAll(contentId).isEmpty());
	}

	/** 0 이하 번호가 조회 키 생성 단계에서 막히는지 검증 */
	@Test
	@DisplayName("콘텐츠·장르 번호가 0 이하이면 예외")
	void invalidId() {
		assertThrows(
				IllegalArgumentException.class,
				() -> contentGenreService.has(INVALID_ID, 1));

		assertThrows(
				IllegalArgumentException.class,
				() -> contentGenreService.has(1, NEGATIVE_ID));

		assertThrows(
				IllegalArgumentException.class,
				() -> contentGenreService.retrieveAll(INVALID_ID));
	}

	/** 테스트용 콘텐츠를 등록하고 번호를 돌려준다 */
	private int createContentId() {
		ContentVO content = new ContentVO();
		content.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		content.setTitleKo("장르 연결 테스트 영화");
		content.setTitleOrg("Genre Link Test Movie");
		content.setOverview("통합 테스트용 줄거리");
		content.setReleaseYear("2026-01-01");
		content.setRuntimeMin(120);
		content.setCountry("US");
		content.setPosterUrl(SAMPLE_PATH);
		content.setBackdropUrl(SAMPLE_PATH);

		return contentService.create(content).getContentId();
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
