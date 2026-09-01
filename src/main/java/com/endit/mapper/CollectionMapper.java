package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.DTO;
import com.endit.cmn.WorkDiv;
import com.endit.domain.CollectionQueryParam;
import com.endit.domain.CollectionVO;

/**
 * <pre>
 * Class Name  : CollectionMapper
 * Description : 컬렉션 정보의 등록, 조회, 수정 및 삭제를 처리하는 Mapper
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13.	jinyoung    최초 생성
 * 2026. 8. 14. jinyoung    전체 삭제 및 전체 건수 조회 기능 추가
 * 2026. 8. 29. jinyoung    전체 공개 목록 및 U-05 공개 범위 목록·건수 조회 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 13.
 */
@Mapper
public interface CollectionMapper extends WorkDiv<CollectionVO> {

	/**
	 * 전체 삭제
	 *
	 * @return 삭제된 행 수
	 */
	int deleteAll();

	/**
	 * 전체 건수 조회
	 *
	 * @return 컬렉션 전체 건수
	 */
	int totalCnt();

	/**
	 * 검색 조건을 반영한 건수 조회
	 *
	 * @param param 검색 조건
	 * @return 검색된 컬렉션 건수
	 */
	int count(DTO param);

	/** 전체 공개 목록 또는 U-05 대상 회원의 접근 가능한 컬렉션 목록 조회 */
	List<CollectionVO> retrieveVisible(CollectionQueryParam param);

	/** 전체 목록 또는 U-05의 공개 범위를 반영한 컬렉션 건수 조회 */
	int countVisible(CollectionQueryParam param);
}
