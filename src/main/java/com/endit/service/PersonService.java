package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.PersonVO;

// P-01·AD-05·AD-06 인물 서비스 - 참여작은 ContentCreditService.retrieveByPerson 담당
public interface PersonService {

	// 단건 조회 - 없으면 null
	PersonVO get(int personId);

	// 목록·검색 (페이징) - param에 totalCnt가 채워져 돌아온다
	List<PersonVO> retrieve(DTO param);

	// 외부ID 중복 확인 (POL-034)
	boolean hasExternalId(String externalId);

	// 등록
	PersonVO create(PersonVO param);

	// 수정 - 비워 둔 필드는 기존 값 유지
	PersonVO update(int personId, PersonVO param);
}
