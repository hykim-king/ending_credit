const RECORD_PAGE_SIZE = 12;
const RECORD_TABS = ["ratings", "watchlist"];

const memberId = Number(document.body.dataset.memberId);
const recordState = {
    ratings: {
        pageNo: 1,
        data: null,
        scrollY: 0
    },
    watchlist: {
        pageNo: 1,
        data: null,
        scrollY: 0
    }
};

let activeTab = normalizeTab(
    document.body.dataset.initialTab
    || new URLSearchParams(window.location.search).get("tab")
);

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("#recordTabs [data-tab]")
        .forEach((tabLink) => {
            tabLink.addEventListener("click", changeTab);
        });

    document.querySelector("#recordRetryButton")
        .addEventListener("click", () => {
            loadRecords(activeTab, recordState[activeTab].pageNo);
        });

    window.addEventListener("popstate", () => {
        const tab = normalizeTab(
            new URLSearchParams(window.location.search).get("tab")
        );

        switchTab(tab, false);
    });

    updateTabView();

    if (!Number.isInteger(memberId) || memberId <= 0) {
        showRecordError("올바른 회원 번호가 필요합니다.");
        return;
    }

    loadRecords(activeTab, 1);
});

/** 평가와 보고싶어요 외의 값은 기본 평가 탭으로 보정한다. */
function normalizeTab(tab) {
    return RECORD_TABS.includes(tab) ? tab : "ratings";
}

/** 탭 링크의 기본 이동을 막고 같은 페이지 안에서 기록 유형을 전환한다. */
function changeTab(event) {
    event.preventDefault();

    const nextTab = normalizeTab(event.currentTarget.dataset.tab);
    switchTab(nextTab, true);
}

/** 현재 탭의 위치를 저장한 뒤 새 탭의 데이터 또는 캐시를 표시한다. */
function switchTab(nextTab, updateHistory) {
    if (nextTab === activeTab) {
        return;
    }

    recordState[activeTab].scrollY = window.scrollY;
    activeTab = nextTab;

    if (updateHistory) {
        const url = new URL(window.location.href);
        url.searchParams.set("tab", activeTab);
        window.history.pushState({ tab: activeTab }, "", url);
    }

    updateTabView();

    const savedState = recordState[activeTab];

    if (savedState.data) {
        renderRecords(activeTab, savedState.data);

        window.requestAnimationFrame(() => {
            window.scrollTo({
                top: savedState.scrollY,
                behavior: "auto"
            });
        });
        return;
    }

    loadRecords(activeTab, savedState.pageNo);
}

/** 선택된 탭의 제목과 활성 상태를 화면에 반영한다. */
function updateTabView() {
    document.querySelectorAll("#recordTabs [data-tab]")
        .forEach((tabLink) => {
            const selected = tabLink.dataset.tab === activeTab;

            tabLink.classList.toggle("active", selected);

            if (selected) {
                tabLink.setAttribute("aria-current", "page");
            } else {
                tabLink.removeAttribute("aria-current");
            }
        });

    document.querySelector("#recordTitle").textContent =
        activeTab === "ratings"
            ? "평가한 영화"
            : "보고싶어요 영화";

    document.title =
        activeTab === "ratings"
            ? "회원 평가 기록"
            : "회원 보고싶어요 기록";
}

/** 현재 탭과 페이지에 해당하는 회원 기록을 조회한다. */
async function loadRecords(tab, pageNo) {
    const requestTab = tab;
    const endpoint = tab === "ratings"
        ? `/api/users/${memberId}/ratings`
        : `/api/users/${memberId}/watchlist`;

    showRecordLoading();

    try {
        const data = await requestGet(endpoint, {
            page: pageNo,
            size: RECORD_PAGE_SIZE,
            sort: "latest"
        });

        recordState[requestTab].pageNo = pageNo;
        recordState[requestTab].data = data;

        // 탭을 빠르게 전환해 이전 요청이 늦게 끝난 경우 현재 화면을 덮지 않는다.
        if (activeTab === requestTab) {
            renderRecords(requestTab, data);
        }
    } catch (error) {
        if (activeTab === requestTab) {
            showRecordError(error.message);
        }
    }
}

/** API 응답의 목록, 전체 건수와 페이지 정보를 화면에 표시한다. */
function renderRecords(tab, data) {
    const items = Array.isArray(data.items) ? data.items : [];
    const page = data.page || {};
    const totalCount = Number(page.totalCnt || 0);

    hideRecordStatus();
    document.querySelector("#recordTotalCount").textContent =
        `${totalCount}개`;

    if (items.length === 0) {
        showRecordEmpty(
            tab === "ratings"
                ? "아직 평가한 영화가 없습니다."
                : "아직 보고싶어요로 등록한 영화가 없습니다."
        );
        renderPagination(page, 1);
        return;
    }

    renderRecordCards(tab, items);
    renderPagination(
        page,
        Number(page.pageNo || recordState[tab].pageNo)
    );
}

