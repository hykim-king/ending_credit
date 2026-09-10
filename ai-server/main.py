"""
엔딩크레딧 AI 서버 - 검색 의도 분류 + 임베딩

Spring Boot(8080)가 이 서버(5000)를 호출하고, 이 서버가 AI를 호출한다.
AI 제공자는 .env의 AI_PROVIDER로 고른다.

  mock    - AI를 부르지 않는다. 키워드 규칙으로 흉내만 낸다(기본값)
  openai  - ChatGPT API
  claude  - Anthropic API

셋 다 같은 모양의 JSON을 돌려주므로 자바 쪽은 무엇이 돌든 모른다.
"""

import json
import os

from dotenv import load_dotenv
from fastapi import FastAPI
from pydantic import BaseModel

load_dotenv()

PROVIDER = os.getenv("AI_PROVIDER", "mock").lower()

app = FastAPI(title="EndingCredit AI Server")


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
    provider: str = "mock"


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


# mock 용 규칙. AI 가 아니라 단어 몇 개만 본다.
RANKING_WORDS = ["top", "순위", "인기", "랭킹", "베스트"]
OUT_OF_SCOPE_WORDS = ["날씨", "코드", "파이썬", "자바", "너 누구", "몇 시"]
STOP_WORDS = ["영화", "추천", "보여줘", "찾아줘", "알려줘", "좀", "해줘", "관련", "같은",
              "느낌", "이야기", "내용", "스토리", "작품", "것", "거", "배경", "나오는"]


# mock 용 사이트 질문 규칙. 낱말 -> faqKey
FAQ_RULES = [
    (["탈퇴"], "withdraw"),
    (["회원가입", "가입"], "signup"),
    (["로그인", "구글"], "login"),
    (["별점", "평가", "평점 남"], "rating"),
    (["코멘트 작성", "코멘트 어떻게", "댓글 작성", "댓글 어떻게", "코멘트 남"], "comment"),
    (["스포일러"], "spoiler"),
    (["신고"], "report"),
    (["컬렉션 만"], "collection"),
]


def search_intent_mock(query: str) -> SearchIntent:
    """AI 를 부르지 않는다. 단어 규칙으로 흉내만 낸다."""
    q = query.strip()
    low = q.lower()

    # 공지 검색 - "공지"라고 명시했으면 사용법 안내보다 우선한다
    if "공지" in q:
        rest = [w for w in q.replace("공지사항", " ").replace("공지", " ").split()
                if w not in STOP_WORDS and len(w) >= 2]
        return SearchIntent(intent="notice_search", keywords=rest[:3])

    # 사이트 사용법 질문 - 영화보다 먼저 본다
    for words, key in FAQ_RULES:
        if any(w in q for w in words):
            return SearchIntent(intent="site_help", faqKey=key)

    # "~비슷한/~같은/~처럼" 이 있으면 비슷한 영화 찾기로 본다.
    # mock 은 그 표현 앞부분을 통째로 제목이라 우기는 수준이다 - 진짜 AI 가 오면 정확해진다
    for marker in ["비슷한", "같은", "처럼"]:
        if marker in q:
            ref = q.split(marker)[0].strip()
            # 조사는 긴 것부터 뗀다("이랑"을 "랑"보다 먼저)
            for junk in ["이랑", "랑", "하고", "과", "와"]:
                if ref.endswith(junk):
                    ref = ref[: -len(junk)]
                    break
            ref = ref.strip()
            if len(ref) >= 2:
                return SearchIntent(intent="similar", title=ref, limit=5)

    # 느낌 표현이면 뜻 검색으로 - mock 은 낱말 사전으로만 구분한다
    MOOD_WORDS = ["쓸쓸", "우울", "따뜻", "잔잔", "무서운", "신나는", "감동"]
    if any(w in q for w in MOOD_WORDS):
        kws = [w for w in q.split() if w not in STOP_WORDS and len(w) >= 2]
        return SearchIntent(intent="semantic", keywords=kws[:5], limit=10)

    if any(w in low for w in OUT_OF_SCOPE_WORDS):
        return SearchIntent(
            intent="out_of_scope",
            message="영화 얘기만 도와드릴 수 있어요. 어떤 영화를 찾으세요?",
        )

    if any(w in low for w in RANKING_WORDS):
        return SearchIntent(intent="ranking", sort="popular", limit=20)

    genre = next((g for g in GENRES if g.lower() in low), None)

    # 연대 표현: "90년대" → 1990~1999
    year_from = year_to = None
    matched_decade = None
    for decade, base in [("90년대", 1990), ("2000년대", 2000), ("2010년대", 2010)]:
        if decade in q:
            year_from, year_to = base, base + 9
            matched_decade = decade
            break

    # 조사·군더더기를 걷어내고 남는 낱말을 키워드로 쓴다
    keywords = [w for w in q.split() if w not in STOP_WORDS and len(w) >= 2]
    for g in GENRES:
        if g in keywords:
            keywords.remove(g)
    # 연대·장르로 이미 조건이 잡힌 낱말은 키워드에서 뺀다.
    # 남겨두면 줄거리에 그 글자가 없어서 결과가 0건이 된다
    if matched_decade:
        keywords = [w for w in keywords if matched_decade not in w]

    return SearchIntent(
        intent="keyword_search" if (keywords or genre or year_from) else "ranking",
        keywords=keywords[:5],
        genre=genre,
        yearFrom=year_from,
        yearTo=year_to,
        sort="latest",
        limit=20,
    )


