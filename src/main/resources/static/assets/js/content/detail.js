/*
 * C-01·C-02 영화 상세 화면.
 * 평가·보고싶어요·컬렉션 담기는 담당 밖(MEMBER_CONTENT·COLLECTION_ITEM) API를 호출만 한다.
 */
(() => {
    "use strict";

    const RATING_API_PATH = "/api/movies/";
    const WATCHLIST_API_PATH = "/api/watchlist/";
    const COLLECTION_API_PATH = "/api/collections/";
    const MEMBER_COLLECTION_API_PATH = "/api/users/";

    // 로그인 병합 전까지 쓰는 임시 회원 식별 헤더(MemberContentController와 같은 약속)
    const MEMBER_ID_HEADER = "X-Member-Id";

    // 출연/제작 4열 x 3행. 이 수를 넘을 때만 캐러셀 화살표와 전체보기가 뜬다
    const CAST_PAGE_SIZE = 12;
    const GALLERY_PAGE_SIZE = 3;
    const GALLERY_GAP = 12;

    // MOD-13 모달이 한 번에 훑는 내 컬렉션 수. 초과분은 D-01에서 담는다
    const COLLECTION_PAGE_SIZE = 50;

    const MAX_SCORE = 5;
    const NO_SCORE = 0;

    // 쓰기 버튼이 없는 화면(비회원)에서 반복해 띄우는 안내
    const LOGIN_REQUIRED_NOTICE = "로그인 후 이용할 수 있습니다.";

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

        /*
         * MEMBER_CONTENT에 "내 별점·보고싶어요 단건 조회" 계약이 없어
         * 진입 시점의 내 기록을 알 수 없다. 0/false에서 출발해 클릭 응답으로만 채운다.
         */
        let score = NO_SCORE;
        let watched = false;

        function paintStars(value) {
            stars.forEach((star) => {
                const on = Number(star.dataset.score) <= value;
                star.classList.toggle("is-on", on);
                star.setAttribute("aria-pressed", String(Number(star.dataset.score) === score));
            });
        }

        function render() {
            paintStars(score);
            label.textContent = score === NO_SCORE ? "평가하기" : score + "점";
        }

        function renderWatch() {
            watchButton.classList.toggle("is-on", watched);
            watchButton.setAttribute("aria-pressed", String(watched));
            watchButton.querySelector("i").className = watched ? "bi bi-check-lg" : "bi bi-plus-lg";
        }

        async function send(url, method, body) {
            const headers = Object.assign({ [MEMBER_ID_HEADER]: memberId }, csrfHeaders());

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
                throw new Error("요청에 실패했습니다.");
            }
        }

        // ACT-C-001 별점 주기 / ACT-C-002 같은 별을 다시 누르면 취소
        stars.forEach((star) => {
            star.addEventListener("click", async () => {
                if (!memberId) {
                    showNotice(LOGIN_REQUIRED_NOTICE);
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
                    showNotice("별점을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.");
                }
            });

            // 누르기 전 몇 점이 될지 미리 보여 준다
            star.addEventListener("mouseenter", () => paintStars(Number(star.dataset.score)));
        });

        box.querySelector(".stars").addEventListener("mouseleave", render);

        // ACT-C-003 보고싶어요 토글
        watchButton.addEventListener("click", async () => {
            if (!memberId) {
                showNotice(LOGIN_REQUIRED_NOTICE);
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
                showNotice("보고싶어요를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.");
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

        // 컬렉션 API는 서버측 CurrentMemberProvider로 회원을 판단하므로 헤더가 필요 없다
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

        function drawRow(collection, included) {
            const item = document.createElement("li");
            const row = document.createElement("button");

            row.type = "button";
            row.className = "picker-item";
            row.setAttribute("aria-pressed", String(included));

            const mark = document.createElement("span");
            const title = document.createElement("span");
            const count = document.createElement("span");

            mark.className = "picker-item-mark";
            title.className = "picker-item-title";
            count.className = "picker-item-count";

            title.textContent = collection.title;

            function paint(on) {
                mark.innerHTML = on ? '<i class="bi bi-check-lg"></i>' : "";
                // 체크 아이콘만으로 상태를 알리지 않도록 문구를 함께 바꾼다
                count.textContent = on ? "담김" : "담기";
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

                    // 409는 이미 담겨 있다는 뜻이라 오류가 아니라 상태 정정이다
                    if (!response.ok && response.status !== 409) {
                        throw new Error("컬렉션 저장에 실패했습니다.");
                    }

                    if (response.status === 409) {
                        on = true;
                        paint(on);
                    }
                } catch (error) {
                    on = previous;
                    paint(on);
                    showStatus("컬렉션을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.");
                } finally {
                    row.disabled = false;
                }
            });

            row.appendChild(mark);
            row.appendChild(title);
            row.appendChild(count);
            item.appendChild(row);
            list.appendChild(item);
        }

        async function load() {
            list.innerHTML = "";
            showStatus("불러오는 중…");

            try {
                const response = await fetch(
                    MEMBER_COLLECTION_API_PATH + memberId + "/collections?pageNo=1&pageSize=" + COLLECTION_PAGE_SIZE,
                    { credentials: "same-origin" });

                if (!response.ok) {
                    throw new Error("컬렉션 목록을 불러오지 못했습니다.");
                }

                const body = await response.json();
                const collections = body.items || [];

                if (collections.length === 0) {
                    showStatus("아직 만든 컬렉션이 없습니다. 컬렉션 화면에서 먼저 만들어 주세요.");
                    return;
                }

                // 담김 여부를 모두 받은 뒤에 한 번에 그린다 - 줄이 순서 없이 튀지 않게
                const included = await Promise.all(
                    collections.map((collection) => isIncluded(collection.collectionId)));

                status.hidden = true;
                collections.forEach((collection, index) => drawRow(collection, included[index]));
            } catch (error) {
                showStatus("컬렉션 목록을 불러오지 못했습니다.");
            }
        }

        button.addEventListener("click", () => {
            if (!memberId) {
                showNotice(LOGIN_REQUIRED_NOTICE);
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

    // ── 출연/제작 페이징 + 전체보기 (ACT-C-006 / MOD-05) ─
    function initCast() {
        const grid = document.querySelector(".cast-grid");
        const prevBtn = document.querySelector(".cast-prev");
        const nextBtn = document.querySelector(".cast-next");

        if (!grid || !prevBtn || !nextBtn) {
            return;
        }

        const cells = Array.prototype.slice.call(grid.querySelectorAll(".cast-cell"));
        const pageCount = Math.ceil(cells.length / CAST_PAGE_SIZE);
        let pageNo = 0;

        // 한 페이지에 다 들어가면 화살표도 전체보기도 띄우지 않는다
        if (pageCount <= 1) {
            prevBtn.hidden = true;
            nextBtn.hidden = true;
            return;
        }

        function render() {
            cells.forEach((cell, index) => {
                cell.hidden = Math.floor(index / CAST_PAGE_SIZE) !== pageNo;
            });
            // 넘어갈 페이지가 없으면 흐리게 두지 않고 감춘다
            prevBtn.hidden = pageNo === 0;
            nextBtn.hidden = pageNo === pageCount - 1;
        }

        prevBtn.addEventListener("click", () => {
            if (pageNo > 0) {
                pageNo--;
                render();
            }
        });
        nextBtn.addEventListener("click", () => {
            if (pageNo < pageCount - 1) {
                pageNo++;
                render();
            }
        });

        render();

        // MOD-05 - 전체 목록은 이미 화면에 있으므로 복제만 한다(추가 조회 없음)
        const moreBtn = document.getElementById("castMoreButton");
        const modal = document.getElementById("castModal");
        const modalGrid = document.getElementById("castModalGrid");

        if (!moreBtn || !modal || !modalGrid) {
            return;
        }

        moreBtn.hidden = false;
        moreBtn.addEventListener("click", () => {
            modalGrid.innerHTML = "";
            cells.forEach((cell) => {
                const copy = cell.cloneNode(true);

                copy.hidden = false;
                modalGrid.appendChild(copy);
            });
            openModal(modal);
        });
    }

    // ── 갤러리 캐러셀 (C-02) ───────────────────────────
    function initGallery() {
        const track = document.querySelector(".gallery-track");
        const prevBtn = document.querySelector(".gallery-prev");
        const nextBtn = document.querySelector(".gallery-next");

        if (!track || !prevBtn || !nextBtn) {
            return;
        }

        const items = track.children;

        // 한 화면에 다 들어가면 화살표를 띄우지 않는다
        if (items.length <= GALLERY_PAGE_SIZE) {
            prevBtn.hidden = true;
            nextBtn.hidden = true;
            return;
        }

        let startIndex = 0;

        function update() {
            const itemWidth = items[0].getBoundingClientRect().width;

            track.style.transform = "translateX(-" + startIndex * (itemWidth + GALLERY_GAP) + "px)";
            // 넘어갈 쪽이 없으면 흐리게 두지 않고 감춘다
            prevBtn.hidden = startIndex === 0;
            nextBtn.hidden = startIndex + GALLERY_PAGE_SIZE >= items.length;
        }

        prevBtn.addEventListener("click", () => {
            startIndex = Math.max(0, startIndex - GALLERY_PAGE_SIZE);
            update();
        });
        nextBtn.addEventListener("click", () => {
            startIndex = Math.min(items.length - GALLERY_PAGE_SIZE, startIndex + GALLERY_PAGE_SIZE);
            update();
        });
        window.addEventListener("resize", update);

        update();
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
        initRecord();
        initCollection();
        initModalDismiss();
        initSpoiler();
        initCast();
        initGallery();
        initGalleryModal();
    });
})();
