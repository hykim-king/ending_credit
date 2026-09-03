package com.endit.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.MemberContentVO;
import com.endit.mapper.MemberContentMapper;
import com.endit.service.MemberContentService;

/**
 * <pre>
 * Class Name  : MemberContentServiceImpl
 * Description : 회원별 콘텐츠 평가와 보고싶어요 상태 전환 및 목록 조회를 처리하는 Service 구현체
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * 2026. 9. 03. jinyoung    회원별 평가·보고싶어요 건수 조회 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@Service
@Transactional(readOnly = true)
public class MemberContentServiceImpl implements MemberContentService {

	private static final String WATCHLIST_YES = "Y";
	private static final String WATCHLIST_NO = "N";

	private static final String SEARCH_MEMBER_RATINGS = "50";
	private static final String SEARCH_MEMBER_WATCHLIST = "60";

	private static final String SORT_LATEST = "latest";

	private final MemberContentMapper memberContentMapper;

	/**
	 * MemberContentMapper를 주입받아 Service 구현체 생성
	 *
	 * @param memberContentMapper 회원 콘텐츠 Mapper
	 */
	public MemberContentServiceImpl(
			MemberContentMapper memberContentMapper) {

		this.memberContentMapper = memberContentMapper;
	}

	@Override
	public List<MemberContentVO> retrieveRatings(
			int memberId,
			DTO param,
			String sort) {

		return retrieve(
				memberId,
				param,
				sort,
				SEARCH_MEMBER_RATINGS);
	}

	@Override
	public List<MemberContentVO> retrieveWatchlist(
			int memberId,
			DTO param,
			String sort) {

		return retrieve(
				memberId,
				param,
				sort,
				SEARCH_MEMBER_WATCHLIST);
	}

	/**
	 * 회원이 평가한 콘텐츠 전체 건수 조회
	 *
	 * MemberContentMapper의 searchDiv 50 조건은
	 * 해당 회원의 RATING_SCORE가 null이 아닌 행만 집계한다.
	 */
	@Override
	public int countRatingByMember(int memberId) {

		validateMemberId(memberId);

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_MEMBER_RATINGS);
		param.setSearchWord(String.valueOf(memberId));

		return memberContentMapper.count(param);
	}

	/**
	 * 회원이 보고싶어요로 등록한 콘텐츠 전체 건수 조회
	 *
	 * MemberContentMapper의 searchDiv 60 조건은
	 * 해당 회원의 WATCHLIST가 Y인 행만 집계한다.
	 */
	@Override
	public int countWatchlistByMember(int memberId) {

		validateMemberId(memberId);

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_MEMBER_WATCHLIST);
		param.setSearchWord(String.valueOf(memberId));

		return memberContentMapper.count(param);
	}

	@Override
	@Transactional
	public MemberContentVO saveRating(
			int memberId,
			int contentId,
			Integer ratingScore) {

		validateRatingScore(ratingScore);

		MemberContentVO memberContent =
				find(memberId, contentId);

		if (memberContent == null) {
			memberContent = new MemberContentVO(
					memberId,
					contentId,
					ratingScore,
					WATCHLIST_NO,
					null,
					null,
					null);

			requireOneRow(
					memberContentMapper.doSave(memberContent),
					"별점 저장에 실패했습니다.");
		} else if (!ratingScore.equals(
				memberContent.getRatingScore())) {

			memberContent.setRatingScore(ratingScore);

			requireOneRow(
					memberContentMapper.doUpdate(memberContent),
					"별점 변경에 실패했습니다.");
		}

		return findRequired(memberId, contentId);
	}

	@Override
	@Transactional
	public void deleteRating(
			int memberId,
			int contentId) {

		MemberContentVO memberContent =
				find(memberId, contentId);

		// 이미 별점이 없는 요청은 최종 상태가 같으므로 성공으로 처리한다.
		if (memberContent == null
				|| memberContent.getRatingScore() == null) {

			return;
		}

		if (WATCHLIST_YES.equals(
				memberContent.getWatchlist())) {

			memberContent.setRatingScore(null);

			requireOneRow(
					memberContentMapper.doUpdate(memberContent),
					"별점 해제에 실패했습니다.");
		} else {
			requireOneRow(
					memberContentMapper.doDelete(memberContent),
					"별점 해제에 실패했습니다.");
		}
	}

	@Override
	@Transactional
	public MemberContentVO addWatchlist(
			int memberId,
			int contentId) {

		MemberContentVO memberContent =
				find(memberId, contentId);

		if (memberContent == null) {
			memberContent = new MemberContentVO(
					memberId,
					contentId,
					null,
					WATCHLIST_YES,
					null,
					null,
					null);

			requireOneRow(
					memberContentMapper.doSave(memberContent),
					"보고싶어요 등록에 실패했습니다.");
		} else if (!WATCHLIST_YES.equals(
				memberContent.getWatchlist())) {

			memberContent.setWatchlist(WATCHLIST_YES);

			requireOneRow(
					memberContentMapper.doUpdate(memberContent),
					"보고싶어요 등록에 실패했습니다.");
		}

		return findRequired(memberId, contentId);
	}

	@Override
	@Transactional
	public void deleteWatchlist(
			int memberId,
			int contentId) {

		MemberContentVO memberContent =
				find(memberId, contentId);

		// 행이 없거나 이미 해제된 요청은 멱등하게 성공 처리한다.
		if (memberContent == null
				|| WATCHLIST_NO.equals(
						memberContent.getWatchlist())) {

			return;
		}

		if (memberContent.getRatingScore() == null) {
			requireOneRow(
					memberContentMapper.doDelete(memberContent),
					"보고싶어요 해제에 실패했습니다.");
		} else {
			memberContent.setWatchlist(WATCHLIST_NO);

			requireOneRow(
					memberContentMapper.doUpdate(memberContent),
					"보고싶어요 해제에 실패했습니다.");
		}
	}

	/**
	 * 회원별 활동 목록 조회
	 */
	private List<MemberContentVO> retrieve(
			int memberId,
			DTO param,
			String sort,
			String searchDiv) {

		validateMemberId(memberId);
		validateRetrieveParam(param, sort);
		normalizePaging(param);

		param.setSearchDiv(searchDiv);
		param.setSearchWord(String.valueOf(memberId));

		int totalCount = memberContentMapper.count(param);
		param.setTotalCnt(totalCount);

		if (totalCount == 0) {
			return Collections.emptyList();
		}

		return memberContentMapper.doRetrieve(param);
	}

	/**
	 * 회원 번호와 콘텐츠 번호로 기록 조회
	 */
	private MemberContentVO find(
			int memberId,
			int contentId) {

		return memberContentMapper.doSelectOne(
				createKey(memberId, contentId));
	}

	/**
	 * 저장 결과를 반드시 조회
	 */
	private MemberContentVO findRequired(
			int memberId,
			int contentId) {

		MemberContentVO memberContent =
				find(memberId, contentId);

		if (memberContent == null) {
			throw new IllegalStateException(
					"저장된 회원 콘텐츠 기록을 조회할 수 없습니다.");
		}

		return memberContent;
	}

	/**
	 * 복합 PK 조회 객체 생성
	 */
	private MemberContentVO createKey(
			int memberId,
			int contentId) {

		validateMemberId(memberId);
		validateContentId(contentId);

		MemberContentVO key = new MemberContentVO();
		key.setMemberId(memberId);
		key.setContentId(contentId);

		return key;
	}

	/**
	 * 목록 조회 조건 검증
	 */
	private void validateRetrieveParam(
			DTO param,
			String sort) {

		if (param == null) {
			throw new IllegalArgumentException(
					"조회 조건은 null일 수 없습니다.");
		}

		if (sort == null
				|| !SORT_LATEST.equalsIgnoreCase(
						sort.trim())) {

			throw new IllegalArgumentException(
					"정렬 조건은 latest만 사용할 수 있습니다.");
		}
	}

	/**
	 * 페이징 기본값 및 최댓값 보정
	 */
	private void normalizePaging(DTO param) {
		if (param.getPageNo() <= 0) {
			param.setPageNo(1);
		}

		if (param.getPageSize() <= 0) {
			param.setPageSize(12);
		} else if (param.getPageSize() > 100) {
			param.setPageSize(100);
		}
	}

	/**
	 * 회원 번호 검증
	 */
	private void validateMemberId(int memberId) {
		if (memberId <= 0) {
			throw new IllegalArgumentException(
					"올바른 회원 번호가 필요합니다.");
		}
	}

	/**
	 * 콘텐츠 번호 검증
	 */
	private void validateContentId(int contentId) {
		if (contentId <= 0) {
			throw new IllegalArgumentException(
					"올바른 콘텐츠 번호가 필요합니다.");
		}
	}

	/**
	 * 별점 범위 검증
	 */
	private void validateRatingScore(
			Integer ratingScore) {

		if (ratingScore == null
				|| ratingScore < 1
				|| ratingScore > 5) {

			throw new IllegalArgumentException(
					"별점은 1점부터 5점까지 입력해야 합니다.");
		}
	}

	/**
	 * Mapper 처리 건수 검증
	 */
	private void requireOneRow(
			int result,
			String message) {

		if (result != 1) {
			throw new IllegalStateException(message);
		}
	}
}