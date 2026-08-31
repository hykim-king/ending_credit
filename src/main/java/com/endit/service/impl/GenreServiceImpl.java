package com.endit.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.GenreVO;
import com.endit.mapper.GenreMapper;
import com.endit.service.GenreService;

@Service
@Transactional(readOnly = true)
public class GenreServiceImpl implements GenreService {

	private static final Logger log = LoggerFactory.getLogger(GenreServiceImpl.class);

	private static final int FIRST_PAGE_NO = 1;
	// 페이징 없이 "전체 조회"를 흉내낼 때 쓰는 페이지 크기.
	// WorkDiv에 페이징 없는 목록 조회가 없어서 큰 페이지 하나로 위장한다.
	// 100은 도메인 근거가 아니라 형제 서비스와 맞춘 값이다(TMDB 영화 장르는 약 19건).
	private static final int RETRIEVE_ALL_PAGE_SIZE = 100;

	private final GenreMapper genreMapper;

	public GenreServiceImpl(GenreMapper genreMapper) {
		this.genreMapper = genreMapper;
	}

	// 장르 마스터 전체 목록 조회
	@Override
	public List<GenreVO> retrieveAll() {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(RETRIEVE_ALL_PAGE_SIZE);

		// searchWord를 비워 두면 genreWhere의 <if>가 걸리지 않아 WHERE 절 없이 전체가 조회된다.
		// 정렬은 매퍼가 ORDER BY genre_id ASC로 고정한다.
		List<GenreVO> genres = genreMapper.doRetrieve(param);

		if (genres == null) {
			return Collections.emptyList();
		}

		// 페이지를 꽉 채웠다면 상한에 잘렸을 수 있다. 조용히 누락되지 않도록 남겨 둔다
		if (genres.size() == RETRIEVE_ALL_PAGE_SIZE) {
			log.warn("장르 전체 조회가 페이지 상한에 도달했습니다. 초과분이 잘렸을 수 있습니다. size={}", genres.size());
		}

		return genres;
	}

	// 장르 단건 조회
	@Override
	public GenreVO get(int genreId) {
		GenreVO item = genreMapper.doSelectOne(createKey(genreId));

		if (item == null) {
			throw new NoSuchElementException("존재하지 않는 장르입니다. genreId=" + genreId);
		}

		return item;
	}

	// 장르 번호를 담은 조회 키 생성
	private GenreVO createKey(int genreId) {
		validateGenreId(genreId);

		GenreVO key = new GenreVO();
		key.setGenreId(genreId);

		return key;
	}

	// 장르 번호가 유효한 양수인지 검증
	private void validateGenreId(int genreId) {
		if (genreId <= 0) {
			throw new IllegalArgumentException("올바른 장르 번호가 필요합니다.");
		}
	}

}
