"""
엔딩크레딧 AI 서버 - 검색 의도 분류 + 임베딩

Spring Boot(8080)가 이 서버(8081)를 호출하고, 이 서버가 OpenAI를 호출한다.
키는 .env 의 OPENAI_API_KEY 에서 읽는다.
"""

import json
import os

from dotenv import load_dotenv
from fastapi import FastAPI
from openai import OpenAI
from pydantic import BaseModel

load_dotenv()  # .env 파일에서 환경변수 불러오기

api_key = os.getenv("OPENAI_API_KEY")
client = OpenAI(api_key=api_key)

app = FastAPI(title="EndingCredit AI Server",
              description="Spring Boot가 호출하는 AI 중계 서버 - 검색 의도 분류(/search-intent)와 임베딩(/embed)",
              version="1.0",
              docs_url="/docs")


# ── 검색 의도 분류 ────────────────────────────────────────────────
# AI 는 "무엇을 찾고 싶은지"만 뽑는다. 영화 목록은 자바가 DB 에서 가져온다.
# 그래서 AI 가 없는 영화를 지어낼 자리가 없다.

class SearchRequest(BaseModel):
    query: str


class SearchIntent(BaseModel):
    intent: str                       # ranking | title_search | keyword_search
                                      # | semantic | similar
                                      # | notice_search | site_help | out_of_scope
    keywords: list[str] = []          # 줄거리·제목에서 찾을 낱말
    title: str | None = None          # 제목으로 콕 집어 찾을 때
    genre: str | None = None          # 장르명(우리 DB 에 있는 것만)
    yearFrom: int | None = None
    yearTo: int | None = None
    sort: str = "latest"              # latest | oldest | rating | popular
    limit: int = 20
    message: str = ""                 # out_of_scope 일 때 화면에 보여줄 안내
    faqKey: str | None = None         # site_help 일 때 어느 안내인지 (자바가 본문을 가짐)
    excludeTitles: list[str] = []     # "~는 빼고" 제외할 영화·시리즈 이름들
    provider: str = "openai"


# 우리 DB 에 실제로 있는 장르. AI 가 이 밖의 값을 내면 자바가 버린다.
GENRES = ["액션", "SF", "드라마", "애니메이션", "스릴러", "코미디", "공포", "로맨스", "범죄", "판타지"]

# 사이트 사용법 안내 키. 답변 본문은 자바(FaqAnswers)가 가진다 - AI 는 고르기만 한다
FAQ_KEYS = ["withdraw", "signup", "login", "rating", "comment", "spoiler", "report", "collection"]

SEARCH_SYSTEM_PROMPT = f"""너는 영화 서비스 "엔딩크레딧"의 검색 의도 분석기다.
사용자 문장을 읽고 검색 조건만 뽑는다. 영화를 직접 추천하거나 제목을 지어내지 마라.

intent 는 여섯 중 하나다.
- ranking        : 순위·인기·최신처럼 목록만 원할 때 (예: "top 20", "최신 영화")
- title_search   : 특정 제목을 찾을 때 (예: "인터스텔라 보여줘")
- keyword_search : 줄거리에 그 단어가 "글자 그대로" 나올 법한 구체 소재일 때
                   (예: "좀비", "마법사", "우주") - 사물·직업·장소·생물
- semantic       : 감정·상황·평가가 섞인 표현일 때 (예: "쓸쓸한", "억울하게 갇히는",
                   "감동적인 실화") - keywords 에 그 느낌 표현을 담는다.
                   둘이 애매하면 semantic 을 고른다
- similar        : 특정 영화와 비슷한 걸 원할 때 (예: "인터스텔라 같은 거") - title 에 그 영화 제목.
                   "OO 는 빼고/말고" 라고 하면 excludeTitles 에 그 이름들을 담는다
- notice_search  : 공지사항을 찾을 때 (예: "점검 공지 있었어?") - keywords 에 찾을 낱말
- site_help      : 이 사이트 사용법을 물을 때 (예: "회원탈퇴 어떻게 해?")
                   faqKey 를 반드시 이 중에서 고른다: {', '.join(FAQ_KEYS)}
                   (withdraw=탈퇴, signup=가입, login=로그인, rating=별점,
                    comment=코멘트 작성, spoiler=스포일러 표시, report=신고, collection=컬렉션)
- out_of_scope   : 영화도 이 사이트도 아닐 때 (예: "오늘 날씨", "코드 짜줘")

genre 는 반드시 이 목록에서만 고른다. 해당 없으면 null 로 둔다.
{', '.join(GENRES)}

sort 는 latest(최신순), oldest(오래된순), rating(평점순), popular(코멘트 많은순) 중 하나다.

반드시 아래 JSON 형식으로만 답한다.
{{"intent": "...", "keywords": [], "title": null, "genre": null,
  "yearFrom": null, "yearTo": null, "sort": "latest", "limit": 20,
  "message": "", "faqKey": null, "excludeTitles": []}}"""


