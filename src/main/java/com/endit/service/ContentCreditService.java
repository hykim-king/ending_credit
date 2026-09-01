package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;

public interface ContentCreditService {

	// 콘텐츠 하나의 출연/제작진 목록 조회
	// 역할 필터는 param.searchMap의 "role"로 넘긴다(DIRECTOR/ACTOR/WRITER/PRODUCER, 그 밖의 값은 예외)
	List<ContentCreditVO> retrieve(int contentId, DTO param);

	// 콘텐츠 하나의 출연/제작진 전체 목록 조회 (DIRECTOR > ACTOR > WRITER > PRODUCER) 순 정렬
	List<ContentCreditVO> retrieveAll(int contentId);

	// 인물 하나의 참여 작품 목록 조회
	List<ContentCreditVO> retrieveByPerson(int personId, DTO param);

	// 크레딧 단건 조회
	ContentCreditVO get(int creditId);

	// 콘텐츠에 크레딧(배우 또는 감독) 등록
	ContentCreditVO create(int contentId, ContentCreditVO param);

	// 크레딧의 배역명/표시순서 등 수정
	ContentCreditVO update(int creditId, ContentCreditVO param);

	// 크레딧 삭제
	void delete(int creditId);

}
