(() => {
    "use strict";

    const ADMIN_MOVIES_API = "/api/admin/movies";
    // 크레딧에 붙일 인물은 AD-05의 목록 API를 그대로 재사용한다.
    // "기존 인물 선택만" 합의에 따라 이 화면에서 인물을 새로 만들지 않는다
    const ADMIN_PEOPLE_API = "/api/admin/people";
    const ADMIN_MOVIES_PATH = "/admin/movies";

    const LIST_PAGE_SIZE = 20;
    const PERSON_RESULT_SIZE = 10;

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
                message = body.message || body.detail || message;
            } catch (error) {
                // 본문이 JSON이 아니면 기본 문구를 쓴다
            }

            const failure = new Error(message);
            failure.status = response.status;
            throw failure;
        }

        return response.status === 204 ? null : response.json();
    }

    function roleLabel(role) {
        const found = ROLES.find((item) => item.value === role);
        return found ? found.label : role;
    }

    function renderPagination(container, pageNo, totalPages, move) {
        container.innerHTML = "";

        if (totalPages <= 1) {
            return;
        }

        const blockSize = 5;
        const start = Math.floor((pageNo - 1) / blockSize) * blockSize + 1;
        const end = Math.min(totalPages, start + blockSize - 1);

        const button = (label, target, active, disabled) => {
            const el = document.createElement("button");
            el.type = "button";
            el.className = "page-button" + (active ? " active" : "");
            el.textContent = label;
            el.disabled = disabled;

            if (!disabled) {
                el.addEventListener("click", () => move(target));
            }

            container.appendChild(el);
        };

        button("<", Math.max(1, start - 1), false, start === 1);

        for (let i = start; i <= end; i++) {
            button(String(i), i, i === pageNo, false);
        }

        button(">", Math.min(totalPages, end + 1), false, end === totalPages);
    }

    // ── AD-02 영화 관리 목록 ──────────────────────────
    function initList() {
        const body = $("#adminMovieBody");
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

        function toPosterCell(movie) {
            const cell = document.createElement("td");

            if (movie.posterUrl) {
                const image = document.createElement("img");
                image.className = "admin-movie-thumb";
                image.src = movie.posterUrl;
                image.alt = movie.titleKo || "";
                image.loading = "lazy";
                cell.appendChild(image);
            } else {
                const empty = document.createElement("span");
                empty.className = "admin-movie-thumb-empty";
                empty.textContent = "없음";
                cell.appendChild(empty);
            }

            return cell;
        }

        // 행 전체 클릭은 마우스용이다. 키보드로도 상세에 닿아야 하므로 제목을 링크로 만든다
        function toTitleCell(movie) {
            const cell = document.createElement("td");
            cell.className = "cell-title";

            const link = document.createElement("a");
            link.className = "row-link";
            link.href = ADMIN_MOVIES_PATH + "/" + movie.contentId;
            link.textContent = movie.titleKo == null ? "" : movie.titleKo;

            cell.appendChild(link);

            return cell;
        }

        function toRow(movie) {
            const row = document.createElement("tr");
            // 행 클릭 → AD-04 읽기 전용 상세. 제목 링크와 목적지가 같아 겹쳐도 문제없다
            row.addEventListener("click", () => {
                location.href = ADMIN_MOVIES_PATH + "/" + movie.contentId;
            });

            const titleKo = toTitleCell(movie);

            const titleOrg = toCell(movie.titleOrg);
            titleOrg.className = "cell-title";

            row.append(
                toCell(movie.contentId),
                toPosterCell(movie),
                titleKo,
                titleOrg,
                toCell(movie.externalId),
                toCell(movie.releaseYear),
                toCell(movie.country),
                toCell(movie.createdDt));

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
                const result = await api(ADMIN_MOVIES_API + "?" + params.toString());
                const items = Array.isArray(result.items) ? result.items : [];
                const totalCnt = result.page ? result.page.totalCnt : 0;
                const totalPages = Math.ceil(totalCnt / LIST_PAGE_SIZE);

                if (items.length === 0) {
                    emptyState.hidden = false;
                    // 마지막 페이지의 행이 사라진 뒤에도 앞 페이지로 돌아갈 수단은 남겨 둔다.
                    // 페이저까지 지우면 주소를 고치거나 새로고침하는 것 말고 빠져나갈 길이 없다
                    renderPagination(pagination, pageNo, totalPages, load);
                    return;
                }

                items.forEach((movie) => body.appendChild(toRow(movie)));

                renderPagination(pagination, pageNo, totalPages, load);
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

        $("#createMovieButton").addEventListener("click", () => {
            location.href = ADMIN_MOVIES_PATH + "/new";
        });

        load(1);
    }

    // ── AD-03 영화 등록 ───────────────────────────────
    function initForm() {
        const form = $("#movieForm");
        const errorBox = $("#formError");
        const saveButton = $("#saveButton");
        const externalId = $("#externalId");
        const titleKo = $("#titleKo");
        const checkResult = $("#checkResult");

        // 담은 크레딧은 저장 전까지 화면 상태다. 단일 POST로 함께 나간다
        const stagedCredits = [];
        let selectedPerson = null;

        function showError(message) {
            errorBox.textContent = message;
            errorBox.hidden = false;
        }

        // 제목과 외부 ID를 채워야 등록이 열린다. 서버도 같은 값을 필수로 본다
        function syncSaveButton() {
            saveButton.disabled = !titleKo.value.trim() || !externalId.value.trim();
        }

        /*
         * Enter로 인한 암묵적 제출을 막는다.
         *
         * 저장 버튼이 type="submit"이라, 제목과 외부 ID만 채워지면 배역·표시순서·이미지 칸에서
         * Enter를 누르는 것만으로 절반만 채워진 폼이 POST된다 - 입력 중이던 크레딧은 담기지도 않은 채다.
         * 등록은 반드시 등록 버튼으로만 이뤄져야 한다. textarea(줄거리)의 줄바꿈은 그대로 둔다.
         */
        form.addEventListener("keydown", (event) => {
            if (event.key === "Enter" && event.target.tagName === "INPUT") {
                event.preventDefault();
            }
        });

        titleKo.addEventListener("input", syncSaveButton);
        externalId.addEventListener("input", () => {
            // 값이 바뀌면 직전 중복 확인 결과는 더 이상 유효하지 않다
            checkResult.hidden = true;
            syncSaveButton();
        });
        syncSaveButton();

        // ── 외부 ID 안내 모달 ──
        // <dialog>이라 Esc와 닫기 버튼(form method="dialog")은 브라우저가 처리한다
        const helpDialog = $("#externalIdHelpDialog");

        $("#externalIdHelp").addEventListener("click", () => helpDialog.showModal());

        // 바깥(백드롭)을 눌러도 닫는다 - 클릭 대상이 dialog 자신이면 내용 밖이다
        helpDialog.addEventListener("click", (event) => {
            if (event.target === helpDialog) {
                helpDialog.close();
            }
        });

        // ── 외부 ID 중복 확인 (ACT-AD-004) ──
        $("#checkButton").addEventListener("click", async () => {
            const value = externalId.value.trim();
            checkResult.hidden = true;

            if (!value) {
                showError("외부 ID를 먼저 입력해 주세요.");
                externalId.focus();
                return;
            }

            errorBox.hidden = true;

            try {
                const result = await api(
                    ADMIN_MOVIES_API + "/check?externalId=" + encodeURIComponent(value));

                checkResult.textContent = result.duplicated
                    ? "이미 등록된 외부 ID입니다."
                    : "사용할 수 있는 외부 ID입니다.";
                checkResult.className = "check-result " + (result.duplicated ? "ng" : "ok");
                checkResult.hidden = false;
            } catch (error) {
                showError(error.message);
            }
        });

        // ── 갤러리 이미지 줄 ──
        const imageRows = $("#imageRows");

        function addImageRow() {
            const row = document.createElement("div");
            row.className = "image-row";

            const input = document.createElement("input");
            input.type = "text";
            input.maxLength = 500;
            input.placeholder = "이미지 주소 또는 TMDB 경로";

            const removeButton = document.createElement("button");
            removeButton.type = "button";
            removeButton.className = "outline-button";
            removeButton.textContent = "삭제";
            removeButton.addEventListener("click", () => row.remove());

            row.append(input, removeButton);
            imageRows.appendChild(row);
        }

        $("#addImageButton").addEventListener("click", addImageRow);
        addImageRow();

        // ── 인물 검색 (AD-05 API 재사용) ──
        const personResults = $("#personResults");
        const personQuery = $("#personQuery");
        const creditAddButton = $("#creditAddButton");

        async function searchPeople() {
            personResults.innerHTML = "";
            selectedPerson = null;
            creditAddButton.disabled = true;
            errorBox.hidden = true;

            const params = new URLSearchParams({
                searchWord: personQuery.value.trim(),
                searchDiv: $("#personSearchDiv").value,
                page: 1,
                size: PERSON_RESULT_SIZE
            });

            try {
                const result = await api(ADMIN_PEOPLE_API + "?" + params.toString());
                const items = Array.isArray(result.items) ? result.items : [];

                if (items.length === 0) {
                    showError("검색된 인물이 없습니다. 인물 관리에서 먼저 등록해 주세요.");
                    return;
                }

                items.forEach((person) => {
                    const item = document.createElement("li");

                    const button = document.createElement("button");
                    button.type = "button";
                    button.className = "person-result";

                    const name = document.createElement("span");
                    name.textContent = person.nameKo || person.nameOrg || "이름 없음";

                    const sub = document.createElement("span");
                    sub.className = "sub";
                    sub.textContent = person.nameOrg || "";

                    button.append(name, sub);
                    button.addEventListener("click", () => {
                        selectedPerson = person;
                        creditAddButton.disabled = false;

                        personResults.querySelectorAll(".person-result")
                            .forEach((el) => el.classList.remove("is-selected"));
                        button.classList.add("is-selected");
                    });

                    item.appendChild(button);
                    personResults.appendChild(item);
                });
            } catch (error) {
                showError(error.message);
            }
        }

        $("#personSearchButton").addEventListener("click", searchPeople);

        personQuery.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                searchPeople();
            }
        });

        // ── 담은 크레딧 ──
        const stageBody = $("#creditStageBody");
        const stageEmpty = $("#creditStageEmpty");

        function renderStage() {
            stageBody.innerHTML = "";
            stageEmpty.hidden = stagedCredits.length > 0;

            stagedCredits.forEach((credit, index) => {
                const row = document.createElement("tr");

                const nameCell = document.createElement("td");
                nameCell.className = "cell-title";
                nameCell.textContent = credit.personName;

                const roleCell = document.createElement("td");
                roleCell.textContent = roleLabel(credit.role);

                const characterCell = document.createElement("td");
                characterCell.textContent = credit.character || "—";

                const orderCell = document.createElement("td");
                orderCell.textContent = credit.displayOrder;

                const actionCell = document.createElement("td");
                const removeButton = document.createElement("button");
                removeButton.type = "button";
                removeButton.className = "danger-button";
                removeButton.textContent = "빼기";
                // 아직 저장 전이라 되돌릴 것이 없다. 확인 없이 바로 뺀다
                removeButton.addEventListener("click", () => {
                    stagedCredits.splice(index, 1);
                    renderStage();
                });
                actionCell.appendChild(removeButton);

                row.append(nameCell, roleCell, characterCell, orderCell, actionCell);
                stageBody.appendChild(row);
            });
        }

        creditAddButton.addEventListener("click", () => {
            errorBox.hidden = true;

            if (!selectedPerson) {
                showError("연결할 인물을 먼저 선택해 주세요.");
                return;
            }

            const role = $("#creditRole").value;
            const character = $("#creditCharacter").value.trim();

            const duplicated = stagedCredits.some(
                (credit) => credit.personId === selectedPerson.personId && credit.role === role);

            if (duplicated) {
                showError("같은 인물의 같은 역할을 이미 담았습니다.");
                return;
            }

            stagedCredits.push({
                personId: selectedPerson.personId,
                personName: selectedPerson.nameKo || selectedPerson.nameOrg || "이름 없음",
                role: role,
                // POL-033 - 배역은 ACTOR만 쓴다
                character: role === "ACTOR" ? character : null,
                displayOrder: Number($("#creditDisplayOrder").value) || 0
            });

            renderStage();

            personResults.innerHTML = "";
            personQuery.value = "";
            $("#creditCharacter").value = "";
            selectedPerson = null;
            creditAddButton.disabled = true;
        });

        renderStage();

        $("#cancelButton").addEventListener("click", () => {
            location.href = ADMIN_MOVIES_PATH;
        });

        // ── 저장 (API-052 단일 POST) ──
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            errorBox.hidden = true;

            if (!titleKo.value.trim()) {
                showError("제목을 입력해 주세요.");
                titleKo.focus();
                return;
            }

            if (!externalId.value.trim()) {
                showError("외부 ID를 입력해 주세요.");
                externalId.focus();
                return;
            }

            const genreIds = Array.from(
                document.querySelectorAll("input[name='genreId']:checked"),
                (checkbox) => Number(checkbox.value));

            const imageUrls = Array.from(imageRows.querySelectorAll("input"))
                .map((input) => input.value.trim())
                .filter((value) => value.length > 0);

            const payload = {
                content: {
                    externalId: externalId.value.trim(),
                    titleKo: titleKo.value.trim(),
                    titleOrg: $("#titleOrg").value.trim(),
                    overview: $("#overview").value.trim(),
                    releaseYear: $("#releaseYear").value,
                    runtimeMin: Number($("#runtimeMin").value) || 0,
                    country: $("#country").value.trim(),
                    posterUrl: $("#posterUrl").value.trim(),
                    backdropUrl: $("#backdropUrl").value.trim()
                },
                genreIds: genreIds,
                imageUrls: imageUrls,
                // 화면 표시용 personName은 서버 계약에 없으므로 빼고 보낸다
                credits: stagedCredits.map((credit) => ({
                    personId: credit.personId,
                    role: credit.role,
                    character: credit.character,
                    displayOrder: credit.displayOrder
                }))
            };

            saveButton.disabled = true;
            saveButton.textContent = "등록 중...";

            try {
                const saved = await api(ADMIN_MOVIES_API, {
                    method: "POST",
                    body: JSON.stringify(payload)
                });

                // 등록에 성공하면 방금 만든 영화의 상세(AD-04)로 보낸다
                location.href = ADMIN_MOVIES_PATH + "/" + saved.contentId;
            } catch (error) {
                showError(error.message);
                saveButton.textContent = "등록";
                syncSaveButton();
            }
        });
    }

    switch (document.body.dataset.page) {
        case "admin-content-list":
            initList();
            break;
        case "admin-content-form":
            initForm();
            break;
        default:
            break;
    }
})();
