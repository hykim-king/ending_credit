/**
 * 임베딩 적재와 뜻 검색
 *
 * 적재:  공책(CONTENT_EMBEDDING)에 없는 영화만 골라 저울에 재서 채운다
 * 검색:  검색어를 좌표로 바꾸고, 공책의 좌표들과 거리(코사인)를 재서 가까운 순으로 준다
 *
 * 저울이 무엇이든(hash/local/openai) 응답의 model 로 구분되므로,
 * 저울이 바뀌면 옛 좌표는 자동으로 "적재 대상"이 되어 다시 재진다.
 */
package com.endit.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.endit.ai.dto.AiSearchItem;
import com.endit.ai.dto.ContentEmbeddingVO;
import com.endit.ai.dto.EmbedResponse;
import com.endit.mapper.AiSearchMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ContentEmbeddingService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** 저울에 한 번에 올리는 묶음 크기 */
	private static final int BATCH_SIZE = 20;

	/** 줄거리에서 저울에 올리는 최대 길이(토큰 낭비 방지) */
	private static final int MAX_TEXT_LENGTH = 1500;

	private final AiEmbeddingClient embeddingClient;
	private final AiSearchMapper aiSearchMapper;
	private final ObjectMapper objectMapper;

	public ContentEmbeddingService(AiEmbeddingClient embeddingClient,
			AiSearchMapper aiSearchMapper, ObjectMapper objectMapper) {
		this.embeddingClient = embeddingClient;
		this.aiSearchMapper = aiSearchMapper;
		this.objectMapper = objectMapper;
		log.debug("embeddingClient: {}", embeddingClient);
	}

	/**
	 * 공책에 없는(또는 다른 저울로 잰) 영화들을 재서 채운다.
	 *
	 * @return 새로 잰 건수. 저울이 꺼져 있으면 -1
	 */
	public int embedNewContents() {
		log.debug("=============================");
		log.debug("{}()", "embedNewContents");
		log.debug("=============================");

		// 지금 어떤 저울이 도는지부터 알아낸다(견본 1건)
		EmbedResponse probe = embeddingClient.embed(List.of("probe"));
		if (null == probe) {
			log.warn("저울(AI 서버)이 꺼져 있어 적재를 건너뜁니다.");
			return -1;
		}

		String model = probe.model();
		List<ContentEmbeddingVO> targets = aiSearchMapper.selectEmbedTargets(model);
		log.debug("적재 대상 {}건 (model={})", targets.size(), model);

		int saved = 0;
		for (int from = 0; from < targets.size(); from += BATCH_SIZE) {
			List<ContentEmbeddingVO> batch =
					targets.subList(from, Math.min(from + BATCH_SIZE, targets.size()));

			// 줄거리 한두 줄만 재면 "억울한 옥살이" 같은 추상 뜻이 좌표에 안 실린다(실측).
			// 제목·장르를 함께 재서 재료를 두껍게 한다
			List<String> texts = batch.stream()
					.map(t -> cut(buildEmbedText(t)))
					.toList();

			EmbedResponse res = embeddingClient.embed(texts);
			if (null == res) {
				log.warn("묶음 적재 중 실패. 지금까지 {}건 저장하고 멈춥니다.", saved);
				return saved;
			}

			for (int i = 0; i < batch.size(); i++) {
				ContentEmbeddingVO row = new ContentEmbeddingVO();
				row.setContentId(batch.get(i).getContentId());
				row.setEmbedding(toJson(res.vectors().get(i)));
				row.setModel(res.model());
				// UPDATE 먼저, 없으면 INSERT (21c CLOB+MERGE 지뢰 회피)
				if (0 == aiSearchMapper.updateEmbedding(row)) {
					aiSearchMapper.insertEmbedding(row);
				}
				saved++;
			}
		}

		log.debug("적재 완료 {}건", saved);
		return saved;
	}

	/**
	 * 뜻 검색 - 문장과 뜻이 가까운 영화를 가까운 순으로.
	 *
	 * @param query 느낌·내용 문장
	 * @param limit 최대 건수
	 * @return 가까운 순 영화 카드. 좌표가 없으면 빈 목록
	 */
	public List<AiSearchItem> searchByMeaning(String query, int limit) {
		EmbedResponse res = embeddingClient.embed(List.of(cut(query)));
		if (null == res) {
			return List.of();
		}
		return rankByDistance(res.vectors().get(0), res.model(), -1L, limit);
	}

	/**
	 * 비슷한 영화 - 기준 영화의 좌표와 가까운 순으로(자기 자신 제외).
	 *
	 * @param title 기준 영화 제목(부분 일치)
	 * @param limit 최대 건수
	 * @return 가까운 순 영화 카드. 기준 영화가 없으면 null, 좌표가 없으면 빈 목록
	 */
	public List<AiSearchItem> findSimilar(String title, int limit) {
		Long refId = aiSearchMapper.selectContentIdByTitle(title);
		if (null == refId) {
			return null;                       // "그런 영화가 없다"를 위로 알린다
		}

		// 기준 영화의 좌표는 공책에 이미 있다 - 저울 호출이 필요 없다.
		// 다만 어떤 저울 좌표인지 알아야 해서 견본 1건으로 저울 이름만 묻는다
		EmbedResponse probe = embeddingClient.embed(List.of("probe"));
		if (null == probe) {
			return List.of();
		}

		List<ContentEmbeddingVO> all = aiSearchMapper.selectEmbeddings(probe.model());
		double[] ref = null;
		for (ContentEmbeddingVO row : all) {
			if (row.getContentId() == refId) {
				ref = parse(row.getEmbedding());
				break;
			}
		}
		if (null == ref) {
			return List.of();                  // 아직 안 재진 영화
		}

		return rank(all, ref, refId, limit);
	}

	/* ── 안 - 거리 계산 ─────────────────────────────────────── */

	private List<AiSearchItem> rankByDistance(double[] queryVec, String model,
			long excludeId, int limit) {
		List<ContentEmbeddingVO> all = aiSearchMapper.selectEmbeddings(model);
		return rank(all, queryVec, excludeId, limit);
	}

	private List<AiSearchItem> rank(List<ContentEmbeddingVO> rows, double[] target,
			long excludeId, int limit) {
		record Scored(long contentId, double score) {
		}

		List<Scored> scored = new ArrayList<>();
		for (ContentEmbeddingVO row : rows) {
			if (row.getContentId() == excludeId) {
				continue;
			}
			scored.add(new Scored(row.getContentId(),
					cosine(target, parse(row.getEmbedding()))));
		}

		List<Long> ids = scored.stream()
				.sorted(Comparator.comparingDouble(Scored::score).reversed())
				.limit(limit)
				.map(Scored::contentId)
				.toList();

		if (ids.isEmpty()) {
			return List.of();
		}

		// IN 조회는 순서를 보장하지 않으므로 거리순으로 다시 줄 세운다
		Map<Long, AiSearchItem> byId = aiSearchMapper.selectItemsByIds(ids).stream()
				.collect(Collectors.toMap(AiSearchItem::getContentId, Function.identity()));

		return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
	}

	/** 코사인 유사도 - 1에 가까울수록 뜻이 비슷하다 */
	private double cosine(double[] a, double[] b) {
		if (null == a || null == b || a.length != b.length) {
			return -1;
		}
		double dot = 0;
		double na = 0;
		double nb = 0;
		for (int i = 0; i < a.length; i++) {
			dot += a[i] * b[i];
			na += a[i] * a[i];
			nb += b[i] * b[i];
		}
		if (0 == na || 0 == nb) {
			return -1;
		}
		return dot / (Math.sqrt(na) * Math.sqrt(nb));
	}

	/** 저울에 올릴 글 = 제목 + 장르 + 줄거리 */
	private String buildEmbedText(ContentEmbeddingVO t) {
		StringBuilder sb = new StringBuilder();
		if (null != t.getTitleKo()) {
			sb.append("제목: ").append(t.getTitleKo()).append(". ");
		}
		if (null != t.getGenres()) {
			sb.append("장르: ").append(t.getGenres()).append(". ");
		}
		sb.append(null == t.getOverview() ? "" : t.getOverview());
		return sb.toString();
	}

	private String cut(String text) {
		if (null == text) {
			return "";
		}
		return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
	}

	private String toJson(double[] vector) {
		try {
			return objectMapper.writeValueAsString(vector);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("좌표 직렬화 실패", e);
		}
	}

	private double[] parse(String json) {
		try {
			return objectMapper.readValue(json, double[].class);
		} catch (JsonProcessingException e) {
			log.warn("좌표 파싱 실패. 해당 행은 건너뜁니다.");
			return null;
		}
	}
}
