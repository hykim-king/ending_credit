package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.PersonLikeVO;

/**
 * <pre>
 * Class Name  : PersonLikeService
 * Description : 인물 좋아요 등록·해제와 회원별 좋아요 목록 조회 기능을 정의하는 Service
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * 2026. 9. 05. eunhu       인기 인물(좋아요 많은 순) 조회 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
public interface PersonLikeService {

	/**
	 * 회원이 좋아요한 인물 목록을 최신순으로 조회
	 *
	 * @param memberId 조회 대상 회원 번호
	 * @param param 페이징 조건과 전체 건수를 전달할 DTO
	 * @param sort 정렬 조건
	 * @return 회원이 좋아요한 인물 목록
	 */
	List<PersonLikeVO> retrieveLikes(int memberId, DTO param, String sort);

	/**
	 * 좋아요를 많이 받은 순으로 인기 인물 목록 조회
	 *
	 * 좋아요 수 내림차순이고, 동수이면 더 최근에 좋아요를 받은 인물을 위에 둔다.
	 * 그것마저 같으면 인물 번호 오름차순으로 순서를 고정한다.
	 * 좋아요가 하나도 없는 인물은 포함하지 않으므로 좋아요가 전혀 없으면 빈 목록이다.
	 * 집계 결과라 회원 번호·좋아요 일시는 채우지 않고, 프로필 이미지는 원본 경로 그대로 반환한다.
	 * 역할과 최근 참여작도 채우지 않는다 - 역할 표기는 호출부가 CONTENT_CREDIT에서 한 규칙으로 만든다.
	 *
	 * @param size 가져올 최대 건수(양수)
	 * @return 인기 인물 목록
	 */
	List<PersonLikeVO> retrievePopular(int size);

	/**
	 * 특정 인물을 좋아요한 전체 회원 수 조회
	 *
	 * @param personId 인물 번호
	 * @return 인물 좋아요 수
	 */
	int countLikes(int personId);

	/**
	 * 회원이 특정 인물을 좋아요했는지 확인
	 *
	 * @param memberId 회원 번호
	 * @param personId 인물 번호
	 * @return 좋아요가 존재하면 true, 존재하지 않으면 false
	 */
	boolean isLiked(int memberId, int personId);

	/**
	 * 회원의 인물 좋아요 등록
	 *
	 * @param memberId 로그인 회원 번호
	 * @param personId 인물 번호
	 * @return 등록되었거나 이미 존재하는 인물 좋아요 정보
	 */
	PersonLikeVO addLike(int memberId, int personId);

	/**
	 * 회원의 인물 좋아요 해제
	 *
	 * @param memberId 로그인 회원 번호
	 * @param personId 인물 번호
	 */
	void deleteLike(int memberId, int personId);
}