package com.endit.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import com.endit.cmn.DTO;
import com.endit.cmn.LoginMember;
import com.endit.domain.CollectionVO;
import com.endit.domain.ContentVO;
import com.endit.domain.PersonLikeVO;
import com.endit.domain.PersonVO;
import com.endit.security.LoginMemberHelper;
import com.endit.service.CollectionService;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentService;
import com.endit.service.PersonLikeService;
import com.endit.service.PersonService;

/**
 * 검색 화면(S-01 검색 시작 / S-02 통합검색 / S-03 영화 전체)의 경로를 처리하는 Controller
 */
@Controller
public class SearchViewController {

	private static final Logger log = LoggerFactory.getLogger(SearchViewController.class);

	// 콘텐츠 검색 축 - 국문/원제 동시 부분일치
	private static final String SEARCH_BY_TITLE = "50";
	// 컬렉션 검색 축 - 제목 부분일치
	private static final String SEARCH_BY_COLLECTION_TITLE = "10";

	// 인물 카드의 역할 표기 - PersonVO에 역할이 없어 CONTENT_CREDIT에서 따로 읽는다
	private static final String LABEL_DIRECTOR = "감독";
	private static final String LABEL_ACTOR = "배우";

	// 인물 검색 시작 화면의 인기 인물 건수 - 영화의 박스오피스 순위와 같은 자리다
	private static final int RANKING_SIZE = 5;

	private static final int FIRST_PAGE_NO = 1;
	private static final String FIRST_PAGE_NO_TEXT = "1";

	// 검색어 길이 상한 - 넘으면 조회하지 않고 검색창만 남긴 채 안내한다
	private static final int MAX_QUERY_LENGTH = 100;

	// S-03 정렬 축 - 기본은 관련도다
	private static final String SEARCH_KEY_SORT = "sort";
	private static final String SORT_RELEVANCE = "relevance";

	// 비회원일 때 화면에 내려보내는 회원 번호 - 카드가 "내 컬렉션"을 아무에게도 붙이지 않는다
	private static final long NO_MEMBER_ID = 0L;

	// S-02 유형별 미리보기 건수 - 영화 결과를 우선 노출한다
	private static final int PREVIEW_MOVIE_SIZE = 6;
	private static final int PREVIEW_PERSON_SIZE = 4;
	// 컬렉션 격자는 3열이라(collection-list.css) 3건이 정확히 한 줄이다
	private static final int PREVIEW_COLLECTION_SIZE = 3;

	// S-03 포스터 그리드 한 페이지 건수
	private static final int MOVIE_PAGE_SIZE = 12;

	// 인물 결과 전체 탐색 한 페이지 건수 - 목록 행이 2열이라 6줄이 된다
	private static final int PERSON_PAGE_SIZE = 12;

	private static final String SEARCH_VIEW = "search/index";
	private static final String MOVIE_SEARCH_VIEW = "search/movies";
	private static final String PERSON_SEARCH_VIEW = "search/people";

	private final ContentService contentService;
	private final PersonService personService;
	private final CollectionService collectionService;
	private final ContentCreditService contentCreditService;
	private final PersonLikeService personLikeService;

	public SearchViewController(
			ContentService contentService,
			PersonService personService,
			CollectionService collectionService,
			ContentCreditService contentCreditService,
			PersonLikeService personLikeService) {
		this.contentService = contentService;
		this.personService = personService;
		this.collectionService = collectionService;
		this.contentCreditService = contentCreditService;
		this.personLikeService = personLikeService;
	}

	/** 검색 시작(S-01)과 통합검색(S-02) - 같은 경로이고 검색어 유무로 갈린다 */
	// 검색창 프래그먼트가 maxlength와 안내 문구에 함께 쓴다 - 상한을 화면에 다시 적지 않게 한다
	@ModelAttribute("maxQueryLength")
	public int toMaxQueryLength() {
		return MAX_QUERY_LENGTH;
	}

