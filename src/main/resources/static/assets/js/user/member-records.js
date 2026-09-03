/**
 * Modification History
 * 2026. 8. 31. jinyoung - 평가·보고싶어요 카드의 TMDB 이미지·공통 페이지네이션 적용
 * 2026. 9. 01. jinyoung - U-03~U-06 4탭 UI와 회원 컬렉션 조회 연결
 * 2026. 9. 03. jinyoung - 작품 정렬·평균 별점·컬렉션 카드 및 더보기 UI 적용
 */

/** ===================================
 *  기록 탭 설정
 *  =================================== */
const RECORD_PAGE_SIZE = 12; // 페이지당 기록 수
const RECORD_PAGINATION_GROUP_SIZE = 5; // 한 구간의 최대 페이지 수
const RECORD_TABS = ["ratings", "comments", "collections", "watchlist"]; // 지원 탭
// 탭별 제목, 빈 상태 문구, 활동 건수 속성, 정렬 옵션
const RECORD_CONFIG = Object.freeze({
    ratings: {
        title: "평가한 작품들",
        empty: "아직 평가한 작품이 없습니다.",
        countKey: "ratingsCount",
        sorts: [
            ["latest", "최신 순"],
            ["oldest", "오래된 순"],
            ["rating_desc", "별점 높은 순"],
            ["rating_asc", "별점 낮은 순"]
        ]
    },
    comments: {
        title: "작성한 코멘트",
        empty: "코멘트 API가 연결되면 작성한 코멘트가 표시됩니다.",
        countKey: "commentsCount",
        sorts: [],
        integrationPending: true
    },
    collections: {
        title: "만든 컬렉션",
        empty: "아직 만든 컬렉션이 없습니다.",
        countKey: "collectionsCount",
        sorts: [
            ["latest", "최신 순"],
            ["oldest", "오래된 순"],
            ["likes", "좋아요 많은 순"]
        ]
    },
    watchlist: {
        title: "보고싶어요 작품들",
        empty: "아직 보고싶어요로 등록한 작품이 없습니다.",
        countKey: "watchlistCount",
        sorts: [
            ["latest", "최신 순"],
            ["oldest", "오래된 순"]
        ]
    }
});

/** ===================================
 *  화면 요소 및 탭별 상태
 *  =================================== */

const recordsPage = document.querySelector("#memberRecordsPage"); // 기록 화면 루트 요소
const memberId = Number(recordsPage.dataset.memberId); // 조회 대상 회원 번호
// 서버에서 전달받은 탭별 전체 건수
const recordCounts = Object.fromEntries(
    RECORD_TABS.map((tab) => [
        tab,
        Number(recordsPage.dataset[RECORD_CONFIG[tab].countKey] || 0)
    ])
);
// 페이지 번호, 정렬값, 조회 결과, 스크롤 위치를 보관하는 탭별 상태
const recordState = Object.fromEntries(
    RECORD_TABS.map((tab) => [
        tab,
        {
            pageNo: 1,
            sort: RECORD_CONFIG[tab].sorts[0]?.[0] || null,
            data: null,
            scrollY: 0
        }
    ])
);
// 페이지 표시선의 이전 위치를 보관하는 탭별 상태
const paginationIndicatorState = Object.fromEntries(
    RECORD_TABS.map((tab) => [tab, null])
);

let activeTab = normalizeTab(recordsPage.dataset.initialTab || new URLSearchParams(window.location.search).get("tab")); // 현재 탭

/** ===================================
 *  화면 초기화 및 이벤트 연결
 *  =================================== */

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("#recordTabs [data-tab]").forEach((tabLink) => tabLink.addEventListener("click", changeTab));

    document.querySelector("#recordRetryButton")
        .addEventListener("click", () => {
            resetTabState(activeTab);
            loadRecords(activeTab, 1);
        });

    document.querySelector("#recordSortButton").addEventListener("click", toggleSortMenu);

    document.addEventListener("click", (event) => {
        if (!event.target.closest("#recordSortWrap")) {
            closeSortMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeSortMenu(true);
        }
    });

    document.querySelector("#collectionLoadMoreButton")
        .addEventListener("click", () => {
            const state = recordState.collections;
            loadRecords("collections", state.pageNo + 1, true);
        });

    window.addEventListener("popstate", () => {
        const tab = normalizeTab(new URLSearchParams(window.location.search).get("tab"));
        switchTab(tab, false);
    });

    updateTabView();

    if (!Number.isInteger(memberId) || memberId <= 0) {
        showRecordError("올바른 회원 번호가 필요합니다.");
        return;
    }

    loadRecords(activeTab, 1);
});

