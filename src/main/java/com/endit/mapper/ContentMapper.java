package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.cmn.WorkDiv;
import com.endit.domain.ContentVO;

@Mapper
public interface ContentMapper extends WorkDiv<ContentVO> {

	// 외부 API가 제공해주는 영화 ID로 이미 있는지 없는지 찾는 쿼리,,
	// 영화 테이블 select
	Integer findContentIdByExternal(@Param("externalId") String externalId);

	/**
	 *
	 * <pre>
	 * Method Name : doRetrieveByIds
	 * Description : content_id 묶음을 한 번에 조회한다.
	 *               결과 순서를 보장하지 않으므로 정렬은 호출부가 한다.
	 *               빈 목록을 넘기면 IN ()이 되어 SQL 문법 오류가 나므로 호출부가 먼저 걸러야 한다.
	 *
	 * </pre>
	 *
	 * @param contentIds
	 * @return List<ContentVO> (poster_url·backdrop_url은 DB 원본 경로)
	 */
	List<ContentVO> doRetrieveByIds(@Param("contentIds") List<Integer> contentIds);

	/**
	 *
	 * <pre>
	 * Method Name : doSelectExternalIdMap
	 * Description : 보유한 콘텐츠 전체의 external_id - content_id - release_year 대응을 뽑는다.
	 *               TMDB 목록을 우리 id로 옮길 때 건건이 findContentIdByExternal을 부르면
	 *               목록 길이만큼 쿼리가 늘어나므로, 통째로 한 번 읽어 메모리에서 맞춘다.
	 *               필터도 페이징도 없다 - 행 수가 CONTENT 전체와 같으니 대량 적재 뒤에는 비용을 다시 본다.
	 *
	 * </pre>
	 *
	 * @return List<ContentVO> (contentId·externalId·releaseYear만 채워진다)
	 */
	List<ContentVO> doSelectExternalIdMap();

	//테스트용
	int deleteAll();

	//테스트용
	int totalCnt();
}
