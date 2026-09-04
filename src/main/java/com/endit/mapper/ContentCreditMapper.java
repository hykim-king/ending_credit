package com.endit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.cmn.WorkDiv;
import com.endit.domain.ContentCreditVO;

@Mapper
public interface ContentCreditMapper extends WorkDiv<ContentCreditVO> {

	// 인물 여럿 중 해당 역할의 크레딧을 가진 person_id만 추린다.
	// 목록 화면이 인물마다 따로 조회하지 않도록 한 번에 받는다
	List<Integer> doSelectPersonIdsByRole(
			@Param("personIds") List<Integer> personIds,
			@Param("role") String role);

	/**
	 *
	 * <pre>
	 * Method Name : doSelectTopPersonByRole
	 * Description : contentIds 안에서 해당 역할 크레딧이 가장 많은 인물 1명.
	 *               모수를 호출부가 정하는 이유는 "화제성"의 기준이 화면마다 다르기 때문이다 -
	 *               홈 큐레이션은 박스오피스 순위 500편을 넘긴다(ContentCreditServiceImpl이 그 상한으로 자른다).
	 *               참여 편수가 같으면 더 앞 순위 작품에 참여한 쪽(MIN(content_id))이 이기고,
	 *               그래도 같으면 person_id가 작은 쪽으로 깬다 - 같은 데이터면 항상 같은 결과여야 한다.
	 *               채워지는 필드는 personId·nameKo·nameOrg 셋뿐이라 전용 VO를 두지 않고 ContentCreditVO를 쓴다.
	 *               nameOrg까지 뽑는 것은 영어 화면이 원어 이름을 쓰기 때문이다(F-01).
	 *               빈 목록을 넘기면 IN ()이 되어 SQL 문법 오류가 나므로 호출부가 먼저 걸러야 한다.
	 *
	 * </pre>
	 *
	 * @param role
	 * @param contentIds
	 * @return ContentCreditVO (해당 역할 크레딧이 없으면 null)
	 */
	ContentCreditVO doSelectTopPersonByRole(
			@Param("role") String role,
			@Param("contentIds") List<Integer> contentIds);

	//테스트용
	int deleteAll();

	//테스트용
	int totalCnt();
}