/** ===================================
 *  탭 전환 및 정렬
 *  =================================== */

/** 요청 탭 이름 정규화 */
function normalizeTab(tab) {

    return RECORD_TABS.includes(tab) ? tab : "ratings";
}

/** 탭 클릭 처리 */
function changeTab(event) {

    event.preventDefault();
    switchTab(normalizeTab(event.currentTarget.dataset.tab), true);
}

/** 활성 탭 전환 */
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

    // 이미 조회한 탭은 저장된 결과와 스크롤 위치를 복원하여 API를 다시 호출하지 않는다.
    if (savedState.data) {
        renderRecords(activeTab, savedState.data);
        window.requestAnimationFrame(() => {
            window.scrollTo({ top: savedState.scrollY, behavior: "auto" });
        });

        return;
    }

    loadRecords(activeTab, savedState.pageNo);
}

/** 정렬 조건 변경 */
function changeSort(sort) {

    const state = recordState[activeTab];

    if (!state.sort || state.sort === sort) {
        return;
    }

    state.sort = sort;
    resetTabState(activeTab, true);
    loadRecords(activeTab, 1);
}

/** 탭 조회 상태 초기화 */
function resetTabState(tab, keepSort = false) {

    const sort = keepSort ? recordState[tab].sort : RECORD_CONFIG[tab].sorts[0]?.[0] || null;
    recordState[tab] = { pageNo: 1, sort, data: null, scrollY: 0 };
}

/** 활성 탭 화면 갱신 */
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

    document.querySelector("#recordTitleText").textContent = RECORD_CONFIG[activeTab].title;
    document.querySelector("#recordTotalCount").textContent = String(recordCounts[activeTab]);
    document.querySelector("#recordTabs").style.setProperty("--active-tab-index", String(RECORD_TABS.indexOf(activeTab)));

    updateSortControl();
}

/** 정렬 드롭다운 갱신 */
function updateSortControl() {

    const config = RECORD_CONFIG[activeTab];
    const sortWrap = document.querySelector("#recordSortWrap");
    const sortButton = document.querySelector("#recordSortButton");
    const sortLabel = document.querySelector("#recordSortLabel");
    const sortMenu = document.querySelector("#recordSortMenu");
    const selectedSort = recordState[activeTab].sort;

    sortMenu.replaceChildren();

    config.sorts.forEach(([value, label]) => {
        const option = document.createElement("button");
        const optionText = document.createElement("span");
        const selected = value === selectedSort;

        option.className = "member-sort-option";
        option.type = "button";
        option.dataset.value = value;
        option.setAttribute("role", "option");
        option.setAttribute("aria-selected", String(selected));
        optionText.textContent = label;
        option.append(optionText);

        if (selected) {
            const check = document.createElement("i");
            check.className = "bi bi-check2";
            check.setAttribute("aria-hidden", "true");
            option.append(check);
        }

        option.addEventListener("click", () => {
            closeSortMenu();
            changeSort(value);
        });
        sortMenu.append(option);
    });

    sortWrap.classList.toggle("d-none", config.sorts.length === 0);
    sortButton.disabled = config.sorts.length === 0;
    sortLabel.textContent = config.sorts.find(([value]) => value === selectedSort)?.[1] || "정렬";

    closeSortMenu();
}

