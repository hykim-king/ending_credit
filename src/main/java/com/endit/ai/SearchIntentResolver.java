/**
 * 검색 의도 분석 계약
 *
 * 사람 말을 검색 조건으로 바꾼다. 영화를 고르지는 않는다.
 */
package com.endit.ai;

import com.endit.ai.dto.SearchIntent;

public interface SearchIntentResolver {

	/**
	 * 검색 문장에서 조건을 뽑는다.
	 * 실패해도 예외를 던지지 않고 SearchIntent.fallbackRanking()을 돌려준다.
	 *
	 * @param query 사용자가 입력한 문장
	 * @return 검색 의도
	 */
	SearchIntent resolve(String query);
}
