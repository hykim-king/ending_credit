package com.endit.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.cmn.DTO;
import com.endit.cmn.LoginMember;
import com.endit.cmn.LocaleTextHelper;
import com.endit.domain.CodeVO;
import com.endit.domain.CollectionVO;
import com.endit.domain.CommentLikeVO;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.ContentGenreVO;
import com.endit.domain.ContentImageVO;
import com.endit.domain.ContentVO;
import com.endit.domain.MemberContentVO;
import com.endit.domain.EnglishContentVO;
import com.endit.domain.UserCommentVO;
import com.endit.mapper.MemberContentMapper;
import com.endit.security.LoginMemberHelper;
import com.endit.service.CodeService;
import com.endit.service.CollectionService;
import com.endit.service.CommentLikeService;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentGenreService;
import com.endit.service.ContentImageService;
import com.endit.service.ContentService;
import com.endit.service.UserCommentService;

/**
 * <pre>
 * 영화 상세 화면(C-01 영화 상세 페이지 + C-02 출연/제작·갤러리)의 경로를 처리하는 Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 9. 05. jinyoung    로그인 회원 조회를 팀 공용 LoginMemberHelper로 통일
 * ------------------------------------------------------------
 * </pre>
 */
@Controller
public class ContentViewController {

	private static final Logger log = LoggerFactory.getLogger(ContentViewController.class);

	// C-02 코멘트 미리보기 카드 수(4열 x 2행). 초과분은 ACT-C-008 더보기로 C-04에 넘긴다
	private static final int COMMENT_PREVIEW_SIZE = 8;

	// C-02 출연/제작 미리보기 수(4열 x 3행). 초과분은 ACT-C-006 전체보기가 API-007로 따로 받는다
	private static final int CAST_PREVIEW_SIZE = 12;

	private static final int FIRST_PAGE_NO = 1;

	// ── 담당 밖 모듈의 약속을 베껴 온 값들 ─────────────────────────────
	// 아래 셋과 컬렉션 정렬 둘(SEARCH_KEY_SORT·COLLECTION_SORT_LIKES)은
	// 저쪽 ServiceImpl·Mapper의 private 상수라 import할 수 없어 값만 다시 적은 것이다.
	// 저쪽에서 값이 바뀌면 컴파일은 그대로 통과하고 조회 결과만 조용히 달라지므로,
	// 담당 밖 매퍼를 건드리는 작업을 할 때는 이 블록을 함께 확인한다.
	// 두 검색구분이 우연히 같은 "20"이지만 서로 다른 매퍼의 축이므로 하나로 합치지 않는다 -
	// 한쪽이 축을 바꿔도 다른 쪽이 따라 깨지면 안 된다.

	// UserCommentMapper의 검색구분 20(영화ID)
	private static final String COMMENT_SEARCH_DIV_CONTENT = "20";

	// MemberContentMapper의 검색구분 20(콘텐츠 번호)
	private static final String RATING_SEARCH_DIV_CONTENT = "20";

	// MEMBER_CONTENT.WATCHLIST의 등록 값(MemberContentServiceImpl.WATCHLIST_YES와 같은 약속)
	private static final String WATCHLIST_YES = "Y";

	// pageNo·pageSize를 0으로 두면 매퍼가 ROWNUM 구간 조건을 걸지 않아 전부 돌려준다
	private static final int NO_PAGING = 0;

	// 별점 눈금(POL: MEMBER_CONTENT.RATING_SCORE는 1~5). 평가 분석 막대가 이 범위만큼 그려진다
	private static final int MIN_RATING_SCORE = 1;
	private static final int MAX_RATING_SCORE = 5;