/** 정렬 드롭다운 열기·닫기 */
function toggleSortMenu() {

    const sortButton = document.querySelector("#recordSortButton");
    const sortMenu = document.querySelector("#recordSortMenu");
    const opening = sortMenu.classList.contains("d-none");

    sortMenu.classList.toggle("d-none", !opening);
    sortButton.classList.toggle("is-open", opening);
    sortButton.setAttribute("aria-expanded", String(opening));

    if (opening) {
        sortMenu.querySelector('[aria-selected="true"]')?.focus();
    }
}

/** 정렬 드롭다운 닫기 */
function closeSortMenu(returnFocus = false) {

    const sortButton = document.querySelector("#recordSortButton");
    const sortMenu = document.querySelector("#recordSortMenu");
    const wasOpen = !sortMenu.classList.contains("d-none");

    sortMenu.classList.add("d-none");
    sortButton.classList.remove("is-open");

    sortButton.setAttribute("aria-expanded", "false");
    if (returnFocus && wasOpen) {
        sortButton.focus();
    }
}

/** ===================================
 *  기록 API 조회
 *  =================================== */

/** 탭별 기록 조회 */
async function loadRecords(tab, pageNo, append = false) {

    const requestTab = tab;
    const config = RECORD_CONFIG[tab];

    // 코멘트 API 연결 전에는 서버를 호출하지 않고 준비 상태 데이터를 사용한다.
    if (config.integrationPending) {
        const pendingData = {
            items: [],
            page: {
                pageNo: 1,
                pageSize: RECORD_PAGE_SIZE,
                totalCnt: recordCounts.comments
            },
            integrationPending: true
        };
        recordState[tab].data = pendingData;
        renderRecords(tab, pendingData);
        return;
    }

    if (append) {
        setLoadMoreLoading(true);
    } else {
        showRecordLoading();
    }

    try {
        const data = await requestGet(createRecordEndpoint(tab), createRecordParams(tab, pageNo));
        const state = recordState[requestTab];
        const items = Array.isArray(data.items) ? data.items : [];
        // 컬렉션 더보기 요청은 기존 목록 뒤에 새 결과를 이어 붙인다.
        const mergedItems = append && state.data ? [...state.data.items, ...items] : items;

        state.pageNo = pageNo;
        state.data = { ...data, items: mergedItems };

        if (activeTab === requestTab) {
            renderRecords(requestTab, state.data);
        }
    } catch (error) {
        if (activeTab === requestTab) {
            showRecordError(error.message);
        }
    } finally {
        if (append) {
            setLoadMoreLoading(false);
        }
    }
}

/** 탭별 API 주소 생성 */
function createRecordEndpoint(tab) {

    if (tab === "collections") {
        return `/api/users/${memberId}/collections`;
    }

    return tab === "ratings" ? `/api/users/${memberId}/ratings` : `/api/users/${memberId}/watchlist`;
}

/** 탭별 API 요청 조건 생성 */
function createRecordParams(tab, pageNo) {

    if (tab === "collections") {
        return {
            pageNo,
            pageSize: RECORD_PAGE_SIZE,
            sort: recordState[tab].sort
        };
    }

    return {
        page: pageNo,
        size: RECORD_PAGE_SIZE,
        sort: recordState[tab].sort
    };
}

/** ===================================
 *  기록 결과 및 작품 카드
 *  =================================== */

/** 탭별 조회 결과 렌더링 */
function renderRecords(tab, data) {

    const items = Array.isArray(data.items) ? data.items : [];
    const page = data.page || {};
    const totalCount = Number(page.totalCnt ?? recordCounts[tab] ?? 0);

    recordCounts[tab] = totalCount;
    hideRecordStatus();
    document.querySelector("#recordTotalCount").textContent = String(totalCount);

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
        return;
    }

    if (tab === "collections") {
        renderCollectionCards(items);
        renderCollectionLoadMore(items.length, totalCount);
    } else {
        renderMovieCards(items);
        renderPagination(page, Number(page.pageNo || recordState[tab].pageNo));
    }
}

