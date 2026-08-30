package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.ContentGenreVO;

public interface ContentGenreService {

	// 콘텐츠 하나에 연결된 장르 목록 조회 - CONTENT_GENRE + GENRE 조인, genre_id/genre_name 반환
	List<ContentGenreVO> retrieve(int contentId, DTO param);

	// 콘텐츠 하나에 연결된 장르 전체 목록 조회 - 페이징 없이 한 번에 다 가져온다
	List<ContentGenreVO> retrieveAll(int contentId);

	// 콘텐츠-장르 연결 단건 조회
	ContentGenreVO get(int contentId, int genreId);

	// 콘텐츠에 장르 연결 등록 - CONTENT_GENRE insert
	ContentGenreVO create(int contentId, int genreId);

	// 콘텐츠-장르 연결 삭제
	void delete(int contentId, int genreId);

}
