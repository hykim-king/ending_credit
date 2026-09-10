/**
 * 파이썬 /embed(저울) 호출 - 글 묶음을 좌표 묶음으로 바꾼다
 *
 * 어떤 저울(hash/local/openai)이 도는지는 파이썬 .env 가 정하고,
 * 응답의 model 값으로 자바가 그걸 알게 된다.
 */
package com.endit.ai;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.endit.ai.dto.EmbedResponse;

@Component
public class AiEmbeddingClient {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String PATH_EMBED = "/embed";

	private final RestTemplate restTemplate;
	private final String baseUrl;
	private final boolean enabled;

	public AiEmbeddingClient(
			@Qualifier("aiRestTemplate") RestTemplate restTemplate,
			@Value("${ai.base-url:http://localhost:5000}") String baseUrl,
			@Value("${ai.enabled:false}") boolean enabled) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
		this.enabled = enabled;
	}

	/**
	 * 글 묶음을 좌표로 바꾼다.
	 *
	 * @param texts 글 목록
	 * @return 좌표 묶음. 꺼져 있거나 실패하면 null (호출한 쪽이 건너뛴다)
	 */
	public EmbedResponse embed(List<String> texts) {
		if (false == enabled || null == texts || texts.isEmpty()) {
			return null;
		}

		try {
			return restTemplate.postForObject(
					baseUrl + PATH_EMBED, Map.of("texts", texts), EmbedResponse.class);
		} catch (RestClientException e) {
			log.warn("임베딩 호출에 실패했습니다. 건너뜁니다.", e);
			return null;
		}
	}
}
