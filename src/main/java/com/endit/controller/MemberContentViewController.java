package com.endit.controller;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
 * 2026. 9. 05. jinyoung    본인 전용 기록 경로 및 LoginMemberHelper 적용
 * 2026. 9. 10. heetae      다른 회원 기록 경로 지원 및 조회자·대상 회원 분리
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 27.
 */
@Controller
@RequestMapping("/members")
public class MemberContentViewController {

	private static final String TAB_RATINGS = "ratings";		 // 평가한 작품 탭
	private static final String TAB_COMMENTS = "comments";		 // 작성한 코멘트 탭
	private static final String TAB_COLLECTIONS = "collections"; // 만든 컬렉션 탭
	private static final String TAB_WATCHLIST = "watchlist";	 // 보고싶어요 탭

	private final MemberService memberService;
	private final MemberContentService memberContentService;
	private final UserCommentService userCommentService;
	private final CollectionService collectionService;

	/**
	 * 회원 기록 화면에 필요한 Service를 주입
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
	 * 회원의 기록 화면과 탭별 건수 전달 상세 목록은 JavaScript에서 REST API로 조회
	 *
	 * URL에 회원 번호가 없으면 로그인 회원 본인의 기록, 있으면 그 회원의 기록을 보여 준다.
	 * 본인 번호로 들어오면 본인 전용 경로로 리다이렉트해 주소를 하나로 통일한다.
	 *
	 * @param memberId 조회할 회원 번호, 본인 기록이면 null
	 * @param tab      최초 표시할 기록 탭
	 * @param model    View에 전달할 데이터
	 * @return 회원 기록 View 이름
	 */
	@GetMapping({ "/records", "/{memberId:[0-9]+}/records" })
	public String records(
			@PathVariable(required = false) Long memberId,
			@RequestParam(defaultValue = TAB_RATINGS) String tab,
			Model model) {

		// 조회자는 로그인 회원. 다른 회원 기록은 비로그인도 볼 수 있어 null이 될 수 있다.
		LoginMember viewer = LoginMemberHelper.getLoginMember();
		Long viewerMemberId = (viewer == null) ? null : viewer.getMemberId();

		String normalizedTab = normalizeTab(tab);

		// 본인 번호로 들어온 요청은 본인 전용 경로로 통일한다.
		if (memberId != null && viewerMemberId != null && memberId.longValue() == viewerMemberId.longValue()) {
			return "redirect:/members/records?tab=" + normalizedTab;
		}

		// 대상은 URL에 있으면 그 회원, 없으면 로그인 회원
		int targetMemberId = (memberId != null)
				? Math.toIntExact(memberId)
				: Math.toIntExact(LoginMemberHelper.getMemberId());

		MemberVO member = memberService.getMember(targetMemberId);
		
		if (member == null) {
			throw new NoSuchElementException("회원을 찾을 수 없습니다.");
		}
		
		Map<String, Object> publicMember = new LinkedHashMap<>();
		publicMember.put("nickname", member.getNickname());
		publicMember.put("profileImgUrl", member.getProfileImgUrl());

		DTO commentParam = new DTO();
		commentParam.setSearchDiv("10");
		commentParam.setSearchWord(String.valueOf(targetMemberId));

		model.addAttribute("memberId", targetMemberId);
		model.addAttribute("viewerMemberId", viewerMemberId);
		model.addAttribute("owner", viewerMemberId != null && viewerMemberId.longValue() == targetMemberId);
		model.addAttribute("tab", normalizedTab);
		model.addAttribute("member", publicMember);
		model.addAttribute("ratingCount", memberContentService.countRatingByMember(targetMemberId));
		model.addAttribute("commentCount", userCommentService.totalCntBySearch(commentParam));
		model.addAttribute("collectionCount", collectionService.countVisibleByMember(targetMemberId, viewerMemberId));
		model.addAttribute("watchlistCount", memberContentService.countWatchlistByMember(targetMemberId));

		return "member/records";
	}

	// 내부 조회 조건·응답 구성

	/**
	 * 지원하지 않는 기록 탭을 기본 평가 탭으로 보정
	 *
	 * @param tab 요청 탭 이름
	 * @return 지원하는 탭 이름 또는 기본값 {@code ratings}
	 */
	private String normalizeTab(String tab) {

		if (tab != null) {
			String normalizedTab = tab.trim().toLowerCase(Locale.ROOT);

			if (TAB_COMMENTS.equals(normalizedTab) || TAB_COLLECTIONS.equals(normalizedTab)
					|| TAB_WATCHLIST.equals(normalizedTab)) {
				return normalizedTab;
			}
		}

		return TAB_RATINGS;
	}
}
