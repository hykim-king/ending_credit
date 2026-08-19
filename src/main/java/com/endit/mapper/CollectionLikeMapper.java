package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.domain.CollectionLikeItemVO;
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
 * 2026. 8. 14. jinyoung    전체 삭제 및 전체 건수 조회 기능 추가
 * 2026. 8. 18. gunwoo      회원별 좋아요 컬렉션 목록에 페이징 + COLLECTION JOIN 추가
 *                          (selectCollectionLikeListByCollection은 화면에 회원목록 UI가
 *                           없어 페이징 미적용, 기존 count 방식 유지)
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
	 * 특정 회원이 좋아요를 누른 컬렉션 목록 조회 (키 값만, 페이징 없음)
	 *
	 * @param memberId 회원 번호
	 * @return 회원별 컬렉션 좋아요 목록
	 */
	List<CollectionLikeVO> selectCollectionLikeListByMember(int memberId);

	/**
	 * 특정 회원이 좋아요를 누른 컬렉션 목록 조회 (화면 표시용, COLLECTION JOIN + 페이징)
	 * 프로필 화면의 "좋아요한 컬렉션" 목록에서 사용
	 *
	 * @param memberId 회원 번호
	 * @param pageNo   페이지 번호 (1부터 시작)
	 * @param pageSize 페이지당 건수
	 * @return 화면 표시용 컬렉션 좋아요 목록
	 */
	List<CollectionLikeItemVO> selectLikedCollectionListByMember(
			@Param("memberId") int memberId,
			@Param("pageNo") int pageNo,
			@Param("pageSize") int pageSize);

	/**
	 * 특정 회원이 좋아요를 누른 컬렉션 전체 건수 조회 (페이징 total count용)
	 *
	 * @param memberId 회원 번호
	 * @return 전체 건수
	 */
	int selectLikedCollectionCountByMember(int memberId);

	/**
	 * 특정 컬렉션에 좋아요를 누른 회원 목록 조회
	 * (화면에 회원 목록을 보여주는 UI가 없어 페이징 미적용, 단순 카운트 표시 용도)
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

	/**
	 * 전체 삭제
	 *
	 * @return 삭제된 행 수
	 */
	int deleteAll();

	/**
	 * 전체 건수 조회
	 *
	 * @return 컬렉션 좋아요 전체 건수
	 */
	int totalCnt();
}