	@GetMapping("/search")
	public String search(@RequestParam(defaultValue = "") String query, Model model) {

		String searchWord = query.trim();
		model.addAttribute("query", searchWord);

		// 상한을 넘으면 조회 없이 S-01로 되돌린다. 검색창과 순위는 그대로 남는다
		if (isTooLong(searchWord)) {
			model.addAttribute("query", "");
			model.addAttribute("queryTooLong", true);
			return SEARCH_VIEW;
		}

		// 박스오피스 순위는 화면이 /api/search/rankings로 따로 읽는다
		if (searchWord.isEmpty()) {
			return SEARCH_VIEW;
		}

		boolean movieShown = addMoviePreview(searchWord, model);
		boolean personShown = addPersonPreview(searchWord, model);
		// 컬렉션 매퍼는 아직 ESCAPE가 없어(타 담당) 걷어낸 검색어로 조회한다
		boolean collectionShown = addCollectionPreview(toWildcardFreeWord(searchWord), model);

		// S-04 판정 - 세 유형이 모두 0건이고 오류도 없을 때. 이때는 초기 순위를 같이 보여준다
		model.addAttribute("noResults", !movieShown && !personShown && !collectionShown);

		return SEARCH_VIEW;
	}

	/**
	 * 영화 전용 검색(S-03) - 검색어가 있으면 결과를 페이징하고, 없으면 검색 시작 화면을 그린다.
	 * 검색어 없는 상태는 S-02 머릿말이 들어오는 자리라 박스오피스 순위를 같이 보여준다
	 */
	@GetMapping("/search/movies")
	public String searchMovies(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(name = "page", defaultValue = FIRST_PAGE_NO_TEXT) int page,
			@RequestParam(defaultValue = SORT_RELEVANCE) String sort,
			Model model) {

		String searchWord = query.trim();
		model.addAttribute("sort", sort);

		// 상한을 넘으면 조회 없이 검색창만 남긴 채 안내한다. 이 화면에 자기 검색창이 있어 되돌릴 이유가 없다
		if (isTooLong(searchWord)) {
			model.addAttribute("query", "");
			model.addAttribute("queryTooLong", true);
			return MOVIE_SEARCH_VIEW;
		}

		// 검색어가 없으면 매퍼의 조건이 통째로 빠져 전 건이 나온다. 조회하지 않고 시작 화면으로 둔다
		model.addAttribute("query", searchWord);

		if (searchWord.isEmpty()) {
			return MOVIE_SEARCH_VIEW;
		}

		// 목록·건수·페이저는 화면이 /api/search/contents로 직접 읽는다(정의서 S-03 데이터 항목).
		// 여기서는 껍데기와 검색어·정렬·한 페이지 건수만 넘긴다
		model.addAttribute("pageNo", page);
		model.addAttribute("pageSize", MOVIE_PAGE_SIZE);

		return MOVIE_SEARCH_VIEW;
	}

	/**
	 * 인물 전용 검색 - 검색어가 있으면 결과를 페이징하고, 없으면 검색 시작 화면을 그린다.
	 * 검색어 없는 상태에서는 좋아요를 많이 받은 인기 인물을 보여준다(영화의 박스오피스 자리).
	 * PersonService는 정렬 축을 받지 않아(이기준 소유) 매퍼의 기본 정렬을 그대로 쓴다
	 */
	@GetMapping("/search/people")
	public String searchPeople(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(name = "page", defaultValue = FIRST_PAGE_NO_TEXT) int page,
			Model model) {

		String searchWord = query.trim();

		// 영화와 같이, 상한을 넘으면 조회 없이 검색창만 남긴 채 안내한다
		if (isTooLong(searchWord)) {
			model.addAttribute("query", "");
			model.addAttribute("queryTooLong", true);
			addPopularPeople(model);
			return PERSON_SEARCH_VIEW;
		}

		// 영화와 같은 이유로, 검색어가 없으면 전 건이 나온다. 조회하지 않고 시작 화면으로 둔다
		if (searchWord.isEmpty()) {
			model.addAttribute("query", searchWord);
			addPopularPeople(model);
			return PERSON_SEARCH_VIEW;
		}

		DTO param = new DTO();
		param.setPageNo(page);
		param.setPageSize(PERSON_PAGE_SIZE);
		param.setSearchWord(searchWord);

		List<PersonVO> people;

		try {
			people = personService.retrieve(param);
		} catch (RuntimeException e) {
			// 한 화면이 통째로 죽는 것보다 빈 결과와 오류 표시가 낫다
			log.warn("인물 전체 검색에 실패했습니다. query={}", searchWord, e);
			people = Collections.emptyList();
			model.addAttribute("personFailed", true);
		}

		model.addAttribute("query", searchWord);
		model.addAttribute("people", people);
		model.addAttribute("personRoles", toPersonRoleLabels(people));
		// 페이저가 쓸 값(pageNo/totalCnt/startNo/endNo/pre/next)이 모두 이 안에 있다
		model.addAttribute("paging", param);

		return PERSON_SEARCH_VIEW;
	}

