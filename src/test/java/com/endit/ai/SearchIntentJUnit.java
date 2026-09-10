/**
 * AI 검색 의도 분석 단위 테스트
 *
 * 판정 품질이 아니라 "AI 가 없어도 검색이 살아있는가"를 본다.
 */
package com.endit.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.endit.ai.dto.SearchIntent;

@DisplayName("AI 검색 의도 분석")
public class SearchIntentJUnit {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** 아무도 듣고 있지 않은 포트 */
	private static final String DEAD_SERVER = "http://localhost:5999";

	private RestTemplate shortTimeoutTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofMillis(1000));
		factory.setReadTimeout(Duration.ofMillis(2000));
		return new RestTemplate(factory);
	}

	@Test
	@DisplayName("AI를 껐으면 최신순 목록으로 떨어뜨린다")
	public void disabledFallsBackToRanking() {
		log.debug("---------------------------");
		log.debug("*disabledFallsBackToRanking()*");
		log.debug("---------------------------");

		SearchIntentResolver resolver = new AiServerIntentResolver(
				shortTimeoutTemplate(), DEAD_SERVER, false);

		SearchIntent intent = resolver.resolve("우주 배경 영화");

		assertNotNull(intent);
		assertEquals(SearchIntent.INTENT_RANKING, intent.getIntent());
		assertEquals("latest", intent.getSort());
		assertEquals("none", intent.getProvider());
		assertFalse(intent.isOutOfScope());
	}

	@Test
	@DisplayName("AI 서버가 죽어 있어도 검색은 살아있다")
	public void serverDownStillSearches() {
		log.debug("---------------------------");
		log.debug("*serverDownStillSearches()*");
		log.debug("---------------------------");
		// 빈 화면 대신 최신순이라도 보여줘야 한다

		SearchIntentResolver resolver = new AiServerIntentResolver(
				shortTimeoutTemplate(), DEAD_SERVER, true);

		SearchIntent intent = resolver.resolve("우주 배경 영화");

		assertEquals(SearchIntent.INTENT_RANKING, intent.getIntent());
		assertTrue(intent.getLimit() > 0);
	}

	@Test
	@DisplayName("빈 검색어는 서버를 부르지 않는다")
	public void blankQueryFallsBack() {
		log.debug("---------------------------");
		log.debug("*blankQueryFallsBack()*");
		log.debug("---------------------------");

		SearchIntentResolver resolver = new AiServerIntentResolver(
				shortTimeoutTemplate(), DEAD_SERVER, true);

		assertEquals("none", resolver.resolve(null).getProvider());
		assertEquals("none", resolver.resolve("   ").getProvider());
	}

	@Test
	@DisplayName("영화와 무관한 의도는 DB를 뒤지지 않도록 표시된다")
	public void outOfScopeIsFlagged() {
		log.debug("---------------------------");
		log.debug("*outOfScopeIsFlagged()*");
		log.debug("---------------------------");

		SearchIntent vo = new SearchIntent();
		vo.setIntent(SearchIntent.INTENT_OUT_OF_SCOPE);

		assertTrue(vo.isOutOfScope());
		assertFalse(SearchIntent.fallbackRanking().isOutOfScope());
	}
}