/** 평가·보고싶어요 작품 카드 렌더링 */
function renderMovieCards(items) {

    const recordList = document.querySelector("#recordList");

    recordList.className = "member-card-grid";
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
        meta.className = "member-card-meta member-card-rating";
        title.textContent = movieTitle;
        renderRatingMeta(meta, item);

        const poster = item.posterUrl
            ? createPosterImage(item.posterUrl, movieTitle)
            : createPosterPlaceholder(movieTitle);

        body.append(title, meta);
        link.append(poster, body);
        recordList.append(link);
    });

    recordList.classList.remove("d-none");
}

/** ===================================
 *  작품 별점 표시
 *  =================================== */

/** 작품 카드 별점 정보 렌더링 */
function renderRatingMeta(meta, item) {

    const averageRating = Number(item.averageRating);
    const myRating = Number(item.ratingScore);
    const hasAverageRating = item.averageRating != null && Number.isFinite(averageRating);
    const hasMyRating = item.ratingScore != null && Number.isFinite(myRating);

    // 평가와 보고싶어요 탭 모두 평균 별점은 왼쪽, 내 별점은 오른쪽에 배치한다.
    if (hasAverageRating) {
        meta.append(createAverageRating(averageRating));
    }

    if (hasMyRating) {
        meta.append(createMemberRatingStars(myRating));
    }
}

/** 평균 별점 요소 생성 */
function createAverageRating(averageRating) {

    const average = document.createElement("span");
    const star = document.createElement("span");
    const score = document.createElement("span");

    average.className = "member-average-rating";
    average.append("평균");
    star.className = "member-average-rating-star";
    star.textContent = "★";
    score.textContent = averageRating.toFixed(1);
    average.append(star, score);

    return average;
}

/** 회원 평가 별 아이콘 생성 */
function createMemberRatingStars(rating) {

    const stars = document.createElement("span");
    const normalizedRating = Math.max(0, Math.min(5, rating));

    stars.className = "member-rating-stars";
    stars.setAttribute("aria-label", `내 평가 ${normalizedRating}점`);

    for (let index = 1;index <= 5;index += 1) {
        const star = document.createElement("span");
        const remaining = normalizedRating - (index - 1);
        star.className = "member-rating-star";
        star.setAttribute("aria-hidden", "true");
        star.textContent = remaining >= 1 ? "★" : "☆";

        // 정수가 아닌 별점은 현재 별의 절반만 채우도록 별도 클래스를 적용한다.
        if (remaining >= 1) {
            star.classList.add("is-filled");
        } else if (remaining > 0) {
            star.classList.add("is-half");
            star.textContent = "★";
        }
        stars.append(star);
    }

    return stars;
}

/** ===================================
 *  컬렉션 카드
 *  =================================== */

/** 컬렉션 카드 목록 렌더링 */
function renderCollectionCards(items) {

    const recordList = document.querySelector("#recordList");

    recordList.className = "collection-card-grid";
    recordList.replaceChildren();

    items.forEach((collection) => {
        recordList.append(createCollectionCard(collection));
    });

    recordList.classList.remove("d-none");
}

/** 컬렉션 링크 카드 생성 */
function createCollectionCard(collection) {

    const article = document.createElement("article");
    const link = document.createElement("a");
    const itemCount = Math.max(0, Number(collection.itemCount || 0));
    const isEmptyCollection = itemCount === 0;
    const previewPosters = [
        collection.previewPosterUrl1,
        collection.previewPosterUrl2,
        collection.previewPosterUrl3,
        collection.previewPosterUrl4,
        collection.previewPosterUrl5
    ].filter(Boolean);
    const visual = createCollectionVisual(collection, itemCount, isEmptyCollection, previewPosters);
    const body = createCollectionBody(collection);

    article.className = "collection-list-card";
    link.className = "collection-list-card-link";
    // 비어 있는 컬렉션은 작품을 바로 추가할 수 있도록 수정 화면으로 연결한다.
    link.href = isEmptyCollection ? `/collections/${collection.collectionId}/edit` : `/collections/${collection.collectionId}`;
    link.setAttribute("aria-label", `${collection.title} 컬렉션 ${isEmptyCollection ? "수정" : "보기"}`);
    link.append(visual, body);
    article.append(link);

    return article;
}

