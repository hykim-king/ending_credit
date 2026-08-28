package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionLikeItemVO;
import com.endit.domain.CollectionLikeVO;

/**
 * <pre>
 * Class Name  : CollectionLikeService
 * Description : 컬렉션 좋아요의 등록, 취소, 조회에 필요한 비즈니스 기능을 정의하는 Service
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. gunwoo      최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 26.
 */
public interface CollectionLikeService {

	/**
	 * 컬렉션 좋아요 등록
	 *
	 * @param memberId 회원 번호
	 * @param collectionId 컬렉션 번호
	 * @return 등록된 컬렉션 좋아요 정보
	 */
	CollectionLikeVO create(int memberId, int collectionId);

	/**
	 * 컬렉션 좋아요 취소
	 *
	 * @param memberId 회원 번호
	 * @param collectionId 컬렉션 번호
	 */
	void delete(int memberId, int collectionId);

	/**
	 * 회원의 특정 컬렉션에 대한 좋아요 정보 조회
	 *
	 * @param memberId 회원 번호
	 * @param collectionId 컬렉션 번호
	 * @return 컬렉션 좋아요 정보
	 */
	CollectionLikeVO get(int memberId, int collectionId);

	/**
	 * 특정 회원이 좋아요를 누른 컬렉션 목록 조회 (화면 표시용, COLLECTION JOIN + 페이징)
	 *
	 * @param memberId 회원 번호
	 * @param param 페이징 조건
	 * @return 좋아요한 컬렉션 목록
	 */
	List<CollectionLikeItemVO> retrieveByMember(int memberId, DTO param);

	/**
	 * 특정 컬렉션에 좋아요를 누른 회원 목록 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 컬렉션별 좋아요 회원 목록
	 */
	List<CollectionLikeVO> retrieveByCollection(int collectionId);

	/**
	 * 특정 컬렉션의 좋아요 개수 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 컬렉션 좋아요 개수
	 */
	int countByCollection(int collectionId);
}
