package com.endit.config;

import java.util.Locale;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/** F-01 국제화. 지원 언어는 한국어·영어 2종이고 기본은 한국어다 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

	// 언어 선택을 담는 쿠키. 세션이 아니라 쿠키인 것은 브라우저를 닫아도 선택이 남아야 하기 때문이다(ACT-G-007 "선택 유지")
	private static final String LOCALE_COOKIE_NAME = "ENDIT_LANG";
	// 1년. 이 값이 지나면 기본 언어로 돌아간다
	private static final int LOCALE_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365;

	// 언어를 바꾸는 통로. 어느 화면에서든 ?lang=en 한 번이면 쿠키가 갱신되므로 전용 컨트롤러를 두지 않는다
	private static final String LOCALE_PARAM_NAME = "lang";

	// 받아 주는 언어. 이 밖의 값은 쿠키에 남기지 않는다 -
	// 남기면 1년짜리 쿠키에 박혀 그 방문자가 계속 폴백 경로로 떨어진다
	private static final Set<String> SUPPORTED_LANGUAGES =
			Set.of(Locale.KOREAN.getLanguage(), Locale.ENGLISH.getLanguage());

	@Bean
	public LocaleResolver localeResolver() {
		CookieLocaleResolver resolver = new CookieLocaleResolver(LOCALE_COOKIE_NAME);
		// 지원하지 않는 언어로 들어와도 한국어로 떨어진다
		resolver.setDefaultLocale(Locale.KOREAN);
		resolver.setCookieMaxAge(java.time.Duration.ofSeconds(LOCALE_COOKIE_MAX_AGE_SECONDS));
		resolver.setCookiePath("/");

		return resolver;
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor interceptor = new SupportedLocaleChangeInterceptor();
		interceptor.setParamName(LOCALE_PARAM_NAME);
		// lang 파라미터는 모든 경로에서 먹으므로 ?lang=1 같은 값이 500이 되면 안 된다.
		// 켜 두면 못 읽는 값은 무시하고 지금 언어를 그대로 쓴다
		interceptor.setIgnoreInvalidLocale(true);

		return interceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localeChangeInterceptor());
	}

	// 지원하지 않는 언어를 "못 읽는 값"으로 돌린다. 그래야 위 ignoreInvalidLocale이 같이 걸러 준다.
	// 화이트리스트를 여기에 두는 것은 파싱과 검증을 한 자리에서 끝내기 위해서다
	private static final class SupportedLocaleChangeInterceptor extends LocaleChangeInterceptor {

		@Override
		protected Locale parseLocaleValue(String localeValue) {
			Locale locale = super.parseLocaleValue(localeValue);

			if (locale == null || !SUPPORTED_LANGUAGES.contains(locale.getLanguage())) {
				throw new IllegalArgumentException("지원하지 않는 언어입니다: " + localeValue);
			}

			return locale;
		}

	}

}
