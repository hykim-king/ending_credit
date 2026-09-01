package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.cmn.WorkDiv;
import com.endit.domain.ContentCreditVO;

@Mapper
public interface ContentCreditMapper extends WorkDiv<ContentCreditVO> {

	// 인물 여럿 중 해당 역할의 크레딧을 가진 person_id만 추린다.
	// 목록 화면이 인물마다 따로 조회하지 않도록 한 번에 받는다
	List<Integer> doSelectPersonIdsByRole(
			@Param("personIds") List<Integer> personIds,
			@Param("role") String role);

	//테스트용
	int deleteAll();

	//테스트용
	int totalCnt();
}
