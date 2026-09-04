package com.endit.service;

import java.util.List;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;
import com.endit.domain.EnglishContentVO;

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

	/**
	 *
	 * <pre>
	 * Method Name : sync
	 * Description : TMDB 인기 목록을 훑어 우리 DB에 없는 영화를 limit건 새로 저장한다.
	 *               이미 있는 영화는 갱신하지 않고 건너뛴다.
	 *               성인등급 작품은 저장하지 않는다 - 비회원도 보는 화면이라 수집 단계에서 막으며,
	 *               등급을 담을 컬럼이 없어 표시 단계에서는 거를 수 없다. 정책을 바꾸려면 재적재해야 한다.
	 *               판정은 한국 우선이다 - KR 등급이 있으면 그것만 보고(19·18·19+·청소년관람불가·제한상영),
	 *               KR이 없을 때만 US를 봐서 R·NC-17·X·AO를 성인으로 친다.
	 *               둘을 OR로 묶지 않는 이유는 미국 R이 폭력·욕설만으로도 붙어 한국 판단을 덮기 때문이다.
	 *               한국·미국 둘 다 등급이 없으면 통과한다.
	 *               걸러진 건은 limit에 세지 않으므로 그만큼 다음 페이지에서 보충된다 -
	 *               즉 limit은 채워지되 훑는 페이지 수가 늘어난다.
	 *               스킵 사유별 건수는 마무리 로그에만 남는다(DB에 등급이 안 남기 때문).
	 *               limit이 0 이하면 기본값 100을 쓴다.
	 *               tmdb.api-key가 비어 있으면 IllegalStateException.
	 *
	 * </pre>
	 *
	 * @param limit
	 * @return int (실제로 새로 저장한 건수)
	 */
	int sync(int limit);

	// 상세페이지 헤더 조회 - CONTENT 단건(제목/원제/줄거리/개봉연도/러닝타임/국가/포스터/배경) 반환
	ContentVO get(int contentId);

	/**
	 *
	 * <pre>
	 * Method Name : getEnglishContent
	 * Description : TMDB에서 영문 줄거리와 이미지 경로를 받아 온다. 영어 화면(F-01)이 쓴다.
	 *               셋이 상세 응답 하나에서 함께 오므로 묶어서 돌려준다 -
	 *               따로 받으면 같은 호출을 두 번 하게 된다.
	 *               DB에 쓰지 않고 서비스 메모리에만 캐시하므로 재시작하면 비워진다 -
	 *               줄거리를 담을 영어 컬럼이 없고 스키마를 바꾸지 않기로 했기 때문이다.
	 *               한 번 받은 콘텐츠는 다시 부르지 않으며, 영문 값이 없는 콘텐츠도
	 *               "없음"으로 기억해 매번 호출하지 않는다.
	 *               TMDB 호출이 실패하면 캐시하지 않고 null을 준다 - 일시 장애를 굳히지 않기 위해서다.
	 *               호출부는 null이면 한국어 줄거리를 그대로 쓰면 된다.
	 *               외부 호출이 한 번 나가므로(실측 약 550ms) 목록이 아니라 단건 화면에서만 부른다.
	 *
	 * </pre>
	 *
	 * @param contentId 캐시 키
	 * @param externalId TMDB 영화 id
	 * @return EnglishContentVO (실패하면 null. 없는 항목은 빈 문자열이며 이미지는 TMDB 원본 경로다)
	 */
	EnglishContentVO getEnglishContent(int contentId, String externalId);

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
	 *                                   **popular은 WHERE를 타지 않는다.** 그래서 아래 필터 중
	 *                                   genreId 하나만 같이 쓸 수 있고(장르별 순위 목록이 따로 있다),
	 *                                   나머지가 섞이면 조용히 버리지 않고 boxoffice로 폴백한다.
	 *                                   released는 장르별 목록이 이미 개봉작만 담고 있어 폴백 사유가 아니다.
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

	/**
	 *
	 * <pre>
	 * Method Name : syncGenreRank
	 * Description : 장르마다 TMDB 인기순위를 받아 장르별 순위 목록을 갱신한다.
	 *               syncRank와 같이 DB에 쓰지 않으므로 재시작하면 비워진다.
	 *               /discover/movie?with_genres=..&amp;sort_by=popularity.desc를 장르 수만큼 부르므로
	 *               보유 장르가 19개면 호출이 최대 95회다(장르당 5페이지) - 기동 시에만 돌린다.
	 *               우리가 보유하지 않은 영화와 아직 개봉하지 않은 영화는 건너뛴다 -
	 *               개봉 판정은 우리 release_year로 하며 contentWhere의 released와 기준이 같다.
	 *               한 장르가 실패해도 나머지는 그대로 채운다.
	 *               한 장르도 못 채우면 기존 목록을 유지하고 0을 반환한다.
	 *               tmdb.api-key가 비어 있으면 IllegalStateException.
	 *
	 * </pre>
	 *
	 * @return int (순위가 하나라도 잡힌 장르 수)
	 */
	int syncGenreRank();

	/**
	 *
	 * <pre>
	 * Method Name : retrieveRank
	 * Description : 그 장르의 인기순위대로 정렬된 content_id 목록. 장르당 최대 50건이며 개봉작만 담긴다.
	 *               syncGenreRank 전이거나 그 장르에 매칭이 없으면 빈 목록이며,
	 *               이때 화면은 다른 장르를 고르거나 적재순으로 폴백한다.
	 *
	 * </pre>
	 *
	 * @param genreId
	 * @return List<Integer> (순위 오름차순, 수정 불가)
	 */
	List<Integer> retrieveRank(int genreId);

	// 외부 ID 중복 검사
	boolean hasExternalId(String externalId);

	// 콘텐츠 등록
	ContentVO create(ContentVO param);

}
