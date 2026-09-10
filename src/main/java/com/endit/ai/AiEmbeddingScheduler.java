/**
 * 임베딩 적재 스케줄러 - 매일 새벽에 "새 영화만" 잰다
 *
 * 프로젝트에는 이미 1조 ContentRankScheduler 가 돌고 있어 낯선 패턴이 아니다.
 * ai.enabled=false 면 서비스 쪽에서 조용히 건너뛴다.
 */
package com.endit.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiEmbeddingScheduler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ContentEmbeddingService contentEmbeddingService;

	public AiEmbeddingScheduler(ContentEmbeddingService contentEmbeddingService) {
		this.contentEmbeddingService = contentEmbeddingService;
	}

	/** 매일 04:10 - TMDB 동기화(1조) 뒤에 돌도록 시각을 비껴 둔다 */
	@Scheduled(cron = "0 10 4 * * *")
	public void embedDaily() {
		int saved = contentEmbeddingService.embedNewContents();
		log.info("임베딩 야간 적재: {}건", saved);
	}
}
