package com.endit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.endit.service.ContentService;

/** 인기순위 갱신 트리거. 갱신 로직은 ContentService.syncRank가 갖고 있고 여기서는 부르기만 한다. */
@Component
public class ContentRankScheduler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ContentService contentService;

	public ContentRankScheduler(ContentService contentService) {
		this.contentService = contentService;
	}

	// ApplicationReadyEvent는 웹 서버가 이미 요청을 받는 시점이라 TMDB가 느려도 기동을 막지 않는다. @PostConstruct는 막는다
	@EventListener(ApplicationReadyEvent.class)
	public void syncOnStartup() {
		try {
			log.info("구동 시 인기순위 동기화: matched={}", contentService.syncRank());
		} catch (Exception e) {
			// 리스너 밖으로 나가면 기동 실패로 번진다. 순위가 비면 화면이 적재순으로 폴백하므로 로그만 남긴다
			log.warn("구동 시 인기순위 동기화 실패. 폴백 정렬로 시작한다", e);
		}
	}

	// 기본값 "-"는 스프링이 정의한 비활성 값이라 스케줄이 등록되지 않는다. 켜려면 endit.rank.cron을 준다
	@Scheduled(cron = "${endit.rank.cron:" + Scheduled.CRON_DISABLED + "}", zone = "Asia/Seoul")
	public void syncDaily() {
		contentService.syncRank();
	}

}
