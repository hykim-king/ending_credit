package com.endit.domain;

/**
 * <pre>
 * Class Name  : EnglishContentVO
 * Description : TMDB에서 en-US로 한 번 받아 온 영문 표시값. DB 테이블이 아니라 캐시에만 산다.
 *               한 번의 상세 응답에서 함께 나오는 값들이라 따로 담지 않고 묶어 둔다.
 * </pre>
 */
public class EnglishContentVO {

	// 없으면 빈 문자열이다. null이 아니어야 "받아 봤는데 없더라"를 캐시할 수 있다
	private final String overview;
	private final String posterPath;
	private final String backdropPath;

	public EnglishContentVO(String overview, String posterPath, String backdropPath) {
		this.overview = overview;
		this.posterPath = posterPath;
		this.backdropPath = backdropPath;
	}

	public String getOverview() {
		return overview;
	}

	// TMDB 원본 경로다. 화면에 쓰려면 ContentImageService가 크기를 붙여 완성해야 한다
	public String getPosterPath() {
		return posterPath;
	}

	public String getBackdropPath() {
		return backdropPath;
	}

}
