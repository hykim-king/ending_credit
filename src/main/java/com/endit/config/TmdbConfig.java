package com.endit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import info.movito.themoviedbapi.TmdbApi;

// TMDB 라이브러리 TmdbApi를 스프링 빈으로 등록해야함. 
@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbConfig {

	// yaml에 넣은 JWT를 TmdbApi에 넘김. 이후 movies, genre, images 호출에 이 토큰이 붙음
	@Bean
	public TmdbApi tmdbApi(TmdbProperties properties) {
		return new TmdbApi(properties.getApiKey());
	}

}
