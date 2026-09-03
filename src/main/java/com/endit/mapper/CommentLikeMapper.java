/**
 * 코멘트 좋아요 Mapper
 * 학원 표준대로 모든 Mapper는 WorkDiv를 상속한다.
 * XML에는 좋아요에 실제 쓰는 것만 구현한다(doSave/doDelete + 확인/집계)
 * — 구현 안 된 메서드는 호출할 때만 오류가 나므로 문제없다(TimeMapper 방식).
 */
package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.WorkDiv;
import com.endit.domain.CommentLikeVO;

@Mapper // MyBatis 매퍼 인터페이스임을 선언(구현체는 MyBatis가 자동 생성)
public interface CommentLikeMapper extends WorkDiv<CommentLikeVO> {
	// doSave   : 좋아요 등록 — 같은 코멘트에 두 번 누르면 PK(MEMBER_ID, COMMENT_ID) 위반으로 실패
	// doDelete : 좋아요 취소
	// (WorkDiv 상속 — 목록/수정/단건조회는 좋아요에 없어서 XML에 구현하지 않는다)

	/**
	 *
	 * <pre>
	 * Method Name : likeCheck
	 * Description : 이 회원이 이 코멘트에 좋아요를 눌렀는지 확인
	 *
	 * </pre>
	 *
	 * @param param
	 * @return 1(눌렀음)/0(안 눌렀음)
	 */
	int likeCheck(CommentLikeVO param);

	/**
	 *
	 * <pre>
	 * Method Name : getLikeCnt
	 * Description : 코멘트 하나의 좋아요 수
	 *
	 * </pre>
	 *
	 * @param commentId
	 * @return int(좋아요 수)
	 */
	int getLikeCnt(long commentId);

	/**
	 *
	 * <pre>
	 * Method Name : totalCnt
	 * Description : 좋아요 총건수
	 *
	 * </pre>
	 *
	 * @return int(총건수)
	 */
	int totalCnt();
}
