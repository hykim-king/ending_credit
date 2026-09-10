/**
 * 파이썬 AI 서버를 호출하는 검색 의도 분석
 *
 * 서버가 죽어 있거나 AI 를 껐으면 최신순 목록으로 떨어뜨린다.
 * AI 때문에 검색 자체가 멈추면 안 된다.
 */
package com.endit.ai;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.endit.ai.dto.SearchIntent;

@Component
public class AiServerIntentResolver implements SearchIntentResolver {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String PATH_SEARCH_INTENT = "/search-intent";

	/** 검색어 상한. 이보다 길면 앞부분만 보낸다 */
	private static final int MAX_QUERY_LENGTH = 200;

	private final RestTemplate restTemplate;
	private final String baseUrl;
	private final boolean enabled;

	public AiServerIntentResolver(
			@Qualifier("aiRestTemplate") RestTemplate restTemplate,
			@Value("${ai.base-url:http://localhost:5000}") String baseUrl,
			@Value("${ai.enabled:false}") boolean enabled) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
		this.enabled = enabled;

		log.debug("=============================");
		log.debug("AiServerIntentResolver enabled={} baseUrl={}", enabled, baseUrl);
		log.debug("=============================");
	}

	@Override
	public SearchIntent resolve(String query) {
		if (false == enabled || null == query || query.isBlank()) {
			return SearchIntent.fallbackRanking();
		}

		String target = query.length() > MAX_QUERY_LENGTH
				? query.substring(0, MAX_QUERY_LENGTH)
				: query;

		try {
			SearchIntent intent = restTemplate.postForObject(
					baseUrl + PATH_SEARCH_INTENT, Map.of("query", target), SearchIntent.class);

			if (null == intent || null == intent.getIntent()) {
				return SearchIntent.fallbackRanking();
			}

			log.debug("검색 의도: {}", intent);

			return intent;

		} catch (RestClientException e) {
			// AI 가 죽어도 검색은 살아야 한다
			log.warn("AI 의도 분석에 실패했습니다. 최신순으로 대체합니다.", e);
			return SearchIntent.fallbackRanking();
		}
	}
}
