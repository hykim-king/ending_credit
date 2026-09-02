package com.endit.service;

import java.util.List;

import com.endit.domain.ContentGenreVO;

/** C-01 장르 태그·AD-03 장르 연결 서비스. 갱신할 컬럼이 없어 update가 없으므로 연결 변경은 delete 후 create로 한다 */
public interface ContentGenreService {

	/**
	 *
	 * <pre>
	 * Method Name : retrieveAll
	 * Description : 콘텐츠 하나에 연결된 장르 전체 목록
	 *               "전체"는 실제로 100건 상한이며 초과분은 잘린다(상한 도달 시 log.warn만 남는다).
	 *               한 영화의 장르가 100개를 넘을 일은 없어 사실상 전체다.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @return List<ContentGenreVO>
	 */
	List<ContentGenreVO> retrieveAll(int contentId);

	/**
	 *
	 * <pre>
	 * Method Name : get
	 * Description : 콘텐츠-장르 연결 단건 조회
	 *               존재 확인만 필요하면 has를 쓴다.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param genreId
	 * @return ContentGenreVO / 없으면 NoSuchElementException
	 */
	ContentGenreVO get(int contentId, int genreId);

	/**
	 *
	 * <pre>
	 * Method Name : has
	 * Description : 콘텐츠에 장르가 이미 연결돼 있는지 확인 - AD-03 중복 연결 방지
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param genreId
	 * @return boolean
	 */
	boolean has(int contentId, int genreId);

	/**
	 *
	 * <pre>
	 * Method Name : create
	 * Description : 콘텐츠에 장르 연결 등록
	 *               이미 연결돼 있으면 IllegalStateException.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param genreId
	 * @return ContentGenreVO (등록 후 재조회한 상태)
	 */
	ContentGenreVO create(int contentId, int genreId);

	/**
	 *
	 * <pre>
	 * Method Name : delete
	 * Description : 콘텐츠-장르 연결 삭제
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param genreId
	 * @return void / 없으면 NoSuchElementException
	 */
	void delete(int contentId, int genreId);

}
