package com.endit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import info.movito.themoviedbapi.TmdbApi;

// TMDB 라이브러리 TmdbApi를 스프링 빈으로 등록해야함.
@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbConfig {

	// yaml에 넣은 JWT를 TmdbApi에 넘김. 이후 movies, genre, images 호출에 이 토큰이 붙음
	@Bean
	public TmdbApi tmdbApi(TmdbProperties properties) {
		validateApiKey(properties.getApiKey());

		return new TmdbApi(properties.getApiKey());
	}

	// api 키 없으면 기동 시 에러 알림 - 안 세우면 조용히 적재순으로 폴백한다
	private void validateApiKey(String apiKey) {
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalStateException(
					"tmdb.api-key 설정이 필요합니다. application.yaml의 tmdb 아래에 api-key가 있는지 확인한다.");
		}
	}

}
