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

	// ── 용도별 이미지 URL 변환 ──
	// 크기는 전부 ContentImageServiceImpl이 정한다. 호출부는 용도만 말하고 크기를 넘기지 않는다.

	// 영화 포스터 URL
	String toPosterUrl(String path);

	// 영화 상세 헤더 배경 URL
	String toBackdropUrl(String path);

	// 크레딧 프로필 URL
	String toCreditProfileUrl(String path);

	// 인물 상세 프로필 URL
	String toPersonProfileUrl(String path);

	// 완성 URL을 DB에 저장할 원본 경로로 되돌린다. 위 변환들의 역변환이다.
	// 화면에서 받은 URL을 그대로 저장하면 완성 URL이 DB에 박히므로 쓰기 경로가 저장 직전에 부른다.
	// 크기를 알려 주는 게 아니라 벗겨내는 쪽이라 "크기 지식을 가두는" 원칙에 걸리지 않는다.
	String toStoredPath(String url);

}
