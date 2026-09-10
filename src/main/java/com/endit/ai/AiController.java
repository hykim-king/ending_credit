/**
 * AI 판정 API
 *
 * 떠 있는 검색 패널이 비동기로 호출한다.
 * AI 가 실패해도 200과 함께 대체 결과를 돌려준다 - 화면이 깨지지 않게 한다.
 */
package com.endit.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.ai.dto.AiSearchResponse;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AiSearchService aiSearchService;
    private final ContentEmbeddingService contentEmbeddingService;

    public AiController(AiSearchService aiSearchService,
            ContentEmbeddingService contentEmbeddingService) {
        this.aiSearchService = aiSearchService;
        this.contentEmbeddingService = contentEmbeddingService;
        log.debug("aiSearchService: {}", aiSearchService);
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
    public AiSearchResponse search(@RequestParam(name = "query", defaultValue = "") String query) {
        log.debug("=============================");
        log.debug("{}()", "search");
        log.debug("query: {}", query);
        log.debug("=============================");

        return aiSearchService.search(query);
    }

    /**
     * 임베딩 수동 적재(검증·초기 구축용).
     * 스케줄러(새벽 4:10)와 같은 일을 지금 즉시 한다.
     * TODO 공용 반영 전 ADMIN 가드 필요 - 지금은 로컬 검증 단계
     *
     * @return 새로 잰 건수(-1: 저울 꺼짐)
     */
    @PostMapping("/embed-now")
    public java.util.Map<String, Integer> embedNow() {
        log.debug("=============================");
        log.debug("{}()", "embedNow");
        log.debug("=============================");

        return java.util.Map.of("saved", contentEmbeddingService.embedNewContents());
    }
}
