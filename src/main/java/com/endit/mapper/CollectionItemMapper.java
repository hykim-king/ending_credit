package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;

/**
 * <pre>
 * Class Name  : CollectionItemMapper
 * Description : 컬렉션에 포함된 콘텐츠 정보의 등록, 조회 및 삭제를 처리하는 Mapper
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13.	jinyoung    최초 생성
 * 2026. 8. 14. jinyoung    전체 삭제 및 전체 건수 조회 기능 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 13.
 */
@Mapper
public interface CollectionItemMapper {

	/**
	 * 목록 조회
	 *
	 * @param param 검색 및 페이징 조건
	 * @return 컬렉션 콘텐츠 목록
	 */
	List<CollectionItemVO> doRetrieve(DTO param);

	/**
	 * 단건 삭제
	 *
	 * @param param 컬렉션 번호와 콘텐츠 번호
	 * @return 1(성공)/0(실패)
	 */
	int doDelete(CollectionItemVO param);

	/**
	 * 등록
	 *
	 * @param param 컬렉션 콘텐츠 정보
	 * @return 1(성공)/0(실패)
	 */
	int doSave(CollectionItemVO param);

	/**
	 * 단건 조회
	 *
	 * @param param 컬렉션 번호와 콘텐츠 번호
	 * @return 컬렉션 콘텐츠 정보
	 */
	CollectionItemVO doSelectOne(CollectionItemVO param);

	/**
	 * 전체 삭제
	 *
	 * @return 삭제된 행 수
	 */
	int deleteAll();

	/**
	 * 전체 건수 조회
	 *
	 * @return 컬렉션 콘텐츠 전체 건수
	 */
	int totalCnt();

	/**
	 * 검색 조건을 반영한 건수 조회
	 *
	 * @param param 검색 조건
	 * @return 검색된 컬렉션 콘텐츠 건수
	 */
	int count(DTO param);
}
