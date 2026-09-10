/**
 * AI 서버 호출용 RestTemplate 설정
 *
 * AI는 느리다. 타임아웃을 반드시 걸어 코멘트 등록 전체가 묶이지 않게 한다.
 */
package com.endit.ai;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiClientConfig {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** 파이썬 서버가 살아있는지 확인하는 시간 */
    private final int connectTimeoutMs;

    /** AI 응답을 기다리는 시간. 실제 AI는 1~5초가 걸린다 */
    private final int readTimeoutMs;

    public AiClientConfig(
            @Value("${ai.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${ai.read-timeout-ms:20000}") int readTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Bean(name = "aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        log.debug("=============================");
        log.debug("aiRestTemplate connect={}ms read={}ms", connectTimeoutMs, readTimeoutMs);
        log.debug("=============================");

        return new RestTemplate(factory);
    }
}
