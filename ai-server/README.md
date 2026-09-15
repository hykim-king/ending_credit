# AI 서버

Spring Boot 가 호출하는 AI 중계 서버다. 검색 의도 분류와 임베딩(뜻 좌표)을 맡는다.

## 실행

```bash
cd ai-server
python -m venv .venv             # 최초 1회
.venv\Scripts\activate           # 가상환경 진입
pip install -r requirements.txt  # 최초 1회
uvicorn main:app --reload --port=8081
```

## 키

`ai-server/.env` 에 적는다. `.env` 는 git 에 올라가지 않는다.

```
OPENAI_API_KEY=sk-...
```

## 확인

```bash
curl http://localhost:8081/health
curl -X POST http://localhost:8081/search-intent -H "Content-Type: application/json" -d "{\"query\":\"90년대 영화\"}"
```
