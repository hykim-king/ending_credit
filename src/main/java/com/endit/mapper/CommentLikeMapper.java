/**
 * <pre>
 * Class Name : CommentLikeMapper
 * Description : 코멘트 좋아요 Mapper
 *               좋아요에 실제 필요한 등록, 삭제, 확인 및 집계 기능만 제공한다.
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 12.  홍선기   최초 생성
 * 2026. 8. 14.  홍선기   deleteAll 제거(테스트가 @Transactional 롤백 방식으로 바뀌어 미사용·전체삭제 위험만 남음)
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
	 * 코멘트 좋아요 등록
	 *
	 * @param param 회원 번호와 코멘트 번호
	 * @return 등록된 행 수
	 */
	int doSave(CommentLikeVO param);

	/**
	 * 코멘트 좋아요 취소
	 *
	 * @param param 회원 번호와 코멘트 번호
	 * @return 삭제된 행 수
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
}