/** 컬렉션 시각 영역 생성 */
function createCollectionVisual(collection, itemCount, isEmptyCollection, previewPosters) {

    const visual = document.createElement("div");
    visual.className = "collection-list-card-visual";

    if (!isEmptyCollection && previewPosters.length > 0) {
        visual.classList.add("has-posters");
        visual.append(createCollectionPosterCollage(previewPosters, visual));
    }

    if (collection.isPublic === "N") {
        const label = document.createElement("span");
        label.className = "collection-list-card-owner-badge";
        label.textContent = "비공개";
        visual.append(label);
    }

    if (isEmptyCollection) {
        visual.classList.add("is-empty");
        visual.append(createEmptyCollectionContent());
        return visual;
    }

    const symbol = document.createElement("span");
    const visualCount = document.createElement("span");

    symbol.className = "collection-list-card-symbol";
    symbol.setAttribute("aria-hidden", "true");
    symbol.innerHTML = '<i class="bi bi-collection-play"></i>';
    visualCount.className = "collection-list-card-visual-count";
    visualCount.textContent = `작품 ${itemCount}`;
    visual.append(symbol, visualCount);

    return visual;
}

/** 빈 컬렉션 안내 요소 생성 */
function createEmptyCollectionContent() {

    const empty = document.createElement("span");
    const iconWrap = document.createElement("span");
    const defaultIcon = document.createElement("i");
    const hoverIcon = document.createElement("i");
    const message = document.createElement("span");
    const title = document.createElement("span");
    const hint = document.createElement("span");

    empty.className = "collection-list-card-empty";
    iconWrap.className = "collection-list-card-empty-icon";
    iconWrap.setAttribute("aria-hidden", "true");
    defaultIcon.className = "bi bi-emoji-dizzy empty-icon-sad";
    hoverIcon.className = "bi bi-emoji-sunglasses empty-icon-hopeful";
    iconWrap.append(defaultIcon, hoverIcon);

    message.className = "collection-list-card-empty-message";
    title.className = "collection-list-card-empty-title";
    title.textContent = "아직 담긴 작품이 없어요";
    hint.className = "collection-list-card-empty-hint";
    hint.textContent = "작품을 추가할까요?";
    message.append(title, hint);
    empty.append(iconWrap, message);

    return empty;
}

/** 컬렉션 본문 정보 생성 */
function createCollectionBody(collection) {

    const body = document.createElement("div");
    const title = document.createElement("h3");
    const titleText = document.createElement("span");
    const description = document.createElement("p");
    const stats = document.createElement("div");

    body.className = "collection-list-card-body";
    title.className = "collection-list-card-title";
    titleText.className = "collection-list-card-title-text";
    titleText.textContent = collection.title || `컬렉션 ${collection.collectionId}`;
    title.append(titleText);
    body.append(title);

    description.className = "collection-list-card-description";
    description.textContent = (collection.description || "").trim();

    if (description.textContent) {
        body.append(description);
    }

    stats.className = "collection-list-card-stats";
    stats.append(
        createCollectionStat("heart", "좋아요", collection.likeCount),
        createCollectionStat("chat", "코멘트", collection.commentCount)
    );
    body.append(stats);

    requestAnimationFrame(() => configureScrollableTitle(title, titleText));

    return body;
}

/** 컬렉션 대표 포스터 콜라주 생성 */
function createCollectionPosterCollage(posterUrls, visual) {

    const collage = document.createElement("div");
    const usesSevenSlotLayout = posterUrls.length === 5;
    // 포스터가 5개일 때는 두 장을 반복 배치하여 일곱 칸 콜라주를 채운다.
    const posterIndexes = usesSevenSlotLayout
        ? [0, 1, 2, 3, 3, 4, 4]
        : posterUrls.map((_, index) => index);

    collage.className = usesSevenSlotLayout
        ? "collection-list-poster-collage poster-count-5 is-seven-slot-layout"
        : `collection-list-poster-collage poster-count-${posterUrls.length} is-simple-layout`;

    posterIndexes.forEach((posterIndex, slotIndex) => {
        const slot = document.createElement("span");
        const poster = document.createElement("img");
        slot.className = `collection-list-poster-slot poster-slot-${slotIndex + 1}`;
        poster.className = "collection-list-preview-poster";
        poster.src = UserListUi.resolveTmdbImageUrl(posterUrls[posterIndex], "w342");
        poster.alt = "";
        poster.loading = "lazy";
        poster.decoding = "async";
        poster.addEventListener("error", () => {
            slot.remove();
            if (!collage.querySelector("img")) {
                collage.remove();
                visual.classList.remove("has-posters");
            }
        });
        slot.append(poster);
        collage.append(slot);
    });

    return collage;
}

