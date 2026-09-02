package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.ContentImageVO;

/** C-01 갤러리·AD-03 이미지 관리 서비스. 나가는 URL은 모두 완성 URL이며, 크기 결정은 이 서비스 안에 갇혀 있다 */
public interface ContentImageService {

	/**
	 *
	 * <pre>
	 * Method Name : retrieve
	 * Description : 콘텐츠 하나의 이미지(포스터/배경 구분 없이) 목록
	 *               넘긴 param에 totalCnt가 채워져 돌아온다 - 더보기·페이저는 이 값을 쓴다.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param param
	 * @return List<ContentImageVO> (썸네일용 imageUrl, 확대용 zoomImageUrl 모두 완성 URL)
	 */
	List<ContentImageVO> retrieve(int contentId, DTO param);

	/**
	 *
	 * <pre>
	 * Method Name : retrieveAll
	 * Description : 콘텐츠 하나의 이미지 전체 목록
	 *               "전체"는 실제로 100건 상한이며 초과분은 잘린다(상한 도달 시 log.warn만 남는다).
	 *               전체 건수가 필요하면 retrieve(int, DTO)를 직접 쓴다.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @return List<ContentImageVO> (완성 URL)
	 */
	List<ContentImageVO> retrieveAll(int contentId);

	/**
	 *
	 * <pre>
	 * Method Name : get
	 * Description : 이미지 단건 조회
	 *
	 * </pre>
	 *
	 * @param imageId
	 * @return ContentImageVO (완성 URL) / 없으면 NoSuchElementException
	 */
	ContentImageVO get(int imageId);

	/**
	 *
	 * <pre>
	 * Method Name : create
	 * Description : 콘텐츠에 이미지 등록
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param param
	 * @return ContentImageVO (채번된 imageId로 재조회한 완성 URL 상태)
	 */
	ContentImageVO create(int contentId, ContentImageVO param);

	/**
	 *
	 * <pre>
	 * Method Name : update
	 * Description : 이미지 URL 수정
	 *               안 채운 contentId·imageUrl은 기존 값을 유지한다.
	 *               완성 URL을 넘겨도 저장 직전에 원본 경로로 되돌린다.
	 *
	 * </pre>
	 *
	 * @param imageId
	 * @param param
	 * @return ContentImageVO (수정 후 재조회한 완성 URL 상태)
	 */
	ContentImageVO update(int imageId, ContentImageVO param);

	/**
	 *
	 * <pre>
	 * Method Name : delete
	 * Description : 이미지 삭제
	 *
	 * </pre>
	 *
	 * @param imageId
	 * @return void / 없으면 NoSuchElementException
	 */
	void delete(int imageId);

	// ── 용도별 이미지 URL 변환 ──
	// 크기는 전부 ContentImageServiceImpl이 정한다. 호출부는 용도만 말하고 크기를 넘기지 않는다.

	/**
	 *
	 * <pre>
	 * Method Name : toPosterUrl
	 * Description : 영화 포스터 URL
	 *
	 * </pre>
	 *
	 * @param path
	 * @return String(완성 URL)
	 */
	String toPosterUrl(String path);

	/**
	 *
	 * <pre>
	 * Method Name : toBackdropUrl
	 * Description : 영화 상세 헤더 배경 URL
	 *
	 * </pre>
	 *
	 * @param path
	 * @return String(완성 URL)
	 */
	String toBackdropUrl(String path);

	/**
	 *
	 * <pre>
	 * Method Name : toCreditProfileUrl
	 * Description : 크레딧 프로필 URL
	 *
	 * </pre>
	 *
	 * @param path
	 * @return String(완성 URL)
	 */
	String toCreditProfileUrl(String path);

	/**
	 *
	 * <pre>
	 * Method Name : toPersonProfileUrl
	 * Description : 인물 상세 프로필 URL
	 *
	 * </pre>
	 *
	 * @param path
	 * @return String(완성 URL)
	 */
	String toPersonProfileUrl(String path);

	/**
	 *
	 * <pre>
	 * Method Name : toStoredPath
	 * Description : 완성 URL을 DB에 저장할 원본 경로로 되돌린다. 위 변환들의 역변환
	 *               화면에서 받은 URL을 그대로 저장하면 완성 URL이 DB에 박히므로 쓰기 경로가 저장 직전에 부른다.
	 *               크기를 알려 주는 게 아니라 벗겨내는 쪽이라 "크기 지식을 가두는" 원칙에 걸리지 않는다.
	 *
	 * </pre>
	 *
	 * @param url
	 * @return String(TMDB 원본 경로)
	 */
	String toStoredPath(String url);

}
