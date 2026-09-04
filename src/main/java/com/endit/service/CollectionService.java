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
 * 2026. 9. 02. jinyoung    전체 목록에 현재 회원의 비공개 컬렉션 노출
 * 2026. 9. 03. jinyoung    회원별 컬렉션 및 공개 범위 건수 조회 추가
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
	 * 검색 및 페이징 조건과 현재 회원을 반영한 컬렉션 목록 조회
	 *
	 * 로그인 회원에게는 공개 컬렉션과 본인이 작성한 비공개 컬렉션을 반환한다.
	 *
	 * @param param 검색 및 페이징 조건
	 * @param currentMemberId 현재 회원 번호 또는 빈 값
	 * @return 현재 회원이 접근할 수 있는 컬렉션 목록
	 */
	List<CollectionVO> retrieve(DTO param, OptionalLong currentMemberId);

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
	 * 회원이 작성한 컬렉션 전체 건수 조회
	 *
	 * 본인 프로필에서 사용하는 집계로,
	 * 공개 여부와 관계없이 공개·비공개 컬렉션을 모두 포함한다.
	 *
	 * @param memberId 조회 대상 회원 번호
	 * @return 회원이 작성한 전체 컬렉션 건수
	 */
	int countByMember(int memberId);

	/**
	 * 조회자가 접근할 수 있는 대상 회원의 컬렉션 건수 조회
	 *
	 * 조회자와 대상 회원이 같으면 공개·비공개 컬렉션을 모두 포함한다.
	 * 조회자가 다른 회원이거나 비회원이면 공개 컬렉션만 포함한다.
	 *
	 * @param targetMemberId 프로필 조회 대상 회원 번호
	 * @param currentMemberId 현재 조회 회원 번호 또는 비회원인 경우 null
	 * @return 조회자가 접근할 수 있는 대상 회원의 컬렉션 건수
	 */
	int countVisibleByMember(
			int targetMemberId,
			Long currentMemberId);

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
