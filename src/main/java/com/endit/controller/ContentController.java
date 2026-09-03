package com.endit.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.service.ContentService;

@RestController
@RequestMapping("/content")
public class ContentController {

	private static final String DEFAULT_SYNC_LIMIT = "50";

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
	 *               운영에서는 ContentRankScheduler가 기동 시와 하루 한 번 같은 일을 한다.
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

}
