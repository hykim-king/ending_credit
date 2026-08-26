package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionVO;

/**
 * <pre>
 * Class Name  : CollectionService
 * Description : 컬렉션의 등록, 조회, 수정 및 삭제에 필요한 비즈니스 기능을 정의하는 Service
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 21. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 21.
 */
public interface CollectionService {

	/**
	 * 검색 및 페이징 조건을 반영한 컬렉션 목록 조회
	 *
	 * @param param 검색 및 페이징 조건
	 * @return 컬렉션 목록
	 */
	List<CollectionVO> retrieve(DTO param);

	/**
	 * 컬렉션 번호를 이용한 단건 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 컬렉션 정보
	 */
	CollectionVO get(int collectionId);

	/**
	 * 컬렉션 등록
	 *
	 * @param param 등록할 컬렉션 정보
	 * @return 등록된 컬렉션 정보
	 */
	CollectionVO create(CollectionVO param);

	/**
	 * 컬렉션 제목, 설명 및 공개 여부 수정
	 *
	 * @param collectionId 컬렉션 번호
	 * @param param 수정할 컬렉션 정보
	 * @return 수정된 컬렉션 정보
	 */
	CollectionVO update(int collectionId, CollectionVO param);

	/**
	 * 컬렉션 삭제
	 *
	 * @param collectionId 컬렉션 번호
	 */
	void delete(int collectionId);
}
