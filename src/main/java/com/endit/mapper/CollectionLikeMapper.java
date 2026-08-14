package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.endit.domain.CollectionLikeVO;

/**
 * <pre>
 * Class Name  : CollectionLikeMapper
 * Description : 컬렉션 좋아요의 등록, 삭제 및 조회 기능을 처리하는 Mapper
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13. gunwoo      최초 생성
 * 2026. 8. 14. jinyoung    주석 및 코드 형식 정리
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 13.
 */
@Mapper
public interface CollectionLikeMapper {

	/**
	 * 컬렉션 좋아요 등록
	 *
	 * @param vo 회원 번호와 컬렉션 번호
	 * @return 등록된 행 수
	 */
	int insertCollectionLike(CollectionLikeVO vo);

	/**
	 * 컬렉션 좋아요 삭제
	 *
	 * @param vo 회원 번호와 컬렉션 번호
	 * @return 삭제된 행 수
	 */
	int deleteCollectionLike(CollectionLikeVO vo);

	/**
	 * 회원 번호와 컬렉션 번호를 이용한 단건 조회
	 *
	 * @param vo 회원 번호와 컬렉션 번호
	 * @return 컬렉션 좋아요 정보, 조회 결과가 없으면 null
	 */
	CollectionLikeVO selectCollectionLike(CollectionLikeVO vo);

	/**
	 * 특정 회원이 좋아요를 누른 컬렉션 목록 조회
	 *
	 * @param memberId 회원 번호
	 * @return 회원별 컬렉션 좋아요 목록
	 */
	List<CollectionLikeVO> selectCollectionLikeListByMember(int memberId);

	/**
	 * 특정 컬렉션에 좋아요를 누른 회원 목록 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 컬렉션별 좋아요 회원 목록
	 */
	List<CollectionLikeVO> selectCollectionLikeListByCollection(int collectionId);

	/**
	 * 특정 컬렉션의 좋아요 개수 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 컬렉션 좋아요 개수
	 */
	int selectCollectionLikeCount(int collectionId);
}