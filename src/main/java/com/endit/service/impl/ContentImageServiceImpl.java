package com.endit.service.impl;

import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.endit.cmn.DTO;
import com.endit.config.TmdbProperties;
import com.endit.domain.ContentImageVO;
import com.endit.mapper.ContentImageMapper;
import com.endit.service.ContentImageService;

@Service
@Transactional(readOnly = true)
public class ContentImageServiceImpl implements ContentImageService {

	private static final String SEARCH_BY_CONTENT = "10";
	// "전체 조회"페이지 크기
	private static final int RETRIEVE_ALL_PAGE_SIZE = 100;
	private static final int FIRST_PAGE_NO = 1;
	private static final int DEFAULT_PAGE_SIZE = 12;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";

	// ── 이미지 크기는 전부 여기에 모은다 ──
	// 다른 ServiceImpl은 크기를 알지 못하고 용도별 to*Url() 메서드만 호출한다.
	// 기본 크기(tmdb.image-size, 현재 w500)를 쓰는 용도는 상수를 따로 두지 않는다.

	// 화면을 가득 채워 띄우는 이미지 - C-01 헤더 배경(뷰포트 전체 너비)과 C-05 확대 모달(전체화면)이 공유한다
	private static final String FULL_SCREEN_IMAGE_SIZE = "original";
	// 크레딧 프로필 - 48px 아바타로 축소 표시
	private static final String CREDIT_PROFILE_IMAGE_SIZE = "w185";
	// 인물 상세 프로필 - 크레딧 아바타보다 크게 표시한다
	private static final String PERSON_PROFILE_IMAGE_SIZE = "w300";

	private static final Logger log = LoggerFactory.getLogger(ContentImageServiceImpl.class);

	private final ContentImageMapper contentImageMapper;
	private final TmdbProperties tmdbProperties;

	public ContentImageServiceImpl(ContentImageMapper contentImageMapper, TmdbProperties tmdbProperties) {
		this.contentImageMapper = contentImageMapper;
		this.tmdbProperties = tmdbProperties;
	}

	// 콘텐츠 하나의 이미지목록 조회
	@Override
	public List<ContentImageVO> retrieve(int contentId, DTO param) {
		validateContentId(contentId);

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		normalizePaging(param);
		param.setSearchDiv(SEARCH_BY_CONTENT);
		param.setSearchWord(String.valueOf(contentId));

		List<ContentImageVO> images = contentImageMapper.doRetrieve(param);

		// doRetrieve가 CROSS JOIN으로 content_id를 걸러낸 총건수를 각 행에 실어 준다.
		param.setTotalCnt(images.isEmpty() ? 0 : images.get(0).getTotalCnt());

		for (ContentImageVO image : images) {
			applyFullImageUrl(image);
		}

		return images;
	}

	// 콘텐츠 하나의 이미지 전체 목록 조회
	@Override
	public List<ContentImageVO> retrieveAll(int contentId) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(RETRIEVE_ALL_PAGE_SIZE);

		List<ContentImageVO> images = retrieve(contentId, param);

		// "전체"는 실제로 한 페이지 상한이다. 초과분이 말없이 잘리지 않도록 남겨 둔다
		if (images.size() == RETRIEVE_ALL_PAGE_SIZE) {
			log.warn("이미지 전체 조회가 페이지 상한에 도달했습니다. 초과분이 잘렸을 수 있습니다. contentId={}, size={}",
					contentId, images.size());
		}

