package com.endit.domain;

/**
 * <pre>
 * Class Name  : CollectionItemVO
 * Description : 컬렉션에 포함된 콘텐츠 정보를 관리하는 VO
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 12.	jinyoung    최초 생성
 * 2026. 8. 19. jinyoung    목록 조회용 콘텐츠 정보 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 12.
 */
public class CollectionItemVO {

	private int collectionId;    // 컬렉션 번호
	private int contentId;       // 콘텐츠 번호
	private String addedDt;      // 콘텐츠 추가 일시
	private String externalId;   // 콘텐츠 외부 연동 번호
	private String titleKo;      // 콘텐츠 한글 제목
	private String titleOrg;     // 콘텐츠 원제
	private String releaseYear;  // 콘텐츠 공개 연도
	private String posterUrl;    // 콘텐츠 포스터 URL

	public CollectionItemVO() {
		super();
	}

	public CollectionItemVO(int collectionId, int contentId, String addedDt) {
		super();
		this.collectionId = collectionId;
		this.contentId = contentId;
		this.addedDt = addedDt;
	}

	public int getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(int collectionId) {
		this.collectionId = collectionId;
	}

	public int getContentId() {
		return contentId;
	}

	public void setContentId(int contentId) {
		this.contentId = contentId;
	}

	public String getAddedDt() {
		return addedDt;
	}

	public void setAddedDt(String addedDt) {
		this.addedDt = addedDt;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
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

	@Override
	public String toString() {
		return "CollectionItemVO [collectionId=" + collectionId + ", contentId=" + contentId + ", addedDt="
				+ addedDt + ", externalId=" + externalId + ", titleKo=" + titleKo + ", titleOrg=" + titleOrg
				+ ", releaseYear=" + releaseYear + ", posterUrl=" + posterUrl + "]";
	}
}
