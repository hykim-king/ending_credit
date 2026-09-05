package com.endit.domain;

import com.endit.cmn.DTO;

public class ContentCreditVO extends DTO {

	// 개봉연도 표시 자릿수. releaseYear는 DATE를 'YYYY-MM-DD'로 뽑은 값이다
	private static final int YEAR_LENGTH = 4;

	private int creditId;
	private int contentId;
	private int personId;
	private String role;
	private String character;
	private int displayOrder;

	private String nameKo;
	private String nameOrg;
	private String profileImageUrl;
	private String titleKo;
	private String posterUrl;
	private String releaseYear;

	public ContentCreditVO() {
		super();
	}

	public ContentCreditVO(int creditId, int contentId, int personId, String role, String character,
			int displayOrder) {
		super();
		this.creditId = creditId;
		this.contentId = contentId;
		this.personId = personId;
		this.role = role;
		this.character = character;
		this.displayOrder = displayOrder;
	}

	public int getCreditId() {
		return creditId;
	}

	public void setCreditId(int creditId) {
		this.creditId = creditId;
	}

	public int getContentId() {
		return contentId;
	}

	public void setContentId(int contentId) {
		this.contentId = contentId;
	}

	public int getPersonId() {
		return personId;
	}

	public void setPersonId(int personId) {
		this.personId = personId;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getCharacter() {
		return character;
	}

	public void setCharacter(String character) {
		this.character = character;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public String getNameKo() {
		return nameKo;
	}

	public void setNameKo(String nameKo) {
		this.nameKo = nameKo;
	}

	public String getNameOrg() {
		return nameOrg;
	}

	public void setNameOrg(String nameOrg) {
		this.nameOrg = nameOrg;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public void setProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public String getTitleKo() {
		return titleKo;
	}

	public void setTitleKo(String titleKo) {
		this.titleKo = titleKo;
	}

	public String getPosterUrl() {
		return posterUrl;
	}

	public void setPosterUrl(String posterUrl) {
		this.posterUrl = posterUrl;
	}

	public String getReleaseYear() {
		return releaseYear;
	}

	public void setReleaseYear(String releaseYear) {
		this.releaseYear = releaseYear;
	}

	/**
	 * <pre>
	 * Method Name : getReleaseYearLabel
	 * Description : 화면에 찍을 개봉연도. 매퍼가 'YYYY-MM-DD'로 뽑으므로 앞 4자만 자른다(POL-031).
	 *               값이 없거나 4자에 못 미치면 null - 뷰가 그대로 th:if로 쓴다.
	 * </pre>
	 * @return String (연도 4자리 또는 null)
	 */
	public String getReleaseYearLabel() {

		if (releaseYear == null || releaseYear.length() < YEAR_LENGTH) {
			return null;
		}

		return releaseYear.substring(0, YEAR_LENGTH);
	}

	@Override
	public String toString() {
		return "ContentCreditVO [creditId=" + creditId + ", contentId=" + contentId + ", personId=" + personId
				+ ", role=" + role + ", character=" + character + ", displayOrder=" + displayOrder + ", nameKo="
				+ nameKo + ", nameOrg=" + nameOrg + ", profileImageUrl=" + profileImageUrl + ", titleKo=" + titleKo
				+ ", posterUrl=" + posterUrl + ", releaseYear=" + releaseYear + ", toString()=" + super.toString()
				+ "]";
	}

}
