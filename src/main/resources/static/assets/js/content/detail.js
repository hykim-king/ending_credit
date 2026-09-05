/*
 * C-01·C-02 영화 상세 화면.
 * 평가·보고싶어요·컬렉션 담기는 담당 밖(MEMBER_CONTENT·COLLECTION_ITEM) API를 호출만 한다.
 */
(() => {
    "use strict";

    const RATING_API_PATH = "/api/movies/";
    const WATCHLIST_API_PATH = "/api/watchlist/";
    const COLLECTION_API_PATH = "/api/collections/";
    const MEMBER_COLLECTION_API_PATH = "/api/members/collections";

    // MOD-05가 API-007에서 한 번에 받는 크레딧 수. 본문 미리보기와 같은 4열 x 3행이다
    const CAST_PAGE_SIZE = 12;
    const GALLERY_PAGE_SIZE = 3;
    const GALLERY_GAP = 12;

    // MOD-13 모달이 한 번에 훑는 내 컬렉션 수. 초과분은 D-01에서 담는다
    const COLLECTION_PAGE_SIZE = 50;

    // 코멘트 저장·좋아요는 담당 밖(USER_COMMENT·COMMENT_LIKE) 컨트롤러를 호출만 한다.
    // 둘 다 JSON이 아니라 폼 인코딩으로 받는다 - @ModelAttribute 바인딩이라 그렇다
    const COMMENT_SAVE_PATH = "/comment/doSave";

    // 코멘트 길이 상한. textarea의 maxlength와 같은 값이어야 세는 것과 막는 것이 어긋나지 않는다.
    // COMMENT_DETAIL은 CLOB이라 DB가 막아 주지 않고, 서버도 통과시키므로 여기가 유일한 방어선이다
    const COMMENT_MAX_LENGTH = 3000;

    // MOD-04 신고. 접수는 팀원 API가 받고 우리는 폼 네 값만 보낸다
    const REPORT_SAVE_PATH = "/report/doSave";
    // DETAIL은 NVARCHAR2(2000)이라 그 너머는 DB가 자른다
    const REPORT_MAX_LENGTH = 2000;
    // 이 사유만 상세가 필수다(DB CK_REPORT_OTHER_DETAIL). 나머지 코드는 화면이 알 필요가 없다
    const REPORT_REASON_OTHER = "OTHER";

    // 켜짐/꺼짐을 색만이 아니라 아이콘 모양으로도 구분한다
    const ICON_THUMB_ON = "bi bi-hand-thumbs-up-fill";
    const ICON_THUMB_OFF = "bi bi-hand-thumbs-up";
    const ICON_FLAG_ON = "bi bi-flag-fill";
    const COMMENT_LIKE_PATH = "/commentLike/upToggleLike";

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

    // 화면 언어. layout.html이 <html lang>에 찍어 두므로 서버에서 따로 넘겨받지 않는다.
    // 이 파일이 그리는 이름은 서버가 그린 미리보기와 같은 규칙이라야 한 화면에서 표기가 갈리지 않는다(F-01)
    const IS_ENGLISH = document.documentElement.lang === "en";

    // ── 문구 ────────────────────────────────────────────
    // 이 파일은 .js라 Thymeleaf가 손대지 않는다. 번역된 문구는 detail.html의
    // 인라인 블록이 window.ENDIT_MSG로 넘겨 준다(F-01).
    // 아래 기본값은 그 블록이 없을 때를 위한 것이므로 지우지 않는다.
    const MSG = Object.assign({
        loginRequired: "로그인 후 이용할 수 있습니다.",
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
        writeTooLong: "코멘트는 3000자까지 쓸 수 있습니다.",
        reportReasonEmpty: "신고 사유를 골라 주세요.",
        reportDetailRequired: "기타를 고르면 상세 내용을 적어야 합니다.",
        reportSaving: "접수하는 중…",
        reportFailed: "신고를 접수하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        reportDone: "신고 접수됨",
        collectionLoadFailed: "컬렉션 목록을 불러오지 못했습니다.",
        collectionEmpty: "아직 만든 컬렉션이 없습니다. 컬렉션 화면에서 먼저 만들어 주세요.",
        collectionOn: "담김",
        collectionOff: "담기",
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
            label.textContent = score === NO_SCORE ? MSG.ratingLabel : formatScore(score);
        }

        function renderWatch() {
            watchButton.classList.toggle("is-on", watched);
            watchButton.setAttribute("aria-pressed", String(watched));
            watchButton.querySelector("i").className = watched ? "bi bi-check-lg" : "bi bi-plus-lg";
        }

        async function send(url, method, body) {
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
                    showNotice(MSG.loginRequired);
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
            star.addEventListener("mouseenter", () => paintStars(Number(star.dataset.score)));
        });

        box.querySelector(".stars").addEventListener("mouseleave", render);

        // ACT-C-003 보고싶어요 토글
        watchButton.addEventListener("click", async () => {
            if (!memberId) {
                showNotice(MSG.loginRequired);
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
                count.textContent = on ? MSG.collectionOn : MSG.collectionOff;
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
            row.appendChild(title);
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
                showNotice(MSG.loginRequired);
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
                    showNotice(MSG.loginRequired);
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
                    icon.className = on ? ICON_THUMB_ON : ICON_THUMB_OFF;
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
                showNotice(MSG.loginRequired);
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
                placeholder.textContent = MSG.profileEmpty;
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
    function initReport() {
        const modal = document.getElementById("reportModal");
        const grid = document.getElementById("commentGrid");

        // 비회원이거나 사유 코드를 못 받으면 서버가 버튼을 아예 안 그린다
        if (!modal || !grid) {
            return;
        }

        const memberId = modal.dataset.memberId;
        const detail = document.getElementById("reportDetail");
        const required = document.getElementById("reportRequired");
        const status = document.getElementById("reportStatus");
        const count = document.getElementById("reportCount");
        const submit = document.getElementById("reportSubmit");
        const reasons = Array.prototype.slice.call(
            modal.querySelectorAll("input[name='reportReason']"));

        // 어느 카드의 신고 버튼을 눌렀는지. 성공하면 그 버튼을 잠가야 해서 들고 있는다
        let target = null;

        function showStatus(message) {
            status.textContent = message;
            status.hidden = false;
        }

        function pickedReason() {
            const picked = reasons.find((radio) => radio.checked);

            return picked ? picked.value : "";
        }

        function renderCount() {
            count.textContent = detail.value.length + " / " + REPORT_MAX_LENGTH;
        }

        // 기타를 고를 때만 상세가 필수라고 알린다
        function renderRequired() {
            required.hidden = pickedReason() !== REPORT_REASON_OTHER;
        }

        reasons.forEach((radio) => radio.addEventListener("change", renderRequired));
        detail.addEventListener("input", renderCount);

        grid.querySelectorAll(".comment-report").forEach((button) => {
            button.addEventListener("click", () => {
                target = button;

                // 이전에 고른 사유·상세가 남으면 엉뚱한 신고가 나간다
                reasons.forEach((radio) => {
                    radio.checked = false;
                });
                detail.value = "";
                renderCount();
                renderRequired();
                status.hidden = true;
                submit.disabled = false;

                openModal(modal);
            });
        });

        // 되돌릴 수 없는 동작이라 확인을 한 번 거친다. 신고 모달은 뒤에 그대로 열려 있다
        const confirmModal = document.getElementById("reportConfirmModal");
        const confirmOk = document.getElementById("reportConfirmOk");
        const confirmCancel = document.getElementById("reportConfirmCancel");

        function closeConfirm() {
            confirmModal.hidden = true;
        }

        confirmCancel.addEventListener("click", closeConfirm);
        confirmModal.addEventListener("click", (event) => {
            if (event.target === confirmModal) {
                closeConfirm();
            }
        });
        // ESC는 확인 모달만 닫는다 - 신고 모달까지 닫히면 쓰던 내용이 날아간다
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !confirmModal.hidden) {
                event.stopPropagation();
                closeConfirm();
            }
        });

        submit.addEventListener("click", () => {
            const reason = pickedReason();

            if (!reason) {
                showStatus(MSG.reportReasonEmpty);
                return;
            }

            // 서버와 DB도 막지만 왕복을 아낀다
            if (reason === REPORT_REASON_OTHER && !detail.value.trim()) {
                showStatus(MSG.reportDetailRequired);
                detail.focus();
                return;
            }

            status.hidden = true;
            confirmModal.hidden = false;
            confirmOk.focus();
        });

        confirmOk.addEventListener("click", async () => {
            const reason = pickedReason();
            const text = detail.value.trim();

            closeConfirm();
            submit.disabled = true;
            showStatus(MSG.reportSaving);

            try {
                const body = await postForm(REPORT_SAVE_PATH, {
                    reportMemberId: memberId,
                    commentId: target.dataset.commentId,
                    reason: reason,
                    detail: text
                });

                if (MESSAGE_OK !== body.id) {
                    throw new Error(MSG.reportFailed);
                }

                // 같은 코멘트를 또 신고하지 못하게 막는 유일한 장치다.
                // 서버에 중복 검사가 없어 새로고침하면 되살아난다
                target.disabled = true;
                // 깃발을 채워 접수됐음을 모양으로 알린다. 라벨도 함께 바꾼다
                target.querySelector("i").className = ICON_FLAG_ON;
                target.setAttribute("aria-label", MSG.reportDone);
                target.setAttribute("title", MSG.reportDone);
                closeModal(modal);
            } catch (error) {
                showStatus(MSG.reportFailed);
                submit.disabled = false;
            }
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
        initCommentLike();
        initCommentWrite();
        initReport();
        initCast();
        initGallery();
        initGalleryModal();
    });
})();
