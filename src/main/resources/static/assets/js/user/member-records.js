/**
 * Modification History
 * 2026. 8. 31. jinyoung - 평가·보고싶어요 카드의 TMDB 포스터 상대 경로 표시 지원
 * 2026. 8. 31. jinyoung - 이미지 URL·페이지네이션 공통 UI 사용
 * 2026. 9. 01. jinyoung - U-03~U-06 4탭 UI와 회원 컬렉션 조회 연결
 */
const RECORD_PAGE_SIZE = 12;
const RECORD_TABS = ["ratings", "comments", "collections", "watchlist"];
const RECORD_CONFIG = Object.freeze({
    ratings: {
        title: "평가한 영화",
        empty: "아직 평가한 영화가 없습니다.",
        documentTitle: "회원 평가 기록"
    },
    comments: {
        title: "작성한 코멘트",
        empty: "코멘트 API가 연결되면 작성한 코멘트가 표시됩니다.",
        documentTitle: "회원 코멘트 기록",
        integrationPending: true
    },
    collections: {
        title: "만든 컬렉션",
        empty: "아직 만든 컬렉션이 없습니다.",
        documentTitle: "회원 컬렉션 기록"
    },
    watchlist: {
        title: "보고싶어요 영화",
        empty: "아직 보고싶어요로 등록한 영화가 없습니다.",
        documentTitle: "회원 보고싶어요 기록"
    }
});

const recordsPage = document.querySelector("#memberRecordsPage");
const memberId = Number(recordsPage.dataset.memberId);
const recordState = Object.fromEntries(
    RECORD_TABS.map((tab) => [
        tab,
        { pageNo: 1, data: null, scrollY: 0 }
    ])
);

let activeTab = normalizeTab(
    recordsPage.dataset.initialTab
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

function normalizeTab(tab) {
    return RECORD_TABS.includes(tab) ? tab : "ratings";
}

function changeTab(event) {
    event.preventDefault();
    switchTab(normalizeTab(event.currentTarget.dataset.tab), true);
}

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
            window.scrollTo({ top: savedState.scrollY, behavior: "auto" });
        });
        return;
    }

    loadRecords(activeTab, savedState.pageNo);
}

function updateTabView() {
    document.querySelectorAll("#recordTabs [data-tab]")
        .forEach((tabLink) => {
            const selected = tabLink.dataset.tab === activeTab;
            tabLink.classList.toggle("active", selected);
            tabLink.setAttribute("aria-selected", String(selected));

            if (selected) {
                tabLink.setAttribute("aria-current", "page");
            } else {
                tabLink.removeAttribute("aria-current");
            }
        });

    const config = RECORD_CONFIG[activeTab];
    document.querySelector("#recordTitle").textContent = config.title;
    document.querySelector("#recordTotalCount").textContent = "0개";
    document.title = config.documentTitle;
}

async function loadRecords(tab, pageNo) {
    const requestTab = tab;
    const config = RECORD_CONFIG[tab];

    if (config.integrationPending) {
        const pendingData = {
            items: [],
            page: { pageNo: 1, pageSize: RECORD_PAGE_SIZE, totalCnt: 0 },
            integrationPending: true
        };
        recordState[tab].data = pendingData;
        renderRecords(tab, pendingData);
        return;
    }

    showRecordLoading();

    try {
        const data = await requestGet(
            createRecordEndpoint(tab),
            createRecordParams(tab, pageNo)
        );

        recordState[requestTab].pageNo = pageNo;
        recordState[requestTab].data = data;

        if (activeTab === requestTab) {
            renderRecords(requestTab, data);
        }
    } catch (error) {
        if (activeTab === requestTab) {
            showRecordError(error.message);
        }
    }
}

function createRecordEndpoint(tab) {
    if (tab === "collections") {
        return `/api/users/${memberId}/collections`;
    }

    return tab === "ratings"
        ? `/api/users/${memberId}/ratings`
        : `/api/users/${memberId}/watchlist`;
}

function createRecordParams(tab, pageNo) {
    if (tab === "collections") {
        return { pageNo, pageSize: RECORD_PAGE_SIZE };
    }

    return {
        page: pageNo,
        size: RECORD_PAGE_SIZE,
        sort: "latest"
    };
}

function renderRecords(tab, data) {
    const items = Array.isArray(data.items) ? data.items : [];
    const page = data.page || {};
    const totalCount = Number(page.totalCnt || 0);

    hideRecordStatus();
    document.querySelector("#recordTotalCount").textContent = `${totalCount}개`;

    if (data.integrationPending) {
        showRecordEmpty(
            "코멘트 연동 준비 중",
            RECORD_CONFIG.comments.empty,
            "bi-chat-square-text"
        );
        return;
    }

    if (items.length === 0) {
        showRecordEmpty(
            "아직 기록이 없습니다.",
            RECORD_CONFIG[tab].empty,
            tab === "collections" ? "bi-collection" : "bi-film"
        );
        renderPagination(page, 1);
        return;
    }

    if (tab === "collections") {
        renderCollectionCards(items);
    } else {
        renderMovieCards(tab, items);
    }

    renderPagination(page, Number(page.pageNo || recordState[tab].pageNo));
}