/** 컬렉션 통계 항목 생성 */
function createCollectionStat(icon, label, count) {

    const stat = document.createElement("span");
    const text = document.createElement("span");

    stat.innerHTML = `<i class="bi bi-${icon}" aria-hidden="true"></i>`;
    text.textContent = `${label} ${Number(count || 0)}`;
    stat.append(text);

    return stat;
}

/** 긴 컬렉션 제목의 이동 거리 계산 */
function configureScrollableTitle(title, titleText) {

    const overflowWidth = Math.ceil(titleText.getBoundingClientRect().width - title.clientWidth);
    const overflowing = overflowWidth > 0;

    title.classList.toggle("is-overflowing", overflowing);

    if (!overflowing) {
        return;
    }

    title.title = titleText.textContent;

    title.style.setProperty("--collection-title-scroll-distance", `-${overflowWidth}px`);
    title.style.setProperty(
        "--collection-title-scroll-duration",
        `${Math.min(7, Math.max(2.4, overflowWidth / 45))}s`
    );
}

/** ===================================
 *  작품 포스터
 *  =================================== */

/** 작품 포스터 이미지 생성 */
function createPosterImage(posterUrl, movieTitle) {

    const image = document.createElement("img");

    image.className = "member-card-poster";
    image.src = UserListUi.resolveTmdbImageUrl(posterUrl, "w500");
    image.alt = `${movieTitle} 포스터`;
    image.loading = "lazy";
    image.decoding = "async";

    image.addEventListener("error", () => {
        image.replaceWith(createPosterPlaceholder(movieTitle));
    });

    return image;
}

/** 작품 포스터 대체 요소 생성 */
function createPosterPlaceholder(movieTitle) {

    const placeholder = document.createElement("div");

    placeholder.className = "member-card-poster-placeholder";
    placeholder.textContent = `${movieTitle} 포스터 없음`;

    return placeholder;
}

/** ===================================
 *  페이지네이션
 *  =================================== */

/** 기록 페이지네이션 렌더링 */
function renderPagination(page, currentPage) {

    const navigation = document.querySelector("#recordPaginationNavigation");
    const pagination = document.querySelector("#recordPagination");

    UserListUi.renderPagination({
        container: pagination,
        page,
        currentPage,
        defaultPageSize: RECORD_PAGE_SIZE,
        maxVisiblePages: RECORD_PAGINATION_GROUP_SIZE,
        onPageChange: (pageNo) => {
            loadRecords(activeTab, pageNo);
            document.querySelector("#recordTitle").scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }
    });

    const pageItems = pagination.querySelectorAll(".page-item");
    const hasPagination = pageItems.length >= 2;
    navigation.classList.toggle("d-none", !hasPagination);

    if (pageItems.length >= 2) {
        const previousButton = pageItems[0].querySelector(".page-link");
        const nextButton = pageItems[pageItems.length - 1].querySelector(".page-link");
        const numberItems = Array.from(pageItems).slice(1, -1);
        const previousIcon = document.createElement("i");
        const nextIcon = document.createElement("i");
        previousIcon.className = "bi bi-chevron-left page-arrow-icon";
        previousIcon.setAttribute("aria-hidden", "true");
        nextIcon.className = "bi bi-chevron-right page-arrow-icon";
        nextIcon.setAttribute("aria-hidden", "true");
        previousButton.replaceChildren(previousIcon);
        previousButton.setAttribute("aria-label", "이전 페이지");
        nextButton.replaceChildren(nextIcon);
        nextButton.setAttribute("aria-label", "다음 페이지");

        numberItems.forEach((item) => item.classList.add("member-page-number"));
        renderPaginationIndicator(pagination, numberItems);
    }
}

