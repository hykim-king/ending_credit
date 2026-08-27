package com.endit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.endit.security.CustomUserDetailsService;

/**
 * <pre>
 * Class Name : SecurityConfig
 * Description : 스프링 시큐리티 설정. "어느 URL을 열고/잠글지"와 "로그인을 어떻게 처리할지"를 정한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** 이메일로 회원을 찾아 주는 서비스 (com.endit.security). 시큐리티가 로그인 검증 중 사용한다. */
	private final CustomUserDetailsService userDetailsService;

	public SecurityConfig(CustomUserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	/**
	 * 비밀번호 해시 도구.
	 *  - 회원가입 때: 원문 비번을 encode()로 해시해서 저장
	 *  - 로그인 때: 시큐리티가 이걸로 입력 비번을 해시해서 저장된 해시와 비교
	 * (12 = 해시 강도. 숫자가 클수록 안전하지만 느려진다. 10이 기본, 12면 넉넉)
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		log.debug("SecurityConfig.filterChain()");

		http
			// ── 1) URL별 접근 권한 ──
			.authorizeHttpRequests(auth -> auth
				// 관리자 영역만 ADMIN 권한 필요 (role "ADMIN" → 권한 "ROLE_ADMIN")
				.requestMatchers("/admin/**").hasRole("ADMIN")
				// 그 외 모든 요청은 개발 편의상 일단 전부 허용
				.anyRequest().permitAll()
			)

			// ── 2) 이메일 로그인(formLogin) ──
			.formLogin(form -> form
				.loginPage("/login/email")           // GET: 로그인 화면
				.loginProcessingUrl("/login/email")  // POST: 이 URL로 폼 제출 → 시큐리티가 가로채 검증
				.usernameParameter("email")          // 아이디 입력칸 name="email" (기본은 username)
				.passwordParameter("password")       // 비번 입력칸 name="password"
				.defaultSuccessUrl("/")              // 성공 시 홈으로 (원래 가려던 보호페이지 있으면 그쪽으로)
				.failureUrl("/login/email?error")    // 실패 시 다시 로그인 화면
				.permitAll()
			)

			// ── 3) 로그아웃 ──
			.logout(logout -> logout
				.logoutUrl("/logout")                // POST /logout
				.logoutSuccessUrl("/")               // 로그아웃 후 홈
				.invalidateHttpSession(true)         // 세션 무효화
				.deleteCookies("JSESSIONID")         // 세션 쿠키 제거
			)

			// ── 4) 로그인 검증에 쓸 UserDetailsService 연결 ──
			.userDetailsService(userDetailsService)

			// ── 5) CSRF: 개발 편의를 위해 지금은 끔 ──
			.csrf(csrf -> csrf.disable());

		return http.build();
	}
}