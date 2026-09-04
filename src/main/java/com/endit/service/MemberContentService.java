package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.MemberContentVO;

/**
 * <pre>
 * Class Name  : MemberContentService
 * Description : 회원별 콘텐츠 평가와 보고싶어요 기록 기능을 정의하는 Service
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * 2026. 9. 03. jinyoung    회원별 평가·보고싶어요 건수 조회 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
public interface MemberContentService {

	/**
	 * 회원의 평가 콘텐츠 목록 조회
	 *
	 * @param memberId 회원 번호
	 * @param param 페이징 조건
	 * @param sort 정렬 조건
	 * @return 평가 콘텐츠 목록
	 */
	List<MemberContentVO> retrieveRatings(
			int memberId,
			DTO param,
			String sort);

	/**
	 * 회원의 보고싶어요 콘텐츠 목록 조회
	 *
	 * @param memberId 회원 번호
	 * @param param 페이징 조건
	 * @param sort 정렬 조건
	 * @return 보고싶어요 콘텐츠 목록
	 */
	List<MemberContentVO> retrieveWatchlist(
			int memberId,
			DTO param,
			String sort);

	/**
	 * 회원이 평가한 콘텐츠 전체 건수 조회
	 *
	 * 평가 점수(RATING_SCORE)가 등록된 콘텐츠만 집계한다.
	 *
	 * @param memberId 조회 대상 회원 번호
	 * @return 회원이 평가한 콘텐츠 건수
	 */
	int countRatingByMember(int memberId);

	/**
	 * 회원이 보고싶어요로 등록한 콘텐츠 전체 건수 조회
	 *
	 * 보고싶어요 상태(WATCHLIST)가 Y인 콘텐츠만 집계한다.
	 *
	 * @param memberId 조회 대상 회원 번호
	 * @return 회원의 보고싶어요 콘텐츠 건수
	 */
	int countWatchlistByMember(int memberId);

	/**
	 * 별점 등록 또는 변경
	 *
	 * @param memberId 로그인 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @param ratingScore 별점
	 * @return 저장된 회원 콘텐츠 기록
	 */
	MemberContentVO saveRating(
			int memberId,
			int contentId,
			Integer ratingScore);

	/**
	 * 별점 해제
	 *
	 * @param memberId 로그인 회원 번호
	 * @param contentId 콘텐츠 번호
	 */
	void deleteRating(
			int memberId,
			int contentId);

	/**
	 * 보고싶어요 등록
	 *
	 * @param memberId 로그인 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @return 저장된 회원 콘텐츠 기록
	 */
	MemberContentVO addWatchlist(
			int memberId,
			int contentId);

	/**
	 * 보고싶어요 해제
	 *
	 * @param memberId 로그인 회원 번호
	 * @param contentId 콘텐츠 번호
	 */
	void deleteWatchlist(
			int memberId,
			int contentId);
}