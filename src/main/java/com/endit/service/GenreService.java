package com.endit.service;

import java.util.List;

import com.endit.domain.GenreVO;

/** 장르 마스터(GENRE) 조회 서비스. 콘텐츠와의 연결은 ContentGenreService 담당 */
public interface GenreService {

	/**
	 *
	 * <pre>
	 * Method Name : retrieveAll
	 * Description : 장르 마스터 전체 목록
	 *               genre_id 오름차순. "전체"는 실제로 100건 상한이며 초과분은 잘린다
	 *               (상한 도달 시 log.warn만 남는다). TMDB 영화 장르가 약 19건이라 사실상 전체다.
	 *
	 * </pre>
	 *
	 * @return List<GenreVO> (없으면 빈 목록)
	 */
	List<GenreVO> retrieveAll();

	/**
	 *
	 * <pre>
	 * Method Name : get
	 * Description : 장르 단건 조회
	 *
	 * </pre>
	 *
	 * @param genreId
	 * @return GenreVO / 없으면 NoSuchElementException
	 */
	GenreVO get(int genreId);

}
