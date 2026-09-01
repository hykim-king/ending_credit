(() => {
    "use strict";

    const MOVIE_DETAIL_PATH = "/movies/";
    const PAGE_BLOCK_SIZE = 5;

    const pageType = document.body.dataset.page;

    /* ── S-01 박스오피스 순위 ─────────────────────────────── */

    const RANKINGS_URL = "/api/search/rankings?source=boxoffice";

    // 순위 한 줄. 제목은 사용자 입력에서 온 값이라 textContent로만 넣는다
    function toRankItem(movie) {
        const item = document.createElement("li");

        const link = document.createElement("a");
        link.className = "ranking-link";
        link.href = MOVIE_DETAIL_PATH + movie.contentId;

        const rank = document.createElement("span");
        rank.className = "ranking-no";
        // no(rnum)가 곧 순위다
        rank.textContent = movie.no;

        const title = document.createElement("span");
        title.className = "ranking-title-text";
        title.textContent = movie.titleKo;
        title.title = movie.titleKo;

        link.append(rank, title);
        item.appendChild(link);

        return item;
    }

    async function loadRankings() {
        const section = document.getElementById("rankingSection");
        const list = document.getElementById("rankingList");

        if (!section || !list) {
            return;
        }

        // 순위를 못 읽어도 검색창은 그대로 둔다. 섹션만 숨긴 채 끝낸다
        let movies;

        try {
            const response = await fetch(RANKINGS_URL, { credentials: "same-origin" });

            if (!response.ok) {
                return;
            }

            movies = await response.json();
        } catch (error) {
            return;
        }

        if (!Array.isArray(movies) || movies.length === 0) {
            return;
        }

        movies.forEach((movie) => list.appendChild(toRankItem(movie)));
        section.hidden = false;
    }

    /* ── S-03 영화 결과 ───────────────────────────────────── */

    // 카드 구조는 fragments/cards.html의 movieCard와 같아야 한다.
    // 서버 렌더와 클라이언트 렌더가 같은 CSS를 쓰므로 클래스 이름을 맞춘다
    function toMovieCard(movie) {
        const card = document.createElement("div");
        card.className = "ec-card";

        const link = document.createElement("a");
        link.className = "ec-poster-link";
        link.href = MOVIE_DETAIL_PATH + movie.contentId;
        link.title = movie.titleKo;

        const poster = document.createElement("div");
        poster.className = "ec-poster";

        if (movie.posterUrl) {
            const image = document.createElement("img");
            image.src = movie.posterUrl;
            image.alt = movie.titleKo;
            image.loading = "lazy";
            poster.appendChild(image);
        } else {
            const empty = document.createElement("div");
            empty.className = "ec-poster-empty";
            empty.textContent = "이미지 없음";
            poster.appendChild(empty);
        }

        link.appendChild(poster);

        const body = document.createElement("div");
        body.className = "ec-card-body";

        const title = document.createElement("div");
        title.className = "ec-card-title";
        title.textContent = movie.titleKo;

        const meta = document.createElement("div");
        meta.className = "ec-card-meta";
        // release_year는 YYYY-MM-DD 문자열이라 앞 4자리만 쓴다
        const year = movie.releaseYear ? movie.releaseYear.substring(0, 4) : "";
        meta.textContent = [year, movie.country].filter(Boolean).join(" · ");

        body.append(title, meta);
        card.append(link, body);

        return card;
    }

    function renderPager(container, pageNo, totalPages, move) {
        container.innerHTML = "";

        if (totalPages <= 1) {
            return;
        }

        const start = Math.floor((pageNo - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
        const end = Math.min(totalPages, start + PAGE_BLOCK_SIZE - 1);

        const button = (label, target, current, disabled) => {
            const el = document.createElement("button");
            el.type = "button";
            el.className = current ? "current" : "";
            el.textContent = label;
            el.disabled = Boolean(disabled);

            if (!el.disabled && !current) {
                el.addEventListener("click", () => move(target));
            }

            container.appendChild(el);
        };

        button("‹", Math.max(1, start - 1), false, start === 1);

        for (let i = start; i <= end; i++) {
            button(String(i), i, i === pageNo, false);
        }

        button("›", Math.min(totalPages, end + 1), false, end === totalPages);
    }

    // 페이지를 옮겨도 새로고침·공유가 되도록 주소를 맞춰 둔다
    function setPageParam(target) {
        const params = new URLSearchParams(location.search);
        params.set("page", String(target));
        history.replaceState(null, "", `${location.pathname}?${params}`);
    }

    function initMovieResults() {
        const grid = document.getElementById("movieGrid");
        const countBox = document.getElementById("movieCount");
        const emptyState = document.getElementById("movieEmpty");
        const errorState = document.getElementById("movieError");
        const retryButton = document.getElementById("movieRetry");
        const pager = document.getElementById("moviePager");

        const query = document.body.dataset.query || "";
        const sort = document.body.dataset.sort || "relevance";
        const size = document.body.dataset.pageSize || "12";

        let pageNo = Number(document.body.dataset.pageNo || 1);
        // S-05 "중복 요청 방지" - 앞선 요청이 끝나기 전에는 다시 보내지 않는다
        let loading = false;

        async function load() {
            if (loading) {
                return;
            }

            loading = true;
            retryButton.disabled = true;

            grid.innerHTML = "";
            pager.innerHTML = "";
            countBox.textContent = "";
            emptyState.hidden = true;
            errorState.hidden = true;

            const params = new URLSearchParams({
                query,
                sort,
                page: String(pageNo),
                size
            });

            try {
                const response = await fetch(`/api/search/contents?${params}`, {
                    credentials: "same-origin"
                });

                if (!response.ok) {
                    throw new Error(String(response.status));
                }

                const data = await response.json();

                countBox.textContent = data.totalCnt;

                if (!data.list || data.list.length === 0) {
                    // S-04 검색 결과 없음
                    emptyState.hidden = false;
                    return;
                }

                data.list.forEach((movie) => grid.appendChild(toMovieCard(movie)));

                // 명세대로 totalPages로 페이저를 그린다
                renderPager(pager, data.pageNo, data.totalPages, (target) => {
                    pageNo = target;
                    setPageParam(target);
                    load();
                    window.scrollTo({ top: 0, behavior: "smooth" });
                });
            } catch (error) {
                // S-05 검색 오류 - 검색어는 그대로 두고 재시도만 제공한다
                errorState.hidden = false;
            } finally {
                loading = false;
                retryButton.disabled = false;
            }
        }

        retryButton.addEventListener("click", load);

        load();
    }

    /* ── S-05 검색 오류 재시도 ────────────────────────────── */

    // S-02는 서버 렌더라 같은 주소를 다시 요청하는 것이 곧 재시도다.
    // 누르는 즉시 잠가 중복 요청을 막는다(정의서 S-05 비고)
    function initRetryButtons() {
        document.querySelectorAll(".retry-button").forEach((button) => {
            button.addEventListener("click", () => {
                button.disabled = true;
                button.textContent = "불러오는 중...";
                location.reload();
            });
        });
    }

    switch (pageType) {
        case "search-movies":
            initMovieResults();
            break;
        default:
            loadRankings();
            initRetryButtons();
            break;
    }
})();
