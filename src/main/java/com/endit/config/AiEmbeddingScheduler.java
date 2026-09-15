/**
 * 임베딩 적재 스케줄러
 *
 *   기동 30초 뒤  - 아직 안 잰 영화를 잰다. 파이썬이 아직 안 떠 있으면 10분마다 다시 시도하고,
 *                  한 번 성공하면 그만둔다 (서버를 켜면 사람이 손댈 것 없이 알아서 채워진다)
 *   매일 04:10   - 그날 새로 들어온 영화만 잰다
 *
 * 프로젝트에는 이미 1조 ContentRankScheduler 가 돌고 있어 낯선 패턴이 아니다.
 */
package com.endit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.endit.service.ContentEmbeddingService;
import com.endit.service.FastApiService;

@Component
public class AiEmbeddingScheduler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ContentEmbeddingService contentEmbeddingService;
	private final FastApiService fastApiService;

	/** 기동 후 첫 적재가 한 번 성공했는가. 성공 전까지는 재시도한다 */
	private volatile boolean caughtUp = false;

	public AiEmbeddingScheduler(ContentEmbeddingService contentEmbeddingService,
			FastApiService fastApiService) {
		this.contentEmbeddingService = contentEmbeddingService;
		this.fastApiService = fastApiService;
	}

	/** 기동 30초 뒤 첫 시도, 성공할 때까지 10분 간격 */
	@Scheduled(initialDelay = 30_000, fixedDelay = 600_000)
	public void embedOnStartup() {
		if (caughtUp) {
			return;
		}
		if (false == fastApiService.isAlive()) {
			log.debug("AI 서버가 아직 없어 임베딩 적재를 미룹니다. 10분 뒤 다시 시도합니다.");
			return;
		}
		int saved = contentEmbeddingService.embedNewContents();
		if (saved < 0) {
			return;                       // 잰다는 도중 서버가 사라짐 - 다음 시도에 맡긴다
		}
		caughtUp = true;
		log.info("임베딩 기동 적재: {}건", saved);
	}

	/** 매일 04:10 - TMDB 동기화(1조) 뒤에 돌도록 시각을 비껴 둔다 */
	@Scheduled(cron = "0 10 4 * * *")
	public void embedDaily() {
		int saved = contentEmbeddingService.embedNewContents();
		log.info("임베딩 야간 적재: {}건", saved);
	}
}
