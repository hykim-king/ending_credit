/**
 * <pre>
 * Class Name : UserCommentMapper
 * Description : 코멘트 Mapper
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 12.  홍선기   최초 생성
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 12.
 */
package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.WorkDiv;
import com.endit.domain.UserCommentVO;

@Mapper // MyBatis 매퍼 인터페이스임을 선언(구현체는 MyBatis가 자동 생성)
public interface UserCommentMapper extends WorkDiv<UserCommentVO> {

	/**
	 *
	 * <pre>
	 * Method Name : totalCnt
	 * Description : 코멘트 총건수
	 *
	 * </pre>
	 *
	 * @return int(총건수)
	 */
	int totalCnt();

	/**
	 *
	 * <pre>
	 * Method Name : deleteAll
	 * Description : JUnit전용: 모든 코멘트 삭제
	 *               (좋아요·신고는 FK ON DELETE CASCADE로 함께 삭제된다)
	 *               ⚠️ 로컬 개발 DB에서만 사용할 것
	 * </pre>
	 *
	 * @return int(삭제건수)
	 */
	int deleteAll();
}
