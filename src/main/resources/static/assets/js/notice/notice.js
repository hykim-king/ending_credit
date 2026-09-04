(() => {
    "use strict";

    const pageType = document.body.dataset.page;
    const $ = (selector) => document.querySelector(selector);

    async function api(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            ...options,
            headers: {
                ...(options.body
                    ? { "Content-Type": "application/json" }
                    : {}),
                ...(options.headers || {})
            }
        });

        if (!response.ok) {
            let message =
                `요청에 실패했습니다. (${response.status})`;

            try {
                const body = await response.json();

                message =
                    body.detail
                    || body.message
                    || body.error
                    || message;

            } catch (_) {
                // JSON이 아닌 오류 응답은 기본 메시지 사용
            }

            const error = new Error(message);
            error.status = response.status;

            throw error;
        }

        if (response.status === 204) {
            return null;
        }

        return response.json();
    }

    function formatDate(value) {
        if (!value) return "-";

        return value
            .substring(0, 10)
            .replaceAll("-", ".");
    }

    function currentUrl() {
        return location.pathname
            + location.search;
    }

    function returnUrl(defaultUrl) {
        return new URLSearchParams(
            location.search
        ).get("returnUrl") || defaultUrl;
    }

    function setParams(values) {
        const params =
            new URLSearchParams(
                location.search
            );

        Object.entries(values)
            .forEach(([key, value]) => {

                if (
                    value === null
                    || value === undefined
                    || value === ""
                ) {
                    params.delete(key);

                } else {
                    params.set(
                        key,
                        String(value)
                    );
                }
            });

        const query = params.toString();

        history.replaceState(
            null,
            "",
            location.pathname
            + (query ? `?${query}` : "")
        );
    }

    function renderPagination(
        container,
        pageNo,
        totalPages,
        move
    ) {

        container.innerHTML = "";

        if (totalPages <= 1) {
            return;
        }

        const blockSize = 5;

        const start =
            Math.floor(
                (pageNo - 1) / blockSize
            ) * blockSize + 1;

        const end =
            Math.min(
                totalPages,
                start + blockSize - 1
            );

        const button = (
            label,
            target,
            active = false,
            disabled = false
        ) => {

            const el =
                document.createElement(
                    "button"
                );

            el.type = "button";

            el.className =
                `page-button${active ? " active" : ""}`;

            el.textContent = label;
            el.disabled = disabled;

            if (!disabled) {
                el.addEventListener(
                    "click",
                    () => move(target)
                );
            }

            container.appendChild(el);
        };

        button(
            "‹",
            Math.max(1, start - 1),
            false,
            start === 1
        );

        for (
            let i = start;
            i <= end;
            i++
        ) {
            button(
                String(i),
                i,
                i === pageNo
            );
        }

        button(
            "›",
            Math.min(
                totalPages,
                end + 1
            ),
            false,
            end === totalPages
        );
    }

    async function loadPublicList() {

        const params =
            new URLSearchParams(
                location.search
            );

        const page =
            Math.max(
                1,
                Number(
                    params.get("page")
                    || 1
                )
            );

        const size =
            Math.max(
                1,
                Number(
                    params.get("size")
                    || 10
                )
            );

        const importantArea =
            $("#importantArea");

        const listArea =
            $("#noticeList");

        const emptyState =
            $("#emptyState");

        const errorState =
            $("#errorState");

        importantArea.innerHTML = "";
        listArea.innerHTML = "";

        emptyState.hidden = true;
        errorState.hidden = true;

        try {
            const data = await api(
                `/api/notices?page=${page}&size=${size}`
            );

            if (!data.list.length) {
                emptyState.hidden = false;
            }

            data.list.forEach(
                (notice) => {

                    const goDetail = () => {

                        const back =
                            encodeURIComponent(
                                currentUrl()
                            );

                        location.href =
                            `/notices/${notice.noticeId}?returnUrl=${back}`;
                    };

                    if (
                        notice.important === "Y"
                    ) {

                        const row =
                            document.createElement(
                                "div"
                            );

                        row.className =
                            "important-notice";

                        row.tabIndex = 0;

                        row.innerHTML = `
                            <span class="important-label">중요</span>
                            <span class="notice-title-cell"></span>
                            <span class="notice-date">${formatDate(notice.createdDt)}</span>
                            <span class="notice-view">${notice.viewCount ?? 0}</span>
                        `;

                        row.querySelector(
                            ".notice-title-cell"
                        ).textContent =
                            notice.title;

                        row.addEventListener(
                            "click",
                            goDetail
                        );

                        row.addEventListener(
                            "keydown",
                            (e) => {
                                if (
                                    e.key === "Enter"
                                    || e.key === " "
                                ) {
                                    goDetail();
                                }
                            }
                        );

                        importantArea
                            .appendChild(row);

                        return;
                    }

                    const row =
                        document.createElement(
                            "div"
                        );

                    row.className =
                        "notice-row";

                    row.tabIndex = 0;

                    row.innerHTML = `
                        <span>${notice.displayNo ?? "-"}</span>
                        <span class="notice-title-cell"></span>
                        <span>${formatDate(notice.createdDt)}</span>
                        <span class="notice-view">${notice.viewCount ?? 0}</span>
                    `;

                    row.querySelector(
                        ".notice-title-cell"
                    ).textContent =
                        notice.title;

                    row.addEventListener(
                        "click",
                        goDetail
                    );

                    row.addEventListener(
                        "keydown",
                        (e) => {

                            if (
                                e.key === "Enter"
                                || e.key === " "
                            ) {
                                goDetail();
                            }
                        }
                    );

                    listArea
                        .appendChild(row);
                }
            );

            renderPagination(
                $("#pagination"),
                data.pageNo,
                data.totalPages,
                (target) => {

                    setParams({
                        page: target,
                        size
                    });

                    loadPublicList();

                    window.scrollTo({
                        top: 0,
                        behavior: "smooth"
                    });
                }
            );

        } catch (error) {
            errorState.hidden = false;
        }
    }

    async function loadPublicDetail() {

        const noticeId =
            document.body.dataset.noticeId;

        $("#backButton")
            .addEventListener(
                "click",
                () => {

                    location.href =
                        returnUrl(
                            "/notices"
                        );
                }
            );

        $("#publicDeleteButton")
            ?.addEventListener(
                "click",
                async () => {
                    const confirmed =
                        window.confirm(
                            "이 공지사항을 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다."
                        );

                    if (!confirmed) {
                        return;
                    }

                    const deleteButton =
                        $("#publicDeleteButton");

                    deleteButton.disabled = true;
                    deleteButton.textContent = "삭제 중...";

                    try {
                        await api(
                            `/api/admin/notices/${noticeId}`,
                            { method: "DELETE" }
                        );

                        location.href = "/notices";

                    } catch (error) {
                        window.alert(error.message);
                        deleteButton.disabled = false;
                        deleteButton.textContent = "공지 삭제";
                    }
                }
            );

        try {
            const notice =
                await api(
                    `/api/notices/${noticeId}`
                );

            $("#detailLoading").hidden =
                true;

            $("#detailContent").hidden =
                false;

            $("#detailTitle").textContent =
                notice.title;

            $("#detailCreatedDt").textContent =
                formatDate(
                    notice.createdDt
                );

            $("#detailViewCount").textContent =
                notice.viewCount ?? 0;

            $("#detailBody").textContent =
                notice.content || "";

            if (
                notice.important === "Y"
            ) {
                $("#detailImportant").hidden =
                    false;
            }

            document.title =
                `${notice.title} | END IT`;

        } catch (error) {

            $("#detailLoading").hidden =
                true;

            $("#detailError").hidden =
                false;
        }
    }

    async function initAdminList() {

        const queryInput =
            $("#adminQuery");

        const importantSelect =
            $("#adminImportant");

        const initial =
            new URLSearchParams(
                location.search
            );

        queryInput.value =
            initial.get("query") || "";

        importantSelect.value =
            initial.get("important") || "";

        const load = async () => {

            const params =
                new URLSearchParams(
                    location.search
                );

            const query =
                params.get("query") || "";

            const important =
                params.get("important") || "";

            const page =
                Math.max(
                    1,
                    Number(
                        params.get("page")
                        || 1
                    )
                );

            const size =
                Math.max(
                    1,
                    Number(
                        params.get("size")
                        || 10
                    )
                );

            const apiParams =
                new URLSearchParams({
                    page: String(page),
                    size: String(size)
                });

            if (query) {
                apiParams.set(
                    "query",
                    query
                );
            }

            if (important) {
                apiParams.set(
                    "important",
                    important
                );
            }

            const body =
                $("#adminNoticeBody");

            const emptyState =
                $("#adminEmptyState");

            const errorState =
                $("#adminErrorState");

            body.innerHTML = "";

            emptyState.hidden = true;
            errorState.hidden = true;

            try {
                const data =
                    await api(
                        `/api/admin/notices?${apiParams}`
                    );

                if (!data.list.length) {
                    emptyState.hidden =
                        false;
                }

                data.list.forEach(
                    (notice) => {

                        const tr =
                            document.createElement(
                                "tr"
                            );

                        tr.tabIndex = 0;

                        tr.innerHTML = `
                            <td>${notice.displayNo ?? "-"}</td>
                            <td class="notice-admin-title"></td>
                            <td>${notice.important}</td>
                            <td>${notice.viewCount ?? 0}</td>
                            <td class="notice-admin-writer"></td>
                            <td>${formatDate(notice.createdDt)}</td>
                            <td>${formatDate(notice.updatedDt)}</td>
                        `;

                        tr.querySelector(
                            ".notice-admin-title"
                        ).textContent =
                            notice.title;

                        tr.querySelector(
                            ".notice-admin-writer"
                        ).textContent =
                            notice.createdNickname
                            || "-";

                        const goEdit = () => {

                            const back =
                                encodeURIComponent(
                                    currentUrl()
                                );

                            location.href =
                                `/admin/notices/${notice.noticeId}?returnUrl=${back}`;
                        };

                        tr.addEventListener(
                            "click",
                            goEdit
                        );

                        tr.addEventListener(
                            "keydown",
                            (e) => {

                                if (
                                    e.key === "Enter"
                                    || e.key === " "
                                ) {
                                    goEdit();
                                }
                            }
                        );

                        body.appendChild(tr);
                    }
                );

                renderPagination(
                    $("#adminPagination"),
                    data.pageNo,
                    data.totalPages,
                    (target) => {

                        setParams({
                            page: target
                        });

                        load();

                        window.scrollTo({
                            top: 0,
                            behavior: "smooth"
                        });
                    }
                );

            } catch (error) {
                errorState.hidden =
                    false;
            }
        };

        const search = () => {

            setParams({
                query:
                    queryInput.value.trim(),

                important:
                    importantSelect.value,

                page: 1
            });

            load();
        };

        $("#adminSearchButton")
            .addEventListener(
                "click",
                search
            );

        queryInput.addEventListener(
            "keydown",
            (e) => {

                if (
                    e.key === "Enter"
                    && !e.isComposing
                ) {
                    search();
                }
            }
        );


        importantSelect.addEventListener(
            "change",
            search
        );

        $("#createNoticeButton")
            .addEventListener(
                "click",
                () => {

                    const back =
                        encodeURIComponent(
                            currentUrl()
                        );

                    location.href =
                        `/admin/notices/new?returnUrl=${back}`;
                }
            );

        await load();
    }

    function initAdminForm() {

        const noticeId =
            document.body.dataset.noticeId;

        const isEdit =
            Boolean(
                noticeId
                && noticeId !== "null"
            );

        const back =
            returnUrl(
                "/admin/notices"
            );

        $("#cancelButton")
            .addEventListener(
                "click",
                () => {

                    location.href =
                        back;
                }
            );

        $("#deleteButton")
            ?.addEventListener(
                "click",
                async () => {

                    if (!isEdit) {
                        return;
                    }

                    const confirmed =
                        window.confirm(
                            "이 공지사항을 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다."
                        );

                    if (!confirmed) {
                        return;
                    }

                    const deleteButton =
                        $("#deleteButton");

                    const errorBox =
                        $("#formError");

                    deleteButton.disabled =
                        true;

                    deleteButton.textContent =
                        "삭제 중...";

                    errorBox.hidden =
                        true;

                    errorBox.textContent =
                        "";

                    try {
                        await api(
                            `/api/admin/notices/${noticeId}`,
                            {
                                method: "DELETE"
                            }
                        );

                        location.href =
                            back;

                    } catch (error) {

                        errorBox.textContent =
                            error.message;

                        errorBox.hidden =
                            false;

                        deleteButton.disabled =
                            false;

                        deleteButton.textContent =
                            "삭제";
                    }
                }
            );

        $("#noticeForm")
            .addEventListener(
                "submit",
                async (event) => {

                    event.preventDefault();

                    const title =
                        $("#title")
                            .value
                            .trim();

                    const content =
                        $("#content")
                            .value
                            .trim();

                    const important =
                        document.querySelector(
                            'input[name="important"]:checked'
                        )?.value;

                    const errorBox =
                        $("#formError");

                    errorBox.hidden =
                        true;

                    errorBox.textContent =
                        "";

                    if (!title) {

                        errorBox.textContent =
                            "제목을 입력해 주세요.";

                        errorBox.hidden =
                            false;

                        $("#title").focus();

                        return;
                    }

                    if (!content) {

                        errorBox.textContent =
                            "본문을 입력해 주세요.";

                        errorBox.hidden =
                            false;

                        $("#content").focus();

                        return;
                    }

                    if (!important) {

                        errorBox.textContent =
                            "중요 여부를 선택해 주세요.";

                        errorBox.hidden =
                            false;

                        return;
                    }

                    const saveButton =
                        $("#saveButton");

                    saveButton.disabled =
                        true;

                    saveButton.textContent =
                        "저장 중...";

                    try {
                        /*
                         * status는 프론트에서 보내지 않는다.
                         * 서버가 항상 PUBLISHED로 저장한다.
                         */
                        const body =
                            JSON.stringify({
                                title,
                                content,
                                important
                            });

                        if (isEdit) {

                            await api(
                                `/api/admin/notices/${noticeId}`,
                                {
                                    method: "PATCH",
                                    body
                                }
                            );

                        } else {

                            await api(
                                "/api/admin/notices",
                                {
                                    method: "POST",
                                    body
                                }
                            );
                        }

                        location.href =
                            back;

                    } catch (error) {

                        errorBox.textContent =
                            error.message;

                        errorBox.hidden =
                            false;

                        saveButton.disabled =
                            false;

                        saveButton.textContent =
                            "저장";
                    }
                }
            );
    }

    $("#retryButton")
        ?.addEventListener(
            "click",
            loadPublicList
        );

    switch (pageType) {

        case "notice-list":
            loadPublicList();
            break;

        case "notice-detail":
            loadPublicDetail();
            break;

        case "admin-notice-list":
            initAdminList();
            break;

        case "admin-notice-form":
            initAdminForm();
            break;

        default:
            break;
    }
})();