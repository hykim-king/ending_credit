/**
 * AI 서버(FastAPI) 호출용 WebClient 설정 (수업 04 방식)
 *
 * AI는 느리다. 연결·응답 타임아웃을 반드시 걸어 검색 전체가 묶이지 않게 한다.
 * 주소·타임아웃은 application.yaml 의 ai.* 에서 읽는다.
 */
package com.endit.ai;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Bean(name = "aiWebClient")
	public WebClient aiWebClient(
			@Value("${ai.base-url:http://localhost:8081}") String baseUrl,
			@Value("${ai.connect-timeout-ms:3000}") int connectTimeoutMs,
			@Value("${ai.read-timeout-ms:20000}") int readTimeoutMs) {

		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
				.responseTimeout(Duration.ofMillis(readTimeoutMs));

		log.debug("=============================");
		log.debug("aiWebClient baseUrl={} connect={}ms read={}ms", baseUrl, connectTimeoutMs, readTimeoutMs);
		log.debug("=============================");

		return WebClient.builder()
				.baseUrl(baseUrl)
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
