package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.WorkDiv;
import com.endit.domain.ContentCreditVO;

@Mapper
public interface ContentCreditMapper extends WorkDiv<ContentCreditVO> {

	//테스트용
	int deleteAll();

	//테스트용
	int totalCnt();
}
