package com.endit.service;

import java.util.List;
import java.util.Set;

import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;

/** C-03 출연·제작진, P-01 필모그래피 서비스. 프로필·포스터는 완성 URL로 채워져 나온다 */
public interface ContentCreditService {

	/**
	 *
	 * <pre>
	 * Method Name : retrieve
	 * Description : 콘텐츠 하나의 출연·제작진 목록
	 *               역할 필터는 param.searchMap의 "role"로 넘긴다(DIRECTOR·ACTOR·WRITER·PRODUCER).
	 *               그 밖의 값이 오면 IllegalArgumentException.
	 *               넘긴 param에 totalCnt가 채워져 돌아온다.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param param
	 * @return List<ContentCreditVO> (프로필·포스터 완성 URL)
	 */
	List<ContentCreditVO> retrieve(int contentId, DTO param);

	/**
	 *
	 * <pre>
	 * Method Name : retrieveAll
	 * Description : 콘텐츠 하나의 출연·제작진 전체 목록
	 *               DIRECTOR > ACTOR > WRITER > PRODUCER 순 정렬.
	 *               "전체"는 실제로 100건 상한이며 초과분은 잘린다(상한 도달 시 log.warn만 남는다).
	 *               전체 건수가 필요하면 retrieve(int, DTO)를 직접 쓴다.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @return List<ContentCreditVO> (프로필·포스터 완성 URL)
	 */
	List<ContentCreditVO> retrieveAll(int contentId);

	/**
	 *
	 * <pre>
	 * Method Name : retrieveByPerson
	 * Description : 인물 하나의 참여 작품 목록
	 *               넘긴 param에 totalCnt가 채워져 돌아온다 - P-01 더보기가 이 값을 쓴다.
	 *
	 * </pre>
	 *
	 * @param personId
	 * @param param
	 * @return List<ContentCreditVO> (프로필·포스터 완성 URL)
	 */
	List<ContentCreditVO> retrieveByPerson(int personId, DTO param);

	/**
	 *
	 * <pre>
	 * Method Name : retrieveDirectorIds
	 * Description : 인물 여럿 중 감독 크레딧을 가진 인물의 id
	 *               목록 화면의 역할 표기용이다. 인물마다 retrieveByPerson을 부르면
	 *               목록 크기만큼 쿼리가 늘어난다.
	 *
	 * </pre>
	 *
	 * @param personIds
	 * @return Set<Integer> (감독 크레딧을 가진 personId만)
	 */
	Set<Integer> retrieveDirectorIds(List<Integer> personIds);

	/**
	 *
	 * <pre>
	 * Method Name : get
	 * Description : 크레딧 단건 조회
	 *
	 * </pre>
	 *
	 * @param creditId
	 * @return ContentCreditVO (완성 URL) / 없으면 NoSuchElementException
	 */
	ContentCreditVO get(int creditId);

	/**
	 *
	 * <pre>
	 * Method Name : create
	 * Description : 콘텐츠에 크레딧(배우 또는 감독) 등록
	 *               역할이 비었거나 네 역할 밖이면 IllegalArgumentException.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param param
	 * @return ContentCreditVO (채번된 creditId로 재조회한 상태)
	 */
	ContentCreditVO create(int contentId, ContentCreditVO param);

	/**
	 *
	 * <pre>
	 * Method Name : update
	 * Description : 크레딧의 배역명·표시순서 등 수정
	 *               안 채운 role은 기존 값을 유지한다(NOT NULL이라 비우면 제약 위반).
	 *               character·displayOrder는 "안 채움"과 "비움"을 구분할 수 없어 전면 대체다.
	 *
	 * </pre>
	 *
	 * @param creditId
	 * @param param
	 * @return ContentCreditVO (수정 후 재조회한 상태)
	 */
	ContentCreditVO update(int creditId, ContentCreditVO param);

	/**
	 *
	 * <pre>
	 * Method Name : delete
	 * Description : 크레딧 삭제
	 *
	 * </pre>
	 *
	 * @param creditId
	 * @return void / 없으면 NoSuchElementException
	 */
	void delete(int creditId);

}
