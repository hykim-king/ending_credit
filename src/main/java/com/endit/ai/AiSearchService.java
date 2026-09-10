/**
 * AI 검색 흐름 지휘
 *
 * ① AI 가 문장에서 조건을 뽑는다   (영화는 안 만짐)
 * ② 우리 SQL 이 그 조건으로 찾는다  (결과는 전부 DB 산)
 *
 * AI 가 죽으면 ①만 건너뛰고 ②는 최신순으로 그대로 돈다.
 */
package com.endit.ai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.endit.ai.dto.AiHelpAnswer;
import com.endit.ai.dto.AiNoticeItem;
import com.endit.ai.dto.AiSearchItem;
import com.endit.ai.dto.AiSearchResponse;
import com.endit.ai.dto.SearchIntent;
import com.endit.mapper.AiSearchMapper;
import com.endit.service.ContentImageService;

@Service
public class AiSearchService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** 상한을 넘는 요청은 여기서 잘라낸다 */
	private static final int MAX_LIMIT = 50;

	private final SearchIntentResolver intentResolver;
	private final AiSearchMapper aiSearchMapper;
	private final ContentEmbeddingService embeddingService;
	private final ContentImageService contentImageService;

	public AiSearchService(SearchIntentResolver intentResolver, AiSearchMapper aiSearchMapper,
			ContentEmbeddingService embeddingService, ContentImageService contentImageService) {
		this.intentResolver = intentResolver;
		this.aiSearchMapper = aiSearchMapper;
		this.embeddingService = embeddingService;
		this.contentImageService = contentImageService;
		log.debug("intentResolver: {}", intentResolver);
		log.debug("aiSearchMapper: {}", aiSearchMapper);
	}

	/**
	 * 자연어 문장으로 영화를 찾는다.
	 *
	 * @param query 사용자 입력 문장
	 * @return 의도 + 영화 목록
	 */
	public AiSearchResponse search(String query) {
		log.debug("=============================");
		log.debug("{}()", "search");
		log.debug("query: {}", query);
		log.debug("=============================");

		SearchIntent intent = intentResolver.resolve(query);

		// 영화와도 사이트와도 무관한 질문이면 아무것도 뒤지지 않는다.
		// 진짜 AI 는 message 를 비워 보내기도 하므로(실측) 기본 안내를 보장한다
		if (intent.isOutOfScope()) {
			if (null == intent.getMessage() || intent.getMessage().isBlank()) {
				intent.setMessage("영화 얘기만 도와드릴 수 있어요. 어떤 영화를 찾으세요?");
			}
			return AiSearchResponse.outOfScope(intent);
		}

		// 사이트 사용법 - AI 는 키만 골랐고 본문은 우리가 적어둔 원문 그대로
		if (intent.isSiteHelp()) {
			AiHelpAnswer answer = FaqAnswers.find(intent.getFaqKey());

			if (null == answer) {
				intent.setMessage("아직 준비되지 않은 안내입니다. 공지사항을 확인해 주세요.");
				return AiSearchResponse.outOfScope(intent);
			}
			return AiSearchResponse.ofHelp(intent, answer);
		}

		// 공지 검색 - 우리 매퍼의 전용 SELECT (2조 SQL 은 손대지 않는다)
		if (intent.isNoticeSearch()) {
			return AiSearchResponse.ofNotices(intent, aiSearchMapper.doSearchNotices(intent));
		}

		// 뜻 검색 - 글자가 아니라 좌표 거리로 찾는다.
		// 좌표는 문장 원문이 아니라 AI 가 발라낸 느낌 낱말로 잰다 -
		// "추천해줘" 같은 군더더기가 좌표를 흐리기 때문이다(실측으로 확인)
		if ("semantic".equals(intent.getIntent())) {
			String target = (null != intent.getKeywords() && false == intent.getKeywords().isEmpty())
					? String.join(" ", intent.getKeywords())
					: query;

			List<AiSearchItem> items = embeddingService.searchByMeaning(
					target, intent.getLimit() > 0 ? intent.getLimit() : 10);

			if (items.isEmpty()) {
				intent.setMessage("아직 뜻 좌표가 준비되지 않아 글자 검색으로 대신합니다.");
				intent.setIntent(SearchIntent.INTENT_KEYWORD);   // 폴백해서 아래로 흘려보낸다
			} else {
				return AiSearchResponse.ofMovies(intent, completePoster(items));
			}
		}

		// 비슷한 영화 - 기준 영화 좌표와의 거리.
		// "OO 는 빼고" 제외 필터를 거치므로 후보를 넉넉히 뽑아 온다
		if ("similar".equals(intent.getIntent())) {
			int limit = intent.getLimit() > 0 ? intent.getLimit() : 5;

			List<AiSearchItem> items = embeddingService.findSimilar(
					intent.getTitle(), limit * 5);

			if (null != items) {
				items = applyExcludes(items, intent.getExcludeTitles(), limit);
			}

			if (null == items) {
				intent.setMessage("\"" + intent.getTitle() + "\" 영화를 찾지 못했습니다.");
				return AiSearchResponse.outOfScope(intent);
			}
			if (items.isEmpty()) {
				intent.setMessage("아직 뜻 좌표가 준비되지 않았습니다. 적재 후 다시 시도해 주세요.");
				return AiSearchResponse.outOfScope(intent);
			}
			return AiSearchResponse.ofMovies(intent, completePoster(items));
		}

		// AI 가 이상한 값을 냈을 때를 대비한 마지막 방어선
		if (intent.getLimit() <= 0 || intent.getLimit() > MAX_LIMIT) {
			intent.setLimit(20);
		}

		List<AiSearchItem> items = aiSearchMapper.doSearchByIntent(intent);

		log.debug("검색 결과 {}건", items.size());

		return AiSearchResponse.ofMovies(intent, completePoster(items));
	}


	/** DB 의 TMDB 상대경로를 화면용 완성 URL 로 - 1조 ContentImageService 를 그대로 부른다 */
	private List<AiSearchItem> completePoster(List<AiSearchItem> items) {
		for (AiSearchItem item : items) {
			item.setPosterUrl(contentImageService.toPosterUrl(item.getPosterUrl()));
		}
		return items;
	}

	/** "OO 는 빼고" - 제외 이름이 제목에 들어간 영화를 걸러내고 limit 개만 남긴다 */
	private List<AiSearchItem> applyExcludes(List<AiSearchItem> items,
			List<String> excludeTitles, int limit) {
		if (null == excludeTitles || excludeTitles.isEmpty()) {
			return items.size() > limit ? items.subList(0, limit) : items;
		}

		List<AiSearchItem> kept = new java.util.ArrayList<>();
		for (AiSearchItem item : items) {
			String titleKo = null == item.getTitleKo() ? "" : item.getTitleKo().toLowerCase();
			String titleOrg = null == item.getTitleOrg() ? "" : item.getTitleOrg().toLowerCase();

			boolean banned = excludeTitles.stream()
					.map(t -> t.toLowerCase().trim())
					.anyMatch(t -> false == t.isEmpty()
							&& (titleKo.contains(t) || titleOrg.contains(t)));

			if (false == banned) {
				kept.add(item);
				if (kept.size() >= limit) {
					break;
				}
			}
		}
		return kept;
	}
}
