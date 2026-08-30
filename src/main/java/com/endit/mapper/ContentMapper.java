package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.cmn.WorkDiv;
import com.endit.domain.ContentVO;

@Mapper
public interface ContentMapper extends WorkDiv<ContentVO> {

	// 외부 API가 제공해주는 영화 ID로 이미 있는지 없는지 찾는 쿼리,,
	// 영화 테이블 select 
	Integer findContentIdByExternal(@Param("externalId") String externalId);

	//테스트용
	int deleteAll();

	//테스트용
	int totalCnt();
}
