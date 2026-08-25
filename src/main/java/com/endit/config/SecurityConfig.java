/**
 * <pre>
 * Class Name : SecurityConfig
 * Description : ⚠️ 개발용 임시 시큐리티 설정
 *               시큐리티 스타터만 있고 설정이 없으면 부트 기본값이 전 요청을 잠가
 *               화면 개발·확인이 불가능하다. 회원(2조) 로그인 연동 전까지
 *               전 요청을 허용하고, CSRF는 학원 표준(fetch getCsrfHeaders)대로 활성 유지한다.
 *               2조 시큐리티(sb13 원형: formLogin + CustomUserDetails) 도입 시 이 클래스를 대체할 것.
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 25.  홍선기   최초 생성 (개발용)
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 25.
 */
package com.endit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	final Logger log = LoggerFactory.getLogger(getClass());

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		log.debug("=============================");
		log.debug("{}()", "filterChain");
		log.debug("=============================");

		// 개발용: 전 요청 허용 (로그인 도입 전) — CSRF는 기본 활성 그대로 둔다
		http.authorizeHttpRequests((auth) -> auth.anyRequest().permitAll());

		return http.build();
	}

}