def search_intent_openai(query: str) -> SearchIntent:
    from openai import OpenAI

    client = OpenAI()
    res = client.chat.completions.create(
        model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
        messages=[
            {"role": "system", "content": SEARCH_SYSTEM_PROMPT},
            {"role": "user", "content": query},
        ],
        response_format={"type": "json_object"},
        temperature=0,
    )
    return _to_intent(json.loads(res.choices[0].message.content), "openai")


def search_intent_claude(query: str) -> SearchIntent:
    from anthropic import Anthropic

    client = Anthropic()
    res = client.messages.create(
        model=os.getenv("CLAUDE_MODEL", "claude-opus-5"),
        max_tokens=1024,
        system=SEARCH_SYSTEM_PROMPT,
        messages=[{"role": "user", "content": query}],
    )
    return _to_intent(json.loads(res.content[0].text), "claude")


def _to_intent(body: dict, provider: str) -> SearchIntent:
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
        provider=provider,
    )


# ── 갈아끼우는 자리 ───────────────────────────────────────────────

INTENT_RESOLVERS = {
    "mock": search_intent_mock,
    "openai": search_intent_openai,
    "claude": search_intent_claude,
}


@app.get("/health")
def health():
    """자바가 서버 생존과 현재 제공자를 확인하는 곳."""
    return {"status": "ok", "provider": PROVIDER}



@app.post("/search-intent", response_model=SearchIntent)
def search_intent(req: SearchRequest):
    resolver = INTENT_RESOLVERS.get(PROVIDER, search_intent_mock)
    return resolver(req.query)


# ── 임베딩(저울) ─────────────────────────────────────────────────
# 문장을 "뜻 좌표" 숫자 배열로 바꾼다. EMBED_PROVIDER 로 저울을 고른다.
#   hash   - 뜻 없음! 배관 검증용 가짜 저울(결정적). 기본값
#   local  - 무료 로컬 모델(sentence-transformers 설치 필요)
#   openai - OpenAI 임베딩 API(키 필요)

EMBED_PROVIDER = os.getenv("EMBED_PROVIDER", "hash").lower()


class EmbedRequest(BaseModel):
    texts: list[str]


class EmbedResponse(BaseModel):
    vectors: list[list[float]]
    model: str
    dims: int


def embed_hash(texts: list[str]) -> EmbedResponse:
    """가짜 저울. 같은 글에는 늘 같은 좌표를 주지만 '뜻'은 전혀 없다.
    테이블 적재→조회→거리계산 배관이 도는지 확인하는 용도뿐이다."""
    import hashlib
    import math

    dims = 64
    vectors = []
    for t in texts:
        raw = hashlib.sha256(t.encode("utf-8")).digest() * 2  # 64바이트
        v = [(b - 128) / 128.0 for b in raw[:dims]]
        norm = math.sqrt(sum(x * x for x in v)) or 1.0
        vectors.append([round(x / norm, 6) for x in v])
    return EmbedResponse(vectors=vectors, model="hash-64-fake", dims=dims)


def embed_local(texts: list[str]) -> EmbedResponse:
    """무료 로컬 저울. 최초 호출 때 모델을 메모리에 올린다(수십 초)."""
    from sentence_transformers import SentenceTransformer

    global _LOCAL_MODEL
    name = os.getenv("LOCAL_EMBED_MODEL", "jhgan/ko-sroberta-multitask")
    if "_LOCAL_MODEL" not in globals():
        _LOCAL_MODEL = SentenceTransformer(name)
    vecs = _LOCAL_MODEL.encode(texts, normalize_embeddings=True)
    return EmbedResponse(
        vectors=[[round(float(x), 6) for x in v] for v in vecs],
        model="local:" + name,
        dims=len(vecs[0]),
    )


def embed_openai(texts: list[str]) -> EmbedResponse:
    from openai import OpenAI

    client = OpenAI()
    name = os.getenv("OPENAI_EMBED_MODEL", "text-embedding-3-small")
    res = client.embeddings.create(model=name, input=texts)
    return EmbedResponse(
        vectors=[d.embedding for d in res.data],
        model="openai:" + name,
        dims=len(res.data[0].embedding),
    )


EMBEDDERS = {"hash": embed_hash, "local": embed_local, "openai": embed_openai}


@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    embedder = EMBEDDERS.get(EMBED_PROVIDER, embed_hash)
    return embedder(req.texts)
