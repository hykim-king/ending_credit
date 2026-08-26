package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionItemVO;

/**
 * <pre>
 * Class Name  : CollectionItemService
 * Description : 컬렉션 작품의 목록 조회, 추가 및 삭제 기능을 정의하는 Service
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
public interface CollectionItemService {

	/**
	 * 컬렉션에 포함된 작품 목록 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @param param 페이징 조건
	 * @return 컬렉션 작품 목록
	 */
	List<CollectionItemVO> retrieve(int collectionId, DTO param);

	/**
	 * 컬렉션 작품 단건 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @param contentId 콘텐츠 번호
	 * @return 컬렉션 작품 정보
	 */
	CollectionItemVO get(int collectionId, int contentId);

	/**
	 * 컬렉션에 작품 추가
	 *
	 * @param collectionId 컬렉션 번호
	 * @param param 추가할 콘텐츠 정보
	 * @return 추가된 컬렉션 작품 정보
	 */
	CollectionItemVO create(int collectionId, CollectionItemVO param);

	/**
	 * 컬렉션에서 작품 삭제
	 *
	 * @param collectionId 컬렉션 번호
	 * @param contentId 콘텐츠 번호
	 */
	void delete(int collectionId, int contentId);
}
