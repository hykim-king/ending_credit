package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.WorkDiv;
import com.endit.domain.ContentImageVO;

@Mapper
public interface ContentImageMapper extends WorkDiv<ContentImageVO> {

	//테스트용
	int deleteAll();

	//테스트용
	int totalCnt();
}
