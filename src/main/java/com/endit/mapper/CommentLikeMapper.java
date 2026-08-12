/**
 * <pre>
 * Class Name : CommentLikeMapper
 * Description : 코멘트 좋아요 Mapper
 *               좋아요는 등록/취소/확인/집계만 있어 WorkDiv(CRUD 5종)를 쓰지 않는다.
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

import com.endit.domain.CommentLikeVO;

@Mapper // MyBatis 매퍼 인터페이스임을 선언(구현체는 MyBatis가 자동 생성)
public interface CommentLikeMapper {

	/**
	 *
	 * <pre>
	 * Method Name : doSave
	 * Description : 좋아요 등록
	 *               같은 코멘트에 두 번 누르면 PK(MEMBER_ID, COMMENT_ID) 위반으로 실패한다.
	 * </pre>
	 *
	 * @param param
	 * @return 1(성공)/0(실패)
	 */
	int doSave(CommentLikeVO param);

	/**
	 *
	 * <pre>
	 * Method Name : doDelete
	 * Description : 좋아요 취소
	 *
	 * </pre>
	 *
	 * @param param
	 * @return 1(성공)/0(실패)
	 */
	int doDelete(CommentLikeVO param);

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

	/**
	 *
	 * <pre>
	 * Method Name : deleteAll
	 * Description : JUnit전용: 모든 좋아요 삭제
	 *               ⚠️ 로컬 개발 DB에서만 사용할 것
	 * </pre>
	 *
	 * @return int(삭제건수)
	 */
	int deleteAll();
}
