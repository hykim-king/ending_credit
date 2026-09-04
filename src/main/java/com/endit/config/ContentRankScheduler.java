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
		// 둘을 한 try에 묶으면 앞이 죽을 때 뒤가 통째로 안 돈다. 서로 다른 선반을 채우므로 따로 잡는다.
		// 리스너 밖으로 나가면 기동 실패로 번지고, 순위가 비면 화면이 적재순으로 폴백하므로 로그만 남긴다
		try {
			log.info("구동 시 인기순위 동기화: matched={}", contentService.syncRank());
		} catch (Exception e) {
			log.warn("구동 시 인기순위 동기화 실패. 박스오피스 선반이 적재순으로 시작한다", e);
		}

		// 장르별은 장르 수만큼 TMDB를 부르므로 전체 순위보다 오래 걸린다. 뒤에 둬서 앞줄이 먼저 살게 한다
		try {
			log.info("구동 시 장르별 인기순위 동기화: 장르={}", contentService.syncGenreRank());
		} catch (Exception e) {
			log.warn("구동 시 장르별 인기순위 동기화 실패. 장르 선반이 적재순으로 시작한다", e);
		}
	}

	// 기본값 "-"는 스프링이 정의한 비활성 값이라 스케줄이 등록되지 않는다.
	// 꺼 둔 것이 설계다 - 학습용이라 상시 구동하지 않으므로 구현만 해 두고 기본은 off다.
	// 켤 일이 생기면 application.yaml에 endit.rank.cron을 주면 된다(코드 수정 없이)
	@Scheduled(cron = "${endit.rank.cron:" + Scheduled.CRON_DISABLED + "}", zone = "Asia/Seoul")
	public void syncDaily() {
		// 스케줄러가 잡아 주므로 여기서는 안 감싼다. 다만 앞이 던지면 장르는 다음 주기까지 안 돈다
		contentService.syncRank();
		contentService.syncGenreRank();
	}

}
