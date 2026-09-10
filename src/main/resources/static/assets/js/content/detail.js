/*
 * C-01·C-02 영화 상세 화면.
 * 평가·보고싶어요·컬렉션 담기는 담당 밖(MEMBER_CONTENT·COLLECTION_ITEM) API를 호출만 한다.
 */
(() => {
    "use strict";

    const RATING_API_PATH = "/api/movies/";
    const WATCHLIST_API_PATH = "/api/watchlist/";
    const COLLECTION_API_PATH = "/api/collections/";
    // 로그인 회원의 컬렉션 목록: 회원 번호는 서버 인증 정보에서 조회
    const MEMBER_COLLECTION_API_PATH = "/api/members/collections";

    // MOD-05가 API-007에서 한 번에 받는 크레딧 수. 본문 미리보기와 같은 4열 x 3행이다
    const CAST_PAGE_SIZE = 12;
    // 캐러셀 두 줄(갤러리·컬렉션)의 칸 수와 간격은 CSS 변수가 유일한 출처다 -
    // 화면 폭에 따라 미디어쿼리가 바꾸므로 여기서 숫자를 다시 적으면 이동량이 어긋난다.
    // 아래 값은 변수를 못 읽었을 때의 폴백이다
    const GALLERY_PAGE_SIZE = 3;
    const GALLERY_GAP = 12;
    const COLLECTION_ROW_PAGE_SIZE = 4;
    const COLLECTION_ROW_GAP = 18;

    // MOD-13 모달이 한 번에 훑는 내 컬렉션 수. 초과분은 D-01에서 담는다
    const COLLECTION_PAGE_SIZE = 50;

    // 코멘트 저장·좋아요는 담당 밖(USER_COMMENT·COMMENT_LIKE) 컨트롤러를 호출만 한다.
    // 둘 다 JSON이 아니라 폼 인코딩으로 받는다 - @ModelAttribute 바인딩이라 그렇다
    const COMMENT_SAVE_PATH = "/comment/doSave";

    // 코멘트 길이 상한. textarea의 maxlength와 같은 값이어야 세는 것과 막는 것이 어긋나지 않는다.
    // COMMENT_DETAIL은 CLOB이라 DB가 막아 주지 않고, 서버도 통과시키므로 여기가 유일한 방어선이다
    const COMMENT_MAX_LENGTH = 1000;

    // MOD-04 신고. 접수는 팀원 API가 받고 우리는 폼 네 값만 보낸다
    const REPORT_SAVE_PATH = "/report/doSave";
    // 신고 상세 길이 상한. textarea의 maxlength와 같은 값이어야 세는 것과 막는 것이 어긋나지 않는다
    const REPORT_MAX_LENGTH = 1000;

    // 채움 깃발로 바꿔 같은 코멘트를 또 신고하지 못하게 막는다
    const ICON_FLAG_ON = "bi bi-flag-fill";

    // 이 사유만 상세가 필수다(DB CK_REPORT_OTHER_DETAIL). 나머지 코드는 화면이 알 필요가 없다
    const REPORT_REASON_OTHER = "OTHER";

    // 켜짐/꺼짐을 색만이 아니라 아이콘 모양으로도 구분한다
    const ICON_HEART_ON = "bi bi-heart-fill";
    const ICON_HEART_OFF = "bi bi-heart";
    const COMMENT_LIKE_PATH = "/commentLike/upToggleLike";
    // 영상은 유튜브에 있고 TMDB는 id만 준다. nocookie 쪽은 재생 전까지 추적 쿠키를 심지 않는다
    const TRAILER_EMBED_PREFIX = "https://www.youtube-nocookie.com/embed/";

    // MessageVO의 성공 코드. 실패는 "0"이다
    const MESSAGE_OK = "1";

    // 한 회원이 한 영화에 코멘트 하나만 쓸 수 있다(UK_USER_COMMENT_CONTENT).
    // 서버가 이 경우를 따로 알려 주지 않아 오라클 오류 코드로 가린다 -
    // 사람이 읽는 문장이 아니라 코드라 화면 언어가 바뀌어도 그대로다
    const ORA_UNIQUE_VIOLATION = "ORA-00001";

    // API-007. 역할 코드는 화면의 역할 칩이 data-role로 갖고 있다(POL-033)
    const CREDIT_API_PATH = "/api/movies/";
    const ROLE_DIRECTOR = "DIRECTOR";

    const NO_SCORE = 0;

    // 서버가 data-my-watchlist에 찍는 boolean의 문자열 표기
    const WATCHLIST_ON = "true";

    // 화면 언어. layout.html이 <html lang>에 찍어 두므로 서버에서 따로 넘겨받지 않는다.
    // 이 파일이 그리는 이름은 서버가 그린 미리보기와 같은 규칙이라야 한 화면에서 표기가 갈리지 않는다(F-01)
    const IS_ENGLISH = document.documentElement.lang === "en";

    // ── 문구 ────────────────────────────────────────────
    // 이 파일은 .js라 Thymeleaf가 손대지 않는다. 번역된 문구는 detail.html의
    // 인라인 블록이 window.ENDIT_MSG로 넘겨 준다(F-01).
    // 아래 기본값은 그 블록이 없을 때를 위한 것이므로 지우지 않는다.
    const MSG = Object.assign({
        loginRequired: "로그인 후 이용할 수 있습니다.",
        loginRating: "로그인 후 별점을 남길 수 있어요.",
        loginWatchlist: "로그인 후 보고싶어요에 담을 수 있어요.",
        loginCollection: "로그인 후 컬렉션에 담을 수 있어요.",
        loginComment: "로그인 후 코멘트를 남길 수 있어요.",
        requestFailed: "요청에 실패했습니다.",
        ratingSaveFailed: "별점을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        watchlistSaveFailed: "보고싶어요를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        collectionSaveFailed: "컬렉션을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        collectionSaveError: "컬렉션 저장에 실패했습니다.",
        likeFailed: "좋아요를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        writeEmpty: "내용을 입력해 주세요.",
        writeFailed: "코멘트를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        writeDuplicate: "이 영화에는 이미 코멘트를 남기셨습니다.",
        writeSaving: "저장하는 중…",
        writeTooLong: "코멘트는 1000자까지 쓸 수 있습니다.",
        reportReasonEmpty: "신고 사유를 선택해 주세요.",
        reportDetailRequired: "기타를 고르면 상세 내용을 적어야 합니다.",
        reportTooLong: "상세 내용은 1000자까지 쓸 수 있습니다.",
        reportDone: "신고가 접수되었습니다.",
        reportFailed: "신고를 접수하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        collectionLoadFailed: "컬렉션 목록을 불러오지 못했습니다.",
        collectionEmpty: "아직 만든 컬렉션이 없습니다. 컬렉션 화면에서 먼저 만들어 주세요.",
        collectionOn: "담김",
        collectionOff: "담기",
        collectionItemCount: "작품 {0}",
        castLoadFailed: "출연/제작을 불러오지 못했습니다.",
        castLoadRetry: "출연/제작을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.",
        castRoleEmpty: "해당 역할의 인물이 없습니다.",
        ratingLabel: "평가하기",
        loading: "불러오는 중…",
        profileEmpty: "사진 없음",
        ratingScore: "{0}점"
    }, window.ENDIT_MSG || {});

    // "{0}점" / "{0} stars"처럼 숫자 자리가 있는 문구를 채운다. 언어마다 자리가 달라 서버 문구를 그대로 쓴다
    function formatScore(score) {
        return MSG.ratingScore.replace("{0}", score);
    }

    function csrfHeaders() {
        return typeof getCsrfHeaders === "function" ? getCsrfHeaders() : {};
    }

    // ── 공통: 안내 문구 한 자리 ─────────────────────────
    const notice = document.getElementById("actionNotice");

    function showNotice(message) {
        if (!notice) {
            return;
        }
        notice.textContent = message;
        notice.hidden = false;
    }

    function clearNotice() {
        if (notice) {
            notice.hidden = true;
        }
    }

    // ── 비회원 로그인 안내 (3조 컬렉션 상세와 같은 모달) ─
    // 무엇을 하려다 막혔는지에 따라 설명 줄만 갈아 끼운다.
    // 모달이나 bootstrap이 없으면 예전처럼 안내 문구 한 줄로 떨어진다
    function showLoginRequired(reasonKey) {
        const modal = document.getElementById("loginRequiredModal");
        const description = document.getElementById("loginRequiredDescription");
        const reason = MSG[reasonKey] || MSG.loginRequired;

        if (!modal || !description || typeof bootstrap === "undefined") {
            showNotice(reason);
            return;
        }

        clearNotice();
        description.textContent = reason;
        bootstrap.Modal.getOrCreateInstance(modal).show();
    }

    // ── 미리보기 격자의 "더보기" 노출 판정 ──────────────
    /*
     * 출연/제작은 화면이 좁아지면 CSS가 뒷줄을 감춰 줄 수를 유지한다(detail.css).
     * 그래서 "다 보여 줬는지"를 서버가 미리 알 수 없다 - 실제로 그려진 칸을 세어 판정한다.
     * 감춘 것이 하나라도 있거나 서버가 전체를 못 보냈으면 전체보기를 띄운다.
     * 코멘트 더보기는 이 판정을 쓰지 않는다 - 수정·삭제로 가는 길이라 항상 보여 준다.
     */
    function initSectionMore(gridId, buttonId) {
        const grid = document.getElementById(gridId);
        const button = document.getElementById(buttonId);

        if (!grid || !button) {
            return;
        }

        const total = Number(grid.dataset.totalCnt) || 0;

        function update() {
            // display:none인 칸은 사각형이 없다 - 몇 번째부터 감췄는지 CSS에 다시 적지 않아도 된다
            const shown = Array.prototype.filter.call(
                grid.children, (cell) => cell.getClientRects().length > 0).length;

            button.hidden = shown >= total;
        }

        // 창 크기가 바뀌면 감춰지는 칸 수가 달라진다
        window.addEventListener("resize", update);
        update();
    }

    // ── C-01 평가 분석 그래프 (Chart.js) ───────────────
    // 점수는 이어진 눈금이라 1→5점을 직선으로 이으면 어느 쪽으로 치우친 평가인지 형태로 읽힌다.
    // 색은 endit.css의 --endit-primary와 같은 값이다 - 캔버스는 CSS 변수를 못 읽어 여기 한 번 더 적는다
    const RATING_LINE_COLOR = "#6550C8";
    // 면은 위가 진하고 아래로 옅어진다. 별점이 높은 쪽으로 색이 차오르는 것이 보인다.
    // 봉우리가 위쪽에 걸리면 옅은 구간만 넓게 보여서, 옅은 wash보다 진하게 잡았다
    const RATING_FILL_TOP = "rgba(101, 80, 200, .40)";
    const RATING_FILL_BOTTOM = "rgba(101, 80, 200, .06)";
    const RATING_GRID_COLOR = "#EFEEF3";
    const RATING_TICK_COLOR = "#8B8493";
    // 눈금은 0과 최댓값 언저리만 있으면 된다. 폭이 220px이라 더 넣으면 숫자가 겹친다
    const RATING_Y_TICK_LIMIT = 3;
    // TMDB 평균선. 우리 분포 위에 겹쳐 어느 쪽이 후한 평가인지 보이게 한다.
    // TMDB는 점수별 분포를 주지 않아 막대를 그릴 수 없다 - 평균 한 점만 선으로 세운다
    const TMDB_LINE_COLOR = "#8FBF1F";
    // 선 색 그대로는 11px 글자가 흰 바탕에서 흐려 대비를 올린 같은 계열을 쓴다
    const TMDB_LABEL_COLOR = "#5F810F";
    const TMDB_LABEL_FONT = "11px sans-serif";
    const TMDB_LABEL_GAP = 4;

    function initRatingChart() {
        const canvas = document.getElementById("ratingChart");

        if (!canvas) {
            return;
        }

        const table = document.getElementById("ratingTable");

        // CDN이 막히면 캔버스 대신 숨겨 둔 표를 펼친다 - 숫자는 남아야 한다
        if (typeof Chart === "undefined") {
            canvas.hidden = true;

            if (table) {
                table.classList.remove("visually-hidden");
            }

            return;
        }

        const counts = (canvas.dataset.counts || "").split(",").map(Number);
        // 서버가 이미 5점 척도로 바꿔 실어 준다(TmdbRatingVO.starAverage)
        const tmdbAverage = Number(canvas.dataset.tmdbAverage);

        new Chart(canvas, {
            type: "line",
            data: {
                labels: ["1★", "2★", "3★", "4★", "5★"],
                datasets: [{
                    label: MSG.ratingChartLabel,
                    data: counts,
                    fill: "origin",
                    // 그리는 순간의 영역 높이를 받아 그라데이션을 만든다.
                    // 캔버스 크기가 폭에 따라 달라져 미리 만들어 두면 어긋난다
                    backgroundColor: (context) => {
                        const area = context.chart.chartArea;

                        if (!area) {
                            return RATING_FILL_BOTTOM;
                        }

                        const gradient = context.chart.ctx.createLinearGradient(0, area.top, 0, area.bottom);

                        gradient.addColorStop(0, RATING_FILL_TOP);
                        gradient.addColorStop(1, RATING_FILL_BOTTOM);

                        return gradient;
                    },
                    borderColor: RATING_LINE_COLOR,
                    borderWidth: 2,
                    // 꺾은선이라야 몰린 점수에서 각이 선다 - 곡선은 봉우리를 뭉갠다
                    tension: 0,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    pointBackgroundColor: RATING_LINE_COLOR,
                    // 점이 선·면과 겹쳐도 남도록 바탕색 테를 두른다
                    pointBorderColor: "#fff",
                    pointBorderWidth: 2
                }]
            },
            plugins: [{
                id: "tmdbAverageLine",

                // TMDB 평균이 x축 어디에 서는지. 라벨은 1★~5★이고 값은 1~5점이라 한 칸 당긴다
                markerX(chart) {
                    const xScale = chart.scales.x;
                    const index = Math.min(Math.max(tmdbAverage - 1, 0), counts.length - 1);
                    const floor = Math.floor(index);
                    const next = Math.min(floor + 1, counts.length - 1);

                    // 칸 사이는 비례로 나눈다
                    return xScale.getPixelForValue(floor)
                        + (xScale.getPixelForValue(next) - xScale.getPixelForValue(floor)) * (index - floor);
                },

                // 선은 우리 그래프 뒤에 깔린다 - 우리 분포가 주인공이다
                beforeDatasetsDraw(chart) {
                    if (!tmdbAverage) {
                        return;
                    }

                    const area = chart.chartArea;
                    const x = this.markerX(chart);
                    const ctx = chart.ctx;

                    ctx.save();
                    ctx.setLineDash([4, 3]);
                    ctx.strokeStyle = TMDB_LINE_COLOR;
                    ctx.lineWidth = 2;
                    ctx.beginPath();
                    ctx.moveTo(x, area.top);
                    ctx.lineTo(x, area.bottom);
                    ctx.stroke();
                    ctx.restore();
                },

                // 글자는 맨 앞에 - 우리 면이 반투명이라 뒤에 두면 흐려진다
                afterDatasetsDraw(chart) {
                    if (!tmdbAverage) {
                        return;
                    }

                    const area = chart.chartArea;
                    const x = this.markerX(chart);
                    const ctx = chart.ctx;
                    const text = MSG.ratingTmdbMarker.replace("{0}", tmdbAverage.toFixed(1));

                    ctx.save();
                    ctx.fillStyle = TMDB_LABEL_COLOR;
                    ctx.font = TMDB_LABEL_FONT;
                    ctx.textBaseline = "top";
                    // 오른쪽 끝에 붙으면 글자가 잘린다 - 넘칠 때만 왼쪽으로 붙여 쓴다
                    ctx.textAlign = x + ctx.measureText(text).width > area.right ? "right" : "left";
                    ctx.fillText(text, ctx.textAlign === "right" ? x - TMDB_LABEL_GAP : x + TMDB_LABEL_GAP,
                        area.top);
                    ctx.restore();
                }
            }],
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    // 그래프 안의 TMDB 라벨이 이미 무엇인지 말해 범례를 두지 않는다
                    legend: { display: false },
                    tooltip: {
                        displayColors: false,
                        callbacks: {
                            title: () => "",
                            // "5점 155명" - 화면 문구와 같은 번들 키를 쓴다(F-01)
                            label: (item) => MSG.ratingBucket
                                .replace("{0}", item.dataIndex + 1)
                                .replace("{1}", item.parsed.y)
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        border: { color: RATING_GRID_COLOR },
                        ticks: { color: RATING_TICK_COLOR, font: { size: 11 } }
                    },
                    y: {
                        beginAtZero: true,
                        border: { display: false },
                        grid: { color: RATING_GRID_COLOR },
                        // 인원이라 소수점이 없다
                        ticks: {
                            color: RATING_TICK_COLOR,
                            font: { size: 11 },
                            precision: 0,
                            maxTicksLimit: RATING_Y_TICK_LIMIT
                        }
                    }
                }
            }
        });
    }

    // ── 평가·보고싶어요 (ACT-C-001~003) ────────────────
    function initRecord() {
        const box = document.getElementById("ratingBox");

        if (!box) {
            return;
        }

        const contentId = box.dataset.contentId;
        // 서버가 비회원에게는 data-member-id를 아예 안 그린다
        const memberId = box.dataset.memberId;

        const stars = Array.prototype.slice.call(box.querySelectorAll(".star"));
        const label = document.getElementById("ratingLabel");
        const watchButton = document.getElementById("watchlistButton");
        const cancelHint = document.getElementById("ratingCancelHint");

        // 다시 들어와도 내 기록이 채워져 있도록 서버가 그려 둔 값에서 출발한다.
        // 비회원·미평가면 data-my-score 자체가 없어 Number(undefined)가 NaN이라 0으로 떨어진다
        let score = Number(box.dataset.myScore) || NO_SCORE;
        let watched = box.dataset.myWatchlist === WATCHLIST_ON;

        function paintStars(value) {
            stars.forEach((star) => {
                const on = Number(star.dataset.score) <= value;
                star.classList.toggle("is-on", on);
                star.setAttribute("aria-pressed", String(Number(star.dataset.score) === score));
            });
        }

        function render() {
            paintStars(score);
            label.textContent = score === NO_SCORE ? MSG.ratingLabel : formatScore(score);
            showCancelHint(null);
        }

        // ACT-C-002를 눈에 보이게 한다 - 지금 준 점수와 같은 별에 올렸을 때만 띄운다.
        // star가 null이면 감춘다. 가로 위치는 그 별의 중앙에 맞춘다
        function showCancelHint(star) {
            if (!cancelHint) {
                return;
            }

            if (!star || score === NO_SCORE || Number(star.dataset.score) !== score) {
                cancelHint.hidden = true;
                return;
            }

            cancelHint.style.left = (star.offsetLeft + star.offsetWidth / 2) + "px";
            cancelHint.hidden = false;
        }

        function renderWatch() {
            watchButton.classList.toggle("is-on", watched);
            watchButton.setAttribute("aria-pressed", String(watched));
            watchButton.querySelector("i").className = watched ? "bi bi-check-lg" : "bi bi-plus-lg";
        }

        async function send(url, method, body) {
            // 평가·보고싶어요 요청은 X-Member-Id 대신 서버 인증 사용, CSRF 헤더 유지
            const headers = csrfHeaders();

            if (body) {
                headers["Content-Type"] = "application/json";
            }

            const response = await fetch(url, {
                method: method,
                credentials: "same-origin",
                headers: headers,
                body: body ? JSON.stringify(body) : undefined
            });

            if (!response.ok) {
                throw new Error(MSG.requestFailed);
            }
        }

        // ACT-C-001 별점 주기 / ACT-C-002 같은 별을 다시 누르면 취소
        stars.forEach((star) => {
            star.addEventListener("click", async () => {
                if (!memberId) {
                    showLoginRequired("loginRating");
                    return;
                }

                clearNotice();

                const clicked = Number(star.dataset.score);
                const previous = score;

                // 낙관적 갱신 - 응답을 기다리지 않고 먼저 바꾼다
                score = clicked === previous ? NO_SCORE : clicked;
                render();

                try {
                    if (score === NO_SCORE) {
                        await send(RATING_API_PATH + contentId + "/rating", "DELETE");
                    } else {
                        await send(RATING_API_PATH + contentId + "/rating", "PUT", { ratingScore: score });
                    }
                } catch (error) {
                    // 실패하면 원상복구한다
                    score = previous;
                    render();
                    showNotice(MSG.ratingSaveFailed);
                }
            });

            // 누르기 전 몇 점이 될지 미리 보여 준다
            star.addEventListener("mouseenter", () => {
                paintStars(Number(star.dataset.score));
                showCancelHint(star);
            });
        });

        box.querySelector(".stars").addEventListener("mouseleave", render);

        // ACT-C-003 보고싶어요 토글
        watchButton.addEventListener("click", async () => {
            if (!memberId) {
                showLoginRequired("loginWatchlist");
                return;
            }

            clearNotice();

            const previous = watched;

            watched = !watched;
            renderWatch();

            try {
                await send(WATCHLIST_API_PATH + contentId, watched ? "POST" : "DELETE");
            } catch (error) {
                watched = previous;
                renderWatch();
                showNotice(MSG.watchlistSaveFailed);
            }
        });

        render();
        renderWatch();
    }

    // ── 컬렉션에 추가 (ACT-C-005 / MOD-13) ─────────────
    function initCollection() {
        const button = document.getElementById("collectionButton");
        const modal = document.getElementById("collectionModal");
        const box = document.getElementById("ratingBox");

        if (!button || !modal || !box) {
            return;
        }

        const contentId = box.dataset.contentId;
        const memberId = box.dataset.memberId;
        const status = document.getElementById("collectionStatus");
        const list = document.getElementById("collectionList");

        // 컬렉션 API는 LoginMemberHelper로 회원을 판단하므로 회원 번호 헤더는 불필요
        function itemUrl(collectionId) {
            return COLLECTION_API_PATH + collectionId + "/items";
        }

        function showStatus(message) {
            status.textContent = message;
            status.hidden = false;
        }

        // 담김 여부는 컬렉션마다 한 번씩 물어본다 - 일괄 조회 API가 없다(docs/known-issues.md)
        async function isIncluded(collectionId) {
            const response = await fetch(itemUrl(collectionId) + "/" + contentId, {
                credentials: "same-origin"
            });

            return response.ok;
        }

        // 목록 화면 카드와 같은 대표 포스터를 쓴다. 없으면 카드 폴백과 같은 필름 아이콘이다
        function toThumb(collection) {
            const thumb = document.createElement("span");

            thumb.className = "picker-item-thumb";

            // 포스터는 완성 URL이 아니라 TMDB 경로(/abc.jpg)로 온다.
            // 목록 화면 카드가 쓰는 변환을 그대로 부른다 - 베이스 주소를 여기 또 적지 않는다
            const posterUrl = typeof resolveCollectionPosterUrl === "function"
                ? resolveCollectionPosterUrl(collection.previewPosterUrl1)
                : collection.previewPosterUrl1;

            if (collection.previewPosterUrl1) {
                const image = document.createElement("img");

                image.src = posterUrl;
                image.alt = "";
                image.loading = "lazy";
                // 주소가 죽어 있으면 빈 칸 대신 아이콘으로 떨어진다
                image.addEventListener("error", () => {
                    thumb.classList.add("is-empty");
                    thumb.innerHTML = '<i class="bi bi-film" aria-hidden="true"></i>';
                });
                thumb.appendChild(image);

                return thumb;
            }

            thumb.classList.add("is-empty");
            thumb.innerHTML = '<i class="bi bi-film" aria-hidden="true"></i>';

            return thumb;
        }

        function drawRow(collection, included) {
            const item = document.createElement("li");
            const row = document.createElement("button");

            row.type = "button";
            row.className = "picker-item";
            row.setAttribute("aria-pressed", String(included));

            const mark = document.createElement("span");
            const body = document.createElement("span");
            const title = document.createElement("span");
            const items = document.createElement("span");
            const count = document.createElement("span");

            mark.className = "picker-item-mark";
            body.className = "picker-item-body";
            title.className = "picker-item-title";
            items.className = "picker-item-items";
            count.className = "picker-item-count";

            title.textContent = collection.title;
            // 몇 편이 담긴 컬렉션인지 알아야 어디에 넣을지 고를 수 있다
            items.textContent = MSG.collectionItemCount.replace("{0}", collection.itemCount || 0);
            body.append(title, items);

            function paint(on) {
                mark.innerHTML = on ? '<i class="bi bi-check-lg"></i>' : "";
                // 체크 아이콘만으로 상태를 알리지 않도록 문구를 함께 바꾼다
                count.textContent = on ? MSG.collectionOn : MSG.collectionOff;
                // 담긴 줄은 문구도 보라로 - 아이콘·문구·색 셋이 같은 말을 한다
                count.classList.toggle("is-on", on);
                row.setAttribute("aria-pressed", String(on));
            }

            let on = included;

            paint(on);

            row.addEventListener("click", async () => {
                const previous = on;

                on = !on;
                paint(on);
                row.disabled = true;

                try {
                    const response = on
                        ? await fetch(itemUrl(collection.collectionId), {
                            method: "POST",
                            credentials: "same-origin",
                            headers: Object.assign({ "Content-Type": "application/json" }, csrfHeaders()),
                            body: JSON.stringify({ contentId: Number(contentId) })
                        })
                        : await fetch(itemUrl(collection.collectionId) + "/" + contentId, {
                            method: "DELETE",
                            credentials: "same-origin",
                            headers: csrfHeaders()
                        });

                    /*
                     * 서버가 이미 원하는 상태인 경우는 오류가 아니다 - 추가 경로의 409는
                     * "이미 담긴 작품", 삭제 경로의 404는 "이미 빠진 작품"이고 둘 다
                     * 눌러서 만들려던 상태와 같다. CollectionItemController가 삭제 실패에도
                     * 409를 쓰므로 경로를 갈라 보지 않으면 없는 항목이 "담김"으로 되돌아온다.
                     */
                    const settled = on
                        ? response.status === 409
                        : response.status === 404;

                    if (!response.ok && !settled) {
                        throw new Error(MSG.collectionSaveError);
                    }
                } catch (error) {
                    on = previous;
                    paint(on);
                    showStatus(MSG.collectionSaveFailed);
                } finally {
                    row.disabled = false;
                }
            });

            row.appendChild(mark);
            row.appendChild(toThumb(collection));
            row.appendChild(body);
            row.appendChild(count);
            item.appendChild(row);
            list.appendChild(item);
        }

        async function load() {
            list.innerHTML = "";
            showStatus(MSG.loading);

            try {
                const response = await fetch(
                    MEMBER_COLLECTION_API_PATH + "?pageNo=1&pageSize=" + COLLECTION_PAGE_SIZE,
                    { credentials: "same-origin" });

                if (!response.ok) {
                    throw new Error(MSG.collectionLoadFailed);
                }

                const body = await response.json();
                const collections = body.items || [];

                if (collections.length === 0) {
                    showStatus(MSG.collectionEmpty);
                    return;
                }

                // 담김 여부를 모두 받은 뒤에 한 번에 그린다 - 줄이 순서 없이 튀지 않게
                const included = await Promise.all(
                    collections.map((collection) => isIncluded(collection.collectionId)));

                status.hidden = true;
                collections.forEach((collection, index) => drawRow(collection, included[index]));
            } catch (error) {
                showStatus(MSG.collectionLoadFailed);
            }
        }

        button.addEventListener("click", () => {
            if (!memberId) {
                showLoginRequired("loginCollection");
                return;
            }

            clearNotice();
            openModal(modal);
            load();
        });
    }

    // ── 목록형 모달 공통 (MOD-05·MOD-13) ───────────────
    function openModal(modal) {
        modal.hidden = false;
    }

    function closeModal(modal) {
        modal.hidden = true;
    }

    function initModalDismiss() {
        const modals = Array.prototype.slice.call(document.querySelectorAll(".picker-modal"));

        modals.forEach((modal) => {
            modal.querySelector(".picker-close").addEventListener("click", () => closeModal(modal));
            modal.addEventListener("click", (event) => {
                // 대화상자 바깥(어두운 배경)을 눌렀을 때만 닫는다
                if (event.target === modal) {
                    closeModal(modal);
                }
            });
        });

        document.addEventListener("keydown", (event) => {
            if (event.key !== "Escape") {
                return;
            }
            modals.forEach((modal) => closeModal(modal));
        });
    }

    // ── 코멘트 스포일러 (C-02) ─────────────────────────
    function initSpoiler() {
        document.querySelectorAll(".comment-spoiler").forEach((button) => {
            button.addEventListener("click", () => {
                const body = button.nextElementSibling;

                if (body) {
                    body.hidden = false;
                }

                button.remove();
            });
        });
    }

    // 담당 밖 컨트롤러 둘은 @ModelAttribute 바인딩이라 JSON이 아니라 폼으로 보내야 한다
    async function postForm(path, fields) {
        const response = await fetch(path, {
            method: "POST",
            credentials: "same-origin",
            headers: Object.assign(
                { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
                csrfHeaders()),
            body: new URLSearchParams(fields).toString()
        });

        // 실패해도 본문을 읽는다. 무엇 때문에 막혔는지가 거기에만 있다
        const body = await response.json().catch(() => null);

        if (!response.ok) {
            const failure = new Error(MSG.requestFailed);

            failure.body = body;
            throw failure;
        }

        return body;
    }

    // ── 코멘트 좋아요 (C-02) ───────────────────────────
    function initCommentLike() {
        const grid = document.getElementById("commentGrid");

        if (!grid) {
            return;
        }

        // 서버가 비회원에게는 data-member-id를 아예 안 그린다
        const memberId = grid.dataset.memberId;

        grid.querySelectorAll(".comment-like").forEach((button) => {
            const count = button.querySelector(".comment-like-count");
            const icon = button.querySelector("i");

            button.addEventListener("click", async () => {
                if (!memberId) {
                    showLoginRequired("loginLike");
                    return;
                }

                clearNotice();
                // 연타하면 토글이 엇갈려 화면 숫자와 서버가 어긋난다
                button.disabled = true;

                try {
                    const body = await postForm(COMMENT_LIKE_PATH, {
                        memberId: memberId,
                        commentId: button.dataset.commentId
                    });

                    // 서버가 detailMessage에 토글 후 좋아요 수를 담아 준다.
                    // message는 한국어 문장이라 상태 판정에 쓰지 않는다(F-01에서 깨진다)
                    count.textContent = body.detailMessage;
                    button.classList.toggle("is-on");

                    const on = button.classList.contains("is-on");

                    button.setAttribute("aria-pressed", String(on));
                    // 채운 따봉으로 바꿔 색 없이도 상태가 보이게 한다
                    icon.className = on ? ICON_HEART_ON : ICON_HEART_OFF;
                } catch (error) {
                    showNotice(MSG.likeFailed);
                } finally {
                    button.disabled = false;
                }
            });
        });
    }

    // ── 코멘트 작성 (ACT-C-005 / MOD-02) ───────────────
    // 목록 화면(C-04)에는 영화 코멘트 작성 버튼이 없다 - 진입은 여기뿐이다(팀 약속)
    function initCommentWrite() {
        const modal = document.getElementById("commentWriteModal");
        const openButton = document.getElementById("commentButton");

        if (!modal || !openButton) {
            return;
        }

        const contentId = modal.dataset.contentId;
        const memberId = modal.dataset.memberId;
        const field = document.getElementById("commentWriteBody");
        const spoiler = document.getElementById("commentWriteSpoiler");
        const submit = document.getElementById("commentWriteSubmit");
        const status = document.getElementById("commentWriteStatus");
        const count = document.getElementById("commentWriteCount");

        function showStatus(message) {
            status.textContent = message;
            status.hidden = false;
        }

        function renderCount() {
            const length = field.value.length;

            count.textContent = length + " / " + COMMENT_MAX_LENGTH;
            // 상한에 닿았다는 것을 숫자만이 아니라 색으로도 알린다
            count.classList.toggle("is-full", length >= COMMENT_MAX_LENGTH);
        }

        field.addEventListener("input", renderCount);

        openButton.addEventListener("click", () => {
            if (!memberId) {
                showLoginRequired("loginComment");
                return;
            }

            clearNotice();
            status.hidden = true;
            renderCount();
            openModal(modal);
            field.focus();
        });

        submit.addEventListener("click", async () => {
            const detail = field.value.trim();

            if (!detail) {
                showStatus(MSG.writeEmpty);
                field.focus();
                return;
            }

            // maxlength는 붙여넣기까지 막지만 개발자도구로는 넘길 수 있어 보내기 전에 한 번 더 본다
            if (detail.length > COMMENT_MAX_LENGTH) {
                showStatus(MSG.writeTooLong);
                field.focus();
                return;
            }

            submit.disabled = true;
            showStatus(MSG.writeSaving);

            try {
                const body = await postForm(COMMENT_SAVE_PATH, {
                    memberId: memberId,
                    contentId: contentId,
                    commentDetail: detail,
                    spoiler: spoiler.checked ? "Y" : "N"
                });

                if (MESSAGE_OK !== body.id) {
                    throw new Error(MSG.writeFailed);
                }

                // 새 코멘트가 미리보기와 건수에 함께 반영돼야 해서 다시 그린다.
                // 카드 8장과 더보기 노출 여부를 서버가 정하므로 부분 갱신으로는 맞출 수 없다
                window.location.reload();
            } catch (error) {
                const detail = error.body ? error.body.detailMessage : "";

                showStatus(detail && detail.indexOf(ORA_UNIQUE_VIOLATION) >= 0
                        ? MSG.writeDuplicate
                        : MSG.writeFailed);
                submit.disabled = false;
            }
        });
    }

    // ── 출연/제작 전체 (ACT-C-006 / MOD-05) ─────────────
    function initCast() {
        const moreBtn = document.getElementById("castMoreButton");
        const modal = document.getElementById("castModal");

        // 미리보기로 다 보이는 영화는 전체보기 버튼 자체가 서버에서 안 그려진다
        if (!moreBtn || !modal) {
            return;
        }

        const grid = document.getElementById("castModalGrid");
        const status = document.getElementById("castModalStatus");
        const loadMoreBtn = document.getElementById("castMoreLoad");
        const chips = Array.prototype.slice.call(
            modal.querySelectorAll(".role-chip"));

        const contentId = modal.dataset.contentId;

        // 역할 코드 → 라벨 표를 칩에서 읽어 온다. POL-033 표가 화면과 JS 두 곳에 갈리지 않는다
        const roleLabels = {};

        chips.forEach((chip) => {
            if (chip.dataset.role) {
                roleLabels[chip.dataset.role] = chip.textContent;
            }
        });

        let role = "";
        let pageNo = 1;
        // 진행 중 요청. 칩을 빠르게 바꾸면 앞 요청의 응답이 비워 놓은 그리드에 뒤늦게 그려져
        // 칩과 목록이 어긋나고 pageNo도 건너뛴다. 새 요청을 걸기 전에 앞을 끊는다
        let inFlight = null;

        function creditUrl() {
            return CREDIT_API_PATH + contentId + "/credits"
                + "?pageNo=" + pageNo
                + "&pageSize=" + CAST_PAGE_SIZE
                + (role ? "&role=" + encodeURIComponent(role) : "");
        }

        // LocaleTextHelper.get과 같은 규칙 - 고른 쪽이 비면 반대쪽으로 떨어진다
        function toName(credit) {
            const preferred = IS_ENGLISH ? credit.nameOrg : credit.nameKo;
            const fallback = IS_ENGLISH ? credit.nameKo : credit.nameOrg;

            return preferred || fallback || "";
        }

        // 본문 그리드의 .cast-cell과 같은 구조로 만든다
        function drawCell(credit) {
            const cell = document.createElement("a");

            cell.className = "cast-cell";
            cell.href = "/people/" + credit.personId;

            if (credit.profileImageUrl) {
                const avatar = document.createElement("img");

                avatar.className = "cast-avatar";
                avatar.src = credit.profileImageUrl;
                avatar.loading = "lazy";
                avatar.alt = "profile";
                cell.appendChild(avatar);
            } else {
                const placeholder = document.createElement("div");

                placeholder.className = "cast-avatar-placeholder";
                // 서버가 그린 크레딧 줄(detail.html)과 같은 폴백 - 아이콘만 넣고 문구는 title로 남긴다
                placeholder.title = MSG.profileEmpty;
                placeholder.innerHTML = "<i class=\"bi bi-person-fill\" aria-hidden=\"true\"></i>";
                cell.appendChild(placeholder);
            }

            const text = document.createElement("div");
            const name = document.createElement("div");
            const roleLine = document.createElement("div");

            text.className = "cast-text";
            name.className = "cast-name";
            name.textContent = toName(credit);
            roleLine.className = "cast-role";

            // POL-033 표에 없는 역할이면 라벨을 만들지 않는다
            const label = roleLabels[credit.role];

            if (label) {
                const roleTag = document.createElement("span");

                roleTag.textContent = label;

                if (credit.role === ROLE_DIRECTOR) {
                    roleTag.className = "is-director";
                }

                roleLine.appendChild(roleTag);
            }

            if (credit.character) {
                const character = document.createElement("span");

                character.textContent = label ? " | " + credit.character : credit.character;
                roleLine.appendChild(character);
            }

            text.appendChild(name);
            text.appendChild(roleLine);
            cell.appendChild(text);
            grid.appendChild(cell);
        }

        function showStatus(message) {
            status.textContent = message;
            status.hidden = false;
        }

        async function load() {
            if (inFlight) {
                inFlight.abort();
            }

            const controller = new AbortController();

            inFlight = controller;
            loadMoreBtn.hidden = true;
            showStatus(MSG.loading);

            try {
                const response = await fetch(creditUrl(), {
                    credentials: "same-origin",
                    signal: controller.signal
                });

                if (!response.ok) {
                    throw new Error(MSG.castLoadFailed);
                }

                const body = await response.json();
                const items = body.items || [];
                const totalCnt = body.page ? body.page.totalCnt : items.length;

                items.forEach(drawCell);

                if (grid.childElementCount === 0) {
                    showStatus(MSG.castRoleEmpty);
                } else {
                    status.hidden = true;
                }

                // 받은 만큼이 전체에 못 미치면 다음 페이지가 남아 있다
                loadMoreBtn.hidden = grid.childElementCount >= totalCnt;
                pageNo += 1;
            } catch (error) {
                // 우리가 끊은 요청은 실패가 아니다. 뒤이어 건 요청이 화면을 마저 그린다
                if (error.name !== "AbortError") {
                    showStatus(MSG.castLoadRetry);
                }
            } finally {
                if (inFlight === controller) {
                    inFlight = null;
                }
            }
        }

        function reload() {
            grid.innerHTML = "";
            pageNo = 1;
            load();
        }

        chips.forEach((chip) => {
            chip.addEventListener("click", () => {
                if (chip.dataset.role === role) {
                    return;
                }

                role = chip.dataset.role;
                chips.forEach((other) => {
                    const on = other === chip;

                    other.classList.toggle("is-on", on);
                    other.setAttribute("aria-pressed", String(on));
                });
                reload();
            });
        });

        loadMoreBtn.addEventListener("click", load);

        moreBtn.addEventListener("click", () => {
            openModal(modal);
            reload();
        });
    }

    // ── 코멘트 신고 (ACT-C-012 / MOD-04) ───────────────
    // 3조 comment_list.js의 신고 흐름을 그대로 쓴다 - 부트스트랩 모달을 열고,
    // 사유 select와 상세만 모아 팀원 API로 보낸 뒤 서버 문구를 그대로 알린다.
    // 요청만 우리 postForm으로 보낸다(CSRF 헤더가 붙어야 POST가 통과한다)
    function initReport() {
        const modalEl = document.getElementById("reportModal");
        const grid = document.getElementById("commentGrid");

        // 비회원이거나 사유 코드를 못 받으면 서버가 버튼을 아예 안 그린다
        if (!modalEl || !grid) {
            return;
        }

        const modal = new bootstrap.Modal(modalEl);
        const memberId = modalEl.dataset.memberId;
        const commentId = document.getElementById("rpCommentId");
        const reason = document.getElementById("rpReason");
        const detail = document.getElementById("rpDetail");
        const status = document.getElementById("rpStatus");
        const count = document.getElementById("rpCount");
        const submit = document.getElementById("btnRpSave");

        // 접수에 성공하면 이 버튼을 잠가야 해서 어느 카드에서 열었는지 들고 있는다
        let openedBy = null;

        function showStatus(message) {
            status.textContent = message;
            status.hidden = false;
        }

        function renderCount() {
            const length = detail.value.length;

            count.textContent = length + " / " + REPORT_MAX_LENGTH;
            // 상한에 닿았다는 것을 숫자만이 아니라 색으로도 알린다
            count.classList.toggle("is-full", length >= REPORT_MAX_LENGTH);
        }

        detail.addEventListener("input", renderCount);

        grid.querySelectorAll(".comment-report").forEach((button) => {
            button.addEventListener("click", () => {
                openedBy = button;
                commentId.value = button.dataset.commentId;
                // 앞서 신고한 사유와 상세가 남으면 고른 적 없는 사유로 접수된다
                reason.selectedIndex = 0;
                detail.value = "";
                status.hidden = true;
                submit.disabled = false;
                renderCount();
                modal.show();
            });
        });

        submit.addEventListener("click", async () => {
            const text = detail.value.trim();

            // 빈 항목이 첫 번째라 아무것도 안 고르고 누를 수 있다
            if (!reason.value) {
                showStatus(MSG.reportReasonEmpty);
                reason.focus();
                return;
            }

            // 기타(OTHER)만 상세가 필수다(DB CK_REPORT_OTHER_DETAIL). 서버와 DB도 막지만 왕복을 아낀다
            if (REPORT_REASON_OTHER === reason.value && !text) {
                showStatus(MSG.reportDetailRequired);
                detail.focus();
                return;
            }

            // maxlength는 붙여넣기까지 막지만 개발자도구로는 넘길 수 있어 보내기 전에 한 번 더 본다
            if (text.length > REPORT_MAX_LENGTH) {
                showStatus(MSG.reportTooLong);
                detail.focus();
                return;
            }

            submit.disabled = true;

            try {
                const body = await postForm(REPORT_SAVE_PATH, {
                    reportMemberId: memberId,
                    commentId: commentId.value,
                    reason: reason.value,
                    detail: text
                });

                // body.message는 한국어 문장이라 상태 판정에도 표시에도 쓰지 않는다(F-01에서 깨진다)
                if (MESSAGE_OK !== String(body.id)) {
                    throw new Error(MSG.reportFailed);
                }

                // 서버에도 DB에도 중복 검사가 없어 잠그는 것은 여기가 유일하다
                if (openedBy) {
                    openedBy.disabled = true;
                    openedBy.querySelector("i").className = ICON_FLAG_ON;
                }

                modal.hide();
                showNotice(MSG.reportDone);
            } catch (error) {
                showStatus(MSG.reportFailed);
                submit.disabled = false;
            }
        });
    }

    // ── 이 작품이 담긴 컬렉션 (C-02) ───────────────────
    // 카드는 3조 collection-list.js의 createCollectionCard가 그린다 - 목록·검색 화면과 같은 모양이라야 한다.
    // 캐러셀 골격은 갤러리와 같지만 그쪽이 단일 인스턴스 전제라 재사용이 안 돼 따로 둔다
    function initCollectionRow() {
        const viewport = document.querySelector(".collection-row-viewport");
        const track = document.querySelector(".collection-row-track");

        // 담긴 컬렉션이 없으면 서버가 섹션째 안 그린다
        if (!viewport || !track) {
            return;
        }

        // 카드 공장이 없으면(스크립트 로드 실패) 빈 줄을 남기지 않고 섹션을 접는다
        if (!window.enditCollectionGrid.isReady()) {
            track.closest(".container").hidden = true;
            return;
        }

        // 캐러셀 칸으로 감싸야 해서 fill 대신 카드만 받아 온다
        window.enditCollectionGrid.toCollections().forEach((collection) => {
            const item = document.createElement("div");

            item.className = "collection-row-item";
            item.append(window.enditCollectionGrid.createCard(collection));
            track.append(item);
        });

        window.enditCarousel.create({
            viewport: viewport,
            track: track,
            prevBtn: document.querySelector(".collection-row-prev"),
            nextBtn: document.querySelector(".collection-row-next"),
            perViewVar: "--collection-per-view",
            gapVar: "--collection-gap",
            perViewFallback: COLLECTION_ROW_PAGE_SIZE,
            gapFallback: COLLECTION_ROW_GAP,
            onUpdate: window.enditCarousel.centerNavOn(".collection-list-card-visual")
        });
    }

    // ── 갤러리 캐러셀 (C-02) ───────────────────────────
    function initGallery() {
        const viewport = document.querySelector(".gallery-viewport");

        if (!viewport) {
            return;
        }

        window.enditCarousel.create({
            viewport: viewport,
            track: viewport.querySelector(".gallery-track"),
            prevBtn: document.querySelector(".gallery-prev"),
            nextBtn: document.querySelector(".gallery-next"),
            perViewVar: "--gallery-per-view",
            gapVar: "--gallery-gap",
            perViewFallback: GALLERY_PAGE_SIZE,
            gapFallback: GALLERY_GAP
        });
    }

    // ── 예고편 ────────────────────────────────────────
    // 갤러리 확대와 같은 전면 오버레이다. 다른 점은 닫을 때 src를 반드시 비워야 한다는 것 -
    // iframe을 남겨 두면 모달이 사라져도 유튜브가 계속 재생돼 소리가 난다
    function initTrailerModal() {
        const button = document.getElementById("trailerButton");
        const modal = document.getElementById("trailerModal");

        // 예고편이 없는 작품은 서버가 버튼을 안 그린다
        if (!button || !modal) {
            return;
        }

        const frame = document.getElementById("trailerFrame");
        const closeBtn = modal.querySelector(".gallery-modal-close");

        function close() {
            frame.src = "";
            modal.classList.remove("active");
        }

        button.addEventListener("click", () => {
            frame.src = TRAILER_EMBED_PREFIX + button.dataset.trailerKey;
            modal.classList.add("active");
        });

        closeBtn.addEventListener("click", close);
        modal.addEventListener("click", (event) => {
            // 대화상자 바깥(어두운 배경)을 눌렀을 때만 닫는다
            if (event.target === modal) {
                close();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && modal.classList.contains("active")) {
                close();
            }
        });
    }

    // ── 갤러리 확대 (MOD-06) ───────────────────────────
    function initGalleryModal() {
        // 확대용 URL은 서버가 data-full에 완성해 준다. 화면은 이미지 크기를 알지 못한다
        const thumbs = Array.prototype.slice.call(document.querySelectorAll(".gallery-item img"));
        const images = thumbs.map((img) => img.getAttribute("data-full"));
        const modal = document.getElementById("galleryModal");
        const modalImg = document.getElementById("galleryModalImg");
        const modalCount = document.getElementById("galleryModalCount");

        if (!modal || !modalImg || images.length === 0) {
            return;
        }

        const closeBtn = modal.querySelector(".gallery-modal-close");
        const prevBtn = modal.querySelector(".gallery-modal-prev");
        const nextBtn = modal.querySelector(".gallery-modal-next");

        let currentIndex = 0;

        function show(index) {
            currentIndex = index;
            modalImg.src = images[currentIndex];
            // 첫 장·마지막 장에서는 화살표를 감춘다
            prevBtn.hidden = currentIndex === 0;
            nextBtn.hidden = currentIndex === images.length - 1;
            // 전부 서버 렌더링이므로 목록 길이가 곧 전체 건수다
            modalCount.textContent = (currentIndex + 1) + " / " + images.length;
        }

        function close() {
            modal.classList.remove("active");
            modalImg.src = "";
        }

        thumbs.forEach((img, index) => {
            img.addEventListener("click", () => {
                show(index);
                modal.classList.add("active");
            });
        });

        prevBtn.addEventListener("click", () => {
            if (currentIndex > 0) {
                show(currentIndex - 1);
            }
        });
        nextBtn.addEventListener("click", () => {
            if (currentIndex < images.length - 1) {
                show(currentIndex + 1);
            }
        });
        closeBtn.addEventListener("click", close);
        modal.addEventListener("click", (event) => {
            if (event.target === modal) {
                close();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && modal.classList.contains("active")) {
                close();
            }
        });
    }

    document.addEventListener("DOMContentLoaded", () => {
        initSectionMore("castGrid", "castMoreButton");
        initRatingChart();
        initRecord();
        initCollection();
        initModalDismiss();
        initSpoiler();
        initCommentLike();
        initCommentWrite();
        initReport();
        initCast();
        initGallery();
        initCollectionRow();
        initGalleryModal();
        initTrailerModal();
    });
})();
