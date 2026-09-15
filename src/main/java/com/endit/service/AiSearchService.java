/**
 * AI 검색 흐름 지휘
 *
 * ① AI 가 문장에서 조건을 뽑는다   (영화는 안 만짐)
 * ② 우리 SQL 이 그 조건으로 찾는다  (결과는 전부 DB 산)
 */
package com.endit.service;

import com.endit.domain.AiSearchResponseVO;

public interface AiSearchService {

	/**
	 * 자연어 문장으로 영화를 찾는다.
	 *
	 * @param query 사용자 입력 문장
	 * @return 의도 + 영화 목록
	 */
	AiSearchResponseVO search(String query);
}
