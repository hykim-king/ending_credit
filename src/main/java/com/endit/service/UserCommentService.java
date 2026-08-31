/**
 * 코멘트 Service 인터페이스
 * CRUD 계약은 WorkDiv를 그대로 상속한다(학원 표준).
 */
package com.endit.service;

import com.endit.cmn.WorkDiv;
import com.endit.domain.UserCommentVO;

public interface UserCommentService extends WorkDiv<UserCommentVO> {

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
	 * Method Name : getContentTitle
	 * Description : 코멘트 화면(C-04) 헤더용 영화 제목
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @return String(없으면 null)
	 */
	String getContentTitle(long contentId);

	/**
	 *
	 * <pre>
	 * Method Name : getCollectionTitle
	 * Description : 컬렉션 코멘트 화면(D-07) 헤더용 컬렉션 제목
	 *
	 * </pre>
	 *
	 * @param collectionId
	 * @return String(없으면 null)
	 */
	String getCollectionTitle(long collectionId);

}
