package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.PersonVO;

/** P-01·AD-05·AD-06 인물 서비스. 참여작은 ContentCreditService.retrieveByPerson 담당 */
public interface PersonService {

	/**
	 *
	 * <pre>
	 * Method Name : get
	 * Description : 인물 단건 조회
	 *               없으면 예외가 아니라 null이다 - 형제 서비스의 get과 계약이 다르다.
	 *
	 * </pre>
	 *
	 * @param personId
	 * @return PersonVO (프로필 완성 URL) / 없으면 null
	 */
	PersonVO get(int personId);

	/**
	 *
	 * <pre>
	 * Method Name : retrieve
	 * Description : 인물 목록·검색 (페이징)
	 *               넘긴 param에 totalCnt가 채워져 돌아온다 - P-01 더보기·AD-05 페이저가 이 값을 쓴다.
	 *               검색어의 %·_는 서비스가 escape하므로 호출부가 걷어낼 필요가 없다.
	 *
	 * </pre>
	 *
	 * @param param
	 * @return List<PersonVO> (프로필 완성 URL, 없으면 빈 목록)
	 */
	List<PersonVO> retrieve(DTO param);

	/**
	 *
	 * <pre>
	 * Method Name : hasExternalId
	 * Description : AD-06 외부ID 중복 확인 (POL-034)
	 *               UK_PERSON_EXTERNAL을 저장 전에 확인한다. 비어 있으면 IllegalArgumentException.
	 *
	 * </pre>
	 *
	 * @param externalId
	 * @return boolean
	 */
	boolean hasExternalId(String externalId);

	/**
	 *
	 * <pre>
	 * Method Name : create
	 * Description : AD-06 인물 등록
	 *               원문명·외부ID는 필수다 - DB가 NOT NULL이라 정의서(선택)가 아니라 DDL에 맞췄다.
	 *               국문명·원문명 중 하나는 있어야 하고, 외부ID가 이미 있으면 IllegalStateException.
	 *
	 * </pre>
	 *
	 * @param param
	 * @return PersonVO (등록 후 재조회한 상태)
	 */
	PersonVO create(PersonVO param);

	/**
	 *
	 * <pre>
	 * Method Name : update
	 * Description : AD-06 인물 수정
	 *               비워 둔 필드는 기존 값을 유지한다(등록과 달리 원문명·외부ID를 강제하지 않는다).
	 *               남의 외부ID로 바꾸려 하면 IllegalStateException.
	 *
	 * </pre>
	 *
	 * @param personId
	 * @param param
	 * @return PersonVO (수정 후 재조회한 상태)
	 */
	PersonVO update(int personId, PersonVO param);
}
