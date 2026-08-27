package com.endit.domain;

/**
 * <pre>
 * Class Name  : RatingRequest
 * Description : 콘텐츠 별점 등록 및 변경 요청값
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
public class RatingRequest {

	private Integer ratingScore; // 별점(1~5점)

	public RatingRequest() {
		super();
	}

	public Integer getRatingScore() {
		return ratingScore;
	}

	public void setRatingScore(Integer ratingScore) {
		this.ratingScore = ratingScore;
	}

	@Override
	public String toString() {
		return "RatingRequest [ratingScore=" + ratingScore + "]";
	}
}