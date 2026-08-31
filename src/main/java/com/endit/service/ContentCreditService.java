package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;

public interface ContentCreditService {

	// 콘텐츠 하나의 출연/제작진 목록 조회 - CONTENT_CREDIT + PERSON + CONTENT 조인, role/이름/프로필/배역/순서 반환
	List<ContentCreditVO> retrieve(int contentId, DTO param);

	// 콘텐츠 하나의 출연/제작진 전체 목록 조회 - 페이징 없이 한 번에 다 가져오고, 감독을 맨 앞으로 정렬한다.
	List<ContentCreditVO> retrieveAll(int contentId);

	// 인물 하나의 참여 작품 목록 조회 - P-01 / AD-06. retrieve(int, DTO)와 파라미터 타입이 같아 이름으로 축을 구분한다
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
