package com.endit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

//tmdb 설정을 담는 클래스
@ConfigurationProperties(prefix = "tmdb")
public class TmdbProperties {

	// TMDB Read Access Token. JWT 문자열
	private String apiKey = "";
	// TMDB에 요청할 언어. ko-KR이면 한국어 제목과 줄거리
	private String language = "ko-KR";
	// 이미지 url에 붙일 가로 크기. w500이면 가로 500픽셀 -> 화면에서 path 에 사용 
	private String imageSize = "w500";

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getImageSize() {
		return imageSize;
	}

	public void setImageSize(String imageSize) {
		this.imageSize = imageSize;
	}

}
