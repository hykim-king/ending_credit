package com.endit.domain;

/**
 * <pre>
 * Class Name  : TmdbRatingVO
 * Description : TMDB 상세 응답에서 한 번에 받아 온 값. DB 테이블이 아니라 캐시에만 산다.
 *               TMDB는 평균과 참여 인원만 주고 점수별 분포는 주지 않는다.
 *               예고편도 같은 응답에서 함께 나와 호출을 아끼려고 여기 같이 담는다.
 * </pre>
 */
public class TmdbRatingVO {

	// TMDB는 10점 만점, 우리는 5점 만점이다
	private static final double STAR_SCALE_DIVISOR = 2;

	// TMDB 척도 그대로인 10점 만점 평균이다. 5점 척도로 바꾸는 것은 getStarAverage가 한다
	private final double voteAverage;
	private final int voteCount;
	// 예고편 YouTube 영상 id. 못 찾으면 null이다
	private final String trailerKey;

	public TmdbRatingVO(double voteAverage, int voteCount, String trailerKey) {
		this.voteAverage = voteAverage;
		this.voteCount = voteCount;
		this.trailerKey = trailerKey;
	}

	public double getVoteAverage() {
		return voteAverage;
	}

	// 우리 화면은 1~5별 한 축만 쓴다. 나누는 자리를 여기 하나로 두어 화면·그래프가 어긋나지 않게 한다
	public double getStarAverage() {
		return voteAverage / STAR_SCALE_DIVISOR;
	}

	public int getVoteCount() {
		return voteCount;
	}

	// TMDB에 아직 평가가 없는 영화가 있다. 그때 평균은 0.0이라 화면에 세우면 안 된다.
	// 예고편은 평가와 무관하게 있을 수 있으므로 trailerKey는 이 값과 상관없이 쓴다
	public boolean isRated() {
		return voteCount > 0;
	}

	// 주소가 아니라 영상 id다. 임베드 주소는 화면이 조립한다
	public String getTrailerKey() {
		return trailerKey;
	}

}