def search_intent_openai(query: str) -> SearchIntent:
    res = client.chat.completions.create(
        model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
        messages=[
            {"role": "system", "content": SEARCH_SYSTEM_PROMPT},
            {"role": "user", "content": query},
        ],
        response_format={"type": "json_object"},
        temperature=0,
    )
    return _to_intent(json.loads(res.choices[0].message.content))


def _to_intent(body: dict) -> SearchIntent:
    """AI 응답을 계약 모양으로 맞춘다. 이상한 값은 여기서 걸러낸다."""
    intent = body.get("intent", "keyword_search")
    if intent not in ("ranking", "title_search", "keyword_search",
                      "semantic", "similar",
                      "notice_search", "site_help", "out_of_scope"):
        intent = "keyword_search"

    faq_key = body.get("faqKey")
    if faq_key not in FAQ_KEYS:           # 목록 밖 값은 버린다
        faq_key = None
    if "site_help" == intent and faq_key is None:
        intent = "out_of_scope"           # 어느 안내인지 모르면 억지로 답하지 않는다

    genre = body.get("genre")
    if genre not in GENRES:      # 우리 DB 에 없는 장르는 버린다
        genre = None

    sort = body.get("sort", "latest")
    if sort not in ("latest", "oldest", "rating", "popular"):
        sort = "latest"

    limit = int(body.get("limit") or 20)
    limit = max(1, min(limit, 50))     # 1~50 으로 가둔다

    return SearchIntent(
        intent=intent,
        keywords=[str(k) for k in (body.get("keywords") or [])][:5],
        title=body.get("title"),
        genre=genre,
        yearFrom=body.get("yearFrom"),
        yearTo=body.get("yearTo"),
        sort=sort,
        limit=limit,
        message=str(body.get("message") or ""),
        faqKey=faq_key,
        excludeTitles=[str(t) for t in (body.get("excludeTitles") or [])][:5],
        provider="openai",
    )


@app.get("/health"
         ,summary="Health API"
         ,description="서버 상태를 돌려준다.")
async def health():
    """자바가 서버 생존을 확인하는 곳."""
    return {"status": "ok", "provider": "openai"}



@app.post("/search-intent", response_model=SearchIntent
          ,summary="Search Intent API"
          ,description="검색 문장을 받아 의도(JSON)로 분류한다.")
async def search_intent(req: SearchRequest):
    return search_intent_openai(req.query)


# ── 임베딩 ───────────────────────────────────────────────────────
# 문장을 "뜻 좌표" 숫자 배열로 바꾼다. OpenAI 임베딩 API 를 쓴다.


class EmbedRequest(BaseModel):
    texts: list[str]


class EmbedResponse(BaseModel):
    vectors: list[list[float]]
    model: str
    dims: int


def embed_openai(texts: list[str]) -> EmbedResponse:
    name = os.getenv("OPENAI_EMBED_MODEL", "text-embedding-3-small")
    res = client.embeddings.create(model=name, input=texts)
    return EmbedResponse(
        vectors=[d.embedding for d in res.data],
        model="openai:" + name,
        dims=len(res.data[0].embedding),
    )


@app.post("/embed", response_model=EmbedResponse
          ,summary="Embed API"
          ,description="문장 묶음을 임베딩(뜻 좌표) 묶음으로 바꾼다.")
async def embed(req: EmbedRequest):
    return embed_openai(req.texts)
