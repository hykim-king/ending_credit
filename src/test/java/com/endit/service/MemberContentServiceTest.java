package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;
import com.endit.domain.MemberContentVO;
import com.endit.domain.MemberVO;
import com.endit.mapper.ContentMapper;
import com.endit.mapper.MemberContentMapper;
import com.endit.mapper.MemberMapper;

/**
 * <pre>
 * Class Name  : MemberContentServiceTest
 * Description : 실제 Spring Bean과 DB를 사용해 회원 콘텐츠 Service를 검증하는 통합 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@SpringBootTest
@Transactional
@DisplayName("MemberContentService 통합 테스트")
class MemberContentServiceTest {

	@Autowired
	private MemberContentService memberContentService;

	@Autowired
	private MemberContentMapper memberContentMapper;

	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private ContentMapper contentMapper;

	private int memberId;
	private int contentId;

	@BeforeEach
	void setUp() {
		memberId = createMemberId();
		contentId = createContentId("DEFAULT");
	}

	@Test
	@DisplayName("평가 목록 조회")
	void retrieveRatings() {

		// Given: 한 콘텐츠에는 별점을, 다른 콘텐츠에는 보고싶어요만 등록한다.
		memberContentService.saveRating(memberId, contentId, 4);
		
		int watchlistOnlyContentId = createContentId("WATCHLIST_ONLY");
		memberContentService.addWatchlist(memberId, watchlistOnlyContentId);

		DTO param = new DTO();

		// When: 회원의 최신 평가 목록을 조회한다.
		List<MemberContentVO> result = memberContentService.retrieveRatings(memberId, param, "latest");

		// Then: 별점이 있는 콘텐츠만 조회되고 콘텐츠 JOIN 및 페이징 정보가 설정되어야 한다.
		assertEquals(1, result.size());
		assertEquals(contentId, result.get(0).getContentId());
		assertEquals(Integer.valueOf(4), result.get(0).getRatingScore());
		assertNotNull(result.get(0).getTitleKo());

		assertEquals(1, param.getPageNo());
		assertEquals(12, param.getPageSize());
		assertEquals(1, param.getTotalCnt());
		assertEquals("50", param.getSearchDiv());
		assertEquals(String.valueOf(memberId), param.getSearchWord());
	}

	@Test
	@DisplayName("보고싶어요 목록 조회")
	void retrieveWatchlist() {

		// Given: 한 콘텐츠에는 보고싶어요를, 다른 콘텐츠에는 별점만 등록한다.
		memberContentService.addWatchlist(memberId, contentId);
		
		int ratingOnlyContentId = createContentId("RATING_ONLY");
		memberContentService.saveRating(memberId, ratingOnlyContentId, 3);

		DTO param = new DTO();

		// When: 회원의 최신 보고싶어요 목록을 조회한다.
		List<MemberContentVO> result = memberContentService.retrieveWatchlist(memberId, param, "latest");

		// Then: WATCHLIST Y인 콘텐츠만 조회되고 보고싶어요 검색 조건이 설정되어야 한다.
		assertEquals(1, result.size());
		assertEquals(contentId, result.get(0).getContentId());
		assertEquals("Y", result.get(0).getWatchlist());
		assertNotNull(result.get(0).getTitleKo());

		assertEquals(1, param.getPageNo());
		assertEquals(12, param.getPageSize());
		assertEquals(1, param.getTotalCnt());
		assertEquals("60", param.getSearchDiv());
		assertEquals(String.valueOf(memberId), param.getSearchWord());
	}

	@Test
	@DisplayName("첫 별점은 보고싶어요 N으로 저장")
	void saveFirstRating() {

		// When: 활동 기록이 없는 콘텐츠에 별점을 최초 저장한다.
		MemberContentVO result = memberContentService.saveRating(memberId, contentId, 4);

		// Then: 별점과 평가 일시는 생성되고 보고싶어요 상태는 N이어야 한다.
		assertEquals(memberId, result.getMemberId());
		assertEquals(contentId, result.getContentId());
		assertEquals(Integer.valueOf(4), result.getRatingScore());
		assertEquals("N", result.getWatchlist());
		assertNotNull(result.getRatedDt());
		assertNull(result.getWatchlistDt());
		assertNotNull(result.getUpdatedDt());
	}

	@Test
	@DisplayName("별점 변경 시 보고싶어요 유지")
	void updateRating() {

		// Given: 별점과 보고싶어요가 모두 등록된 행을 준비한다.
		memberContentService.addWatchlist(memberId, contentId);
		memberContentService.saveRating(memberId, contentId, 3);

		MemberContentVO beforeUpdate = selectMemberContent();
		String watchlistDt = beforeUpdate.getWatchlistDt();

		// When: 기존 별점을 다른 점수로 변경한다.
		MemberContentVO result = memberContentService.saveRating(memberId, contentId, 5);

		// Then: 별점만 변경되고 기존 보고싶어요 상태와 등록 일시는 유지되어야 한다.
		assertEquals(Integer.valueOf(5), result.getRatingScore());
		assertEquals("Y", result.getWatchlist());
		assertNotNull(result.getRatedDt());
		assertEquals(watchlistDt, result.getWatchlistDt());
	}

	@Test
	@DisplayName("보고싶어요가 있으면 별점만 해제")
	void deleteRatingKeepsRow() {

		// Given: 별점과 보고싶어요가 모두 등록된 행을 준비한다.
		memberContentService.addWatchlist(memberId, contentId);
		memberContentService.saveRating(memberId, contentId, 4);

		// When: 별점을 해제한다.
		memberContentService.deleteRating(memberId, contentId);
		MemberContentVO result = selectMemberContent();

		// Then: 행과 보고싶어요는 유지되고 별점 및 평가 일시는 제거되어야 한다.
		assertNotNull(result);
		assertNull(result.getRatingScore());
		assertEquals("Y", result.getWatchlist());
		assertNull(result.getRatedDt());
		assertNotNull(result.getWatchlistDt());
	}

	@Test
	@DisplayName("별점만 있으면 행 삭제")
	void deleteRatingDeletesRow() {

		// Given: 별점만 등록된 행을 준비한다.
		memberContentService.saveRating(memberId, contentId, 4);

		// When: 유일한 활동인 별점을 해제한다.
		memberContentService.deleteRating(memberId, contentId);

		// Then: 빈 회원 콘텐츠 행을 남기지 않아야 한다.
		assertNull(selectMemberContent());
	}

	@Test
	@DisplayName("보고싶어요 등록 시 별점 유지")
	void addWatchlist() throws InterruptedException {

		// Given: 별점만 등록된 행을 준비한다.
		MemberContentVO rating = memberContentService.saveRating(memberId, contentId, 3);
		String ratedDt = rating.getRatedDt();

		// Oracle DATE의 초 단위 비교로 평가 일시가 다시 저장되는 오류를 확인한다.
		Thread.sleep(1_100);

		// When: 같은 콘텐츠를 보고싶어요에 추가한다.
		MemberContentVO result = memberContentService.addWatchlist(memberId, contentId);

		// Then: 기존 별점과 평가 일시는 유지되고 보고싶어요 정보만 추가되어야 한다.
		assertEquals(Integer.valueOf(3), result.getRatingScore());
		assertEquals(ratedDt, result.getRatedDt());
		assertEquals("Y", result.getWatchlist());
		assertNotNull(result.getWatchlistDt());
	}

	@Test
	@DisplayName("별점이 있으면 보고싶어요만 해제")
	void deleteWatchlistKeepsRow() throws InterruptedException {

		// Given: 별점과 보고싶어요가 모두 등록된 행을 준비한다.
		memberContentService.saveRating(memberId, contentId, 5);
		memberContentService.addWatchlist(memberId, contentId);

		MemberContentVO beforeDelete = selectMemberContent();
		String ratedDt = beforeDelete.getRatedDt();

		// 보고싶어요 해제 시 평가 일시가 다시 저장되는 오류를 확인한다.
		Thread.sleep(1_100);

		// When: 보고싶어요를 해제한다.
		memberContentService.deleteWatchlist(memberId, contentId);
		MemberContentVO result = selectMemberContent();

		// Then: 별점은 유지되고 WATCHLIST와 WATCHLIST_DT만 해제되어야 한다.
		assertNotNull(result);
		assertEquals(Integer.valueOf(5), result.getRatingScore());
		assertEquals(ratedDt, result.getRatedDt());
		assertEquals("N", result.getWatchlist());
		assertNull(result.getWatchlistDt());
	}

	@Test
	@DisplayName("보고싶어요만 있으면 행 삭제")
	void deleteWatchlistDeletesRow() {

		// Given: 별점 없이 보고싶어요만 등록된 행을 준비한다.
		memberContentService.addWatchlist(memberId, contentId);

		// When: 보고싶어요를 해제한다.
		memberContentService.deleteWatchlist(memberId, contentId);

		// Then: 빈 활동 행이 DB에 남지 않아야 한다.
		assertNull(selectMemberContent());
	}

	@Test
	@DisplayName("보고싶어요 등록·해제 반복 요청 허용")
	void watchlistIdempotency() {

		// When: 같은 보고싶어요 등록 요청을 두 번 수행한다.
		MemberContentVO first = memberContentService.addWatchlist(memberId, contentId);
		MemberContentVO second = memberContentService.addWatchlist(memberId, contentId);

		// Then: 중복 행 없이 동일한 최종 Y 상태를 반환해야 한다.
		assertEquals(first.getMemberId(), second.getMemberId());
		assertEquals(first.getContentId(), second.getContentId());
		assertEquals(first.getWatchlistDt(), second.getWatchlistDt());
		assertEquals("Y", second.getWatchlist());

		// When, Then: 해제 요청을 반복해도 예외가 발생하지 않고 최종 행은 없어야 한다.
		memberContentService.deleteWatchlist(memberId, contentId);
		assertDoesNotThrow(() -> memberContentService.deleteWatchlist(memberId, contentId));
		assertNull(selectMemberContent());
	}

	@Test
	@DisplayName("별점 범위 검증")
	void validateRatingRange() {
		assertThrows(IllegalArgumentException.class,
				() -> memberContentService.saveRating(memberId, contentId, 0));

		assertThrows(IllegalArgumentException.class,
				() -> memberContentService.saveRating(memberId, contentId, 6));

		assertThrows(IllegalArgumentException.class,
				() -> memberContentService.saveRating(memberId, contentId, null));
	}

	@Test
	@DisplayName("정렬 조건 검증")
	void validateSort() {
		assertThrows(IllegalArgumentException.class,
				() -> memberContentService.retrieveRatings(memberId, new DTO(), "rating"));
	}

	@Test
	@DisplayName("평가가 없으면 빈 목록 반환")
	void retrieveEmptyRatings() {

		DTO param = new DTO();

		// When: 평가 기록이 없는 신규 회원의 평가 목록을 조회한다.
		List<MemberContentVO> result = memberContentService.retrieveRatings(memberId, param, "latest");

		// Then: 빈 목록과 전체 건수 0을 반환해야 한다.
		assertTrue(result.isEmpty());
		assertEquals(0, param.getTotalCnt());
	}

	/**
	 * 현재 테스트 회원과 콘텐츠의 복합 PK 조회
	 */
	private MemberContentVO selectMemberContent() {
		MemberContentVO key = new MemberContentVO();

		key.setMemberId(memberId);
		key.setContentId(contentId);

		return memberContentMapper.doSelectOne(key);
	}

	/**
	 * MEMBER_CONTENT 외래 키를 만족하는 테스트 회원 생성
	 */
	private int createMemberId() {
		String token = createToken();

		MemberVO member = new MemberVO();
		member.setEmail("member-content-" + token + "@test.local");
		member.setPassword("encoded-password");
		member.setNickname("회원콘텐츠" + token.substring(0, 6));
		member.setIntroduction("회원 콘텐츠 Service 통합 테스트 회원");
		member.setRole("USER");

		assertEquals(1, memberMapper.insertMember(member));
		assertNotNull(member.getMemberId());

		return member.getMemberId().intValue();
	}

	/**
	 * MEMBER_CONTENT 외래 키를 만족하는 테스트 콘텐츠 생성
	 */
	private int createContentId(String prefix) {
		String token = createToken();

		ContentVO content = new ContentVO(
				0,
				prefix + "_" + token,
				"회원 콘텐츠 통합 테스트 영화",
				"Member Content Integration Test",
				"회원 콘텐츠 Service 통합 테스트 영화",
				"2026-08-27",
				120,
				"Korea",
				"https://example.com/poster.jpg",
				"https://example.com/backdrop.jpg",
				null);

		assertEquals(1, contentMapper.doSave(content));

		return content.getContentId();
	}

	/**
	 * DB 고유 제약조건 충돌 방지용 문자열 생성
	 */
	private String createToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
