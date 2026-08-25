/**
 * <pre>
 * Class Name : CodeVO
 * Description : 공통코드(COMMON_CODE) VO — 화면 select 렌더링용 (학원 27~28장 패턴)
 *               학원 CMN_CODE와 컬럼 구성이 다르다: 팀 스키마는
 *               (GROUP_CODE, CODE) 복합 PK + CODE_NAME + SORT_ORDER.
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 25.  홍선기   최초 생성
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 25.
 */
package com.endit.domain;

import com.endit.cmn.DTO;

public class CodeVO extends DTO {

	private String groupCode; // 코드 그룹 (예: REPORT_REASON)
	private String code;      // 코드 값 (예: SPOILER)
	private String codeName;  // 화면 표시명 (예: 스포일러)
	private int sortOrder;    // 표시 순서
	private String createdDt; // 등록일

	public CodeVO() {
		super();
	}

	public CodeVO(String groupCode, String code, String codeName, int sortOrder, String createdDt) {
		super();
		this.groupCode = groupCode;
		this.code = code;
		this.codeName = codeName;
		this.sortOrder = sortOrder;
		this.createdDt = createdDt;
	}

	public String getGroupCode() {
		return groupCode;
	}

	public void setGroupCode(String groupCode) {
		this.groupCode = groupCode;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCodeName() {
		return codeName;
	}

	public void setCodeName(String codeName) {
		this.codeName = codeName;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(String createdDt) {
		this.createdDt = createdDt;
	}

	@Override
	public String toString() {
		return "CodeVO [groupCode=" + groupCode + ", code=" + code + ", codeName=" + codeName + ", sortOrder="
				+ sortOrder + ", createdDt=" + createdDt + "]";
	}

}
