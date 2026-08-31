/**
 * <pre>
 * Class Name : UserCommentMapper
 * Description : 코멘트 Mapper
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
	 * Method Name : getContentTitle
	 * Description : 코멘트 화면(C-04) 헤더용 영화 제목 (한글명 없으면 원제)
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
