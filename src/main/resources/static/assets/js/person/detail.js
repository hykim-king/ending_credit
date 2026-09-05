(() => {
    "use strict";

    const MOVIE_DETAIL_PATH = "/movies/";
    const PEOPLE_API_PATH = "/api/people/";

    // POL-033이 정한 역할 4종. PersonViewController.ROLE_LABELS와 같은 표다 -
    // 더보기로 붙는 행은 서버 렌더를 거치지 않아 여기서 같은 표기를 만들어야 한다
    const ROLE_LABELS = {
        DIRECTOR: "감독",
        ACTOR: "배우",
        WRITER: "각본",
        PRODUCER: "제작"
    };

    function csrfHeaders() {
        return typeof getCsrfHeaders === "function" ? getCsrfHeaders() : {};
    }

    // ── 좋아요 (ACT-P-001) ────────────────────────────
    function initLike() {
        const button = document.getElementById("personLikeButton");
        const notice = document.getElementById("personLikeNotice");

        if (!button) {
            return;
        }

        const personId = button.dataset.personId;
        const countEl = button.querySelector(".person-like-count");
        const iconEl = button.querySelector("i");

        // 서버가 그린 상태에서 출발한다
        let liked = button.dataset.liked === "true";
        let count = Number(countEl.textContent) || 0;

        function render() {
            button.classList.toggle("is-liked", liked);
            button.setAttribute("aria-pressed", String(liked));
            iconEl.className = liked ? "bi bi-heart-fill" : "bi bi-heart";
            countEl.textContent = count;
        }

        button.addEventListener("click", async () => {
            const memberId = button.dataset.memberId;

            // FL-01 - 비회원의 쓰기 행동은 로그인으로 유도한다. C-09가 아직 없어 안내로 대체한다
            if (!memberId) {
                notice.textContent = "좋아요는 로그인 후 이용할 수 있습니다.";
                notice.hidden = false;
                return;
            }

            notice.hidden = true;

            // 낙관적 갱신 - 응답을 기다리지 않고 먼저 바꾼다
            const previousLiked = liked;
            const previousCount = count;

            liked = !liked;
            count += liked ? 1 : -1;
            render();

            button.disabled = true;

            try {
                const response = await fetch(PEOPLE_API_PATH + personId + "/likes", {
                    method: liked ? "POST" : "DELETE",
                    credentials: "same-origin",
                    headers: csrfHeaders()
                });

                if (!response.ok) {
                    throw new Error("좋아요 처리에 실패했습니다.");
                }
            } catch (error) {
                // 실패하면 원상복구한다
                liked = previousLiked;
                count = previousCount;
                render();

                notice.textContent = "좋아요를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
                notice.hidden = false;
            } finally {
                button.disabled = false;
            }
        });
    }

    // ── 참여 작품 더보기 ──────────────────────────────
    function initFilmography() {
        const section = document.querySelector(".filmography");

        if (!section) {
            return;
        }

        const list = document.getElementById("filmographyList");
        const moreButton = document.getElementById("filmographyMore");
        const errorBox = document.getElementById("filmographyError");
        const errorText = document.getElementById("filmographyErrorText");
        const retryButton = document.getElementById("filmographyRetry");
        const emptyBox = document.getElementById("filmographyEmpty");

        const personId = section.dataset.personId;
        const pageSize = Number(section.dataset.pageSize) || 12;

        let totalCnt = Number(section.dataset.totalCnt) || 0;
        let pageNo = 1;

        // 제목·배역은 외부 데이터라 textContent로만 넣는다
        function toRow(credit) {
            const row = document.createElement("li");
            row.className = "filmo-row";

            const link = document.createElement("a");
            link.className = "filmo-link";
            link.href = MOVIE_DETAIL_PATH + credit.contentId;

            const year = document.createElement("span");
            year.className = "filmo-year";
            // release_year가 YYYY-MM-DD 문자열이라 앞 4자리만 쓴다
            year.textContent = credit.releaseYear ? credit.releaseYear.slice(0, 4) : "";

            const poster = document.createElement("span");
            poster.className = "filmo-poster";

            if (credit.posterUrl) {
                const image = document.createElement("img");
                image.src = credit.posterUrl;
                image.alt = credit.titleKo || "";
                image.loading = "lazy";
                poster.appendChild(image);
            }

            const title = document.createElement("span");
            title.className = "filmo-title";
            title.textContent = credit.titleKo;

            const role = document.createElement("span");
            role.className = "filmo-role";
            role.textContent = toRoleText(credit);

            link.append(year, poster, title, role);
            row.appendChild(link);

            return row;
        }

        // POL-033 - 배역은 ACTOR만 쓴다
        function toRoleText(credit) {
            const label = ROLE_LABELS[credit.role] || "";

            if (credit.role === "ACTOR" && credit.character) {
                return label + " | " + credit.character;
            }

            return label;
        }

        function showError(show) {
            errorBox.classList.toggle("is-hidden", !show);
        }

        async function loadPage(nextPageNo) {
            moreButton.disabled = true;
            moreButton.textContent = "불러오는 중...";

            try {
                const response = await fetch(
                    PEOPLE_API_PATH + personId + "/filmography?page=" + nextPageNo + "&size=" + pageSize,
                    { credentials: "same-origin" });

                if (!response.ok) {
                    throw new Error("참여 작품을 불러오지 못했습니다.");
                }

                const body = await response.json();
                const items = Array.isArray(body.items) ? body.items : [];

                // 다시 시도로 1페이지를 새로 받는 경우엔 기존 행을 걷어낸다
                if (nextPageNo === 1) {
                    list.replaceChildren();
                }

                items.forEach((credit) => list.appendChild(toRow(credit)));

                pageNo = nextPageNo;
                totalCnt = body.page && body.page.totalCnt ? body.page.totalCnt : totalCnt;

                showError(false);
                emptyBox.classList.toggle("is-hidden", list.children.length > 0);
                moreButton.classList.toggle("is-hidden", list.children.length >= totalCnt);
            } catch (error) {
                errorText.textContent = "참여 작품을 불러오지 못했습니다.";
                showError(true);
            } finally {
                moreButton.disabled = false;
                moreButton.innerHTML = '더보기 <i class="bi bi-chevron-down"></i>';
            }
        }

        moreButton.addEventListener("click", () => loadPage(pageNo + 1));

        // 03의 오류 상태 "목록 재시도" - 1페이지부터 다시 받는다
        retryButton.addEventListener("click", () => loadPage(1));
    }

    initLike();
    initFilmography();
})();
