package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.ContentImageVO;

public interface ContentImageService {

	// 콘텐츠 하나의 이미지(포스터/배경 구분 없이) 목록 조회
	List<ContentImageVO> retrieve(int contentId, DTO param);

	// 콘텐츠 하나의 이미지 전체 목록 조회 - 페이징 없이 한 번에 다 가져온다
	List<ContentImageVO> retrieveAll(int contentId);

	// 이미지 단건 조회
	ContentImageVO get(int imageId);

	// 콘텐츠에 이미지 등록
	ContentImageVO create(int contentId, ContentImageVO param);

	// 이미지 URL 수정
	ContentImageVO update(int imageId, ContentImageVO param);

	// 이미지 삭제
	void delete(int imageId);

	// TMDB 이미지 경로를 기본 크기(tmdb.image-size)의 완성 URL로 변환
	String toFullImageUrl(String path);

	// TMDB 이미지 경로를 지정한 크기의 완성 URL로 변환
	String toFullImageUrl(String path, String size);

}
