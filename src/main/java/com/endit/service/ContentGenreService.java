package com.endit.service;

import java.util.List;

import com.endit.domain.ContentGenreVO;

public interface ContentGenreService {

	// 콘텐츠 하나에 연결된 장르 전체 목록 조회
	List<ContentGenreVO> retrieveAll(int contentId);

	// 콘텐츠-장르 연결 단건 조회
	ContentGenreVO get(int contentId, int genreId);

	// 콘텐츠에 장르가 이미 연결돼 있는지 확인 - AD-03 중복 연결 방지
	boolean has(int contentId, int genreId);

	// 콘텐츠에 장르 연결 등록
	ContentGenreVO create(int contentId, int genreId);

	// 콘텐츠-장르 연결 삭제
	void delete(int contentId, int genreId);

}
