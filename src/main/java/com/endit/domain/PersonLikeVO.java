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
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 12.
 */
package com.endit.domain;

public class PersonLikeVO {

	private int memberId;       // 회원 번호
	private int personId;       // 인물 번호
	private String createdDt;   // 좋아요 등록 일시

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

	@Override
	public String toString() {
		return "PersonLikeVO [memberId=" + memberId + ", personId=" + personId + ", createdDt=" + createdDt + "]";
	}
}
