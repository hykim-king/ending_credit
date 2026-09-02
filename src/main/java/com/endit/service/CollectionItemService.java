package com.endit.service;

import java.util.List;
import java.util.OptionalLong;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;

/**
 * <pre>
 * Class Name  : CollectionItemService
 * Description : 컬렉션 작품의 목록 조회, 추가 및 삭제 기능을 정의하는 Service
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * 2026. 8. 29. jinyoung    조회 공개 범위와 변경 소유권 계약 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
public interface CollectionItemService {

	/**
	 * 컬렉션에 포함된 작품 목록 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @param param 페이징 조건
	 * @param currentMemberId 현재 회원 번호 또는 빈 값
	 * @return 컬렉션 작품 목록
	 */
	List<CollectionItemVO> retrieve(
			int collectionId, DTO param, OptionalLong currentMemberId);

	/**
	 * 컬렉션 작품 단건 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @param contentId 콘텐츠 번호
	 * @param currentMemberId 현재 회원 번호 또는 빈 값
	 * @return 컬렉션 작품 정보
	 */
	CollectionItemVO get(
			int collectionId, int contentId, OptionalLong currentMemberId);

	/**
	 * 컬렉션에 작품 추가
	 *
	 * @param memberId 요청 회원 번호
	 * @param collectionId 컬렉션 번호
	 * @param param 추가할 콘텐츠 정보
	 * @return 추가된 컬렉션 작품 정보
	 */
	CollectionItemVO create(
			long memberId, int collectionId, CollectionItemVO param);

	/**
	 * 컬렉션에서 작품 삭제
	 *
	 * @param memberId 요청 회원 번호
	 * @param collectionId 컬렉션 번호
	 * @param contentId 콘텐츠 번호
	 */
	void delete(long memberId, int collectionId, int contentId);
}
