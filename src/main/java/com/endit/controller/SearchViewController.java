package com.endit.controller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

	// 정렬 축 - 박스오피스는 적재 순서를 순위로 쓴다
	private static final String SEARCH_KEY_SORT = "sort";
	private static final String SORT_BOX_OFFICE = "boxoffice";

	// 인물 카드의 역할 표기 - PersonVO에 역할이 없어 CONTENT_CREDIT에서 따로 읽는다
	private static final String SEARCH_KEY_ROLE = "role";
	private static final String ROLE_DIRECTOR = "DIRECTOR";
	private static final String LABEL_DIRECTOR = "감독";
	private static final String LABEL_ACTOR = "배우";
	// 건수만 필요하므로 한 건만 받는다
	private static final int ROLE_PROBE_SIZE = 1;

	private static final int FIRST_PAGE_NO = 1;
	private static final String FIRST_PAGE_NO_TEXT = "1";

	// S-01 박스오피스 순위 노출 건수
	private static final int RANKING_SIZE = 10;

	// S-02 유형별 미리보기 건수 - 영화 결과를 우선 노출한다
	private static final int PREVIEW_MOVIE_SIZE = 6;
	private static final int PREVIEW_PERSON_SIZE = 4;
	private static final int PREVIEW_COLLECTION_SIZE = 4;

	// S-03 포스터 그리드 한 페이지 건수
	private static final int MOVIE_PAGE_SIZE = 12;

	private static final String SEARCH_VIEW = "search/index";
	private static final String MOVIE_SEARCH_VIEW = "search/movies";

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

		if (searchWord.isEmpty()) {
			addRankings(model);
			return SEARCH_VIEW;
		}

		addMoviePreview(searchWord, model);
		addPersonPreview(searchWord, model);
		addCollectionPreview(searchWord, model);

		return SEARCH_VIEW;
	}

	/** 영화 결과 전체 탐색(S-03) - 검색어를 유지한 채 페이징한다 */
	@GetMapping("/search/movies")
	public String searchMovies(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(name = "pageNo", defaultValue = FIRST_PAGE_NO_TEXT) int pageNo,
			Model model) {

		String searchWord = query.trim();

		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(MOVIE_PAGE_SIZE);
		param.setSearchDiv(SEARCH_BY_TITLE);
		param.setSearchWord(searchWord);

		List<ContentVO> movies;

		try {
			movies = contentService.retrieve(param);
		} catch (RuntimeException e) {
			// 한 화면이 통째로 죽는 것보다 빈 결과와 오류 표시가 낫다
			log.warn("영화 전체 검색에 실패했습니다. query={}", searchWord, e);
			movies = Collections.emptyList();
			model.addAttribute("movieFailed", true);
		}

		model.addAttribute("query", searchWord);
		model.addAttribute("movies", movies);
		// 페이저가 쓸 값(pageNo/totalCnt/startNo/endNo/pre/next)이 모두 이 안에 있다
		model.addAttribute("paging", param);

		return MOVIE_SEARCH_VIEW;
	}

	// 박스오피스 순위 - 실패하거나 0건이면 검색창만 남기고 섹션을 숨긴다
	private void addRankings(Model model) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(RANKING_SIZE);
		param.getSearchMap().put(SEARCH_KEY_SORT, SORT_BOX_OFFICE);

		try {
			// no(rnum)가 곧 순위다. 매퍼가 content_id 오름차순으로 정렬해 준다
			model.addAttribute("rankings", contentService.retrieve(param));
		} catch (RuntimeException e) {
			log.warn("박스오피스 순위 조회에 실패했습니다.", e);
			model.addAttribute("rankings", Collections.emptyList());
		}
	}

	// 영화 미리보기 - 전체 건수가 미리보기 수를 넘으면 화면이 더보기를 노출한다
	private void addMoviePreview(String searchWord, Model model) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(PREVIEW_MOVIE_SIZE);
		param.setSearchDiv(SEARCH_BY_TITLE);
		param.setSearchWord(searchWord);

		List<ContentVO> movies;

		try {
			movies = contentService.retrieve(param);
		} catch (RuntimeException e) {
			// 섹션마다 따로 잡아야 한 유형의 실패가 나머지 유형을 가리지 않는다
			log.warn("영화 검색에 실패했습니다. query={}", searchWord, e);
			movies = Collections.emptyList();
			model.addAttribute("movieFailed", true);
		}

		model.addAttribute("movies", movies);
		model.addAttribute("movieTotalCnt", param.getTotalCnt());
		model.addAttribute("hasMoreMovies", param.getTotalCnt() > movies.size());
	}

	// 인물 미리보기 - PersonMapper의 otherwise 분기가 이름 부분일치를 맡는다
	private void addPersonPreview(String searchWord, Model model) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(PREVIEW_PERSON_SIZE);
		param.setSearchWord(searchWord);

		List<PersonVO> people;

		try {
			people = personService.retrieve(param);
		} catch (RuntimeException e) {
			log.warn("인물 검색에 실패했습니다. query={}", searchWord, e);
			people = Collections.emptyList();
			model.addAttribute("personFailed", true);
		}

		model.addAttribute("people", people);
		model.addAttribute("personTotalCnt", param.getTotalCnt());
		model.addAttribute("personRoles", toRoleLabels(people));
	}

	// 인물별 역할 표기를 만든다. PersonVO에는 역할이 없어 CONTENT_CREDIT을 인물마다 한 번씩 더 읽는다.
	// 미리보기가 4명 고정이라 감수하는 비용이고, 목록이 길어지면 매퍼에 조인을 넣어야 한다
	private Map<Integer, String> toRoleLabels(List<PersonVO> people) {
		Map<Integer, String> labels = new LinkedHashMap<>();

		for (PersonVO person : people) {
			DTO param = new DTO();
			param.setPageNo(FIRST_PAGE_NO);
			param.setPageSize(ROLE_PROBE_SIZE);
			param.getSearchMap().put(SEARCH_KEY_ROLE, ROLE_DIRECTOR);

			try {
				contentCreditService.retrieveByPerson(person.getPersonId(), param);
				// 감독 크레딧이 하나라도 있으면 감독으로 본다
				labels.put(person.getPersonId(),
						param.getTotalCnt() > 0 ? LABEL_DIRECTOR : LABEL_ACTOR);
			} catch (RuntimeException e) {
				// 역할은 부가 정보다. 못 읽어도 인물 카드 자체는 그려야 한다
				log.warn("역할 조회에 실패했습니다. personId={}", person.getPersonId(), e);
			}
		}

		return labels;
	}

	// 컬렉션 미리보기 - 담당 밖 서비스라 호출만 하고 계약은 그쪽에 맡긴다
	private void addCollectionPreview(String searchWord, Model model) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(PREVIEW_COLLECTION_SIZE);
		param.setSearchDiv(SEARCH_BY_COLLECTION_TITLE);
		param.setSearchWord(searchWord);

		List<CollectionVO> collections;

		try {
			collections = collectionService.retrieve(param);
		} catch (RuntimeException e) {
			log.warn("컬렉션 검색에 실패했습니다. query={}", searchWord, e);
			collections = Collections.emptyList();
			model.addAttribute("collectionFailed", true);
		}

		model.addAttribute("collections", collections);
		model.addAttribute("collectionTotalCnt", param.getTotalCnt());
	}

}
