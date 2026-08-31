/**
 * 코멘트 좋아요 Service 인터페이스
 * 좋아요에는 목록/수정/단건조회가 없어 WorkDiv를 상속하지 않고
 * 필요한 메서드만 직접 선언한다(학원 UserService 선례).
 */
package com.endit.service;

import com.endit.domain.CommentLikeVO;

public interface CommentLikeService {

	// upToggleLike의 토글 결과 (매직넘버 해결 — 컨트롤러·테스트가 함께 쓰므로 인터페이스에 둔다)
	int LIKE_ON = 1;  // 이번 토글로 좋아요가 등록됨
	int LIKE_OFF = 0; // 이번 토글로 좋아요가 취소됨

	/**
	 *
	 * <pre>
	 * Method Name : doSave
	 * Description : 좋아요 등록
	 *
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
	 * Method Name : upToggleLike
	 * Description : 좋아요 토글 — 안 눌렀으면 등록, 눌렀으면 취소를 한 트랜잭션으로 조합
	 *               (조합/부수효과 메서드는 up~ 접두 — 학원 upDoSelectOneWithReadCnt 선례)
	 *
	 * </pre>
	 *
	 * @param param
	 * @return LIKE_ON(1: 등록됨)/LIKE_OFF(0: 취소됨)
	 */
	int upToggleLike(CommentLikeVO param);

}
