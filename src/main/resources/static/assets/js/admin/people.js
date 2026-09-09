(() => {
    "use strict";

    const ADMIN_PEOPLE_API = "/api/admin/people";
    const ADMIN_CREDITS_API = "/api/admin/credits";
    const PEOPLE_API = "/api/people/";
    // S-03과 같은 엔드포인트다. /api/search/movies는 D-04(컬렉션 작품 검색)가 쓰고 있어
    // 응답 계약이 다르므로 섞어 쓰지 않는다
    const MOVIE_SEARCH_API = "/api/search/contents";
    const ADMIN_PEOPLE_PATH = "/admin/people";

    const LIST_PAGE_SIZE = 20;
    // 크레딧은 페이징 없이 한 화면에서 정정한다. 인물 한 명의 크레딧 수보다 넉넉하게 잡는다
    const CREDIT_PAGE_SIZE = 100;
    const MOVIE_RESULT_SIZE = 10;

    // POL-033의 역할 4종
    const ROLES = [
        { value: "DIRECTOR", label: "감독" },
        { value: "ACTOR", label: "배우" },
        { value: "WRITER", label: "각본" },
        { value: "PRODUCER", label: "제작" }
    ];

    const $ = (selector) => document.querySelector(selector);

    // 서버가 MessageVO로 내려 주는 오류 문구를 꺼내 쓴다
    async function api(url, options = {}) {
        const response = await fetch(url, Object.assign({ credentials: "same-origin" }, options, {
            headers: Object.assign(
                options.body ? { "Content-Type": "application/json" } : {},
                options.headers || {})
        }));

        if (!response.ok) {
            let message = "요청을 처리하지 못했습니다.";

            try {
                const body = await response.json();
                // MessageVO의 필드명은 message / detailMessage 다
                message = body.message || body.detailMessage || message;
            } catch (error) {
                // 본문이 JSON이 아니면 기본 문구를 쓴다
            }

            const failure = new Error(message);
            failure.status = response.status;
            throw failure;
        }

        return response.status === 204 ? null : response.json();
    }

    // release_year가 YYYY-MM-DD 문자열이라 앞 4자리만 쓴다
    function year(releaseYear) {
        return releaseYear ? releaseYear.slice(0, 4) : "";
    }

    function renderPagination(container, pageNo, totalPages, move) {
        container.innerHTML = "";

        if (totalPages < 1) {
            return;
        }

        // 회원/신고 관리자 목록과 같은 10페이지 단위 페이징
        const blockSize = 10;
        const start = Math.floor((pageNo - 1) / blockSize) * blockSize + 1;
        const end = Math.min(totalPages, start + blockSize - 1);

        const list = document.createElement("ul");
        list.className = "pagination pagination-sm justify-content-center";

        const item = (label, target, active = false, disabled = false) => {
            const li = document.createElement("li");
            li.className = "page-item"
                + (active ? " active" : "")
                + (disabled ? " disabled" : "");

            const link = document.createElement("a");
            link.className = "page-link";
            link.href = "javascript:void(0)";
            link.textContent = label;

            if (!disabled) {
                link.addEventListener("click", () => move(target));
            }

            li.appendChild(link);
            list.appendChild(li);
        };

        item("이전", Math.max(1, start - 1), false, start === 1);

        for (let i = start; i <= end; i++) {
            item(String(i), i, i === pageNo);
        }

        item("다음", Math.min(totalPages, end + 1), false, end === totalPages);

        container.appendChild(list);
    }

    // ── AD-05 인물 관리 목록 ──────────────────────────
    function initList() {
        const body = $("#adminPersonBody");
        const emptyState = $("#adminEmptyState");
        const errorState = $("#adminErrorState");
        const pagination = $("#adminPagination");
        const queryInput = $("#adminQuery");
        const searchDiv = $("#adminSearchDiv");

        function toCell(text) {
            const cell = document.createElement("td");
            cell.textContent = text == null ? "" : text;
            return cell;
        }

        function toPhotoCell(person) {
            const cell = document.createElement("td");

            if (person.profileImageUrl) {
                const image = document.createElement("img");
                image.className = "admin-person-thumb";
                image.src = person.profileImageUrl;
                image.alt = person.nameKo || person.nameOrg || "";
                image.loading = "lazy";
                cell.appendChild(image);
            } else {
                const empty = document.createElement("span");
                empty.className = "admin-person-thumb-empty";
                empty.textContent = "없음";
                cell.appendChild(empty);
            }

            return cell;
        }

        function toRow(person) {
            const row = document.createElement("tr");
            row.addEventListener("click", () => {
                location.href = ADMIN_PEOPLE_PATH + "/" + person.personId;
            });

            const nameKo = toCell(person.nameKo);
            nameKo.className = "cell-name";

            const nameOrg = toCell(person.nameOrg);
            nameOrg.className = "cell-name";

            row.append(
                toCell(person.personId),
                toPhotoCell(person),
                nameKo,
                nameOrg,
                toCell(person.externalId),
                toCell(person.createdDt),
                toCell(person.updatedDt));

            return row;
        }

        async function load(pageNo) {
            body.innerHTML = "";
            emptyState.hidden = true;
            errorState.hidden = true;
            pagination.innerHTML = "";

            const params = new URLSearchParams({
                searchWord: queryInput.value.trim(),
                searchDiv: searchDiv.value,
                page: pageNo,
                size: LIST_PAGE_SIZE
            });

            try {
                const result = await api(ADMIN_PEOPLE_API + "?" + params.toString());
                const items = Array.isArray(result.items) ? result.items : [];

                if (items.length === 0) {
                    emptyState.hidden = false;
                    return;
                }

                items.forEach((person) => body.appendChild(toRow(person)));

                const totalCnt = result.page ? result.page.totalCnt : 0;
                renderPagination(pagination, pageNo, Math.ceil(totalCnt / LIST_PAGE_SIZE), load);
            } catch (error) {
                errorState.textContent = error.message;
                errorState.hidden = false;
            }
        }

        $("#adminSearchButton").addEventListener("click", () => load(1));

        queryInput.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                load(1);
            }
        });

        $("#createPersonButton").addEventListener("click", () => {
            location.href = ADMIN_PEOPLE_PATH + "/new";
        });

        load(1);
    }

    // ── AD-06 인물 등록·수정 ──────────────────────────
    function initForm() {
        const isEdit = document.body.dataset.edit === "true";
        const personId = Number(document.body.dataset.personId) || 0;

        const form = $("#personForm");
        const errorBox = $("#formError");
        const saveButton = $("#saveButton");
        const nameKo = $("#nameKo");
        const nameOrg = $("#nameOrg");
        const externalId = $("#externalId");

        /*
         * ACT-AD-006 - 원문명·외부ID는 DB가 NOT NULL이라 필수다. 서버도 같은 규칙을 막는다.
         * 예전에는 둘이 빌 때 저장 버튼을 잠갔는데, 눌러도 아무 일이 없어 무엇이 모자란지 알 수 없었다.
         * 지금은 항상 누를 수 있게 두고 눌렀을 때 빈 항목을 이름으로 알려 준다.
         * 조사가 달라 라벨마다 같이 들고 다닌다("원문명을" / "외부 ID를").
         */
        const REQUIRED_FIELDS = [
            { input: nameOrg, label: "원문명", particle: "을" },
            { input: externalId, label: "외부 ID", particle: "를" }
        ];

        // TMDB 인물 ID는 숫자다. 주소나 이름을 통째로 붙여 넣는 실수를 저장 전에 잡는다
        const EXTERNAL_ID_PATTERN = /^[0-9]+$/;

        $("#cancelButton").addEventListener("click", () => {
            location.href = ADMIN_PEOPLE_PATH;
        });

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            errorBox.hidden = true;

            const missing = REQUIRED_FIELDS.filter((field) => !field.input.value.trim());

            if (missing.length > 0) {
                // 하나씩 알리면 저장을 두 번 눌러야 둘 다 안다. 한 문장으로 묶는다
                const names = missing.map((field) => field.label).join("과 ");
                const particle = missing[missing.length - 1].particle;

                errorBox.textContent = names + particle + " 채워 주세요.";
                errorBox.hidden = false;
                missing[0].input.focus();
                return;
            }

            if (!EXTERNAL_ID_PATTERN.test(externalId.value.trim())) {
                // 직접 정해 넣는 경우까지 막지 않도록, 숫자이기만 하면 통과시킨다
                errorBox.textContent =
                    "외부 ID는 숫자만 넣습니다. TMDB에 없는 인물이면 겹치지 않는 숫자를 직접 정해 주세요.";
                errorBox.hidden = false;
                externalId.focus();
                return;
            }

            const payload = {
                nameKo: nameKo.value.trim(),
                nameOrg: nameOrg.value.trim(),
                externalId: externalId.value.trim(),
                profileImageUrl: $("#profileImageUrl").value.trim()
            };

            saveButton.disabled = true;
            saveButton.textContent = "저장 중...";

            try {
                if (isEdit) {
                    await api(ADMIN_PEOPLE_API + "/" + personId, {
                        method: "PATCH",
                        body: JSON.stringify(payload)
                    });
                } else {
                    await api(ADMIN_PEOPLE_API, {
                        method: "POST",
                        body: JSON.stringify(payload)
                    });
                }

                // ACT-AD-006 성공 시 목록으로 돌아간다
                location.href = ADMIN_PEOPLE_PATH;
            } catch (error) {
                errorBox.textContent = error.message;
                errorBox.hidden = false;
                saveButton.textContent = "저장";
                saveButton.disabled = false;
            }
        });

        if (isEdit) {
            initCredits(personId);
        }
    }

    // ── AD-06 크레딧 정정 ─────────────────────────────
    function initCredits(personId) {
        const body = $("#creditBody");
        const emptyState = $("#creditEmptyState");
        const errorState = $("#creditErrorState");
        const errorBox = $("#creditError");
        const movieResults = $("#creditMovieResults");
        const movieQuery = $("#creditMovieQuery");
        const addButton = $("#creditAddButton");

        let selectedContentId = 0;

        function showError(message) {
            errorBox.textContent = message;
            errorBox.hidden = false;
        }

        function toRoleSelect(role) {
            const select = document.createElement("select");

            ROLES.forEach((item) => {
                const option = document.createElement("option");
                option.value = item.value;
                option.textContent = item.label;
                option.selected = item.value === role;
                select.appendChild(option);
            });

            return select;
        }

        function toInput(type, value) {
            const input = document.createElement("input");
            input.type = type;
            input.value = value;

            if (type === "number") {
                input.min = "0";
            } else {
                input.maxLength = 200;
            }

            return input;
        }

        function toCell(child) {
            const cell = document.createElement("td");

            if (typeof child === "string") {
                cell.textContent = child;
            } else {
                cell.appendChild(child);
            }

            return cell;
        }

        function toRow(credit) {
            const row = document.createElement("tr");

            const roleSelect = toRoleSelect(credit.role);
            const characterInput = toInput("text", credit.character || "");
            const orderInput = toInput("number", credit.displayOrder);

            const actionCell = document.createElement("td");
            actionCell.className = "cell-actions";

            const saveButton = document.createElement("button");
            saveButton.type = "button";
            saveButton.className = "outline-button";
            saveButton.textContent = "저장";

            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "danger-button";
            deleteButton.textContent = "삭제";

            saveButton.addEventListener("click", async () => {
                errorBox.hidden = true;
                saveButton.disabled = true;

                try {
                    await api(ADMIN_CREDITS_API + "/" + credit.creditId, {
                        method: "PATCH",
                        body: JSON.stringify({
                            role: roleSelect.value,
                            // POL-033 - 배역은 ACTOR만 쓴다
                            character: roleSelect.value === "ACTOR" ? characterInput.value.trim() : null,
                            displayOrder: Number(orderInput.value) || 0
                        })
                    });

                    await load();
                } catch (error) {
                    showError(error.message);
                    saveButton.disabled = false;
                }
            });

            // 브라우저 확인창 대신 두 번 누르게 한다 - 되돌릴 수 없는 동작이라 한 번 더 묻는다
            let armed = false;
            let armedTimer = null;

            deleteButton.addEventListener("click", async () => {
                errorBox.hidden = true;

                if (!armed) {
                    armed = true;
                    deleteButton.textContent = "정말 삭제";
                    armedTimer = setTimeout(() => {
                        armed = false;
                        deleteButton.textContent = "삭제";
                    }, 4000);
                    return;
                }

                clearTimeout(armedTimer);
                deleteButton.disabled = true;

                try {
                    await api(ADMIN_CREDITS_API + "/" + credit.creditId, { method: "DELETE" });
                    await load();
                } catch (error) {
                    showError(error.message);
                    deleteButton.disabled = false;
                    deleteButton.textContent = "삭제";
                    armed = false;
                }
            });

            actionCell.append(saveButton, deleteButton);

            const titleCell = toCell(credit.titleKo);
            titleCell.className = "cell-title";

            row.append(
                toCell(year(credit.releaseYear)),
                titleCell,
                toCell(roleSelect),
                toCell(characterInput),
                toCell(orderInput),
                actionCell);

            return row;
        }

        async function load() {
            body.innerHTML = "";
            emptyState.hidden = true;
            errorState.hidden = true;

            try {
                const result = await api(
                    PEOPLE_API + personId + "/filmography?page=1&size=" + CREDIT_PAGE_SIZE);
                const items = Array.isArray(result.items) ? result.items : [];

                if (items.length === 0) {
                    emptyState.hidden = false;
                    return;
                }

                items.forEach((credit) => body.appendChild(toRow(credit)));
            } catch (error) {
                errorState.textContent = error.message;
                errorState.hidden = false;
            }
        }

        // 작품 선택 - 이미 등록된 콘텐츠만 고른다(AD-03의 "기존 인물 선택만"과 대칭)
        async function searchMovies() {
            movieResults.innerHTML = "";
            selectedContentId = 0;
            addButton.disabled = true;
            errorBox.hidden = true;

            const params = new URLSearchParams({
                query: movieQuery.value.trim(),
                page: 1,
                size: MOVIE_RESULT_SIZE
            });

            try {
                const result = await api(MOVIE_SEARCH_API + "?" + params.toString());
                // 이 엔드포인트만 PageResponse라 items가 아니라 list다
                const items = Array.isArray(result.list) ? result.list : [];

                if (items.length === 0) {
                    showError("검색된 작품이 없습니다.");
                    return;
                }

                items.forEach((movie) => {
                    const item = document.createElement("li");

                    const button = document.createElement("button");
                    button.type = "button";
                    button.className = "credit-add-result";

                    const title = document.createElement("span");
                    title.textContent = movie.titleKo;

                    const releaseYear = document.createElement("span");
                    releaseYear.className = "year";
                    releaseYear.textContent = year(movie.releaseYear);

                    button.append(title, releaseYear);
                    button.addEventListener("click", () => {
                        selectedContentId = movie.contentId;
                        addButton.disabled = false;

                        movieResults.querySelectorAll(".credit-add-result")
                            .forEach((el) => el.classList.remove("is-selected"));
                        button.classList.add("is-selected");
                    });

                    item.appendChild(button);
                    movieResults.appendChild(item);
                });
            } catch (error) {
                showError(error.message);
            }
        }

        $("#creditMovieSearchButton").addEventListener("click", searchMovies);

        movieQuery.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                searchMovies();
            }
        });

        addButton.addEventListener("click", async () => {
            errorBox.hidden = true;

            if (selectedContentId <= 0) {
                showError("연결할 작품을 먼저 선택해 주세요.");
                return;
            }

            const role = $("#creditRole").value;
            const character = $("#creditCharacter").value.trim();

            addButton.disabled = true;

            try {
                await api(ADMIN_CREDITS_API, {
                    method: "POST",
                    body: JSON.stringify({
                        contentId: selectedContentId,
                        personId: personId,
                        role: role,
                        character: role === "ACTOR" ? character : null,
                        displayOrder: Number($("#creditDisplayOrder").value) || 0
                    })
                });

                movieResults.innerHTML = "";
                movieQuery.value = "";
                $("#creditCharacter").value = "";
                selectedContentId = 0;

                await load();
            } catch (error) {
                showError(error.message);
                addButton.disabled = false;
            }
        });

        load();
    }

    switch (document.body.dataset.page) {
        case "admin-person-list":
            initList();
            break;
        case "admin-person-form":
            initForm();
            break;
        default:
            break;
    }
})();
