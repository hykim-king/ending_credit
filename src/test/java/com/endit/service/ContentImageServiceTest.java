package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.endit.domain.ContentImageVO;
import com.endit.domain.ContentVO;

/**
 * <pre>
 * Class Name  : ContentImageServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 콘텐츠 이미지 Service를 검증하는 통합 테스트
 *               이미지 크기는 ContentImageServiceImpl이 전부 정하므로, 용도별 변환 메서드가
 *               각각 어떤 크기의 URL을 만드는지가 이 테스트의 핵심 검증 대상이다.
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
@DisplayName("ContentImageService 통합 테스트")
class ContentImageServiceTest {

	private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";
	private static final String THUMBNAIL_PREFIX = TMDB_IMAGE_BASE_URL + "w500/";
	private static final String FULL_SCREEN_PREFIX = TMDB_IMAGE_BASE_URL + "original/";
	private static final String CREDIT_PROFILE_PREFIX = TMDB_IMAGE_BASE_URL + "w185/";
	private static final String PERSON_PROFILE_PREFIX = TMDB_IMAGE_BASE_URL + "w300/";

	private static final String SAMPLE_PATH = "/sample-image.jpg";
	private static final int INVALID_CONTENT_ID = 0;
	private static final int NEGATIVE_CONTENT_ID = -1;

	@Autowired
	private ContentImageService contentImageService;

	@Autowired
	private ContentService contentService;

	/** 목록 조회 결과에 썸네일용과 확대용 URL이 서로 다른 크기로 채워지는지 검증 */
	@Test
	@DisplayName("목록 조회 시 썸네일과 확대용 URL이 각각 채워짐")
	void retrieve() {
		int contentId = createContentId();
		contentImageService.create(contentId, newImage());

		DTO param = new DTO();
		List<ContentImageVO> result = contentImageService.retrieve(contentId, param);

		assertEquals(1, result.size());
		assertTrue(result.get(0).getImageUrl().startsWith(THUMBNAIL_PREFIX));
		assertTrue(result.get(0).getZoomImageUrl().startsWith(FULL_SCREEN_PREFIX));
	}

	/** 전체 목록 조회도 retrieve에 위임하므로 두 URL이 동일하게 채워지는지 검증 */
	@Test
	@DisplayName("전체 목록 조회도 두 URL이 채워짐")
	void retrieveAll() {
		int contentId = createContentId();
		contentImageService.create(contentId, newImage());

		List<ContentImageVO> result = contentImageService.retrieveAll(contentId);

		assertEquals(1, result.size());
		assertTrue(result.get(0).getImageUrl().startsWith(THUMBNAIL_PREFIX));
		assertTrue(result.get(0).getZoomImageUrl().startsWith(FULL_SCREEN_PREFIX));
	}

	/** 검색 조건으로 걸러낸 전체 건수가 param에 실리는지 검증 */
	@Test
	@DisplayName("목록 조회 시 전체 건수가 param에 설정")
	void retrieveSetsTotalCnt() {
		int contentId = createContentId();
		contentImageService.create(contentId, newImage());
		contentImageService.create(contentId, newImage());

		DTO param = new DTO();
		contentImageService.retrieve(contentId, param);

		assertEquals(2, param.getTotalCnt());
	}

	/** 잘못된 콘텐츠 번호에 대한 입력값 검증 */
	@Test
	@DisplayName("콘텐츠 번호가 0 이하이면 예외 발생")
	void retrieveInvalidContentId() {
		assertThrows(
				IllegalArgumentException.class,
				() -> contentImageService.retrieve(INVALID_CONTENT_ID, new DTO()));

		assertThrows(
				IllegalArgumentException.class,
				() -> contentImageService.retrieve(NEGATIVE_CONTENT_ID, new DTO()));
	}

	/** null 조회 조건에 대한 입력값 검증 */
	@Test
	@DisplayName("조회 조건이 null이면 예외 발생")
	void retrieveNull() {
		assertThrows(
				IllegalArgumentException.class,
				() -> contentImageService.retrieve(1, null));
	}

	/** 포스터가 설정값 크기를 쓰는지 검증 (DB 접근 없음) */
	@Test
	@DisplayName("포스터는 설정값 크기 사용")
	void toDefaultSizeUrl() {
		assertEquals(THUMBNAIL_PREFIX + "sample-image.jpg",
				contentImageService.toPosterUrl(SAMPLE_PATH));
	}

	/** 화면을 가득 채우는 이미지가 원본 크기를 쓰는지 검증 (DB 접근 없음) */
	@Test
	@DisplayName("헤더 배경은 원본 크기 사용")
	void toFullScreenUrl() {
		assertEquals(FULL_SCREEN_PREFIX + "sample-image.jpg",
				contentImageService.toBackdropUrl(SAMPLE_PATH));
	}

	/** 크레딧 프로필이 축소 크기를 쓰는지 검증 (DB 접근 없음) */
	@Test
	@DisplayName("크레딧 프로필은 w185 사용")
	void toCreditProfileUrl() {
		assertEquals(CREDIT_PROFILE_PREFIX + "sample-image.jpg",
				contentImageService.toCreditProfileUrl(SAMPLE_PATH));
	}

	/** 인물 상세 프로필이 크레딧 아바타보다 큰 크기를 쓰는지 검증 (DB 접근 없음) */
	@Test
	@DisplayName("인물 상세 프로필은 w300 사용")
	void toPersonProfileUrl() {
		assertEquals(PERSON_PROFILE_PREFIX + "sample-image.jpg",
				contentImageService.toPersonProfileUrl(SAMPLE_PATH));
	}

	/** 경로가 비어 있을 때 변환 결과 검증 (DB 접근 없음) */
	@Test
	@DisplayName("경로가 비어 있으면 null 반환")
	void toUrlWithBlankPath() {
		assertNull(contentImageService.toPosterUrl(null));
		assertNull(contentImageService.toBackdropUrl(" "));
		assertNull(contentImageService.toCreditProfileUrl(""));
	}

	/** 이미지 등록 후 단건 조회 검증. 등록 반환값도 목록과 같은 풀 URL 계약을 따른다 */
	@Test
	@DisplayName("이미지 등록")
	void create() {
		int contentId = createContentId();

		ContentImageVO created = contentImageService.create(contentId, newImage());

		assertTrue(created.getImageId() > 0);
		assertEquals(contentId, created.getContentId());
		assertTrue(created.getImageUrl().startsWith(THUMBNAIL_PREFIX));
		assertEquals(created.getImageId(), contentImageService.get(created.getImageId()).getImageId());
	}

	/** 단건 조회 반환값도 풀 URL로 완성되는지 검증 */
	@Test
	@DisplayName("단건 조회의 이미지 경로가 풀 URL로 완성")
	void getCompletesImageUrl() {
		int contentId = createContentId();
		int imageId = contentImageService.create(contentId, newImage()).getImageId();

		ContentImageVO result = contentImageService.get(imageId);

		assertTrue(result.getImageUrl().startsWith(THUMBNAIL_PREFIX));
		assertTrue(result.getZoomImageUrl().startsWith(FULL_SCREEN_PREFIX));
	}

	/**
	 * 같은 트랜잭션에서 두 번 조회해도 접두사가 중복되지 않는지 검증.
	 * MyBatis 1차 캐시가 같은 VO 인스턴스를 돌려주므로 같은 객체가 두 번 변환될 수 있다
	 */
	@Test
	@DisplayName("같은 트랜잭션에서 두 번 조회해도 접두사가 중복되지 않음")
	void getTwiceKeepsSinglePrefix() {
		int contentId = createContentId();
		int imageId = contentImageService.create(contentId, newImage()).getImageId();

		contentImageService.get(imageId);
		ContentImageVO result = contentImageService.get(imageId);

		assertEquals(THUMBNAIL_PREFIX + "sample-image.jpg", result.getImageUrl());
		assertEquals(FULL_SCREEN_PREFIX + "sample-image.jpg", result.getZoomImageUrl());
	}

	/**
	 * 매퍼의 doUpdate가 content_id를 무조건 SET하므로,
	 * 호출부가 안 채운 소속이 0으로 덮이지 않고 유지되는지 검증
	 */
	@Test
	@DisplayName("수정 시 채우지 않은 콘텐츠 번호는 기존 값 유지")
	void updateKeepsContentId() {
		int contentId = createContentId();
		int imageId = contentImageService.create(contentId, newImage()).getImageId();

		ContentImageVO param = new ContentImageVO();
		param.setImageUrl("/changed-image.jpg");

		ContentImageVO result = contentImageService.update(imageId, param);

		assertEquals(contentId, result.getContentId());
	}

	/**
	 * 목록에서 받은 VO(완성 URL 상태)를 그대로 수정 저장해도
	 * DB엔 원본 경로만 남아 접두사가 중복되지 않는지 검증
	 */
	@Test
	@DisplayName("완성 URL로 수정해도 접두사가 중복되지 않음")
	void updateDoesNotStoreFullUrl() {
		int contentId = createContentId();
		int imageId = contentImageService.create(contentId, newImage()).getImageId();

		// retrieve가 imageUrl을 제자리에서 풀 URL로 바꾼 VO를 그대로 되돌려 준다
		ContentImageVO listed = contentImageService.retrieve(contentId, new DTO()).get(0);
		contentImageService.update(imageId, listed);

		ContentImageVO result = contentImageService.get(imageId);

		assertEquals(THUMBNAIL_PREFIX + "sample-image.jpg", result.getImageUrl());
		assertEquals(FULL_SCREEN_PREFIX + "sample-image.jpg", result.getZoomImageUrl());
	}

	/** 테스트용 콘텐츠를 등록하고 번호를 돌려준다 */
	private int createContentId() {
		ContentVO content = new ContentVO();
		content.setExternalId(UUID.randomUUID().toString().substring(0, 12));
		content.setTitleKo("이미지 테스트 영화");
		content.setTitleOrg("Image Test Movie");
		content.setOverview("통합 테스트용 줄거리");
		content.setReleaseYear("2026-01-01");
		content.setRuntimeMin(120);
		content.setCountry("US");
		content.setPosterUrl(SAMPLE_PATH);
		content.setBackdropUrl(SAMPLE_PATH);

		return contentService.create(content).getContentId();
	}

	/** 등록에 쓸 이미지 정보를 만든다. DB에는 TMDB 원본 경로만 저장된다 */
	private ContentImageVO newImage() {
		ContentImageVO param = new ContentImageVO();
		param.setImageUrl(SAMPLE_PATH);

		return param;
	}

}
