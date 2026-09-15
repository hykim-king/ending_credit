/**
 * 파이썬 AI 서버를 호출하는 검색 의도 분석 (WebClient - 수업 04·05 방식)
 *
 * 서버가 죽어 있거나 AI 를 껐으면 최신순 목록으로 떨어뜨린다.
 * AI 때문에 검색 자체가 멈추면 안 된다.
 */
package com.endit.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.endit.ai.dto.SearchIntentRequestVO;
import com.endit.ai.dto.SearchIntentResponseVO;

@Component
public class AiServerIntentResolver implements SearchIntentResolver {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String PATH_SEARCH_INTENT = "/search-intent";

	/** 검색어 상한. 이보다 길면 앞부분만 보낸다 */
	private static final int MAX_QUERY_LENGTH = 200;

	private final WebClient webClient;
	private final boolean enabled;

	public AiServerIntentResolver(
			@Qualifier("aiWebClient") WebClient webClient,
			@Value("${ai.enabled:false}") boolean enabled) {
		this.webClient = webClient;
		this.enabled = enabled;

		log.debug("=============================");
		log.debug("AiServerIntentResolver enabled={}", enabled);
		log.debug("=============================");
	}

	@Override
	public SearchIntentResponseVO resolve(String query) {
		if (false == enabled || null == query || query.isBlank()) {
			return SearchIntentResponseVO.fallbackRanking();
		}

		String target = query.length() > MAX_QUERY_LENGTH
				? query.substring(0, MAX_QUERY_LENGTH)
				: query;

		try {
			// POST 방식 호출 - Java 객체를 JSON 으로 보내고, JSON 응답을 Java 객체로 받는다
			SearchIntentResponseVO intent = webClient
					.post()
					.uri(PATH_SEARCH_INTENT)
					.bodyValue(new SearchIntentRequestVO(target))
					.retrieve()
					.bodyToMono(SearchIntentResponseVO.class)
					.block();

			if (null == intent || null == intent.getIntent()) {
				return SearchIntentResponseVO.fallbackRanking();
			}

			log.debug("검색 의도: {}", intent);

			return intent;

		} catch (RuntimeException e) {
			// 연결 실패·타임아웃·응답 오류 - AI 가 죽어도 검색은 살아야 한다
			log.warn("AI 의도 분석에 실패했습니다. 최신순으로 대체합니다.", e);
			return SearchIntentResponseVO.fallbackRanking();
		}
	}
}
