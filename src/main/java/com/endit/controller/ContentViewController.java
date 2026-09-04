package com.endit.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.endit.auth.CurrentMemberProvider;
import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.ContentGenreVO;
import com.endit.domain.ContentImageVO;
import com.endit.domain.ContentVO;
import com.endit.domain.UserCommentVO;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentGenreService;
import com.endit.service.ContentImageService;
import com.endit.service.ContentService;
import com.endit.service.UserCommentService;

/**
 * 영화 상세 화면(C-01 영화 상세 페이지 + C-02 출연/제작·갤러리)의 경로를 처리하는 Controller
 */
@Controller
public class ContentViewController {

	private static final Logger log = LoggerFactory.getLogger(ContentViewController.class);

	// C-02 코멘트 미리보기 카드 수. 초과분은 ACT-C-008 더보기로 C-04에 넘긴다
	private static final int COMMENT_PREVIEW_SIZE = 4;
	private static final int FIRST_PAGE_NO = 1;

	// UserCommentMapper의 검색구분 20(영화ID). 코멘트는 담당 밖이라 그쪽 약속을 따른다
	private static final String COMMENT_SEARCH_DIV_CONTENT = "20";

	// 미리보기는 좋아요순 — 왓챠와 같은 "대표 코멘트" 성격이다. C-04는 자체 정렬 4종을 쓴다
	private static final String COMMENT_PREVIEW_SORT = "likes";

	// POL-033 역할 4종 표기 - PersonViewController.ROLE_LABELS와 같은 표다
	private static final Map<String, String> ROLE_LABELS = Map.of(
			"DIRECTOR", "감독",
			"ACTOR", "배우",
			"WRITER", "각본",
			"PRODUCER", "제작");

	private static final String CONTENT_DETAIL_VIEW = "content/detail";

	private final ContentService contentService;
	private final ContentGenreService contentGenreService;
	private final ContentCreditService contentCreditService;
	private final ContentImageService contentImageService;
	private final UserCommentService userCommentService;
	private final CurrentMemberProvider currentMemberProvider;

	public ContentViewController(
			ContentService contentService,
			ContentGenreService contentGenreService,
			ContentCreditService contentCreditService,
			ContentImageService contentImageService,
			UserCommentService userCommentService,
			CurrentMemberProvider currentMemberProvider) {
		this.contentService = contentService;
		this.contentGenreService = contentGenreService;
		this.contentCreditService = contentCreditService;
		this.contentImageService = contentImageService;
		this.userCommentService = userCommentService;
		this.currentMemberProvider = currentMemberProvider;
	}

	/** 영화 상세 화면 */
	@GetMapping("/movies/{contentId}")
	public String detail(@PathVariable int contentId, Model model) {

		ContentVO content;

		try {
			content = contentService.get(contentId);
		} catch (NoSuchElementException e) {
			model.addAttribute("notFound", true);
			return CONTENT_DETAIL_VIEW;
		}

		List<ContentGenreVO> genres = contentGenreService.retrieveAll(contentId);
		List<ContentCreditVO> castAndCrew = contentCreditService.retrieveAll(contentId);
		// 썸네일용·확대용 URL이 모두 채워진 상태로 넘긴다.
		List<ContentImageVO> galleryImages = contentImageService.retrieveAll(contentId);

		model.addAttribute("content", content);
		model.addAttribute("genres", genres);
		model.addAttribute("castAndCrew", castAndCrew);
		model.addAttribute("galleryImages", galleryImages);
		model.addAttribute("roleLabels", ROLE_LABELS);
		model.addAttribute("notFound", false);

		// ACT-C-001~005 - 쓰기 버튼이 보낼 회원 번호. 비회원이면 null이고 화면이 안내로 바꾼다
		model.addAttribute("loginMemberId", toCurrentMemberId());

		addComments(contentId, model);

		return CONTENT_DETAIL_VIEW;
	}

	/**
	 * C-02 코멘트 미리보기. USER_COMMENT는 담당 밖이라 이미 있는 서비스를 읽기만 한다.
	 * 정의서 C-02 "한 섹션 실패가 전체를 막지 않음"이라 실패해도 화면은 그린다.
	 */
	private void addComments(int contentId, Model model) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(COMMENT_PREVIEW_SIZE);
		param.setSearchDiv(COMMENT_SEARCH_DIV_CONTENT);
		param.setSearchWord(String.valueOf(contentId));
		param.getSearchMap().put("sort", COMMENT_PREVIEW_SORT);

		List<UserCommentVO> comments;

		try {
			// do* 는 WorkDiv를 상속한 타 조 계약이라 서비스에서도 이 이름이다
			comments = userCommentService.doRetrieve(param);
		} catch (RuntimeException e) {
			// 코멘트 자리만 비우고 나머지 섹션은 살린다
			log.warn("코멘트 조회에 실패했습니다. contentId={}", contentId, e);
			comments = Collections.emptyList();
		}

		model.addAttribute("comments", comments);
		// 총건수는 각 행의 totalCnt에 실려 온다(CROSS JOIN) - 건수만 따로 세지 않는다
		model.addAttribute("commentTotalCnt", comments.isEmpty() ? 0 : comments.get(0).getTotalCnt());
	}

	// C-01 쓰기 버튼용 로그인 회원 번호 - 타 조 API가 X-Member-Id 헤더를 받는 동안만 필요하다
	private Integer toCurrentMemberId() {
		OptionalLong memberId = currentMemberProvider.findCurrentMemberId();

		return memberId.isPresent() ? (int) memberId.getAsLong() : null;
	}

}
