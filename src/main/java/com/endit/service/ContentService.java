package com.endit.service;

public interface ContentService {

	// TMDB 인기 목록을 limit건까지 보고, 우리 db에 없는 영화만 저장. 
	// 신규 저장 건수 반환 -> 이미 있는 영화는 업데이트 하지 않는다(importPopular에서 영화 아이디 받아와서, 우리 db 에 있는지
	// 확인하고, 있으면 무시, 없으면 상세조회해서 컨텐츠 및 하위테이블 채워넣기함. 그래서 
	// 신규 저장 건수를 반환한다는 말.. 혹시 이해 안되면 클맨 한테 부탁하거나 저한테 슬랙주세효
	int importPopular(int limit);

}
