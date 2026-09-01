package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;

/**
 * <pre>
 * Class Name  : ContentServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 콘텐츠 Service를 검증하는 통합 테스트
 *               TMDB 연동(sync)은 외부 API를 실제로 호출하므로 이 테스트에서 다루지 않는다.
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
@DisplayName("ContentService 통합 테스트")
class ContentServiceTest {

	private static final String SEARCH_BY_TITLE_KO = "10";
	private static final String SEARCH_BY_EXTERNAL_ID = "40";
	private static final String SEARCH_BY_TITLE = "50";
	private static final String UNSUPPORTED_SEARCH_DIV = "99";

	private static final String SEARCH_KEY_SORT = "sort";
	private static final String SORT_BOX_OFFICE = "boxoffice";
	private static final String UNSUPPORTED_SORT = "rating";

	private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";
	private static final int OVER_MAX_PAGE_SIZE = 500;
	private static final int MAX_PAGE_SIZE = 100;
	private static final int DEFAULT_PAGE_SIZE = 12;
	private static final int FIRST_PAGE_NO = 1;

	@Autowired
	private ContentService contentService;

	/** 페이징값을 주지 않았을 때의 기본값 보정과 전체 건수 설정 검증 */
	@Test
	@DisplayName("목록 조회 시 기본 페이징과 전체 건수 설정")
	void retrieve() {
		createContent("페이징 검증 영화");

		DTO param = new DTO();

		List<ContentVO> result = contentService.retrieve(param);

		assertNotNull(result);
		assertEquals(FIRST_PAGE_NO, param.getPageNo());
		assertEquals(DEFAULT_PAGE_SIZE, param.getPageSize());
		assertTrue(param.getTotalCnt() > 0);
	}

	/** 허용 범위를 넘는 페이지 크기의 상한 보정 검증 */
	@Test
	@DisplayName("페이지 크기가 상한을 넘으면 100으로 보정")
	void retrieveOverMaxPageSize() {
		DTO param = new DTO();
		param.setPageSize(OVER_MAX_PAGE_SIZE);

		contentService.retrieve(param);

		assertEquals(MAX_PAGE_SIZE, param.getPageSize());
	}

	/** 박스오피스 정렬축이 콘텐츠 번호 오름차순으로 도는지 검증 - S-01 순위 */
	@Test
	@DisplayName("박스오피스 정렬은 콘텐츠 번호 오름차순")
	void retrieveSortsByBoxOffice() {
		// 검색어로 좁히지 않으면 content_id ASC 1페이지가 테이블 전체의 최저 번호 100건이라,
		// 시퀀스에서 최고 번호를 받는 이 두 행이 결과에 들어오지 못한다. 공용 마커로 둘만 잡는다
		String marker = UUID.randomUUID().toString().substring(0, 8);
		ContentVO first = createContent("정렬검증-" + marker + "-A");
		ContentVO second = createContent("정렬검증-" + marker + "-B");

		assertTrue(first.getContentId() < second.getContentId(), "SEQ_CONTENT가 오름차순으로 채번해야 합니다.");

		DTO param = new DTO();
		param.setPageSize(MAX_PAGE_SIZE);
		param.setSearchDiv(SEARCH_BY_TITLE);
		param.setSearchWord(marker);
		param.getSearchMap().put(SEARCH_KEY_SORT, SORT_BOX_OFFICE);

		List<ContentVO> result = contentService.retrieve(param);

		// 등록일 내림차순(기본)이면 나중에 만든 쪽이 먼저 오므로, 두 정렬이 서로 반대가 된다
		assertEquals(2, result.size());
		assertEquals(first.getContentId(), result.get(0).getContentId());
		assertEquals(second.getContentId(), result.get(1).getContentId());
	}

	/** 모르는 정렬값이 조용히 기본 정렬로 넘어가지 않고 막히는지 검증 */
	@Test
	@DisplayName("허용값 밖 정렬은 거부")
	void retrieveRejectsUnknownSort() {
		DTO param = new DTO();
		param.getSearchMap().put(SEARCH_KEY_SORT, UNSUPPORTED_SORT);

		assertThrows(
				IllegalArgumentException.class,
				() -> contentService.retrieve(param));
	}

	/**
	 * 검색어의 %가 LIKE 와일드카드가 아니라 글자로 취급되는지 검증.
	 * 이스케이프가 없으면 "%" 하나에 전체 행이 걸린다
	 */
	@Test
	@DisplayName("검색어의 %는 와일드카드가 아니라 글자로 취급")
	void retrieveEscapesLikeWildcard() {
		String marker = UUID.randomUUID().toString().substring(0, 8);
		ContentVO withPercent = createContent("와일드" + marker + "%카드");
		createContent("와일드" + marker + "X카드");

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_BY_TITLE);
		param.setSearchWord(marker + "%");

		List<ContentVO> result = contentService.retrieve(param);

		// 이스케이프가 없으면 marker로 시작하는 2건이 모두 걸린다
		assertEquals(1, result.size());
		assertEquals(withPercent.getContentId(), result.get(0).getContentId());
		assertEquals(1, param.getTotalCnt());
	}

	/** 국문 제목 검색축의 실제 DB 조회 검증 */
	@Test
	@DisplayName("국문 제목으로 검색")
	void retrieveByTitleKo() {
		String title = "제목검색-" + UUID.randomUUID();
		ContentVO saved = createContent(title);

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_BY_TITLE_KO);
		param.setSearchWord(title);

		List<ContentVO> result = contentService.retrieve(param);

		assertEquals(1, result.size());
		assertEquals(saved.getContentId(), result.get(0).getContentId());
		assertEquals(1, param.getTotalCnt());
	}

	/** 외부 ID 검색축의 실제 DB 조회 검증 */
	@Test
	@DisplayName("외부 ID로 검색")
	void retrieveByExternalId() {
		ContentVO saved = createContent("외부ID 검색 영화");

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_BY_EXTERNAL_ID);
		param.setSearchWord(saved.getExternalId());

		List<ContentVO> result = contentService.retrieve(param);

		assertEquals(1, result.size());
		assertEquals(saved.getContentId(), result.get(0).getContentId());
	}

	/** 검색어가 없을 때 조건 없이 전체가 조회되는지 검증 */
	@Test
	@DisplayName("검색어가 없으면 전체 조회")
	void retrieveWithoutSearchWord() {
		createContent("전체 조회 영화");

		DTO param = new DTO();

		List<ContentVO> result = contentService.retrieve(param);

		assertFalse(result.isEmpty());
		assertTrue(param.getTotalCnt() >= result.size());
	}

	/** 검색 결과가 없을 때의 반환값과 전체 건수 검증 */
	@Test
	@DisplayName("조회 결과가 없으면 빈 목록 반환")
	void retrieveEmpty() {
		DTO param = new DTO();
		param.setSearchDiv(SEARCH_BY_TITLE_KO);
		param.setSearchWord("존재하지않는영화-" + UUID.randomUUID());

		List<ContentVO> result = contentService.retrieve(param);

		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	/** null 조회 조건에 대한 입력값 검증 */
	@Test
	@DisplayName("조회 조건이 null이면 예외 발생")
	void retrieveNull() {
		assertThrows(
				IllegalArgumentException.class,
				() -> contentService.retrieve(null));
	}

	/**
	 * 지원하지 않는 검색 구분이 조건 없는 전체 조회로 새지 않는지 검증.
	 * contentWhere에 otherwise가 없어 매퍼까지 가면 WHERE 절이 통째로 빠진다.
	 */
	@Test
	@DisplayName("지원하지 않는 검색 구분이면 예외 발생")
	void retrieveUnsupportedSearchDiv() {
		DTO param = new DTO();
		param.setSearchDiv(UNSUPPORTED_SEARCH_DIV);
		param.setSearchWord("아무거나");

		assertThrows(
				IllegalArgumentException.class,
				() -> contentService.retrieve(param));
	}

	/** 목록 조회 결과의 이미지 경로가 풀 URL로 완성되는지 검증 */
	@Test
	@DisplayName("목록 조회 결과의 포스터 경로가 풀 URL로 완성")
	void retrieveCompletesImageUrl() {
		ContentVO saved = createContent("포스터 변환 영화");

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_BY_EXTERNAL_ID);
		param.setSearchWord(saved.getExternalId());

		List<ContentVO> result = contentService.retrieve(param);

		assertTrue(result.get(0).getPosterUrl().startsWith(TMDB_IMAGE_BASE_URL));
		assertTrue(result.get(0).getBackdropUrl().startsWith(TMDB_IMAGE_BASE_URL));
	}

	/** 등록된 외부 ID에 대한 중복 검사 검증 */
	@Test
	@DisplayName("등록된 외부 ID이면 true")
	void hasExternalId() {
		ContentVO saved = createContent("중복 검사 영화");

		assertTrue(contentService.hasExternalId(saved.getExternalId()));
	}

	/** 등록되지 않은 외부 ID에 대한 중복 검사 검증 */
	@Test
	@DisplayName("등록되지 않은 외부 ID이면 false")
	void hasExternalIdNotFound() {
		assertFalse(contentService.hasExternalId(UUID.randomUUID().toString()));
	}

	/** 비어 있는 외부 ID에 대한 입력값 검증 */
	@Test
	@DisplayName("외부 ID가 비어 있으면 예외 발생")
	void hasExternalIdBlank() {
		assertThrows(
				IllegalArgumentException.class,
				() -> contentService.hasExternalId(null));

		assertThrows(
				IllegalArgumentException.class,
				() -> contentService.hasExternalId(" "));
	}

	/** 실제 DB 등록 결과와 재조회 검증 */
	@Test
	@DisplayName("콘텐츠 등록")
	void create() {
		ContentVO result = createContent("등록 영화");

		assertTrue(result.getContentId() > 0);
		assertEquals("등록 영화", result.getTitleKo());
		assertNotNull(result.getCreatedDt());
		assertEquals(result.getContentId(), contentService.get(result.getContentId()).getContentId());
	}

	/** 외부 ID 유니크 제약(POL-030)에 대한 서버 검증 */
	@Test
	@DisplayName("이미 등록된 외부 ID면 등록하지 않음")
	void createDuplicateExternalId() {
		ContentVO saved = createContent("중복 등록 영화");

		ContentVO param = newContent("다른 제목 영화");
		param.setExternalId(saved.getExternalId());

		assertThrows(
				IllegalStateException.class,
				() -> contentService.create(param));
	}

	/** 필수 제목이 없는 콘텐츠 등록 방지 검증 */
	@Test
	@DisplayName("제목이 없는 콘텐츠는 등록하지 않음")
	void createWithoutTitle() {
		ContentVO param = newContent(" ");

		assertThrows(
				IllegalArgumentException.class,
				() -> contentService.create(param));
	}

	/** 등록 반환값의 이미지 경로가 풀 URL로 완성되는지 검증 */
	@Test
	@DisplayName("등록 반환값의 포스터 경로가 풀 URL로 완성")
	void createCompletesImageUrl() {
		ContentVO result = createContent("등록 변환 영화");

		assertTrue(result.getPosterUrl().startsWith(TMDB_IMAGE_BASE_URL));
	}

	/** 서비스로 콘텐츠를 등록하고 등록 결과를 돌려준다 */
	private ContentVO createContent(String titleKo) {
		return contentService.create(newContent(titleKo));
	}

	/** 외부 ID 유니크 제약을 피하려고 UUID 기반으로 만든다 */
	private ContentVO newContent(String titleKo) {
		ContentVO param = new ContentVO();
		param.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		param.setTitleKo(titleKo);
		param.setTitleOrg("Test Movie");
		param.setOverview("통합 테스트용 줄거리");
		param.setReleaseYear("2026-01-01");
		param.setRuntimeMin(120);
		param.setCountry("US");
		param.setPosterUrl("/test-poster.jpg");
		param.setBackdropUrl("/test-backdrop.jpg");

		return param;
	}

}