		return images;
	}

	// 이미지 단건 조회
	@Override
	public ContentImageVO get(int imageId) {
		ContentImageVO item = getStored(imageId);
		applyFullImageUrl(item);

		return item;
	}

	// 콘텐츠에 이미지 등록
	@Override
	@Transactional
	public ContentImageVO create(int contentId, ContentImageVO param) {
		validateContentId(contentId);

		if (param == null) {
			throw new IllegalArgumentException("등록할 이미지 정보가 필요합니다.");
		}

		param.setContentId(contentId);

		int result = contentImageMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("이미지 등록에 실패했습니다.");
		}

		// doSave가 시퀀스로 채번한 imageId를 param이 그대로 들고 있으므로 재조회에 사용
		return get(param.getImageId());
	}

	// 이미지 URL 수정
	@Override
	@Transactional
	public ContentImageVO update(int imageId, ContentImageVO param) {
		ContentImageVO stored = getStored(imageId);

		if (param == null) {
			throw new IllegalArgumentException("수정할 이미지 정보가 필요합니다.");
		}

		param.setImageId(imageId);

		// doUpdate가 content_id를 무조건 SET하므로, 호출부가 안 채웠으면 기존 소속을 유지한다
		if (param.getContentId() <= 0) {
			param.setContentId(stored.getContentId());
		}

		// doUpdate가 image_url도 무조건 SET하므로, 호출부가 안 채웠으면 기존 경로를 유지한다.
		// 목록 조회로 받은 VO가 그대로 되돌아오면 완성 URL이 저장되므로 원본 경로로 되돌린다
		String imageUrl = toStoredPath(param.getImageUrl());
		param.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl : stored.getImageUrl());

		int result = contentImageMapper.doUpdate(param);

		if (result != 1) {
			throw new IllegalStateException("이미지 수정에 실패했습니다.");
		}

		return get(imageId);
	}

	// 이미지 삭제
	@Override
	@Transactional
	public void delete(int imageId) {
		ContentImageVO existing = getStored(imageId);
		int result = contentImageMapper.doDelete(existing);

		if (result != 1) {
			throw new IllegalStateException("이미지 삭제에 실패했습니다.");
		}
	}

	// 영화 포스터 URL
	@Override
	public String toPosterUrl(String path) {
		return toFullImageUrl(path, tmdbProperties.getImageSize());
	}

	// 영화 상세 헤더 배경 URL
	@Override
	public String toBackdropUrl(String path) {
		return toFullImageUrl(path, FULL_SCREEN_IMAGE_SIZE);
	}

	// 크레딧 프로필 URL
	@Override
	public String toCreditProfileUrl(String path) {
		return toFullImageUrl(path, CREDIT_PROFILE_IMAGE_SIZE);
	}

	// 인물 상세 프로필 URL
	@Override
	public String toPersonProfileUrl(String path) {
		return toFullImageUrl(path, PERSON_PROFILE_IMAGE_SIZE);
	}

	// 갤러리 썸네일 URL - 이 서비스의 목록 조회에서만 쓰므로 인터페이스에 열지 않는다
	private String toGalleryUrl(String path) {
		return toFullImageUrl(path, tmdbProperties.getImageSize());
	}

	// 갤러리 확대 URL - 이 서비스의 목록 조회에서만 쓰므로 인터페이스에 열지 않는다
	private String toGalleryZoomUrl(String path) {
		return toFullImageUrl(path, FULL_SCREEN_IMAGE_SIZE);
	}

	// DB엔 TMDB 원본 경로만 있으므로 썸네일용과 확대용 URL을 여기서 둘 다 완성한다
	private void applyFullImageUrl(ContentImageVO image) {
		String path = image.getImageUrl();
		image.setImageUrl(toGalleryUrl(path));
		image.setZoomImageUrl(toGalleryZoomUrl(path));
	}

	// TMDB 경로를 완성 URL로 조립하는 유일한 지점. 크기 지식이 새지 않도록 private으로 닫아 둔다.
	// 이미 완성된 URL이 들어와도 크기 구간을 걷어내고 다시 붙이므로 두 번 변환해도 결과가 같다 -
	// 서비스들이 VO의 URL 필드를 제자리에서 바꾸는데, MyBatis 1차 캐시는 같은 트랜잭션에서
	// 같은 인스턴스를 돌려주므로 같은 VO가 두 번 변환될 수 있다
	private String toFullImageUrl(String path, String size) {
		String storedPath = toStoredPath(path);

		if (!StringUtils.hasText(storedPath)) {
			return null;
		}

		return TMDB_IMAGE_BASE_URL + size + storedPath;
	}

	// 완성 URL에서 크기 구간을 걷어내 DB에 저장할 원본 경로만 남긴다. toFullImageUrl의 역변환.
	// PersonServiceImpl의 쓰기 경로도 같은 역변환이 필요해 인터페이스로 열어 뒀다
	@Override
	public String toStoredPath(String url) {
		if (!StringUtils.hasText(url) || !url.startsWith(TMDB_IMAGE_BASE_URL)) {
			return url;
		}

		String sizeAndPath = url.substring(TMDB_IMAGE_BASE_URL.length());
		int pathIndex = sizeAndPath.indexOf('/');

		if (pathIndex < 0) {
			return url;
		}

		return sizeAndPath.substring(pathIndex);
	}

	// 페이지 번호와 페이지 크기를 허용 범위의 기본값으로 보정
	private void normalizePaging(DTO param) {
		if (param.getPageNo() <= 0) {
			param.setPageNo(FIRST_PAGE_NO);
		}

		if (param.getPageSize() <= 0) {
			param.setPageSize(DEFAULT_PAGE_SIZE);
		} else if (param.getPageSize() > MAX_PAGE_SIZE) {
			param.setPageSize(MAX_PAGE_SIZE);
		}
	}

	// DB에 저장된 그대로(TMDB 원본 경로) 단건 조회. 저장 경로가 완성 URL을 다시 쓰지 않도록 읽기용 get과 나눠 둔다
	private ContentImageVO getStored(int imageId) {
		ContentImageVO item = contentImageMapper.doSelectOne(createKey(imageId));

		if (item == null) {
			throw new NoSuchElementException("존재하지 않는 이미지입니다. imageId=" + imageId);
		}

		return item;
	}

	// 이미지 번호를 담은 조회 키 생성
	private ContentImageVO createKey(int imageId) {
		validateImageId(imageId);

		ContentImageVO key = new ContentImageVO();
		key.setImageId(imageId);

		return key;
	}

	// 콘텐츠 번호가 유효한 양수인지 검증
	private void validateContentId(int contentId) {
		if (contentId <= 0) {
			throw new IllegalArgumentException("올바른 콘텐츠 번호가 필요합니다.");
		}
	}

	// 이미지 번호가 유효한 양수인지 검증
	private void validateImageId(int imageId) {
		if (imageId <= 0) {
			throw new IllegalArgumentException("올바른 이미지 번호가 필요합니다.");
		}
	}

}
