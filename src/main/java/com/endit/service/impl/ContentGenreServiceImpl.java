package com.endit.service.impl;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.ContentGenreVO;
import com.endit.mapper.ContentGenreMapper;
import com.endit.service.ContentGenreService;

@Service
@Transactional(readOnly = true)
public class ContentGenreServiceImpl implements ContentGenreService {

	private static final String SEARCH_BY_CONTENT = "10";
	// 페이징 없이 "전체 조회"를 흉내낼 때 쓰는 페이지 크기 - 콘텐츠 하나가 가질 수 있는 장르 수보다 넉넉하게 잡음
	private static final int RETRIEVE_ALL_PAGE_SIZE = 100;

	private final ContentGenreMapper contentGenreMapper;

	public ContentGenreServiceImpl(ContentGenreMapper contentGenreMapper) {
		this.contentGenreMapper = contentGenreMapper;
	}

	// 콘텐츠 하나에 연결된 장르 목록 조회 - CONTENT_GENRE + GENRE 조인, genre_id/genre_name 반환
	@Override
	public List<ContentGenreVO> retrieve(int contentId, DTO param) {
		validateContentId(contentId);

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		normalizePaging(param);
		param.setSearchDiv(SEARCH_BY_CONTENT);
		param.setSearchWord(String.valueOf(contentId));

		List<ContentGenreVO> genres = contentGenreMapper.doRetrieve(param);

		// doRetrieve가 CROSS JOIN으로 content_id까지 걸러낸 총건수를 각 행에 실어 준다.
		// 매퍼의 totalCnt()는 필터 없는 전체 테이블 건수라 여기에 쓰면 안 된다
		param.setTotalCnt(genres.isEmpty() ? 0 : genres.get(0).getTotalCnt());

		return genres;
	}

	// 콘텐츠 하나에 연결된 장르 전체 목록 조회 - 페이징 없이 한 번에 다 가져온다
	@Override
	public List<ContentGenreVO> retrieveAll(int contentId) {
		DTO param = new DTO();
		param.setPageNo(1);
		param.setPageSize(RETRIEVE_ALL_PAGE_SIZE);
		return retrieve(contentId, param);
	}

	// 콘텐츠-장르 연결 단건 조회
	@Override
	public ContentGenreVO get(int contentId, int genreId) {
		ContentGenreVO key = createKey(contentId, genreId);
		ContentGenreVO item = contentGenreMapper.doSelectOne(key);

		if (item == null) {
			throw new NoSuchElementException(
					"연결되지 않은 장르입니다. contentId=" + contentId + ", genreId=" + genreId);
		}

		return item;
	}

	// 콘텐츠에 장르 연결 등록 - CONTENT_GENRE insert
	@Override
	@Transactional
	public ContentGenreVO create(int contentId, int genreId) {
		ContentGenreVO key = createKey(contentId, genreId);

		if (contentGenreMapper.doSelectOne(key) != null) {
			throw new IllegalStateException("이미 연결된 장르입니다.");
		}

		int result = contentGenreMapper.doSave(key);

		if (result != 1) {
			throw new IllegalStateException("장르 연결 등록에 실패했습니다.");
		}

		return contentGenreMapper.doSelectOne(key);
	}

	// 콘텐츠-장르 연결 삭제
	@Override
	@Transactional
	public void delete(int contentId, int genreId) {
		ContentGenreVO existing = get(contentId, genreId);
		int result = contentGenreMapper.doDelete(existing);

		if (result != 1) {
			throw new IllegalStateException("장르 연결 삭제에 실패했습니다.");
		}
	}

	/** 페이지 번호와 페이지 크기를 허용 범위의 기본값으로 보정 */
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

	/** 콘텐츠 번호와 장르 번호를 담은 조회 키 생성 */
	private ContentGenreVO createKey(int contentId, int genreId) {
		validateContentId(contentId);
		validateGenreId(genreId);

		ContentGenreVO key = new ContentGenreVO();
		key.setContentId(contentId);
		key.setGenreId(genreId);

		return key;
	}

	/** 콘텐츠 번호가 유효한 양수인지 검증 */
	private void validateContentId(int contentId) {
		if (contentId <= 0) {
			throw new IllegalArgumentException("올바른 콘텐츠 번호가 필요합니다.");
		}
	}

	/** 장르 번호가 유효한 양수인지 검증 */
	private void validateGenreId(int genreId) {
		if (genreId <= 0) {
			throw new IllegalArgumentException("올바른 장르 번호가 필요합니다.");
		}
	}

}
