package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.cmn.DTO;
import com.endit.domain.PersonLikeVO;

/**
 * <pre>
 * Class Name  : PersonLikeMapper
 * Description : 회원의 인물 좋아요 정보의 등록, 조회 및 삭제를 처리하는 Mapper
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13.	jinyoung    최초 생성
 * 2026. 8. 14. jinyoung    전체 삭제 및 전체 건수 조회 기능 추가
 * 2026. 9. 05. eunhu       인기 인물(좋아요 많은 순) 조회 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 13.
 */
@Mapper
public interface PersonLikeMapper {

	/**
	 * 목록 조회
	 *
	 * @param param 검색 및 페이징 조건
	 * @return 인물 좋아요 목록
	 */
	List<PersonLikeVO> doRetrieve(DTO param);

	/**
	 * 좋아요를 많이 받은 순 인물 목록 조회
	 *
	 * 동수는 최근 좋아요 순, 그다음 인물 번호 오름차순으로 순서를 고정한다.
	 *
	 * @param size 가져올 최대 건수
	 * @return 인기 인물 목록(회원 번호·좋아요 일시·역할·최근 참여작은 채우지 않는다)
	 */
	List<PersonLikeVO> doRetrievePopular(@Param("size") int size);

	/**
	 * 단건 삭제
	 *
	 * @param param 회원 번호와 인물 번호
	 * @return 1(성공)/0(실패)
	 */
	int doDelete(PersonLikeVO param);

	/**
	 * 등록
	 *
	 * @param param 인물 좋아요 정보
	 * @return 1(성공)/0(실패)
	 */
	int doSave(PersonLikeVO param);

	/**
	 * 단건 조회
	 *
	 * @param param 회원 번호와 인물 번호
	 * @return 인물 좋아요 정보
	 */
	PersonLikeVO doSelectOne(PersonLikeVO param);

	/**
	 * 전체 삭제
	 *
	 * @return 삭제된 행 수
	 */
	int deleteAll();

	/**
	 * 전체 건수 조회
	 *
	 * @return 인물 좋아요 전체 건수
	 */
	int totalCnt();

	/**
	 * 검색 조건을 반영한 건수 조회
	 *
	 * @param param 검색 조건
	 * @return 검색된 인물 좋아요 건수
	 */
	int count(DTO param);
}
