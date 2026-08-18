package com.endit.domain;

import com.endit.cmn.DTO;

public class ContentCreditVO extends DTO {

	private int creditId;
	private int contentId;
	private int personId;
	private String role;
	private String character;
	private int displayOrder;

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

	@Override
	public String toString() {
		return "ContentCreditVO [creditId=" + creditId + ", contentId=" + contentId + ", personId=" + personId
				+ ", role=" + role + ", character=" + character + ", displayOrder=" + displayOrder + ", toString()="
				+ super.toString() + "]";
	}

}
