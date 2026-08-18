package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.cmn.WorkDiv;
import com.endit.domain.PersonVO;

@Mapper
public interface PersonMapper extends WorkDiv<PersonVO> {

	// 외부 API가 제공해주는 인물 ID로 이미 있는지 없는지 찾는 쿼리,,
	Integer findPersonIdByExternal(@Param("externalId") String externalId);

	//테스트용
	int deleteAll();

	//테스트용
	int totalCnt();
}
