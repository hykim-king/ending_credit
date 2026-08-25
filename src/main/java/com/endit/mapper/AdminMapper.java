/**
 * <pre>
 * Class Name : AdminMapper
 * Description : ⚠️ 관리자 임시 조회 전용 Mapper (4조 데모용)
 *               대시보드 집계와 영화·인물·회원·공지 목록을 "읽기만" 한다.
 *               해당 도메인 담당 조(1·2조)가 관리 기능을 구현하면 이 임시 조회는 대체·삭제한다.
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 25.  홍선기   최초 생성 (임시)
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 25.
 */
package com.endit.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper // MyBatis 매퍼 인터페이스임을 선언(구현체는 MyBatis가 자동 생성)
public interface AdminMapper {

	/**
	 * 대시보드 집계 (테이블별 건수 + 미처리 신고 수)
	 *
	 * @return Map(키: 대문자 컬럼 별칭)
	 */
	Map<String, Object> getSummary();

	/**
	 * 영화 목록 (조회 전용)
	 *
	 * @return List<Map>
	 */
	List<Map<String, Object>> getContentList();

	/**
	 * 인물 목록 (조회 전용)
	 *
	 * @return List<Map>
	 */
	List<Map<String, Object>> getPersonList();

	/**
	 * 회원 목록 (조회 전용)
	 *
	 * @return List<Map>
	 */
	List<Map<String, Object>> getMemberList();

	/**
	 * 공지 목록 (조회 전용)
	 *
	 * @return List<Map>
	 */
	List<Map<String, Object>> getNoticeList();

}
