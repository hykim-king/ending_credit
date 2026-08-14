package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.WorkDiv;
import com.endit.domain.ContentGenreVO;

@Mapper
public interface ContentGenreMapper extends WorkDiv<ContentGenreVO> {

	//테스트용
	int deleteAll();

	//테스트용
	int selectAllCount();
}
