package com.endit.service.impl;

import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final int FIRST_PAGE_NO = 1;
	private static final int DEFAULT_PAGE_SIZE = 12;
	private static final int MAX_PAGE_SIZE = 100;

	private static final Logger log = LoggerFactory.getLogger(ContentGenreServiceImpl.class);

	private final ContentGenreMapper contentGenreMapper;

	public ContentGenreServiceImpl(ContentGenreMapper contentGenreMapper) {
		this.contentGenreMapper = contentGenreMapper;
	}

	// 콘텐츠 하나에 연결된 장르 목록 조회 - CONTENT_GENRE + GENRE 조인, genre_id/genre_name 반환.
	// 한 영화의 장르에는 페이징 화면이 없어 인터페이스에 열지 않고 retrieveAll의 내부 구현으로만 쓴다
	private List<ContentGenreVO> retrieve(int contentId, DTO param) {
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
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(RETRIEVE_ALL_PAGE_SIZE);

		List<ContentGenreVO> genres = retrieve(contentId, param);

		// "전체"는 실제로 한 페이지 상한이다. 초과분이 말없이 잘리지 않도록 남겨 둔다
		if (genres.size() == RETRIEVE_ALL_PAGE_SIZE) {
			log.warn("장르 연결 전체 조회가 페이지 상한에 도달했습니다. 초과분이 잘렸을 수 있습니다. contentId={}, size={}",
					contentId, genres.size());
		}

		return genres;
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

	// 콘텐츠-장르 연결 존재 확인 - 없으면 예외 대신 false를 돌려주므로 등록 전 검사에 쓴다
	@Override
	public boolean has(int contentId, int genreId) {
		return contentGenreMapper.doSelectOne(createKey(contentId, genreId)) != null;
	}

	// 콘텐츠에 장르 연결 등록 - CONTENT_GENRE insert
	@Override
	@Transactional
	public ContentGenreVO create(int contentId, int genreId) {
		if (has(contentId, genreId)) {
			throw new IllegalStateException("이미 연결된 장르입니다.");
		}

		ContentGenreVO key = createKey(contentId, genreId);

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

	// 콘텐츠 번호와 장르 번호를 담은 조회 키 생성
	private ContentGenreVO createKey(int contentId, int genreId) {
		validateContentId(contentId);
		validateGenreId(genreId);

		ContentGenreVO key = new ContentGenreVO();
		key.setContentId(contentId);
		key.setGenreId(genreId);

		return key;
	}

	// 콘텐츠 번호가 유효한 양수인지 검증
	private void validateContentId(int contentId) {
		if (contentId <= 0) {
			throw new IllegalArgumentException("올바른 콘텐츠 번호가 필요합니다.");
		}
	}

	// 장르 번호가 유효한 양수인지 검증
	private void validateGenreId(int genreId) {
		if (genreId <= 0) {
			throw new IllegalArgumentException("올바른 장르 번호가 필요합니다.");
		}
	}

}
