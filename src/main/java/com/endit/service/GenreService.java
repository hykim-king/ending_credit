package com.endit.service;

import java.util.List;

import com.endit.domain.GenreVO;

public interface GenreService {

	// 장르 마스터 전체 목록 조회
	List<GenreVO> retrieveAll();

	// 장르 단건 조회
	GenreVO get(int genreId);

}
