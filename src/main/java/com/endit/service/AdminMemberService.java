/**
 * 회원 관리 Service 인터페이스 (AD-07)
 * 2조 MemberMapper를 읽기 전용으로 재사용한다 — 회원 도메인은 2조 소유라
 * 매퍼를 새로 만들지 않고 조회·삭제 계약만 이 층에 둔다.
 */
package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.MemberVO;

public interface AdminMemberService {

	/**
	 *
	 * <pre>
	 * Method Name : doRetrieve
	 * Description : 회원 목록 (검색 email/nickname, 페이징)
	 *
	 * </pre>
	 *
	 * @param param 검색·페이징 조건
	 * @return List<MemberVO>
	 */
	List<MemberVO> doRetrieve(DTO param);

	/**
	 *
	 * <pre>
	 * Method Name : totalCnt
	 * Description : 검색조건이 걸린 회원 수
	 *
	 * </pre>
	 *
	 * @param param 검색조건
	 * @return int
	 */
	int totalCnt(DTO param);

	/**
	 *
	 * <pre>
	 * Method Name : doSelectOne
	 * Description : 회원 단건
	 *
	 * </pre>
	 *
	 * @param memberId 회원번호
	 * @return MemberVO(없으면 null)
	 */
	MemberVO doSelectOne(long memberId);

	/**
	 *
	 * <pre>
	 * Method Name : upWithdrawMember
	 * Description : 관리자 강퇴 — 회원 행을 삭제한다.
	 *               FK가 전부 ON DELETE CASCADE라 그 회원의 코멘트·좋아요·컬렉션·평가·공지까지
	 *               함께 사라진다. 되돌릴 수 없다.
	 *               MEMBER에 STATUS 컬럼이 없어 정지(SUSPENDED) 방식은 현재 스키마로 불가.
	 *
	 * </pre>
	 *
	 * @param memberId 회원번호
	 * @return int(삭제 건수)
	 */
	int upWithdrawMember(long memberId);
}