	// CollectionMapper의 검색구분 30(그 콘텐츠를 담은 컬렉션) - 이 축은 팀원 매퍼에 우리가 넣은 것이다
	private static final String COLLECTION_SEARCH_DIV_CONTENT = "30";
	// 정렬 통로와 값(위 "담당 밖 약속" 블록과 같은 성격) - CollectionServiceImpl의 같은 상수가 private이다.
	// 값이 어긋나면 정렬만 조용히 기본값으로 떨어지고 addCollections의 catch도 이를 잡지 못한다
	private static final String SEARCH_KEY_SORT = "sort";
	private static final String COLLECTION_SORT_LIKES = "likes";
	// 캐러셀에 깔 장수. 넘치는 만큼은 전체보기가 받는다
	private static final int COLLECTION_PREVIEW_SIZE = 20;
	// 전체보기 한 페이지(3열 x 8줄)
	private static final int COLLECTION_PAGE_SIZE = 24;

	private static final String CONTENT_COLLECTIONS_VIEW = "content/collections";

	// 미리보기는 좋아요순 — 왓챠와 같은 "대표 코멘트" 성격이다. C-04는 자체 정렬 4종을 쓴다
	private static final String COMMENT_PREVIEW_SORT = "likes";

	// POL-033 역할 4종 표기 - PersonViewController.ROLE_LABELS와 같은 표다.
	// MOD-05 역할 칩이 이 순서 그대로 그려지므로 매퍼의 감독 우선 정렬과 같은 순서를 유지한다.
	// 라벨은 messages*.properties의 role.* 에 있고 여기에는 코드와 순서만 둔다(F-01)
	private static final List<String> ROLE_CODES = List.of("DIRECTOR", "ACTOR", "WRITER", "PRODUCER");
	private static final String MSG_PREFIX_ROLE = "role.";

	// MOD-04 신고 사유. 목록·순서는 COMMON_CODE가, 문구는 messages 번들이 맡는다.
	// 관리자 신고 화면(ReportCommentController)과 같은 코드 그룹이라 사유가 갈리지 않는다
	private static final String CODE_GROUP_REPORT_REASON = "REPORT_REASON";
	private static final String CODE_PARAM_KEY = "code";
	private static final String MSG_PREFIX_REPORT_REASON = "report.reason.";

	private static final String CONTENT_DETAIL_VIEW = "content/detail";

	private final ContentService contentService;
	private final ContentGenreService contentGenreService;
	private final ContentCreditService contentCreditService;
	private final ContentImageService contentImageService;
	private final UserCommentService userCommentService;
	private final CodeService codeService;
	private final CommentLikeService commentLikeService;
	private final MemberContentMapper memberContentMapper;
	private final CollectionService collectionService;
	private final LocaleTextHelper localeText;
	private final MessageSource messageSource;

	public ContentViewController(
			ContentService contentService,
			ContentGenreService contentGenreService,
			ContentCreditService contentCreditService,
			ContentImageService contentImageService,
			UserCommentService userCommentService,
			CodeService codeService,
			CommentLikeService commentLikeService,
			MemberContentMapper memberContentMapper,
			CollectionService collectionService,
			LocaleTextHelper localeText,
			MessageSource messageSource) {
		this.contentService = contentService;
		this.contentGenreService = contentGenreService;
		this.contentCreditService = contentCreditService;
		this.contentImageService = contentImageService;
		this.userCommentService = userCommentService;
		this.codeService = codeService;
		this.commentLikeService = commentLikeService;
		this.memberContentMapper = memberContentMapper;
		this.collectionService = collectionService;
		this.localeText = localeText;
		this.messageSource = messageSource;
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

		applyEnglishContent(content);

		List<ContentGenreVO> genres = contentGenreService.retrieveAll(contentId);
		// 썸네일용·확대용 URL이 모두 채워진 상태로 넘긴다.
		List<ContentImageVO> galleryImages = contentImageService.retrieveAll(contentId);

		model.addAttribute("content", content);
		model.addAttribute("genres", genres);
		model.addAttribute("galleryImages", galleryImages);
		model.addAttribute("roleLabels", toRoleLabels());
		model.addAttribute("notFound", false);

		// ACT-C-001~005 - 쓰기 버튼이 보낼 회원 번호. 비회원이면 null이고 화면이 안내로 바꾼다
		Integer loginMemberId = toCurrentMemberId();
		model.addAttribute("loginMemberId", loginMemberId);

		addCastAndCrew(contentId, model);
		addRating(contentId, model);
		addMyRecord(contentId, loginMemberId, model);
		addComments(contentId, loginMemberId, model);
		addReportReasons(model);
		addCollections(contentId, COLLECTION_PREVIEW_SIZE, FIRST_PAGE_NO, model);

		return CONTENT_DETAIL_VIEW;
	}

