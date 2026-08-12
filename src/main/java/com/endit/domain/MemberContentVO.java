/**
 * <pre>
 * Class Name  : MemberContentVO
 * Description : 회원별 콘텐츠 평가 및 보고 싶어요 정보를 관리하는 VO
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 12.	jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 12.
 */
package com.endit.domain;

public class MemberContentVO {

	private int memberId;           // 회원 번호
	private int contentId;          // 콘텐츠 번호
	private Integer ratingScore;    // 별점(1~5점, 미평가 시 NULL)
	private String watchlist;       // 보고 싶어요 여부(Y: 등록, N: 미등록)
	private String ratedDt;         // 별점 등록·수정 일시
	private String watchlistDt;     // 보고 싶어요 등록 일시
	private String updatedDt;       // 최종 수정 일시

	public MemberContentVO() {
		super();
	}

	public MemberContentVO(int memberId, int contentId, Integer ratingScore, String watchlist, String ratedDt,
			String watchlistDt, String updatedDt) {
		super();
		this.memberId = memberId;
		this.contentId = contentId;
		this.ratingScore = ratingScore;
		this.watchlist = watchlist;
		this.ratedDt = ratedDt;
		this.watchlistDt = watchlistDt;
		this.updatedDt = updatedDt;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public int getContentId() {
		return contentId;
	}

	public void setContentId(int contentId) {
		this.contentId = contentId;
	}

	public Integer getRatingScore() {
		return ratingScore;
	}

	public void setRatingScore(Integer ratingScore) {
		this.ratingScore = ratingScore;
	}

	public String getWatchlist() {
		return watchlist;
	}

	public void setWatchlist(String watchlist) {
		this.watchlist = watchlist;
	}

	public String getRatedDt() {
		return ratedDt;
	}

	public void setRatedDt(String ratedDt) {
		this.ratedDt = ratedDt;
	}

	public String getWatchlistDt() {
		return watchlistDt;
	}

	public void setWatchlistDt(String watchlistDt) {
		this.watchlistDt = watchlistDt;
	}

	public String getUpdatedDt() {
		return updatedDt;
	}

	public void setUpdatedDt(String updatedDt) {
		this.updatedDt = updatedDt;
	}

	@Override
	public String toString() {
		return "MemberContentVO [memberId=" + memberId + ", contentId=" + contentId + ", ratingScore="
				+ ratingScore + ", watchlist=" + watchlist + ", ratedDt=" + ratedDt + ", watchlistDt=" + watchlistDt
				+ ", updatedDt=" + updatedDt + "]";
	}
}