	// 인물 검색 시작 화면의 순위 - 영화의 박스오피스와 같은 자리라 건수를 맞춘다.
	// 좋아요가 없으면 빈 목록이고, 그때는 화면이 섹션을 통째로 숨긴다
	private void addPopularPeople(Model model) {
		List<PersonLikeVO> popularPeople;

		try {
			popularPeople = personLikeService.retrievePopular(RANKING_SIZE);
		} catch (RuntimeException e) {
			// 순위는 부가 정보다. 못 읽어도 검색창은 그대로 남아야 한다
			log.warn("인기 인물 조회에 실패했습니다.", e);
			popularPeople = Collections.emptyList();
		}

		model.addAttribute("popularPeople", popularPeople);
		model.addAttribute("popularRoles", toPopularRoleLabels(popularPeople));
	}

	// 인기 인물의 역할 표기 - 검색 결과와 같은 규칙을 써야 한 화면에서 라벨이 갈리지 않는다
	private Map<Integer, String> toPopularRoleLabels(List<PersonLikeVO> popularPeople) {
		List<Integer> personIds = new ArrayList<>();

		for (PersonLikeVO person : popularPeople) {
			personIds.add(person.getPersonId());
		}

		return toRoleLabels(personIds);
	}

	// 검색어 상한 판정. 빈 검색어는 S-01이 받으므로 하한은 여기서 따로 막지 않는다
	private boolean isTooLong(String searchWord) {
		return searchWord.length() > MAX_QUERY_LENGTH;
	}

	// S-01 컬렉션 미리보기용 - CollectionMapper에 ESCAPE가 들어오면 이 메서드를 지운다
	private String toWildcardFreeWord(String searchWord) {
		return searchWord.replace("%", "").replace("_", "");
	}

	// 영화 미리보기 - 전체 건수가 미리보기 수를 넘으면 화면이 더보기를 노출한다.
	// 결과나 오류로 보여줄 게 있으면 true. S-04(0건) 판정에 쓴다
	private boolean addMoviePreview(String searchWord, Model model) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(PREVIEW_MOVIE_SIZE);
		param.setSearchDiv(SEARCH_BY_TITLE);
		param.setSearchWord(searchWord);
		// S-03과 같은 기본 정렬이라야 더보기로 넘어갈 때 순서가 어긋나지 않는다
		param.getSearchMap().put(SEARCH_KEY_SORT, SORT_RELEVANCE);

		List<ContentVO> movies;
		boolean failed = false;

		try {
			movies = contentService.retrieve(param);
		} catch (RuntimeException e) {
			// 섹션마다 따로 잡아야 한 유형의 실패가 나머지 유형을 가리지 않는다
			log.warn("영화 검색에 실패했습니다. query={}", searchWord, e);
			movies = Collections.emptyList();
			failed = true;
			model.addAttribute("movieFailed", true);
		}

		model.addAttribute("movies", movies);
		model.addAttribute("movieTotalCnt", param.getTotalCnt());
		model.addAttribute("hasMoreMovies", param.getTotalCnt() > movies.size());

