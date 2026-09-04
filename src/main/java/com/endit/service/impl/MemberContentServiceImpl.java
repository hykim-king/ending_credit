package com.endit.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
 * 2026. 9. 03. jinyoung    회원별 기록 정렬 및 평가·보고싶어요 건수 조회 추가
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@Service
@Transactional(readOnly = true)
public class MemberContentServiceImpl implements MemberContentService {

	private static final String WATCHLIST_YES = "Y";	// 보고싶어요 등록
	private static final String WATCHLIST_NO = "N";		// 보고싶어요 미등록

	private static final String SEARCH_MEMBER_RATINGS = "50";	// 회원별 평가 검색 구분
	private static final String SEARCH_MEMBER_WATCHLIST = "60";	// 회원별 보고싶어요 검색 구분

	private static final String SORT_LATEST = "latest";				// 최신순
	private static final String SORT_OLDEST = "oldest";				// 오래된순
	private static final String SORT_RATING_DESC = "rating_desc";	// 별점 높은순
	private static final String SORT_RATING_ASC = "rating_asc";		// 별점 낮은순

	private final MemberContentMapper memberContentMapper;

	/**
	 * Service 구현체 생성
	 *
	 * @param memberContentMapper 회원 콘텐츠 Mapper
	 */
	public MemberContentServiceImpl(MemberContentMapper memberContentMapper) {

		this.memberContentMapper = memberContentMapper;
	}

	/**
	 * 회원 평가 목록 조회
	 *
	 * @param memberId 조회할 회원 번호
	 * @param param 검색 및 페이징 조건
	 * @param sort 정렬 조건
	 * @return 회원 평가 목록
	 */
	@Override
	public List<MemberContentVO> retrieveRatings(
			int memberId, DTO param, String sort) {

		return retrieve(memberId, param, sort, SEARCH_MEMBER_RATINGS);
	}

	/**
	 * 회원 보고싶어요 목록 조회
	 *
	 * @param memberId 조회할 회원 번호
	 * @param param 검색 및 페이징 조건
	 * @param sort 정렬 조건
	 * @return 회원 보고싶어요 목록
	 */
	@Override
	public List<MemberContentVO> retrieveWatchlist(
			int memberId, DTO param, String sort) {

		return retrieve(memberId, param, sort, SEARCH_MEMBER_WATCHLIST);
	}

	/**
	 * 회원 평가 콘텐츠 전체 건수 조회
	 *
	 * @param memberId 조회할 회원 번호
	 * @return 별점이 등록된 콘텐츠 건수
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
	 * 회원 보고싶어요 콘텐츠 전체 건수 조회
	 *
	 * @param memberId 조회할 회원 번호
	 * @return 보고싶어요로 등록된 콘텐츠 건수
	 */
	@Override
	public int countWatchlistByMember(int memberId) {

		validateMemberId(memberId);

		DTO param = new DTO();
		param.setSearchDiv(SEARCH_MEMBER_WATCHLIST);
		param.setSearchWord(String.valueOf(memberId));

		return memberContentMapper.count(param);
	}

	/**
	 * 회원 콘텐츠 별점 등록·수정
	 *
	 * @param memberId 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @param ratingScore 별점
	 * @return 저장된 회원 콘텐츠 기록
	 */
	@Override
	@Transactional
	public MemberContentVO saveRating(
			int memberId, int contentId, Integer ratingScore) {

		validateRatingScore(ratingScore);

		MemberContentVO memberContent = find(memberId, contentId);

		// 기존 기록이 없으면 새 행 등록, 있으면 별점만 변경
		if (memberContent == null) {
			memberContent = new MemberContentVO(
					memberId, contentId, ratingScore, WATCHLIST_NO, null, null, null);

			requireOneRow(memberContentMapper.doSave(memberContent), "별점 저장에 실패했습니다.");

		} else if (!ratingScore.equals(memberContent.getRatingScore())) {
			memberContent.setRatingScore(ratingScore);

			requireOneRow(memberContentMapper.doUpdate(memberContent), "별점 변경에 실패했습니다.");
		}

		return findRequired(memberId, contentId);
	}

