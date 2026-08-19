package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.cmn.WorkDiv;
import com.endit.domain.GenreVO;

@Mapper
public interface GenreMapper extends WorkDiv<GenreVO> {

	// 외부 API가 제공해주는 장르 ID로 이미 있는지 없는지 찾는 쿼리,,
	// 장르 테이블 삽입용 
	Integer findGenreIdByExternal(@Param("externalGenreId") String externalGenreId);

	//테스트용
	int deleteAll();

	//테스트용
	int totalCnt();
}
