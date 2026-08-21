package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.DTO;
import com.endit.cmn.WorkDiv;
import com.endit.domain.MemberContentVO;

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
}
