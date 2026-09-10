# AI 서버

Spring Boot 가 호출하는 AI 중계 서버다. 검색 의도 분류와 임베딩(뜻 좌표)을 맡는다.

## 실행

```bash
cd ai-server
python -m uvicorn main:app --port 5000
```

## 제공자 바꾸기

`.env` 의 `AI_PROVIDER` 한 줄만 고친다.

| 값 | 동작 | API 키 |
|---|---|---|
| `mock` | AI 를 부르지 않는다. 키워드 규칙으로 흉내 | 불필요 |
| `openai` | ChatGPT API | `OPENAI_API_KEY` |
| `claude` | Anthropic API | `ANTHROPIC_API_KEY` |

키가 생기기 전까지는 `mock` 으로 둔다. 자바 쪽은 무엇이 돌든 모른다.

## 확인

```bash
curl http://localhost:5000/health
curl -X POST http://localhost:5000/search-intent -H "Content-Type: application/json" -d "{\"query\":\"90년대 영화\"}"
```
