package com.endit.domain;

import com.endit.cmn.DTO;

public class PersonVO extends DTO {

	private int personId;
	private String externalId;
	private String nameKo;
	private String nameOrg;
	private String profileImageUrl;
	private String createdDt;
	private String updatedDt;

	public PersonVO() {
		super();
	}

	public PersonVO(int personId, String externalId, String nameKo, String nameOrg, String profileImageUrl,
			String createdDt, String updatedDt) {
		super();
		this.personId = personId;
		this.externalId = externalId;
		this.nameKo = nameKo;
		this.nameOrg = nameOrg;
		this.profileImageUrl = profileImageUrl;
		this.createdDt = createdDt;
		this.updatedDt = updatedDt;
	}

	public int getPersonId() {
		return personId;
	}

	public void setPersonId(int personId) {
		this.personId = personId;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
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

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	public String getUpdatedDt() {
		return updatedDt;
	}

	public void setUpdatedDt(String updatedDt) {
		this.updatedDt = updatedDt;
	}

	@Override
	public String toString() {
		return "PersonVO [personId=" + personId + ", externalId=" + externalId + ", nameKo=" + nameKo + ", nameOrg="
				+ nameOrg + ", profileImageUrl=" + profileImageUrl + ", createdDt=" + createdDt + ", updatedDt="
				+ updatedDt + ", toString()=" + super.toString() + "]";
	}

}
