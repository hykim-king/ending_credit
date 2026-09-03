package com.endit.controller;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.cmn.DTO;
import com.endit.cmn.LoginMember;
import com.endit.domain.MemberVO;
import com.endit.security.LoginMemberHelper;
import com.endit.service.CollectionService;
import com.endit.service.MemberContentService;
import com.endit.service.MemberService;
import com.endit.service.UserCommentService;

/**
 * <pre>
 * Class Name  : MemberContentViewController 
 * Description : 회원의 작품, 코멘트, 컬렉션, 보고싶어요 기록 화면을 처리하는 Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 27. jinyoung    최초 생성
 * 2026. 9. 01. jinyoung    U-03~U-06 기록 화면 4개 탭 경로 지원
 * 2026. 9. 03. jinyoung    기록 화면 설명 및 탭 정규화 코드 정리
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@Controller
@RequestMapping("/users/{memberId}/records")
public class MemberContentViewController {

	private static final String TAB_RATINGS = "ratings";		 //
	private static final String TAB_COMMENTS = "comments";		 //
	private static final String TAB_COLLECTIONS = "collections"; //
	private static final String TAB_WATCHLIST = "watchlist";	 //

	private final MemberService memberService;
	private final MemberContentService memberContentService;
	private final UserCommentService userCommentService;
	private final CollectionService collectionService;

	/**
	 * 회원 기록 화면에 필요한 Service를 주입받는다.
	 *
	 * @param memberService        회원 Service
	 * @param memberContentService 회원 작품 기록 Service
	 * @param userCommentService   회원 코멘트 Service
	 * @param collectionService    컬렉션 Service
	 */
	public MemberContentViewController(
			MemberService memberService,
			MemberContentService memberContentService,
			UserCommentService userCommentService,
			CollectionService collectionService) {

		this.memberService = memberService;
		this.memberContentService = memberContentService;
		this.userCommentService = userCommentService;
		this.collectionService = collectionService;
	}

	/**
	 * 회원 기록 화면 반환
	 *
	 * 네 가지 기록은 같은 HTML에서 탭으로 전환하고, 실제 목록 데이터는 JavaScript가 REST API로 조회한다.
	 *
	 * @param memberId 조회할 회원 번호
	 * @param tab      최초 표시할 기록 탭
	 * @param model    View에 전달할 데이터
	 * @return 회원 기록 View 이름
	 */
	@GetMapping
	public String records(@PathVariable int memberId,
			@RequestParam(defaultValue = TAB_RATINGS) String tab, Model model) {

		MemberVO member = memberService.getMember(memberId);

		if (member == null) {
			throw new NoSuchElementException("회원을 찾을 수 없습니다.");
		}

		LoginMember loginMember = LoginMemberHelper.getLoginMember();
		Long currentMemberId = loginMember == null ? null : loginMember.getMemberId();

		DTO commentParam = new DTO();
		commentParam.setSearchDiv("10");
		commentParam.setSearchWord(String.valueOf(memberId));

		model.addAttribute("memberId", memberId);
		model.addAttribute("tab", normalizeTab(tab));
		model.addAttribute("member", member);
		model.addAttribute("ratingCount", memberContentService.countRatingByMember(memberId));
		model.addAttribute("commentCount", userCommentService.totalCntBySearch(commentParam));
		model.addAttribute("collectionCount", collectionService.countVisibleByMember(memberId, currentMemberId));
		model.addAttribute("watchlistCount", memberContentService.countWatchlistByMember(memberId));

		return "user/records";
	}

	/**
	 * 화면설계서의 네 가지 기록 탭 외의 값은 기본 평가 탭으로 보정한다.
	 *
	 * @param tab 요청 탭 이름
	 * @return 지원하는 탭 이름 또는 기본값 {@code ratings}
	 */
	private String normalizeTab(String tab) {
		if (tab != null) {
			String normalizedTab = tab.trim().toLowerCase(Locale.ROOT);

			if (TAB_COMMENTS.equals(normalizedTab)
					|| TAB_COLLECTIONS.equals(normalizedTab)
					|| TAB_WATCHLIST.equals(normalizedTab)) {

				return normalizedTab;
			}
		}

		return TAB_RATINGS;
	}
}