		return failed || !movies.isEmpty();
	}

	// 인물 미리보기 - PersonMapper의 otherwise 분기가 이름 부분일치를 맡는다
	private boolean addPersonPreview(String searchWord, Model model) {
		// 빈 검색어를 그대로 조회하면 조건이 빠져 전 건이 나온다
		if (searchWord.isEmpty()) {
			model.addAttribute("people", Collections.emptyList());
			model.addAttribute("personTotalCnt", 0);
			model.addAttribute("personRoles", Collections.emptyMap());
			model.addAttribute("hasMorePeople", false);
			return false;
		}

		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(PREVIEW_PERSON_SIZE);
		param.setSearchWord(searchWord);

		List<PersonVO> people;
		boolean failed = false;

		try {
			people = personService.retrieve(param);
		} catch (RuntimeException e) {
			log.warn("인물 검색에 실패했습니다. query={}", searchWord, e);
			people = Collections.emptyList();
			failed = true;
			model.addAttribute("personFailed", true);
		}

		model.addAttribute("people", people);
		model.addAttribute("personTotalCnt", param.getTotalCnt());
		model.addAttribute("personRoles", toPersonRoleLabels(people));
		model.addAttribute("hasMorePeople", param.getTotalCnt() > people.size());

		return failed || !people.isEmpty();
	}

	// 인물별 역할 표기를 만든다. PersonVO에는 역할이 없어 CONTENT_CREDIT을 따로 읽는다
	private Map<Integer, String> toPersonRoleLabels(List<PersonVO> people) {
		List<Integer> personIds = new ArrayList<>();

		for (PersonVO person : people) {
			personIds.add(person.getPersonId());
		}

		return toRoleLabels(personIds);
	}

	/*
	 * 역할 표기 규칙이 사는 유일한 자리다. 검색 결과와 인기 인물이 같은 화면에 함께 뜨므로
	 * 두 곳이 각자 판정하면 같은 사람에게 다른 라벨이 붙는다(실제로 크레딧 0건에서 갈렸다).
	 * 감독 크레딧이 있으면 감독, 크레딧은 있는데 감독이 아니면 배우,
	 * 크레딧이 아예 없으면 감독도 배우도 아니므로 라벨을 걸지 않는다.
	 */
	private Map<Integer, String> toRoleLabels(List<Integer> personIds) {
		Map<Integer, String> labels = new LinkedHashMap<>();

		if (personIds.isEmpty()) {
			return labels;
		}

		Set<Integer> directorIds;
		Set<Integer> creditedIds;

		try {
			directorIds = contentCreditService.retrieveDirectorIds(personIds);
			creditedIds = contentCreditService.retrieveCreditedIds(personIds);
		} catch (RuntimeException e) {
			// 역할은 부가 정보다. 못 읽어도 인물 카드 자체는 그려야 한다
			log.warn("역할 조회에 실패했습니다. personIds={}", personIds, e);
			return labels;
		}

		for (Integer personId : personIds) {
			if (!creditedIds.contains(personId)) {
				continue;
			}

			labels.put(personId, directorIds.contains(personId) ? LABEL_DIRECTOR : LABEL_ACTOR);
		}

		return labels;
	}

	// 컬렉션 미리보기 - 담당 밖 서비스라 호출만 하고 계약은 그쪽에 맡긴다.
	// 카드는 컬렉션 목록 화면의 createCollectionCard가 그리므로 목록을 그대로 넘긴다
	private boolean addCollectionPreview(String searchWord, Model model) {
		// 더보기는 컬렉션 목록 화면으로 나간다. 그쪽 매퍼에 ESCAPE가 없어 걷어낸 검색어를 그대로 실어 보낸다
		model.addAttribute("collectionQuery", searchWord);
		// 카드의 "내 컬렉션" 배지 판정값
		model.addAttribute("currentMemberId",
				findCurrentMemberId().orElse(NO_MEMBER_ID));

		// 인물과 같은 이유로, 와일드카드만 있던 검색어는 조회하지 않는다
		if (searchWord.isEmpty()) {
			model.addAttribute("collections", Collections.emptyList());
			model.addAttribute("collectionTotalCnt", 0);
			model.addAttribute("hasMoreCollections", false);
			return false;
		}

		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(PREVIEW_COLLECTION_SIZE);
		param.setSearchDiv(SEARCH_BY_COLLECTION_TITLE);
		param.setSearchWord(searchWord);

		List<CollectionVO> collections;
		boolean failed = false;

		try {
			// 컬렉션 목록 화면과 같은 오버로드를 써야 본인 비공개분과 좋아요 여부가 같이 나온다
			collections = collectionService.retrieve(
					param, findCurrentMemberId());
		} catch (RuntimeException e) {
			log.warn("컬렉션 검색에 실패했습니다. query={}", searchWord, e);
			collections = Collections.emptyList();
			failed = true;
			model.addAttribute("collectionFailed", true);
		}

		model.addAttribute("collections", collections);
		model.addAttribute("collectionTotalCnt", param.getTotalCnt());
		model.addAttribute("hasMoreCollections", param.getTotalCnt() > collections.size());

		return failed || !collections.isEmpty();
	}

	/**
	 * 팀 공용 인증 정보에서 컬렉션 조회에 사용할 현재 회원 번호 조회
	 *
	 * @return 로그인 회원 번호, 비회원이면 빈 OptionalLong
	 */
	private static OptionalLong findCurrentMemberId() {
		LoginMember loginMember = LoginMemberHelper.getLoginMember();

		return loginMember == null ? OptionalLong.empty() : OptionalLong.of(loginMember.getMemberId());
	}

}