/** 활성 페이지 이동 표시선 생성 */
function renderPaginationIndicator(pagination, numberItems) {

    const activeIndex = numberItems.findIndex((item) => item.classList.contains("active"));

    if (activeIndex < 0 || numberItems.length === 0) {
        return;
    }

    const track = document.createElement("li");
    const indicator = document.createElement("span");
    const firstRect = numberItems[0].getBoundingClientRect();
    const lastRect = numberItems[numberItems.length - 1].getBoundingClientRect();
    const activeRect = numberItems[activeIndex].getBoundingClientRect();
    const paginationRect = pagination.getBoundingClientRect();
    const startPage = Number(numberItems[0].querySelector(".page-link").textContent);
    const previousState = paginationIndicatorState[activeTab];
    // 같은 페이지 구간에서는 직전 위치를 시작점으로 사용하여 표시선이 자연스럽게 이동한다.
    const previousIndex = previousState?.startPage === startPage
        ? Math.min(previousState.activeIndex, numberItems.length - 1)
        : activeIndex;
    const previousRect = numberItems[previousIndex].getBoundingClientRect();

    track.className = "member-page-indicator-track";
    track.setAttribute("aria-hidden", "true");
    track.style.left = `${firstRect.left - paginationRect.left}px`;
    track.style.width = `${lastRect.right - firstRect.left}px`;
    indicator.className = "member-page-indicator";
    indicator.style.width = `${activeRect.width}px`;
    indicator.style.transform = `translateX(${previousRect.left - firstRect.left}px)`;
    track.append(indicator);
    pagination.append(track);

    // 시작 위치를 먼저 반영한 뒤 다음 프레임에서 활성 페이지로 이동시킨다.
    void indicator.offsetWidth;
    window.requestAnimationFrame(() => {
        indicator.style.transform =
            `translateX(${activeRect.left - firstRect.left}px)`;
    });

    paginationIndicatorState[activeTab] = { startPage, activeIndex };
}

/** ===================================
 *  더보기 및 화면 상태
 *  =================================== */

/** 컬렉션 더보기 버튼 표시 */
function renderCollectionLoadMore(loadedCount, totalCount) {

    document.querySelector("#collectionLoadMoreWrap").classList.toggle("d-none", loadedCount >= totalCount);
}

/** 컬렉션 더보기 로딩 상태 표시 */
function setLoadMoreLoading(loading) {

    const button = document.querySelector("#collectionLoadMoreButton");

    button.disabled = loading;
    button.textContent = loading ? "불러오는 중..." : "더보기";
}

/** 기록 로딩 상태 표시 */
function showRecordLoading() {

    hideRecordStatus();
    document.querySelector("#recordLoading").classList.remove("d-none");
}

/** 기록 빈 상태 표시 */
function showRecordEmpty(title, message, iconName) {

    const empty = document.querySelector("#recordEmpty");

    document.querySelector("#recordEmptyTitle").textContent = title;
    document.querySelector("#recordEmptyMessage").textContent = message;
    document.querySelector("#recordEmptyIcon").className = `bi ${iconName} member-state-icon`;

    empty.classList.remove("d-none");
}

/** 기록 오류 상태 표시 */
function showRecordError(message) {

    hideRecordStatus();

    document.querySelector("#recordErrorMessage").textContent = message;
    document.querySelector("#recordError").classList.remove("d-none");
}

/** 기록 조회 상태 초기화 */
function hideRecordStatus() {

    document.querySelector("#recordLoading").classList.add("d-none");
    document.querySelector("#recordError").classList.add("d-none");
    document.querySelector("#recordEmpty").classList.add("d-none");
    document.querySelector("#recordList").classList.add("d-none");
    document.querySelector("#recordPaginationNavigation").classList.add("d-none");
    document.querySelector("#recordPagination").replaceChildren();
    document.querySelector("#collectionLoadMoreWrap").classList.add("d-none");
}
