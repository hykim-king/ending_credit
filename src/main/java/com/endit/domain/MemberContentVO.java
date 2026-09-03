package com.endit.domain;

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
 * 2026. 8. 19. jinyoung    목록 조회용 콘텐츠 정보 추가
 * 2026. 9. 03. jinyoung    기록 카드용 전체 회원 평균 별점 필드 설명 정리
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 12.
 */
public class MemberContentVO {

	private int memberId;           // 회원 번호
	private int contentId;          // 콘텐츠 번호
	private Integer ratingScore;    // 별점(1~5점, 미평가 시 NULL)
	private String watchlist;       // 보고 싶어요 여부(Y: 등록, N: 미등록)
	private String ratedDt;         // 별점 등록·수정 일시
	private String watchlistDt;     // 보고 싶어요 등록 일시
	private String updatedDt;       // 최종 수정 일시
	private String titleKo;         // 영화 국문 제목
	private String titleOrg;        // 영화 원문 제목
	private String releaseYear;     // 개봉 연도
	private String posterUrl;       // 포스터 URL
	private Double averageRating;   // 전체 회원 평가 평균 별점

	public MemberContentVO() {
		super();
	}

	public MemberContentVO(
			int memberId,
			int contentId,
			Integer ratingScore,
			String watchlist,
			String ratedDt,
			String watchlistDt,
			String updatedDt) {
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

	public String getTitleKo() {
		return titleKo;
	}

	public void setTitleKo(String titleKo) {
		this.titleKo = titleKo;
	}

	public String getTitleOrg() {
		return titleOrg;
	}

	public void setTitleOrg(String titleOrg) {
		this.titleOrg = titleOrg;
	}

	public String getReleaseYear() {
		return releaseYear;
	}

	public void setReleaseYear(String releaseYear) {
		this.releaseYear = releaseYear;
	}

	public String getPosterUrl() {
		return posterUrl;
	}

	public void setPosterUrl(String posterUrl) {
		this.posterUrl = posterUrl;
	}

	public Double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(Double averageRating) {
		this.averageRating = averageRating;
	}

	@Override
	public String toString() {
		return "MemberContentVO [memberId=" + memberId
				+ ", contentId=" + contentId
				+ ", ratingScore=" + ratingScore
				+ ", watchlist=" + watchlist
				+ ", ratedDt=" + ratedDt
				+ ", watchlistDt=" + watchlistDt
				+ ", updatedDt=" + updatedDt
				+ ", titleKo=" + titleKo
				+ ", titleOrg=" + titleOrg
				+ ", releaseYear=" + releaseYear
				+ ", posterUrl=" + posterUrl
				+ ", averageRating=" + averageRating
				+ "]";
	}
}
