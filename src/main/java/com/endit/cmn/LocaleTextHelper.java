package com.endit.cmn;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * <pre>
 * Class Name  : LocaleTextHelper
 * Description : DB에 언어 컬럼이 없어 이미 있는 두 칸을 로케일로 골라 쓰는 자리.
 *               제목(title_ko/title_org)과 인물 이름(name_ko/name_org)이 대상이다.
 * </pre>
 */
@Component("localeText")
public class LocaleTextHelper {

	/**
	 * <pre>
	 * Method Name : get
	 * Description : 현재 로케일이 영어면 원어 값을, 아니면 한국어 값을 돌려준다.
	 *               고른 쪽이 비어 있으면 다른 쪽으로 떨어지고, 둘 다 비면 null이다 -
	 *               한국어 제목이 없는 콘텐츠가 있어 어느 쪽도 있다고 보장할 수 없다.
	 *               템플릿에서 ${@localeText.get(movie.titleKo, movie.titleOrg)}로 쓴다.
	 *               VO의 파생 getter로 두지 않은 것은 도메인 객체가 LocaleContextHolder를
	 *               알게 되기 때문이다.
	 * </pre>
	 * @param korean
	 * @param original
	 * @return String (없으면 null)
	 */
	public String get(String korean, String original) {
		String preferred = isEnglish() ? original : korean;

		if (StringUtils.hasText(preferred)) {
			return preferred;
		}

		return isEnglish() ? korean : original;
	}

	/**
	 * <pre>
	 * Method Name : isEnglish
	 * Description : 지금 요청의 언어가 영어인지. en-US·en-GB를 갈라 볼 이유가 없어 언어만 본다.
	 *               문구가 아니라 데이터를 다르게 가져와야 하는 자리(영문 줄거리)가 쓴다.
	 * </pre>
	 * @return boolean
	 */
	public boolean isEnglish() {
		return Locale.ENGLISH.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage());
	}

}