function renderMovieCards(tab, items) {
    const recordList = document.querySelector("#recordList");
    recordList.replaceChildren();

    items.forEach((item) => {
        const link = document.createElement("a");
        const body = document.createElement("div");
        const title = document.createElement("h3");
        const meta = document.createElement("p");
        const movieTitle = item.titleKo || item.titleOrg || `콘텐츠 ${item.contentId}`;

        link.className = "member-media-card";
        link.href = `/movies/${item.contentId}`;
        body.className = "member-card-body";
        title.className = "member-card-title";
        meta.className = tab === "ratings"
            ? "member-card-meta member-card-rating"
            : "member-card-meta";
        title.textContent = movieTitle;
        meta.textContent = createRecordMeta(tab, item);

        const poster = item.posterUrl
            ? createPosterImage(item.posterUrl, movieTitle)
            : createPosterPlaceholder(movieTitle);

        body.append(title, meta);
        link.append(poster, body);
        recordList.append(link);
    });

    recordList.classList.remove("d-none");
}

function renderCollectionCards(items) {
    const recordList = document.querySelector("#recordList");
    recordList.replaceChildren();

    items.forEach((item) => {
        const link = document.createElement("a");
        const cover = document.createElement("div");
        const icon = document.createElement("i");
        const coverTitle = document.createElement("strong");
        const body = document.createElement("div");
        const description = document.createElement("p");
        const stats = document.createElement("div");

        link.className = "member-collection-card";
        link.href = `/collections/${item.collectionId}`;
        cover.className = "member-collection-cover";
        icon.className = "bi bi-collection";
        icon.setAttribute("aria-hidden", "true");
        coverTitle.textContent = item.title || `컬렉션 ${item.collectionId}`;
        body.className = "member-card-body";
        description.className = "member-card-meta";
        description.textContent = item.description || "설명이 없습니다.";
        stats.className = "member-collection-stats";
        stats.textContent =
            `작품 ${Number(item.itemCount || 0)} · 좋아요 ${Number(item.likeCount || 0)}`;

        cover.append(icon, coverTitle);
        body.append(description, stats);
        link.append(cover, body);
        recordList.append(link);
    });

    recordList.classList.remove("d-none");
}

function createRecordMeta(tab, item) {
    const year = formatReleaseYear(item.releaseYear);

    if (tab === "ratings") {
        const score = Number(item.ratingScore);
        const stars = Number.isInteger(score) && score >= 0 && score <= 5
            ? `★ ${score}`
            : "평가 정보 없음";

        return year ? `${year} · ${stars}` : stars;
    }

    return year || item.titleOrg || "";
}

function createPosterImage(posterUrl, movieTitle) {
    const image = document.createElement("img");

    image.className = "member-card-poster";
    image.src = UserListUi.resolveTmdbImageUrl(posterUrl, "w500");
    image.alt = `${movieTitle} 포스터`;
    image.addEventListener("error", () => {
        image.replaceWith(createPosterPlaceholder(movieTitle));
    });

    return image;
}

function createPosterPlaceholder(movieTitle) {
    const placeholder = document.createElement("div");
    placeholder.className = "member-card-poster-placeholder";
    placeholder.textContent = `${movieTitle} 포스터 없음`;
    return placeholder;
}

function formatReleaseYear(releaseYear) {
    return releaseYear ? String(releaseYear).substring(0, 4) : "";
}

function renderPagination(page, currentPage) {
    UserListUi.renderPagination({
        container: document.querySelector("#recordPagination"),
        page,
        currentPage,
        defaultPageSize: RECORD_PAGE_SIZE,
        onPageChange: (pageNo) => {
            loadRecords(activeTab, pageNo);
            document.querySelector("#recordTitle").scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }
    });
}

function showRecordLoading() {
    hideRecordStatus();
    document.querySelector("#recordLoading").classList.remove("d-none");
}

function showRecordEmpty(title, message, iconName) {
    const empty = document.querySelector("#recordEmpty");
    document.querySelector("#recordEmptyTitle").textContent = title;
    document.querySelector("#recordEmptyMessage").textContent = message;
    document.querySelector("#recordEmptyIcon").className =
        `bi ${iconName} member-state-icon`;
    empty.classList.remove("d-none");
}

function showRecordError(message) {
    hideRecordStatus();
    document.querySelector("#recordErrorMessage").textContent = message;
    document.querySelector("#recordError").classList.remove("d-none");
}

function hideRecordStatus() {
    document.querySelector("#recordLoading").classList.add("d-none");
    document.querySelector("#recordError").classList.add("d-none");
    document.querySelector("#recordEmpty").classList.add("d-none");
    document.querySelector("#recordList").classList.add("d-none");
    document.querySelector("#recordPagination").replaceChildren();
}
