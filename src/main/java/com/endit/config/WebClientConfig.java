/**
 * AI 서버(FastAPI) 호출용 WebClient 설정
 *
 * AI는 느리다. 연결·응답 타임아웃을 반드시 걸어 검색 전체가 묶이지 않게 한다.
 * 주소·타임아웃은 application.yaml 의 ai.* 에서 읽는다.
 */
package com.endit.config;

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

	/** 응답 본문 버퍼 한도 16MB (임베딩 묶음 응답용) */
	private static final int MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024;

	@Bean
	public WebClient fastApiWebClient(
			@Value("${ai.base-url:http://localhost:8081}") String baseUrl,
			@Value("${ai.connect-timeout-ms:3000}") int connectTimeoutMs,
			@Value("${ai.read-timeout-ms:20000}") int readTimeoutMs) {

		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
				.responseTimeout(Duration.ofMillis(readTimeoutMs));

		log.debug("=============================");
		log.debug("fastApiWebClient baseUrl={} connect={}ms read={}ms", baseUrl, connectTimeoutMs, readTimeoutMs);
		log.debug("=============================");

		return WebClient.builder()
				.baseUrl(baseUrl)
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				// 임베딩 응답은 20편 묶음에 500KB 를 넘는다(실측 575KB). 기본 한도 256KB 면
				// DataBufferLimitException 으로 적재가 첫 묶음에서 끊긴다 - 넉넉히 잡는다
				.codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
				.build();
	}
}
