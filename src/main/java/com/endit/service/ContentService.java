package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;

/**
 * <pre>
 * Class Name  : ContentService
 * Description : 콘텐츠 조회 및 TMDB 콘텐츠 적재 기능을 정의하는 Service
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 29. jinyoung    컬렉션 작품 선택용 콘텐츠 조회 계약 추가
 * 2026. 8. 31. jinyoung    영화 상세용 sync·get 계약과 통합
 * ------------------------------------------------------------
 * </pre>
 */
public interface ContentService {

	// TMDB 인기 목록을 limit건까지 보고, 우리 db에 없는 영화만 저장.
	// 신규 저장 건수 반환 -> 이미 있는 영화는 업데이트 하지 않는다(sync에서 영화 아이디 받아와서, 우리 db 에 있는지
	// 확인하고, 있으면 무시, 없으면 상세조회해서 컨텐츠 및 하위테이블 채워넣기함.
	int sync(int limit);

	// 상세페이지 헤더 조회 - CONTENT 단건(제목/원제/줄거리/개봉연도/러닝타임/국가/포스터/배경) 반환
	ContentVO get(int contentId);

	/**
	 * <pre>
	 * Method Name : retrieve
	 * Description : 검색+페이징 목록 조회. 전체 건수는 param.totalCnt에 실어 돌려준다.
	 *               포스터·배경 URL은 완성된 풀 URL이다.
	 *               searchMap을 통로로 두 가지를 더 받는다.
	 *               searchMap["sort"] - latest(개봉일) / boxoffice(적재순) / relevance(관련도, 검색어 필요)
	 *                                   / registered(적재 시각) / popular(TMDB 실시간 순위).
	 *                                   그 밖의 값은 IllegalArgumentException.
	 *                                   popular만 매퍼가 아니라 서비스가 정렬하며, 검색어가 있거나
	 *                                   순위가 비어 있으면 boxoffice로 폴백한다.
	 *                                   **popular은 WHERE를 타지 않으므로 아래 필터와 같이 못 쓴다.**
	 *               searchMap["released"] - "Y"면 이미 개봉한 것(release_year <= SYSDATE)만.
	 *                                       개봉일 미상도 빠진다. 안 넣으면 전부 조회한다.
	 *               searchMap["genreId"] - 그 장르에 걸린 콘텐츠만. GENRE.genre_id 값이다.
	 *               searchMap["personId"] - 그 인물이 참여한 콘텐츠만. PERSON.person_id 값이다.
	 *               searchMap["personRole"] - personId와 함께 줄 때만 뜻이 있다. POL-033 4종 중 하나로
	 *                                         참여 역할을 좁힌다. 안 주면 역할 구분 없이 참여작 전부.
	 *               searchMap["decade"] - "1990"이면 1990-01-01 이상 2000-01-01 미만 개봉작.
	 *                                     네 자리 연도 문자열이며 연대 시작 연도를 준다.
	 *               param이 null이면 IllegalArgumentException.
	 * </pre>
	 * @param param
	 * @return List<ContentVO> (없으면 빈 목록)
	 */
	List<ContentVO> retrieve(DTO param);

	/**
	 *
	 * <pre>
	 * Method Name : syncRank
	 * Description : TMDB 인기순위를 받아 순위 목록을 갱신한다.
	 *               DB에 쓰지 않고 서비스 메모리의 목록만 통째로 교체하므로, 재시작하면 비워진다.
	 *               우리가 보유하지 않은 영화는 건너뛴다 - 여기서 새로 적재하지 않는다.
	 *               매칭이 한 건도 없으면 기존 순위를 유지하고 0을 반환한다.
	 *               TMDB 호출이 중간에 실패해도 예외를 던지지 않고 수집분까지만 반영한다.
	 *               tmdb.api-key가 비어 있으면 IllegalStateException.
	 *
	 * </pre>
	 *
	 * @return int (우리 DB와 매칭된 건수)
	 */
	int syncRank();

	/**
	 *
	 * <pre>
	 * Method Name : retrieveRank
	 * Description : 인기순위대로 정렬된 content_id 목록. 최대 100건이다.
	 *               syncRank 전이거나 매칭이 0건이면 빈 목록이며, 이때 화면은 적재순으로 폴백한다.
	 *
	 * </pre>
	 *
	 * @return List<Integer> (순위 오름차순, 수정 불가)
	 */
	List<Integer> retrieveRank();

	// 외부 ID 중복 검사
	boolean hasExternalId(String externalId);

	// 콘텐츠 등록
	ContentVO create(ContentVO param);

}
