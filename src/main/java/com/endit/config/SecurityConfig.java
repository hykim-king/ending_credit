package com.endit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.endit.security.CustomOAuth2UserService;
import com.endit.security.CustomUserDetailsService;
import com.endit.security.OAuth2FailureHandler;
import com.endit.security.OAuth2SuccessHandler;

/**
 * <pre>
 * Class Name : SecurityConfig
 * Description : 스프링 시큐리티 설정. "어느 URL을 열고/잠글지"와 "로그인을 어떻게 처리할지"를 정한다.
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 9. 05. jinyoung    회원 기록 댓글 API를 로그인 필수 경로로 추가
 * ------------------------------------------------------------
 * </pre>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** 이메일로 회원을 찾아 주는 서비스 (com.endit.security). 시큐리티가 로그인 검증 중 사용한다. */
	private final CustomUserDetailsService userDetailsService;
	
	/** 구글 정보를 받아 회원을 찾거나 신규 판단하는 서비스 */
	private final CustomOAuth2UserService customOAuth2UserService;
 
	/** 구글 로그인 성공 처리(기존 회원 → 홈) */
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
 
	/** 구글 로그인 실패/신규/중복 분기 처리 */
	private final OAuth2FailureHandler oAuth2FailureHandler;
 
	public SecurityConfig(
			CustomUserDetailsService userDetailsService,
			CustomOAuth2UserService customOAuth2UserService,
			OAuth2SuccessHandler oAuth2SuccessHandler,
			OAuth2FailureHandler oAuth2FailureHandler) {
		this.userDetailsService      = userDetailsService;
		this.customOAuth2UserService = customOAuth2UserService;
		this.oAuth2SuccessHandler    = oAuth2SuccessHandler;
		this.oAuth2FailureHandler    = oAuth2FailureHandler;
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
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				// 로그인 회원 본인의 기록·좋아요 화면과 조회 API
				.requestMatchers("/members/records", "/members/likes",
						"/members/me/records", "/members/me/likes").authenticated()
				.requestMatchers("/api/members/comments", "/api/members/comments/**").authenticated()
				.requestMatchers(HttpMethod.GET,
						"/api/members/ratings",
						"/api/members/watchlist",
						"/api/members/collections",
						"/api/members/likes").authenticated()
				// 컬렉션 등록·수정 화면
				.requestMatchers("/collections/new", "/collections/*/edit").authenticated()
				// 별점·보고싶어요·인물 좋아요 변경
				.requestMatchers("/api/movies/*/rating", "/api/watchlist/*",
						"/api/people/*/likes").authenticated()
				// 컬렉션 변경과 로그인 회원의 좋아요 상태 조회
				.requestMatchers(HttpMethod.POST, "/api/collections/**").authenticated()
				.requestMatchers(HttpMethod.PATCH, "/api/collections/**").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/collections/**").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/collections/*/likes").authenticated()
				// 그 외 모든 요청은 개발 편의상 일단 전부 허용
				.anyRequest().permitAll()
			)

			// ── 2) 이메일 로그인(formLogin) ──
			.formLogin(form -> form
				.loginPage("/login")                 // GET: 로그인 화면
				.loginProcessingUrl("/login/email")  // POST: 이 URL로 폼 제출 → 시큐리티가 가로채 검증
				.usernameParameter("email")          // 아이디 입력칸 name="email" (기본은 username)
				.passwordParameter("password")       // 비번 입력칸 name="password"
				.defaultSuccessUrl("/")              // 성공 시 홈으로 (원래 가려던 보호페이지 있으면 그쪽으로)
				.failureUrl("/login?error")          // 실패 시 다시 로그인 화면
				.permitAll()
			)
			
			// ── 2-2) 구글 로그인(oauth2Login) ──
			.oauth2Login(oauth -> oauth
				.loginPage("/login")                          // 로그인 화면 (이메일이랑 공유)
				.userInfoEndpoint(userInfo -> userInfo
					.userService(customOAuth2UserService)     // 구글 정보 받아 회원 찾기/신규판단
				)
				.successHandler(oAuth2SuccessHandler)          // 기존 회원 로그인 성공 → 홈
				.failureHandler(oAuth2FailureHandler)          // 신규/이메일중복/오류 → 화면 분기
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
