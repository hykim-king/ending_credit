/**
 * 파이썬 /embed(저울) 호출 - 글 묶음을 좌표 묶음으로 바꾼다 (WebClient - 수업 05 방식)
 *
 * 저울은 OpenAI 임베딩 API 하나다. 응답의 model 값에 모델 이름이 실린다.
 */
package com.endit.ai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.endit.ai.dto.EmbedRequestVO;
import com.endit.ai.dto.EmbedResponseVO;

@Component
public class AiEmbeddingClient {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String PATH_EMBED = "/embed";

	private final WebClient webClient;
	private final boolean enabled;

	public AiEmbeddingClient(
			@Qualifier("aiWebClient") WebClient webClient,
			@Value("${ai.enabled:false}") boolean enabled) {
		this.webClient = webClient;
		this.enabled = enabled;
	}

	/**
	 * 글 묶음을 좌표로 바꾼다.
	 *
	 * @param texts 글 목록
	 * @return 좌표 묶음. 꺼져 있거나 실패하면 null (호출한 쪽이 건너뛴다)
	 */
	public EmbedResponseVO embed(List<String> texts) {
		if (false == enabled || null == texts || texts.isEmpty()) {
			return null;
		}

		try {
			return webClient
					.post()
					.uri(PATH_EMBED)
					.bodyValue(new EmbedRequestVO(texts))
					.retrieve()
					.bodyToMono(EmbedResponseVO.class)
					.block();
		} catch (RuntimeException e) {
			log.warn("임베딩 호출에 실패했습니다. 건너뜁니다.", e);
			return null;
		}
	}
}