	/**
	 * <pre>
	 * Method Name : collections
	 * Description : C-02 "이 작품이 담긴 컬렉션" 전체보기. 상세의 캐러셀이 다 못 실은 나머지를 여기서 본다.
	 *               한 페이지 24건이며 정렬은 상세와 같은 좋아요순이다.
	 *               공개 범위는 컬렉션 서비스가 정한다 - 남의 비공개는 빠지고 내 비공개는 보인다.
	 *               마지막 페이지를 넘는 pageNo는 마지막 페이지로 당겨진다(조회가 한 번 더 나간다).
	 *               없는 contentId면 상세와 같은 notFound 화면이다.
	 * </pre>
	 * @param contentId
	 * @param pageNo
	 * @param model
	 * @return String (content/collections)
	 */
	@GetMapping("/movies/{contentId}/collections")
	public String collections(
			@PathVariable int contentId,
			@RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
			Model model) {

		ContentVO content;

		try {
			content = contentService.get(contentId);
		} catch (NoSuchElementException e) {
			model.addAttribute("notFound", true);
			return CONTENT_COLLECTIONS_VIEW;
		}

		applyEnglishContent(content);

		model.addAttribute("content", content);
		model.addAttribute("notFound", false);
		model.addAttribute("loginMemberId", toCurrentMemberId());
		model.addAttribute("pageSize", COLLECTION_PAGE_SIZE);

		int currentPageNo = Math.max(pageNo, FIRST_PAGE_NO);
		int totalCnt = addCollections(contentId, COLLECTION_PAGE_SIZE, currentPageNo, model);
		int lastPage = toLastPage(totalCnt, COLLECTION_PAGE_SIZE);

		// 마지막 페이지를 넘겨 들어오면 건수는 그대로인데 목록만 비어 "담은 컬렉션이 없습니다"가 함께 뜬다
		if (currentPageNo > lastPage) {
			currentPageNo = lastPage;
			addCollections(contentId, COLLECTION_PAGE_SIZE, currentPageNo, model);
		}

		model.addAttribute("pageNo", currentPageNo);

		return CONTENT_COLLECTIONS_VIEW;
	}

	/**
	 * C-02 출연/제작 미리보기. 화면에는 CAST_PREVIEW_SIZE만 그리고 전체 건수를 함께 넘긴다.
	 * retrieveAll이 아니라 retrieve를 쓰는 이유는 param에 실려 오는 전체 건수를 받기 위해서다 -
	 * retrieveAll은 100건 상한에 걸린 수를 돌려주므로 "전체보기"의 판단 근거가 되지 못한다.
	 */
	private void addCastAndCrew(int contentId, Model model) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(CAST_PREVIEW_SIZE);

		List<ContentCreditVO> castAndCrew = contentCreditService.retrieve(contentId, param);

