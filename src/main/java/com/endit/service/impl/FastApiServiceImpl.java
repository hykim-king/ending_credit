/**
 * 파이썬 AI 서버(FastAPI) 호출 - WebClient
 *
 * 서버가 죽어 있으면 예외 대신 대체값을 돌려준다.
 */
package com.endit.service.impl;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.endit.domain.EmbedRequestVO;
import com.endit.domain.EmbedResponseVO;
import com.endit.domain.SearchIntentRequestVO;
import com.endit.domain.SearchIntentResponseVO;
import com.endit.service.FastApiService;

@Service
public class FastApiServiceImpl implements FastApiService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** 검색어 상한. 이보다 길면 앞부분만 보낸다 */
	private static final int MAX_QUERY_LENGTH = 200;

	/** 생존 확인은 길게 기다리지 않는다 */
	private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(3);

	private final WebClient webClient;

	public FastApiServiceImpl(WebClient webClient) {
		super();
		this.webClient = webClient;
	}

	@Override
	public boolean isAlive() {
		try {
			webClient
					.get()
					.uri("/health")
					.retrieve()
					.toBodilessEntity()
					.timeout(HEALTH_TIMEOUT)
					.block();
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	public SearchIntentResponseVO searchIntent(SearchIntentRequestVO request) {
		log.debug("=============================");
		log.debug("searchIntent()");
		log.debug("request: {}", request);
		log.debug("=============================");

		String query = null == request ? null : request.getQuery();
		if (null == query || query.isBlank()) {
			return SearchIntentResponseVO.fallbackRanking();
		}

		if (query.length() > MAX_QUERY_LENGTH) {
			request.setQuery(query.substring(0, MAX_QUERY_LENGTH));
		}

		try {
			// POST 방식 호출 - Java 객체를 JSON 으로 보내고, JSON 응답을 Java 객체로 받는다
			SearchIntentResponseVO outVO = webClient
					.post()
					.uri("/search-intent")
					.bodyValue(request)
					.retrieve()
					.bodyToMono(SearchIntentResponseVO.class)
					.block();

			if (null == outVO || null == outVO.getIntent()) {
				return SearchIntentResponseVO.fallbackRanking();
			}

			log.debug("검색 의도: {}", outVO);

			return outVO;

		} catch (RuntimeException e) {
			// 연결 실패·타임아웃·응답 오류 - AI 가 죽어도 검색은 살아야 한다
			log.warn("AI 의도 분석에 실패했습니다. 최신순으로 대체합니다.", e);
			return SearchIntentResponseVO.fallbackRanking();
		}
	}

	@Override
	public EmbedResponseVO embed(EmbedRequestVO request) {
		if (null == request || null == request.getTexts() || request.getTexts().isEmpty()) {
			return null;
		}

		try {
			return webClient
					.post()
					.uri("/embed")
					.bodyValue(request)
					.retrieve()
					.bodyToMono(EmbedResponseVO.class)
					.block();
		} catch (RuntimeException e) {
			log.warn("임베딩 호출에 실패했습니다. 건너뜁니다.", e);
			return null;
		}
	}
}
