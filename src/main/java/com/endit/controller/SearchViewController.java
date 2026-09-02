package com.endit.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.endit.cmn.DTO;
import com.endit.domain.CollectionVO;
import com.endit.domain.ContentVO;
import com.endit.domain.PersonVO;
import com.endit.service.CollectionService;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentService;
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

	private static final int FIRST_PAGE_NO = 1;
	private static final String FIRST_PAGE_NO_TEXT = "1";

	// 검색어 길이 상한 - 넘으면 조회하지 않고 검색창만 남긴 채 안내한다
	private static final int MAX_QUERY_LENGTH = 100;

	// S-03 정렬 축 - 기본은 관련도다
	private static final String SEARCH_KEY_SORT = "sort";
	private static final String SORT_RELEVANCE = "relevance";

	// S-02 유형별 미리보기 건수 - 영화 결과를 우선 노출한다
	private static final int PREVIEW_MOVIE_SIZE = 6;
	private static final int PREVIEW_PERSON_SIZE = 4;
	private static final int PREVIEW_COLLECTION_SIZE = 4;

	// S-03 포스터 그리드 한 페이지 건수
	private static final int MOVIE_PAGE_SIZE = 12;

	// 인물 결과 전체 탐색 한 페이지 건수 - 목록 행이 2열이라 6줄이 된다
	private static final int PERSON_PAGE_SIZE = 12;

	private static final String SEARCH_VIEW = "search/index";
	private static final String MOVIE_SEARCH_VIEW = "search/movies";
	private static final String PERSON_SEARCH_VIEW = "search/people";
	// 검색어 없이 전체 탐색 화면에 들어온 요청이 되돌아갈 곳
	private static final String REDIRECT_SEARCH = "redirect:/search";

	private final ContentService contentService;
	private final PersonService personService;
	private final CollectionService collectionService;
	private final ContentCreditService contentCreditService;

	public SearchViewController(
			ContentService contentService,
			PersonService personService,
			CollectionService collectionService,
			ContentCreditService contentCreditService) {
		this.contentService = contentService;
		this.personService = personService;
		this.collectionService = collectionService;
		this.contentCreditService = contentCreditService;
	}

	/** 검색 시작(S-01)과 통합검색(S-02) - 같은 경로이고 검색어 유무로 갈린다 */
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

	/** 영화 결과 전체 탐색(S-03) - 검색어를 유지한 채 페이징한다 */
	@GetMapping("/search/movies")
	public String searchMovies(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(name = "page", defaultValue = FIRST_PAGE_NO_TEXT) int page,
			@RequestParam(defaultValue = SORT_RELEVANCE) String sort,
			RedirectAttributes redirectAttributes,
			Model model) {

		String searchWord = query.trim();
		model.addAttribute("sort", sort);

		// 상한을 넘긴 검색어의 안내는 S-01이 맡는다. 검색어를 실어 보내야 거기서 같은 판정이 선다
		if (isTooLong(searchWord)) {
			redirectAttributes.addAttribute("query", searchWord);
			return REDIRECT_SEARCH;
		}

		// 검색어가 없으면 매퍼의 조건이 통째로 빠져 전 건이 나온다. 검색 시작으로 되돌린다
		if (searchWord.isEmpty()) {
			return REDIRECT_SEARCH;
		}

		// 목록·건수·페이저는 화면이 /api/search/contents로 직접 읽는다(정의서 S-03 데이터 항목).
		// 여기서는 껍데기와 검색어·정렬·한 페이지 건수만 넘긴다
		model.addAttribute("query", searchWord);
		model.addAttribute("pageNo", page);
		model.addAttribute("pageSize", MOVIE_PAGE_SIZE);

		return MOVIE_SEARCH_VIEW;
	}

	/**
	 * 인물 결과 전체 탐색 - 검색어를 유지한 채 페이징한다.
	 * PersonService는 정렬 축을 받지 않아(이기준 소유) 매퍼의 기본 정렬을 그대로 쓴다
	 */
	@GetMapping("/search/people")
	public String searchPeople(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(name = "page", defaultValue = FIRST_PAGE_NO_TEXT) int page,
			RedirectAttributes redirectAttributes,
			Model model) {

		String searchWord = query.trim();

		// 영화와 같이 상한 안내는 S-01이 맡는다
		if (isTooLong(searchWord)) {
			redirectAttributes.addAttribute("query", searchWord);
			return REDIRECT_SEARCH;
		}

		// 영화와 같은 이유로, 검색어가 없으면 전 건이 나오므로 검색 시작으로 되돌린다
		if (searchWord.isEmpty()) {
			return REDIRECT_SEARCH;
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
		model.addAttribute("personRoles", toRoleLabels(people));
		// 페이저가 쓸 값(pageNo/totalCnt/startNo/endNo/pre/next)이 모두 이 안에 있다
		model.addAttribute("paging", param);

		return PERSON_SEARCH_VIEW;
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
		model.addAttribute("personRoles", toRoleLabels(people));
		model.addAttribute("hasMorePeople", param.getTotalCnt() > people.size());

		return failed || !people.isEmpty();
	}

	// 인물별 역할 표기를 만든다. PersonVO에는 역할이 없어 CONTENT_CREDIT을 따로 읽는다.
	private Map<Integer, String> toRoleLabels(List<PersonVO> people) {
		Map<Integer, String> labels = new LinkedHashMap<>();

		if (people.isEmpty()) {
			return labels;
		}

		List<Integer> personIds = new ArrayList<>();

		for (PersonVO person : people) {
			personIds.add(person.getPersonId());
		}

		Set<Integer> directorIds;

		try {
			directorIds = contentCreditService.retrieveDirectorIds(personIds);
		} catch (RuntimeException e) {
			// 역할은 부가 정보다. 못 읽어도 인물 카드 자체는 그려야 한다
			log.warn("역할 조회에 실패했습니다. personIds={}", personIds, e);
			return labels;
		}

		for (PersonVO person : people) {
			// 감독 크레딧이 하나라도 있으면 감독으로 본다
			labels.put(person.getPersonId(),
					directorIds.contains(person.getPersonId()) ? LABEL_DIRECTOR : LABEL_ACTOR);
		}

		return labels;
	}

	// 컬렉션 미리보기 - 담당 밖 서비스라 호출만 하고 계약은 그쪽에 맡긴다
	private boolean addCollectionPreview(String searchWord, Model model) {
		// 인물과 같은 이유로, 와일드카드만 있던 검색어는 조회하지 않는다
		if (searchWord.isEmpty()) {
			model.addAttribute("collections", Collections.emptyList());
			model.addAttribute("collectionTotalCnt", 0);
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
			collections = collectionService.retrieve(param);
		} catch (RuntimeException e) {
			log.warn("컬렉션 검색에 실패했습니다. query={}", searchWord, e);
			collections = Collections.emptyList();
			failed = true;
			model.addAttribute("collectionFailed", true);
		}

		model.addAttribute("collections", collections);
		model.addAttribute("collectionTotalCnt", param.getTotalCnt());

		return failed || !collections.isEmpty();
	}

}
