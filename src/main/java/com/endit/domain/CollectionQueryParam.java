package com.endit.domain;

import com.endit.cmn.DTO;

/**
 * <pre>
 * Class Name  : CollectionQueryParam
 * Description : 컬렉션 공개 범위와 대상 회원을 반영하는 내부 목록 조회 조건
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    현재 회원 및 U-05 대상 회원 조회 조건 정의
 * ------------------------------------------------------------
 * </pre>
 */
public class CollectionQueryParam extends DTO {

	private Long currentMemberId;
	private Long targetMemberId;

	public Long getCurrentMemberId() {
		return currentMemberId;
	}

	public void setCurrentMemberId(Long currentMemberId) {
		this.currentMemberId = currentMemberId;
	}

	public Long getTargetMemberId() {
		return targetMemberId;
	}

	public void setTargetMemberId(Long targetMemberId) {
		this.targetMemberId = targetMemberId;
	}
}
