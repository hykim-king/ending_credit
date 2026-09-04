package com.endit.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.domain.ContentVO;
import com.endit.service.ContentService;

@RestController
@RequestMapping("/content")
public class ContentController {

	private static final String DEFAULT_SYNC_LIMIT = "50";

	// 순위 조회용. 정렬 축 상수가 ContentServiceImpl에 private이라 호출부에서 다시 정의한다
	private static final String SEARCH_KEY_SORT = "sort";
	private static final String SORT_POPULAR = "popular";
	private static final String SEARCH_KEY_GENRE_ID = "genreId";

	// @RequestParam의 defaultValue는 문자열만 받는다. 서비스의 페이지 상한과 같은 값이다
	private static final String FIRST_PAGE_NO = "1";
	private static final String DEFAULT_RANK_VIEW_SIZE = "100";

	private final ContentService contentService;

	public ContentController(ContentService contentService) {
		this.contentService = contentService;
	}

	/**
	 *
	 * <pre>
	 * Method Name : sync
	 * Description : TMDB 인기 영화를 훑어 우리 DB에 없는 것만 적재한다.
	 *               이미 있는 영화는 건드리지 않으므로 반복 호출해도 갱신은 일어나지 않는다.
	 *               DB에 쓰는 동작이지만 수동 재적재를 주소창에서 부를 수 있게 GET도 받는다.
	 *               limit을 채울 때까지 페이지를 넘기므로, DB가 이미 찬 상태에서는 오래 걸릴 수 있다.
	 *
	 * </pre>
	 *
	 * @param limit
	 * @return Map (insertedCount - 신규 저장 건수)
	 */
	@RequestMapping(value = "/tmdb/popular", method = { RequestMethod.GET, RequestMethod.POST })
	public Map<String, Object> sync(@RequestParam(defaultValue = DEFAULT_SYNC_LIMIT) int limit) {
		int insertedCount = contentService.sync(limit);

		Map<String, Object> result = new HashMap<>();
		result.put("insertedCount", insertedCount);
		return result;
	}

	/**
	 *
	 * <pre>
	 * Method Name : syncRank
	 * Description : TMDB 인기순위를 지금 즉시 다시 받아 순위 목록을 갱신한다.
	 *               ContentRankScheduler가 기동 시에도 같은 일을 한다. 하루 한 번 도는 쪽은
	 *               구현만 해 두고 꺼 놓았으므로(학습용 프로젝트라 상시 구동하지 않는다),
	 *               기동 이후 순위를 다시 받는 통로는 지금 이 엔드포인트뿐이다.
	 *               DB에 쓰지는 않지만 외부 호출을 일으키고 서버 상태를 바꾸므로 POST로 둔다.
	 *
	 * </pre>
	 *
	 * @return Map (matchedCount - 우리 DB와 매칭된 건수)
	 */
	@PostMapping("/tmdb/rank")
	public Map<String, Object> syncRank() {
		int matchedCount = contentService.syncRank();

		Map<String, Object> result = new HashMap<>();
		result.put("matchedCount", matchedCount);
		return result;
	}

	/**
	 *
	 * <pre>
	 * Method Name : syncGenreRank
	 * Description : 장르별 인기순위를 지금 즉시 다시 받는다. 홈의 장르 선반이 이 목록으로 정렬된다.
	 *               장르 수만큼 TMDB를 부르므로 전체 순위(/tmdb/rank)보다 오래 걸린다.
	 *               전체 순위와 따로 둔 이유는 둘의 비용이 크게 다르고, 장르 쪽만 다시 받고 싶을 때가 있어서다.
	 *               DB에 쓰지는 않지만 외부 호출을 일으키고 서버 상태를 바꾸므로 POST로 둔다.
	 *
	 * </pre>
	 *
	 * @return Map (genreCount - 순위가 하나라도 잡힌 장르 수)
	 */
	@PostMapping("/tmdb/rank/genre")
	public Map<String, Object> syncGenreRank() {
		int genreCount = contentService.syncGenreRank();

		Map<String, Object> result = new HashMap<>();
		result.put("genreCount", genreCount);
		return result;
	}

	/**
	 *
	 * <pre>
	 * Method Name : retrieveRank
	 * Description : 지금 들고 있는 인기순위를 순번과 제목까지 붙여 내려준다. 화면이 아니라 교차검증용이다.
	 *               순위는 서비스 메모리에만 있어 이 통로가 없으면 눈으로 확인할 방법이 없다.
	 *               TMDB의 같은 목록(/discover/movie?with_genres=..&amp;sort_by=popularity.desc)과
	 *               나란히 놓고 우리 목록이 그쪽 순서의 부분수열인지 보면 검증이 된다 -
	 *               보유하지 않은 영화만 빠질 뿐 앞뒤 관계는 뒤집히지 않기 때문이다.
	 *               genreId를 주면 그 장르 순위, 안 주면 전체 순위다.
	 *               한 번에 최대 100건이라(서비스의 페이지 상한) 그보다 뒤는 page로 넘긴다.
	 *               순위가 비어 있으면 서비스가 적재순으로 폴백하므로, 이 응답이 content_id 오름차순이면
	 *               순위가 안 잡힌 것이다.
	 *
	 * </pre>
	 *
	 * @param genreId
	 * @param page
	 * @param size
	 * @return List (no - 순위 숫자, contentId, externalId - TMDB id, titleKo)
	 */
	@GetMapping("/tmdb/rank")
	public List<Map<String, Object>> retrieveRank(
			@RequestParam(required = false) Integer genreId,
			@RequestParam(defaultValue = FIRST_PAGE_NO) int page,
			@RequestParam(defaultValue = DEFAULT_RANK_VIEW_SIZE) int size) {

		DTO param = new DTO();
		param.setPageNo(page);
		param.setPageSize(size);
		param.getSearchMap().put(SEARCH_KEY_SORT, SORT_POPULAR);

		if (genreId != null) {
			param.getSearchMap().put(SEARCH_KEY_GENRE_ID, String.valueOf(genreId));
		}

		List<Map<String, Object>> rows = new ArrayList<>();

		// 포스터 URL 같은 화면용 필드는 대조에 방해만 되므로 네 칸만 추린다
		for (ContentVO content : contentService.retrieve(param)) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("no", content.getNo());
			row.put("contentId", content.getContentId());
			row.put("externalId", content.getExternalId());
			row.put("titleKo", content.getTitleKo());
			rows.add(row);
		}

		return rows;
	}

}