	/**
	 * 회원 콘텐츠 별점 삭제
	 *
	 * @param memberId 회원 번호
	 * @param contentId 콘텐츠 번호
	 */
	@Override
	@Transactional
	public void deleteRating(int memberId, int contentId) {

		MemberContentVO memberContent = find(memberId, contentId);

		// 기록이 없거나 이미 별점이 없는 요청은 성공 처리
		if (memberContent == null || memberContent.getRatingScore() == null) {
			return;
		}

		if (WATCHLIST_YES.equals(memberContent.getWatchlist())) {
			memberContent.setRatingScore(null);

			requireOneRow(memberContentMapper.doUpdate(memberContent), "별점 해제에 실패했습니다.");

		} else {
			requireOneRow(memberContentMapper.doDelete(memberContent), "별점 해제에 실패했습니다.");
		}
	}

	/**
	 * 회원 콘텐츠 보고싶어요 등록
	 *
	 * @param memberId 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @return 저장된 회원 콘텐츠 기록
	 */
	@Override
	@Transactional
	public MemberContentVO addWatchlist(int memberId, int contentId) {

		MemberContentVO memberContent = find(memberId, contentId);

		// 기존 기록이 없으면 새 행 등록, 있으면 보고싶어요만 변경
		if (memberContent == null) {
			memberContent = new MemberContentVO(
					memberId, contentId, null, WATCHLIST_YES, null, null, null);

			requireOneRow(memberContentMapper.doSave(memberContent), "보고싶어요 등록에 실패했습니다.");

		} else if (!WATCHLIST_YES.equals(memberContent.getWatchlist())) {
			memberContent.setWatchlist(WATCHLIST_YES);

			requireOneRow(memberContentMapper.doUpdate(memberContent), "보고싶어요 등록에 실패했습니다.");
		}

		return findRequired(memberId, contentId);
	}

	/**
	 * 회원 콘텐츠 보고싶어요 삭제
	 *
	 * @param memberId 회원 번호
	 * @param contentId 콘텐츠 번호
	 */
	@Override
	@Transactional
	public void deleteWatchlist(int memberId, int contentId) {

		MemberContentVO memberContent = find(memberId, contentId);

		// 기록이 없거나 이미 해제된 요청은 성공 처리
		if (memberContent == null
				|| WATCHLIST_NO.equals(memberContent.getWatchlist())) {
			return;
		}

		if (memberContent.getRatingScore() == null) {
			requireOneRow(memberContentMapper.doDelete(memberContent), "보고싶어요 해제에 실패했습니다.");

		} else {
			memberContent.setWatchlist(WATCHLIST_NO);

			requireOneRow(memberContentMapper.doUpdate(memberContent), "보고싶어요 해제에 실패했습니다.");
		}
	}

	/**
	 * 회원별 활동 목록 공통 조회
	 *
	 * @param memberId  조회할 회원 번호
	 * @param param     검색 및 페이징 조건
	 * @param sort      요청 정렬값
	 * @param searchDiv 평가 또는 보고싶어요 검색 구분
	 * @return 회원 활동 목록
	 */
	private List<MemberContentVO> retrieve(
			int memberId, DTO param, String sort, String searchDiv) {

		// 회원·정렬·페이징 조회 조건 검증 및 정규화
		validateMemberId(memberId);
		String normalizedSort = normalizeSort(param, sort, searchDiv);
		normalizePaging(param);

		// Mapper 검색 조건과 정렬값 설정
		param.setSearchDiv(searchDiv);
		param.setSearchWord(String.valueOf(memberId));
		param.getSearchMap().put("sort", normalizedSort);

		int totalCount = memberContentMapper.count(param);
		param.setTotalCnt(totalCount);

		if (totalCount == 0) {
			return Collections.emptyList();
		}

		return memberContentMapper.doRetrieve(param);
	}

