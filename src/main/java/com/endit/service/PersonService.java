package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.PersonVO;

/**
 * 인물(배우·감독) 서비스 — 인물 상세 / 목록
 * 담당: 이기준
 */
public interface PersonService {

	/** 인물 단건 조회 */
	PersonVO get(int personId);

	/** 인물 목록·검색 (페이징) */
	List<PersonVO> retrieve(DTO param);

	/** 인물 참여 작품(필모그래피) */
	List<ContentCreditVO> getFilmography(int personId);
}
