package com.endit.service;

import java.util.List;
import java.util.OptionalLong;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionCreateRequest;
import com.endit.domain.CollectionUpdateRequest;
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
 * 2026. 8. 29. jinyoung    인증 회원·요청 DTO·전체 공개 목록·U-05·소유권 계약 추가
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
	 * @return 공개 컬렉션 목록
	 */
	List<CollectionVO> retrieve(DTO param);

	/**
	 * U-05 대상 회원의 컬렉션 목록 조회
	 *
	 * 대상 회원 본인이 조회하면 공개와 비공개를 모두 반환하고,
	 * 타인 또는 비회원이 조회하면 공개 컬렉션만 반환한다.
	 *
	 * @param memberId U-05 대상 회원 번호
	 * @param param 검색 및 페이징 조건
	 * @param currentMemberId 현재 회원 번호 또는 빈 값
	 * @return 접근 가능한 대상 회원의 컬렉션 목록
	 */
	List<CollectionVO> retrieveByMember(
			long memberId, DTO param, OptionalLong currentMemberId);

	/**
	 * 컬렉션 번호를 이용한 단건 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @param currentMemberId 현재 회원 번호 또는 빈 값
	 * @return 컬렉션 정보
	 */
	CollectionVO get(int collectionId, OptionalLong currentMemberId);

	/**
	 * 컬렉션 변경 권한을 확인하고 소유 컬렉션 반환
	 *
	 * @param collectionId 컬렉션 번호
	 * @param memberId 요청 회원 번호
	 * @return 소유 컬렉션 정보
	 */
	CollectionVO getOwned(int collectionId, long memberId);

	/**
	 * 컬렉션 등록
	 *
	 * @param memberId 작성 회원 번호
	 * @param request 등록할 컬렉션 정보
	 * @return 등록된 컬렉션 정보
	 */
	CollectionVO create(long memberId, CollectionCreateRequest request);

	/**
	 * 컬렉션 제목, 설명 및 포함 작품 전체 스냅샷 수정
	 *
	 * @param memberId 요청 회원 번호
	 * @param collectionId 컬렉션 번호
	 * @param request 수정할 컬렉션 정보
	 * @return 수정된 컬렉션 정보
	 */
	CollectionVO update(
			long memberId, int collectionId, CollectionUpdateRequest request);

	/**
	 * 컬렉션 삭제
	 *
	 * @param memberId 요청 회원 번호
	 * @param collectionId 컬렉션 번호
	 */
	void delete(long memberId, int collectionId);
}
