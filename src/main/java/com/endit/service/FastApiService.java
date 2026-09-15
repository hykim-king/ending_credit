/**
 * 파이썬 AI 서버(FastAPI) 호출
 *
 * 서버가 죽어 있거나 ai.enabled=false 면 예외 대신 대체값을 돌려준다 -
 * AI 때문에 검색 자체가 멈추면 안 된다.
 */
package com.endit.service;

import com.endit.domain.EmbedRequestVO;
import com.endit.domain.EmbedResponseVO;
import com.endit.domain.SearchIntentRequestVO;
import com.endit.domain.SearchIntentResponseVO;

public interface FastApiService {

	/**
	 * 검색 문장에서 의도(조건)를 뽑는다.
	 *
	 * @param request 사용자 입력 문장
	 * @return 의도. AI 가 꺼져 있거나 실패하면 최신순 목록 의도
	 */
	SearchIntentResponseVO searchIntent(SearchIntentRequestVO request);

	/**
	 * 글 묶음을 좌표(임베딩) 묶음으로 바꾼다.
	 *
	 * @param request 글 목록
	 * @return 좌표 묶음. 꺼져 있거나 실패하면 null (호출한 쪽이 건너뛴다)
	 */
	EmbedResponseVO embed(EmbedRequestVO request);
}