/** 회원 콘텐츠 목록을 영화 카드로 생성한다. */
function renderRecordCards(tab, items) {
    const recordList = document.querySelector("#recordList");
    recordList.replaceChildren();

    items.forEach((item) => {
        const column = document.createElement("div");
        const link = document.createElement("a");
        const card = document.createElement("article");
        const body = document.createElement("div");
        const title = document.createElement("h3");
        const meta = document.createElement("p");

        link.className = "text-decoration-none text-dark h-100";
        link.href = `/movies/${item.contentId}`;

        card.className = "card h-100 border-0 shadow-sm";
        body.className = "card-body";
        title.className = "h6 card-title text-truncate";
        meta.className = "card-text text-secondary small mb-0";

        const movieTitle =
            item.titleKo
            || item.titleOrg
            || `콘텐츠 ${item.contentId}`;

        title.textContent = movieTitle;
        meta.textContent = createRecordMeta(tab, item);

        const poster = item.posterUrl
            ? createPosterImage(item.posterUrl, movieTitle)
            : createPosterPlaceholder(movieTitle);

        body.append(title, meta);
        card.append(poster, body);
        link.append(card);
        column.append(link);
        recordList.append(column);
    });

    recordList.classList.remove("d-none");
}

/** 영화 카드의 평가 또는 개봉 연도 정보를 구성한다. */
function createRecordMeta(tab, item) {
    const year = formatReleaseYear(item.releaseYear);

    if (tab === "ratings") {
        const score = Number(item.ratingScore);
        const stars = Number.isInteger(score)
            ? "★".repeat(score) + "☆".repeat(5 - score)
            : "평가 정보 없음";

        return year ? `${year} · ${stars}` : stars;
    }

    return year || item.titleOrg || "";
}

/** 포스터 이미지를 생성하고 로드 실패 시 대체 영역으로 변경한다. */
function createPosterImage(posterUrl, movieTitle) {
    const image = document.createElement("img");

    image.className = "card-img-top bg-secondary-subtle";
    image.src = posterUrl;
    image.alt = `${movieTitle} 포스터`;
    image.style.aspectRatio = "2 / 3";
    image.style.objectFit = "cover";

    image.addEventListener("error", () => {
        image.replaceWith(createPosterPlaceholder(movieTitle));
    });

    return image;
}

/** 포스터가 없거나 로드에 실패한 영화의 대체 영역을 생성한다. */
function createPosterPlaceholder(movieTitle) {
    const placeholder = document.createElement("div");

    placeholder.className =
        "card-img-top bg-secondary-subtle d-flex "
        + "align-items-center justify-content-center text-secondary p-3";
    placeholder.style.aspectRatio = "2 / 3";
    placeholder.textContent = `${movieTitle} 포스터 없음`;

    return placeholder;
}

/** 날짜 문자열에서 화면에 사용할 개봉 연도만 추출한다. */
function formatReleaseYear(releaseYear) {
    if (!releaseYear) {
        return "";
    }

    return String(releaseYear).substring(0, 4);
}

/** 전체 건수와 페이지 크기를 이용해 페이지 버튼을 생성한다. */
function renderPagination(page, currentPage) {
    const pagination = document.querySelector("#recordPagination");
    const pageSize = Number(page.pageSize || RECORD_PAGE_SIZE);
    const totalCount = Number(page.totalCnt || 0);
    const totalPages = Math.ceil(totalCount / pageSize);

    pagination.replaceChildren();

    if (totalPages <= 1) {
        return;
    }

    const startPage =
        Math.floor((currentPage - 1) / 10) * 10 + 1;
    const endPage =
        Math.min(startPage + 9, totalPages);

    pagination.append(
        createPageButton(
            "이전",
            startPage - 1,
            startPage === 1,
            false
        )
    );

    for (
        let pageNo = startPage;
        pageNo <= endPage;
        pageNo += 1
    ) {
        pagination.append(
            createPageButton(
                String(pageNo),
                pageNo,
                false,
                pageNo === currentPage
            )
        );
    }

    pagination.append(
        createPageButton(
            "다음",
            endPage + 1,
            endPage === totalPages,
            false
        )
    );
}

/** 기록 목록의 단일 페이지 버튼을 생성한다. */
function createPageButton(
    label,
    pageNo,
    disabled,
    selected
) {
    const item = document.createElement("li");
    const button = document.createElement("button");

    item.className =
        `page-item${disabled ? " disabled" : ""}`
        + `${selected ? " active" : ""}`;

    button.className = "page-link";
    button.type = "button";
    button.textContent = label;
    button.disabled = disabled;

    if (selected) {
        button.setAttribute("aria-current", "page");
    }

    button.addEventListener("click", () => {
        loadRecords(activeTab, pageNo);

        document.querySelector("#recordTitle")
            .scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
    });

    item.append(button);

    return item;
}

/** 목록 요청 중 상태만 표시한다. */
function showRecordLoading() {
    hideRecordStatus();

    document.querySelector("#recordLoading")
        .classList.remove("d-none");
}

/** 조회 결과가 없는 상태를 표시한다. */
function showRecordEmpty(message) {
    const empty = document.querySelector("#recordEmpty");

    empty.querySelector("#recordEmptyMessage")
        .textContent = message;
    empty.classList.remove("d-none");
}

/** 조회 오류 메시지와 재시도 버튼을 표시한다. */
function showRecordError(message) {
    hideRecordStatus();

    document.querySelector("#recordErrorMessage")
        .textContent = message;
    document.querySelector("#recordError")
        .classList.remove("d-none");
}

/** loading, empty, error, list 상태를 모두 숨긴다. */
function hideRecordStatus() {
    document.querySelector("#recordLoading")
        .classList.add("d-none");
    document.querySelector("#recordError")
        .classList.add("d-none");
    document.querySelector("#recordEmpty")
        .classList.add("d-none");
    document.querySelector("#recordList")
        .classList.add("d-none");
    document.querySelector("#recordPagination")
        .replaceChildren();
}