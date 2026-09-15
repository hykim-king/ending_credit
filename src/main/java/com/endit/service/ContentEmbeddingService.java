/**
 * 임베딩 적재와 뜻 검색
 */
package com.endit.service;

import java.util.List;

import com.endit.domain.AiSearchItemVO;

public interface ContentEmbeddingService {

	/**
	 * 공책(CONTENT_EMBEDDING)에 없는 영화들을 재서 채운다.
	 *
	 * @return 새로 잰 건수. 저울이 꺼져 있으면 -1
	 */
	int embedNewContents();

	/**
	 * 뜻 검색 - 문장과 뜻이 가까운 영화를 가까운 순으로.
	 *
	 * @param query 느낌·내용 문장
	 * @param limit 최대 건수
	 * @return 가까운 순 영화 카드. 좌표가 없으면 빈 목록
	 */
	List<AiSearchItemVO> searchByMeaning(String query, int limit);

	/**
	 * 비슷한 영화 - 기준 영화의 좌표와 가까운 순으로(자기 자신 제외).
	 *
	 * @param title 기준 영화 제목(부분 일치)
	 * @param limit 최대 건수
	 * @return 가까운 순 영화 카드. 기준 영화가 없으면 null, 좌표가 없으면 빈 목록
	 */
	List<AiSearchItemVO> findSimilar(String title, int limit);
}
