/**
 * <pre>
 * Class Name  : CollectionLikeMapper
 * Description : COLLECTION_LIKE 테이블에 대한 CRUD 처리를 담당하는 
 * 				 Mapper 인터페이스
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13.	gunwoo        최초생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 13.
 */
package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.endit.domain.CollectionLikeVO;

@Mapper // MyBatis 매퍼 인터페이스임을 선언(구현체는 MyBatis가 자동 생성)
public interface CollectionLikeMapper {

	/**
	 * 좋아요 등록 (회원이 컬렉션에 좋아요를 누름)
	 * @param vo memberId, collectionId 필수
	 * @return insert된 행 수
	 */
	public int insertCollectionLike(CollectionLikeVO vo);

	/**
	 * 좋아요 취소 (회원이 컬렉션 좋아요를 누른 것을 삭제)
	 * @param vo memberId, collectionId 필수
	 * @return delete된 행 수
	 */
	public int deleteCollectionLike(CollectionLikeVO vo);

	/**
	 * 특정 회원+컬렉션 조합의 좋아요 정보 단건 조회
	 * (좋아요를 눌렀는지 여부 확인 용도로도 사용 가능)
	 * @param vo memberId, collectionId 필수
	 * @return CollectionLikeVO, 없으면 null
	 */
	public CollectionLikeVO selectCollectionLike(CollectionLikeVO vo);

	/**
	 * 특정 회원이 좋아요 누른 컬렉션 목록 조회
	 * @param memberId
	 * @return CollectionLikeVO 목록
	 */
	public List<CollectionLikeVO> selectCollectionLikeListByMember(int memberId);

	/**
	 * 특정 컬렉션에 좋아요를 누른 회원 목록 조회
	 * @param collectionId
	 * @return CollectionLikeVO 목록
	 */
	public List<CollectionLikeVO> selectCollectionLikeListByCollection(int collectionId);

	/**
	 * 특정 컬렉션의 좋아요 총 개수 조회
	 * @param collectionId
	 * @return 좋아요 개수
	 */
	public int selectCollectionLikeCount(int collectionId);

}