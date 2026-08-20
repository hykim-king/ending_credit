package com.endit.domain;

/**
 * <pre>
 * Class Name  : PersonLikeVO
 * Description : 회원의 인물 좋아요 정보를 관리하는 VO
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 12.	jinyoung    최초 생성
 * 2026. 8. 19. jinyoung    목록 조회용 인물 정보 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 12.
 */
public class PersonLikeVO {

	private int memberId;       // 회원 번호
	private int personId;       // 인물 번호
	private String createdDt;   // 좋아요 등록 일시
	private String externalId;  // 외부 인물 식별값
	private String nameKo;      // 인물명 국문
	private String nameOrg;     // 인물명 원문
	private String profileImageUrl; // 프로필 이미지 URL

	public PersonLikeVO() {
		super();
	}

	public PersonLikeVO(int memberId, int personId, String createdDt) {
		super();
		this.memberId = memberId;
		this.personId = personId;
		this.createdDt = createdDt;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public int getPersonId() {
		return personId;
	}

	public void setPersonId(int personId) {
		this.personId = personId;
	}

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
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

	@Override
	public String toString() {
		return "PersonLikeVO [memberId=" + memberId + ", personId=" + personId + ", createdDt=" + createdDt
				+ ", externalId=" + externalId + ", nameKo=" + nameKo + ", nameOrg=" + nameOrg
				+ ", profileImageUrl=" + profileImageUrl + "]";
	}
}
