/**
 * AI 검색 API
 *
 * 떠 있는 검색 패널이 비동기로 호출한다.
 * AI 가 실패해도 200과 함께 대체 결과를 돌려준다 - 화면이 깨지지 않게 한다.
 */
package com.endit.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.domain.AiSearchResponseVO;
import com.endit.service.AiSearchService;
import com.endit.service.FastApiService;

@RestController
@RequestMapping("/api/ai")
public class AiApiController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AiSearchService aiSearchService;
    private final FastApiService fastApiService;

    public AiApiController(AiSearchService aiSearchService, FastApiService fastApiService) {
        this.aiSearchService = aiSearchService;
        this.fastApiService = fastApiService;
        log.debug("aiSearchService: {}", aiSearchService);
    }

    /**
     * 위젯이 페이지를 열 때 "AI 살아있어?" 묻는 곳.
     * 파이썬이 없으면 위젯이 스스로 숨는다 - 고장난 것처럼 보이지 않게.
     *
     * @return alive true/false
     */
    @GetMapping("/health")
    public Map<String, Boolean> health() {
        return Map.of("alive", fastApiService.isAlive());
    }

    /**
     * 자연어 문장으로 영화를 찾는다.
     *
     * AI 는 검색 조건만 뽑고 영화 목록은 DB 가 낸다. 없는 영화가 섞일 수 없다.
     *
     * @param query 사용자 입력 문장
     * @return 의도 + 영화 목록
     */
    @GetMapping("/search")
    public AiSearchResponseVO search(@RequestParam(name = "query", defaultValue = "") String query) {
        log.debug("=============================");
        log.debug("{}()", "search");
        log.debug("query: {}", query);
        log.debug("=============================");

        return aiSearchService.search(query);
    }
}