		model.addAttribute("castAndCrew", castAndCrew);
		model.addAttribute("castTotalCnt", param.getTotalCnt());
	}

	/**
	 * C-02 이 작품이 담긴 컬렉션. 상세는 앞 20건만, 전체보기는 한 페이지씩 같은 조회로 받는다.
	 * COLLECTION은 담당 밖이라 팀원 서비스를 읽기만 한다 - 공개 범위·좋아요·대표 포스터가 거기서 붙어 온다.
	 * 전체 건수는 서비스가 param에 채워 주므로 따로 세지 않고, 호출부가 마지막 페이지를 계산하도록 돌려준다.
	 * 정의서 C-02 "한 섹션 실패가 전체를 막지 않음"이라 실패해도 화면은 그린다.
	 */
	private int addCollections(int contentId, int pageSize, int pageNo, Model model) {
		List<CollectionVO> collections = Collections.emptyList();
		int totalCnt = 0;

		try {
			DTO param = new DTO();
			param.setPageNo(pageNo);
			param.setPageSize(pageSize);
			param.setSearchDiv(COLLECTION_SEARCH_DIV_CONTENT);
			param.setSearchWord(String.valueOf(contentId));
			param.getSearchMap().put(SEARCH_KEY_SORT, COLLECTION_SORT_LIKES);

			collections = collectionService.retrieve(param, findCurrentMemberId());
			totalCnt = param.getTotalCnt();
		} catch (RuntimeException e) {
			// 컬렉션 줄만 빼고 나머지 섹션은 살린다
			log.warn("컬렉션 목록 조회에 실패했습니다. contentId={}", contentId, e);
		}

		model.addAttribute("collections", collections);
		model.addAttribute("collectionTotalCnt", totalCnt);

		return totalCnt;
	}

	// 건수가 0이어도 첫 페이지가 마지막 페이지다 - 빈 목록이 0페이지로 떨어지면 페이저 계산이 깨진다
	private int toLastPage(int totalCnt, int pageSize) {
		return Math.max((totalCnt + pageSize - 1) / pageSize, FIRST_PAGE_NO);
	}

	/**
	 * C-01 평균 별점과 평가 인원, 그리고 점수별 인원(평가 분석).
	 * MEMBER_CONTENT는 담당 밖이라 고치지 않고 목록 매퍼를 읽기만 한다 -
	 * "이 영화의 평균"을 주는 서비스 계약이 아직 없어, 행마다 실려 오는 AVERAGE_RATING을 쓴다.
	 * 점수별 인원은 그 행들을 세기만 한 것이라 조회가 늘지 않는다 - 집계 쿼리를 따로 두지 않은 이유다.
	 * 그 영화의 기록 수만큼 행을 읽으므로 평가가 아주 많아지면 집계 계약으로 옮길 자리다.
	 * 정의서 C-02 "한 섹션 실패가 전체를 막지 않음"이라 실패해도 화면은 그린다.
	 */
	private void addRating(int contentId, Model model) {
		Double average = null;
		int raters = 0;
		// 아무도 안 준 점수도 0짜리 막대로 그려야 눈금이 유지된다
		Map<Integer, Integer> counts = toEmptyRatingCounts();

		try {
			DTO param = new DTO();
			param.setPageNo(NO_PAGING);
			param.setPageSize(NO_PAGING);
			param.setSearchDiv(RATING_SEARCH_DIV_CONTENT);
			param.setSearchWord(String.valueOf(contentId));

			for (MemberContentVO record : memberContentMapper.doRetrieve(param)) {
				// 보고싶어요만 누른 기록은 평가가 아니다
				if (record.getRatingScore() == null) {
					continue;
				}

				raters++;
				// AVERAGE_RATING은 LEFT JOIN이라 어느 행에나 같은 값이 실려 온다
				average = record.getAverageRating();
				// 눈금 밖 점수가 들어오면 막대를 새로 만들지 않고 버린다
				counts.computeIfPresent(record.getRatingScore(), (score, count) -> count + 1);
			}
		} catch (RuntimeException e) {
			// 평균 자리만 비우고 나머지 섹션은 살린다
			log.warn("평가 집계 조회에 실패했습니다. contentId={}", contentId, e);
			average = null;
			raters = 0;
			counts = toEmptyRatingCounts();
		}

		// 평가 0건이면 0.0이 아니라 "없음"이어야 한다(정의서 C-01 빈 상태)
		model.addAttribute("ratingAverage", raters == 0 ? null : average);
		model.addAttribute("raterCount", raters);
		// 눈금 상한은 Chart.js가 데이터에서 잡는다 - 서버가 최댓값을 따로 넘기지 않는다
		model.addAttribute("ratingCounts", counts);
	}

	// 1~5점 자리를 0으로 채운 눈금
	private Map<Integer, Integer> toEmptyRatingCounts() {
		Map<Integer, Integer> counts = new LinkedHashMap<>();

		for (int score = MIN_RATING_SCORE; score <= MAX_RATING_SCORE; score++) {
			counts.put(score, 0);
		}

		return counts;
	}

	/**
	 * C-01 다시 들어왔을 때 채워 둘 내 별점과 보고싶어요. 비회원이거나 기록이 없으면 별점은 null, 보고싶어요는 false다.
	 * MEMBER_CONTENT는 담당 밖이라 고치지 않고 복합 PK 단건 조회를 읽기만 한다 - 두 값이 한 행이라 조회도 한 번이다.
	 * 정의서 C-02 "한 섹션 실패가 전체를 막지 않음"이라 실패해도 화면은 그린다.
	 */
	private void addMyRecord(int contentId, Integer loginMemberId, Model model) {
		Integer myRatingScore = null;
		boolean myWatchlist = false;

		if (loginMemberId != null) {
			try {
				MemberContentVO key = new MemberContentVO();
				key.setMemberId(loginMemberId);
				key.setContentId(contentId);

				MemberContentVO record = memberContentMapper.doSelectOne(key);

				// 별점만 준 기록은 WATCHLIST가 N이고, 보고싶어요만 누른 기록은 RATING_SCORE가 비어 있다
				if (record != null) {
					myRatingScore = record.getRatingScore();
					myWatchlist = WATCHLIST_YES.equals(record.getWatchlist());
				}
			} catch (RuntimeException e) {
				// 별·보고싶어요 자리만 비우고 나머지 섹션은 살린다
				log.warn("내 평가 기록 조회에 실패했습니다. contentId={}", contentId, e);
			}
		}

		model.addAttribute("myRatingScore", myRatingScore);
		model.addAttribute("myWatchlist", myWatchlist);
	}

	/**
	 * C-02 코멘트 미리보기. USER_COMMENT는 담당 밖이라 이미 있는 서비스를 읽기만 한다.
	 * 정의서 C-02 "한 섹션 실패가 전체를 막지 않음"이라 실패해도 화면은 그린다.
	 */
	private void addComments(int contentId, Integer loginMemberId, Model model) {
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
		model.addAttribute("likedCommentIds", toLikedCommentIds(comments, loginMemberId));
	}

	/**
	 * 미리보기 코멘트 중 내가 좋아요한 것들. 진입 시 따봉을 채워 두는 데 쓴다.
	 * COMMENT_LIKE는 담당 밖이라 이미 있는 likeCheck 계약을 코멘트마다 한 번씩 부른다 -
	 * 미리보기가 COMMENT_PREVIEW_SIZE장뿐이라 감당되는 수다(숫자를 여기 적으면 상수와 어긋난다).
	 * 목록에 좋아요 여부를 실어 주는 계약이 생기면 이 반복은 사라진다.
	 */
	private Set<Long> toLikedCommentIds(List<UserCommentVO> comments, Integer loginMemberId) {
		// 비회원은 좋아요한 것이 있을 수 없다
		if (loginMemberId == null || comments.isEmpty()) {
			return Collections.emptySet();
		}

		Set<Long> liked = new HashSet<>();

		try {
			for (UserCommentVO comment : comments) {
				CommentLikeVO key = new CommentLikeVO();
				key.setMemberId(loginMemberId);
				key.setCommentId(comment.getCommentId());

				if (commentLikeService.likeCheck(key) > 0) {
					liked.add(comment.getCommentId());
				}
			}
		} catch (RuntimeException e) {
			// 따봉이 빈 채로 뜰 뿐 카드는 그대로 보인다
			log.warn("코멘트 좋아요 여부 조회에 실패했습니다.", e);
			return Collections.emptySet();
		}

		return liked;
	}

	// 화면이 코드→라벨로 찾아 쓰므로 맵으로 넘긴다. 순서가 필요해 LinkedHashMap이다
	private Map<String, String> toRoleLabels() {
		Map<String, String> labels = new LinkedHashMap<>();

		for (String code : ROLE_CODES) {
			labels.put(code, messageSource.getMessage(MSG_PREFIX_ROLE + code, null,
					MSG_PREFIX_ROLE + code, LocaleContextHolder.getLocale()));
		}

		return Collections.unmodifiableMap(labels);
	}

	// 영어 화면이면 줄거리·포스터·배경을 영문판으로 갈아끼운다. DB에 영어 컬럼이 없어 서비스가 TMDB에서 받아 캐시한다.
	// 못 받으면 한국어 값이 그대로 남으므로 여기서 실패를 따로 다루지 않는다.
	// 이미지는 TMDB 원본 경로로 오므로 화면에 내보내기 전에 크기를 붙여 완성해야 한다
	private void applyEnglishContent(ContentVO content) {
		if (!localeText.isEnglish()) {
			return;
		}

		EnglishContentVO english =
				contentService.getEnglishContent(content.getContentId(), content.getExternalId());

		if (english == null) {
			return;
		}

		if (StringUtils.hasText(english.getOverview())) {
			content.setOverview(english.getOverview());
		}

		if (StringUtils.hasText(english.getPosterPath())) {
			content.setPosterUrl(contentImageService.toPosterUrl(english.getPosterPath()));
		}

		if (StringUtils.hasText(english.getBackdropPath())) {
			content.setBackdropUrl(contentImageService.toBackdropUrl(english.getBackdropPath()));
		}
	}

	/**
	 * MOD-04 신고 사유 목록. COMMON_CODE에서 코드와 순서를 받고 문구만 번들로 갈아 끼운다 -
	 * CODE_NAME이 한국어 한 컬럼이라 그대로 쓰면 영어 화면이 깨진다(F-01).
	 * 번들에 키가 없으면 CODE_NAME을 그대로 쓰므로, 사유가 늘어도 화면에는 바로 뜬다.
	 * 조회가 실패하면 빈 목록이라 화면이 신고 버튼을 그리지 않는다.
	 */
	private void addReportReasons(Model model) {
		List<CodeVO> reasons;

		try {
			Map<String, Object> codeParam = new HashMap<>();
			codeParam.put(CODE_PARAM_KEY, new String[] { CODE_GROUP_REPORT_REASON });
			reasons = codeService.doRetrieve(codeParam);

			for (CodeVO reason : reasons) {
				reason.setCodeName(messageSource.getMessage(
						MSG_PREFIX_REPORT_REASON + reason.getCode(), null,
						reason.getCodeName(), LocaleContextHolder.getLocale()));
			}
		} catch (RuntimeException e) {
			// 신고 버튼만 빠지고 나머지 섹션은 살린다
			log.warn("신고 사유 코드 조회에 실패했습니다.", e);
			reasons = Collections.emptyList();
		}

		model.addAttribute("reportReasons", reasons);
	}

	/**
	 * 팀 공용 인증 정보에서 화면에 사용할 로그인 회원 번호 조회
	 *
	 * @return 로그인 회원 번호, 비회원이면 null
	 * @throws ArithmeticException 회원 번호가 int 범위를 벗어난 경우
	 */
	private Integer toCurrentMemberId() {
		OptionalLong currentMemberId = findCurrentMemberId();

		return currentMemberId.isEmpty() ? null : Math.toIntExact(currentMemberId.getAsLong());
	}

	/**
	 * 팀 공용 인증 정보에서 컬렉션 서비스에 전달할 현재 회원 번호 조회
	 *
	 * @return 로그인 회원 번호, 비회원이면 빈 OptionalLong
	 */
	private static OptionalLong findCurrentMemberId() {
		LoginMember loginMember = LoginMemberHelper.getLoginMember();

		return loginMember == null ? OptionalLong.empty() : OptionalLong.of(loginMember.getMemberId());
	}

}
