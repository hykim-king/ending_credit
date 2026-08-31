package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;

/**
 * <pre>
 * Class Name  : ContentService
 * Description : 콘텐츠 조회 및 TMDB 콘텐츠 적재 기능을 정의하는 Service
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    컬렉션 작품 선택용 콘텐츠 조회 계약 추가
 * 2026. 8. 31. jinyoung    영화 상세용 sync·get 계약과 통합
 * ------------------------------------------------------------
 * </pre>
 */
public interface ContentService {

	/** 제목 검색과 페이징 조건으로 저장된 콘텐츠를 조회한다. */
	List<ContentVO> retrieve(DTO param);

	// TMDB 인기 목록을 limit건까지 보고, 우리 db에 없는 영화만 저장.
	// 신규 저장 건수 반환 -> 이미 있는 영화는 업데이트 하지 않는다(sync에서 영화 아이디 받아와서, 우리 db 에 있는지
	// 확인하고, 있으면 무시, 없으면 상세조회해서 컨텐츠 및 하위테이블 채워넣기함. 그래서
	// 신규 저장 건수를 반환한다는 말.. 혹시 이해 안되면 클맨 한테 부탁하거나 저한테 슬랙주세효
	int sync(int limit);

	// 상세페이지 헤더 조회 - CONTENT 단건(제목/원제/줄거리/개봉연도/러닝타임/국가/포스터/배경) 반환
	ContentVO get(int contentId);

}
