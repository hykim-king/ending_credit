/**
 * AI 검색 전용 Mapper
 *
 * 기존 ContentMapper 는 제목만 훑는다. 여기서는 줄거리(overview)까지 본다.
 * 1조 매퍼를 건드리지 않으려고 따로 둔다.
 */
package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.endit.ai.dto.AiNoticeItem;
import com.endit.ai.dto.ContentEmbeddingVO;
import com.endit.ai.dto.AiSearchItem;
import com.endit.ai.dto.SearchIntent;

@Mapper
public interface AiSearchMapper {

	/**
	 * AI 가 뽑은 조건으로 영화를 찾는다.
	 *
	 * @param param 검색 의도
	 * @return 영화 목록
	 */
	List<AiSearchItem> doSearchByIntent(SearchIntent param);

	/**
	 * 공개 공지를 제목 낱말로 찾는다.
	 * 2조 공개 목록 SQL 에는 검색 조건이 없어서 여기 따로 둔다.
	 *
	 * @param param 검색 의도(keywords 사용)
	 * @return 공지 목록(중요 공지 우선, 최신순 5건)
	 */
	List<AiNoticeItem> doSearchNotices(SearchIntent param);

	/** 좌표가 없거나 다른 저울로 잰 영화들(적재 대상) */
	List<ContentEmbeddingVO> selectEmbedTargets(String model);

	/** 좌표 갱신(있을 때). 0건이면 insertEmbedding 으로 */
	int updateEmbedding(ContentEmbeddingVO param);

	/** 좌표 삽입(없을 때) */
	int insertEmbedding(ContentEmbeddingVO param);

	/** 이 저울로 잰 좌표 전부(뜻 검색용) */
	List<ContentEmbeddingVO> selectEmbeddings(String model);

	/** 제목으로 영화 번호 하나(비슷한 영화 찾기의 기준) */
	Long selectContentIdByTitle(String title);

	/** 번호 목록으로 카드 정보 - 순서는 자바가 거리순으로 다시 매긴다 */
	List<com.endit.ai.dto.AiSearchItem> selectItemsByIds(java.util.List<Long> ids);
}
