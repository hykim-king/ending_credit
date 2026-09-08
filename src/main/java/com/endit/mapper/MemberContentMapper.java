package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.DTO;
import com.endit.cmn.WorkDiv;
import com.endit.domain.GenrePreferenceVO;
import com.endit.domain.MemberContentVO;
import com.endit.domain.RatingDistributionVO;

/**
 * <pre>
 * Class Name  : MemberContentMapper
 * Description : 회원별 콘텐츠 평가 및 보고 싶어요 정보의 등록, 조회, 수정 및 삭제를 처리하는 Mapper
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13.	jinyoung    최초 생성
 * 2026. 8. 14. jinyoung    전체 삭제 및 전체 건수 조회 기능 추가
 * 2026. 9. 09. heetae		선호 장르 조회 기능 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 13.
 */
@Mapper
public interface MemberContentMapper extends WorkDiv<MemberContentVO> {

	/**
	 * 전체 삭제
	 *
	 * @return 삭제된 행 수
	 */
	int deleteAll();

	/**
	 * 전체 건수 조회
	 *
	 * @return 회원별 콘텐츠 전체 건수
	 */
	int totalCnt();

	/**
	 * 검색 조건을 반영한 건수 조회
	 *
	 * @param param 검색 조건
	 * @return 검색된 회원별 콘텐츠 건수
	 */
	int count(DTO param);
	
	/**
	 * 회원이 평가한 영화들의 장르별 개수 집계 (선호 장르 분석용).
	 * 평가 개수 내림차순 정렬. 1등이 선호 장르.
	 *
	 * @param param searchNumber에 회원 번호를 담아 전달
	 * @return 장르별 집계 목록 (genreName, ratedCnt)
	 */
	List<GenrePreferenceVO> selectGenrePreference(DTO param);
	
	/**
	 * 회원 별점 분포 조회
	 *
	 * @param param searchWord에 회원 번호를 담아 전달
	 * @return 별점(1~5)별 개수 목록
	 */
	List<RatingDistributionVO> selectRatingDistribution(DTO param);
}
