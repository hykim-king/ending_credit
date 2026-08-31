package com.endit.service.impl;

import java.util.List;
import java.util.NoSuchElementException;

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
	private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";

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
			image.setImageUrl(toFullImageUrl(image.getImageUrl()));
		}

		return images;
	}

	// 콘텐츠 하나의 이미지 전체 목록 조회
	@Override
	public List<ContentImageVO> retrieveAll(int contentId) {
		DTO param = new DTO();
		param.setPageNo(1);
		param.setPageSize(RETRIEVE_ALL_PAGE_SIZE);
		return retrieve(contentId, param);
	}

	// 이미지 단건 조회
	@Override
	public ContentImageVO get(int imageId) {
		ContentImageVO item = contentImageMapper.doSelectOne(createKey(imageId));

		if (item == null) {
			throw new NoSuchElementException("존재하지 않는 이미지입니다. imageId=" + imageId);
		}

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
		get(imageId);

		if (param == null) {
			throw new IllegalArgumentException("수정할 이미지 정보가 필요합니다.");
		}

		param.setImageId(imageId);

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
		ContentImageVO existing = get(imageId);
		int result = contentImageMapper.doDelete(existing);

		if (result != 1) {
			throw new IllegalStateException("이미지 삭제에 실패했습니다.");
		}
	}

	// TMDB 이미지 경로를 기본 크기(tmdb.image-size)의 완성 URL로 변환
	@Override
	public String toFullImageUrl(String path) {
		return toFullImageUrl(path, tmdbProperties.getImageSize());
	}

	// TMDB 이미지 경로를 지정한 크기의 완성 URL로 변환
	@Override
	public String toFullImageUrl(String path, String size) {
		if (!StringUtils.hasText(path)) {
			return null;
		}

		return TMDB_IMAGE_BASE_URL + size + path;
	}

	// 페이지 번호와 페이지 크기를 허용 범위의 기본값으로 보정
	private void normalizePaging(DTO param) {
		if (param.getPageNo() <= 0) {
			param.setPageNo(1);
		}

		if (param.getPageSize() <= 0) {
			param.setPageSize(12);
		} else if (param.getPageSize() > 100) {
			param.setPageSize(100);
		}
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

	/** 이미지 번호가 유효한 양수인지 검증 */
	private void validateImageId(int imageId) {
		if (imageId <= 0) {
			throw new IllegalArgumentException("올바른 이미지 번호가 필요합니다.");
		}
	}

}