	/**
	 * 회원 번호와 콘텐츠 번호로 기록 조회
	 *
	 * @param memberId 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @return 회원 콘텐츠 기록 또는 {@code null}
	 */
	private MemberContentVO find(int memberId, int contentId) {

		return memberContentMapper.doSelectOne(createKey(memberId, contentId));
	}

	/**
	 * 저장된 회원 콘텐츠 기록 조회
	 *
	 * @param memberId 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @return 저장된 회원 콘텐츠 기록
	 */
	private MemberContentVO findRequired(int memberId, int contentId) {

		MemberContentVO memberContent = find(memberId, contentId);

		if (memberContent == null) {
			throw new IllegalStateException("저장된 회원 콘텐츠 기록을 조회할 수 없습니다.");
		}

		return memberContent;
	}

	/**
	 * 복합 PK 조회 객체 생성
	 *
	 * @param memberId 회원 번호
	 * @param contentId 콘텐츠 번호
	 * @return 복합 PK가 설정된 조회 객체
	 */
	private MemberContentVO createKey(int memberId, int contentId) {

		validateMemberId(memberId);
		validateContentId(contentId);

		MemberContentVO key = new MemberContentVO();
		key.setMemberId(memberId);
		key.setContentId(contentId);

		return key;
	}

	/**
	 * 목록 정렬 조건 검증 및 정규화
	 *
	 * @param param     목록 조회 조건
	 * @param sort      요청 정렬값
	 * @param searchDiv 조회할 기록 종류
	 * @return 정규화한 정렬값
	 */
	private String normalizeSort(DTO param, String sort, String searchDiv) {

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		if (sort == null || sort.isBlank()) {
			throw new IllegalArgumentException("정렬 조건이 필요합니다.");
		}

		String normalizedSort = sort.trim().toLowerCase(Locale.ROOT);

		// 평가 목록에서만 별점순 정렬 허용
		boolean supported = SORT_LATEST.equals(normalizedSort)
				|| SORT_OLDEST.equals(normalizedSort)
				|| (SEARCH_MEMBER_RATINGS.equals(searchDiv)
					&& (SORT_RATING_DESC.equals(normalizedSort)
						|| SORT_RATING_ASC.equals(normalizedSort)));

		if (!supported) {

			throw new IllegalArgumentException("지원하지 않는 정렬 조건입니다.");
		}

		return normalizedSort;
	}

	/**
	 * 페이지 번호와 페이지 크기 정규화
	 *
	 * @param param 검색 및 페이징 조건
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
	 *
	 * @param memberId 회원 번호
	 */
	private void validateMemberId(int memberId) {

		if (memberId <= 0) {
			throw new IllegalArgumentException("올바른 회원 번호가 필요합니다.");
		}
	}

	/**
	 * 콘텐츠 번호 검증
	 *
	 * @param contentId 콘텐츠 번호
	 */
	private void validateContentId(int contentId) {

		if (contentId <= 0) {
			throw new IllegalArgumentException("올바른 콘텐츠 번호가 필요합니다.");
		}
	}

	/**
	 * 별점 범위 검증
	 *
	 * @param ratingScore 별점
	 */
	private void validateRatingScore(Integer ratingScore) {

		if (ratingScore == null || ratingScore < 1 || ratingScore > 5) {

			throw new IllegalArgumentException("별점은 1점부터 5점까지 입력해야 합니다.");
		}
	}

	/**
	 * Mapper 처리 건수 검증
	 *
	 * @param result Mapper 처리 건수
	 * @param message 처리 실패 메시지
	 */
	private void requireOneRow(int result, String message) {

		if (result != 1) {
			throw new IllegalStateException(message);
		}
	}
}
