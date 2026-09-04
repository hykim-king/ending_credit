/**
 * Modification History
 * 2026. 8. 31. jinyoung - 제목 검색 적용
 * 2026. 9. 01. jinyoung - 목록 카드·포스터 콜라주·검색 결과 UI 적용
 * 2026. 9. 02. jinyoung - 빈 설명과 내 컬렉션 표시 개선
 * 2026. 9. 03. jinyoung - 5개 단위 페이지 이동 표시
 */

/** ===================================
 *  상수 및 화면 상태
 *  =================================== */
const DEFAULT_PAGE_SIZE = "12"; // 기본 페이지 크기
const PAGE_GROUP_SIZE = 5; // 한 구간의 최대 페이지 수
const TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w342"; // TMDB 포스터 주소
let collectionPaginationIndicatorState = null; // 이전 활성 페이지 위치

/** ===================================
 *  화면 초기화 및 이벤트 연결
 *  =================================== */

document.addEventListener("DOMContentLoaded", () => {

    const searchForm = document.querySelector("#searchForm");
    const pageSize = document.querySelector("#pageSize");
    const query = new URLSearchParams(window.location.search);
    const requestedPageSize = query.get("pageSize");

    if (query.has("searchWord")) {
        document.querySelector("#searchWord").value = query.get("searchWord");
    }

    if (["6", "12", "24"].includes(requestedPageSize)) {
        pageSize.value = requestedPageSize;
    }

    const initialPage = Math.max(1, Number(query.get("pageNo")) || 1);
    setSearchResultMode((query.get("searchWord") || "").trim());

    searchForm.addEventListener("submit", (event) => {
        event.preventDefault();
        loadCollections(1);
    });

    pageSize.addEventListener("change", () => loadCollections(1));
    loadCollections(initialPage);
});

/** ===================================
 *  목록 조회 및 로딩 상태
 *  =================================== */

/** 컬렉션 목록 조회 및 화면 갱신 */
async function loadCollections(pageNo) {

    const errorMessage = document.querySelector("#errorMessage");
    const searchWord = document.querySelector("#searchWord").value.trim();
    const pageSize = document.querySelector("#pageSize").value;

    hideError(errorMessage);
    showLoading();

    try {
        const data = await requestGet("/api/collections", {
            pageNo,
            pageSize,
            searchDiv: "10",
            searchWord
        });

        const collections = data.items || [];
        const totalCount = Number(data.page?.totalCnt || 0);

        renderCollections(collections, searchWord, data.currentMemberId);
        renderPagination(data.page || {}, pageNo);
        updateResultHeading(searchWord, totalCount);
        updateLocation(pageNo, pageSize, searchWord);
        setSearchResultMode(searchWord);

    } catch (error) {
        showLoadFailure(errorMessage, error.message);
    }
}

/** 목록 로딩 상태 표시 */
function showLoading() {

    document.querySelector("#collectionLoading").classList.remove("d-none");
    document.querySelector("#collectionList").classList.add("d-none");
    document.querySelector("#collectionListEmpty").classList.add("d-none");
    document.querySelector("#paginationNavigation").classList.add("d-none");
}

/** ===================================
 *  컬렉션 카드 생성
 *  =================================== */

/** 컬렉션 목록 또는 빈 상태 렌더링 */
function renderCollections(collections, searchWord, currentMemberId) {

    const collectionList = document.querySelector("#collectionList");
    const loading = document.querySelector("#collectionLoading");
    const empty = document.querySelector("#collectionListEmpty");

    collectionList.replaceChildren();
    loading.classList.add("d-none");

    if (collections.length === 0) {
        updateEmptyState(searchWord);
        empty.classList.remove("d-none");

        return;
    }

    collections.forEach((collection) => {
        collectionList.append(createCollectionCard(collection, currentMemberId));
    });

    collectionList.classList.remove("d-none");
}

/** 컬렉션 링크 카드 생성 */
function createCollectionCard(collection, currentMemberId) {

    const article = document.createElement("article");
    article.className = "collection-list-card";

    const link = document.createElement("a");
    link.className = "collection-list-card-link";
    link.href = `/collections/${collection.collectionId}`;
    link.setAttribute("aria-label", `${collection.title} 컬렉션 보기`);

    const visual = document.createElement("div");
    visual.className = "collection-list-card-visual";

    const previewPosters = [
        collection.previewPosterUrl1,
        collection.previewPosterUrl2,
        collection.previewPosterUrl3,
        collection.previewPosterUrl4,
        collection.previewPosterUrl5
    ].filter(Boolean);

    if (previewPosters.length > 0) {
        visual.classList.add("has-posters");
        visual.append(createCollectionPosterCollage(previewPosters, visual));
    }

    const isOwnedByCurrentMember = Number(currentMemberId) > 0 && Number(collection.memberId) === Number(currentMemberId);
    const visualLabel = document.createElement("span");
    visualLabel.className = isOwnedByCurrentMember ? "collection-list-card-owner-badge" : "collection-list-card-label";
    visualLabel.textContent = isOwnedByCurrentMember ? "내 컬렉션" : "COLLECTION";

    const symbol = document.createElement("span");
    symbol.className = "collection-list-card-symbol";
    symbol.setAttribute("aria-hidden", "true");
    symbol.innerHTML = '<i class="bi bi-collection-play"></i>';

    const visualCount = document.createElement("span");
    visualCount.className = "collection-list-card-visual-count";
    visualCount.textContent = `작품 ${Number(collection.itemCount || 0)}`;

    visual.append(visualLabel, symbol, visualCount);

    const body = document.createElement("div");
    body.className = "collection-list-card-body";

    const title = document.createElement("h3");
    title.className = "collection-list-card-title";
    const titleText = document.createElement("span");
    titleText.className = "collection-list-card-title-text";
    titleText.textContent = collection.title;
    title.append(titleText);

    const descriptionText = (collection.description || "").trim();

    const author = document.createElement("div");
    author.className = "collection-list-card-author";

    const nickname = collection.nickname || `회원 ${collection.memberId}`;
    const avatar = createAuthorAvatar(collection.profileImgUrl, nickname);
    const authorName = document.createElement("span");
    authorName.textContent = nickname;
    author.append(avatar, authorName);

    const stats = document.createElement("div");
    stats.className = "collection-list-card-stats";
    stats.append(
        createStat(
            collection.likedByCurrentMember ? "heart-fill" : "heart",
            "좋아요",
            collection.likeCount,
            collection.likedByCurrentMember
        ),
        createStat("chat", "코멘트", collection.commentCount)
    );

    body.append(title);

    if (descriptionText) {
        const description = document.createElement("p");
        description.className = "collection-list-card-description";
        description.textContent = descriptionText;
        body.append(description);
    }

    body.append(author, stats);
    link.append(visual, body);
    article.append(link);

    requestAnimationFrame(() => configureScrollableTitle(title, titleText));

    return article;
}

/** 긴 컬렉션 제목의 이동 거리 계산 */
function configureScrollableTitle(title, titleText) {

    const overflowWidth = Math.ceil(titleText.getBoundingClientRect().width - title.clientWidth);
    const isOverflowing = overflowWidth > 0;

    title.classList.toggle("is-overflowing", isOverflowing);
    title.toggleAttribute("title", isOverflowing);

    if (!isOverflowing) {
        title.style.removeProperty("--collection-title-scroll-distance");
        title.style.removeProperty("--collection-title-scroll-duration");

        return;
    }

    title.title = titleText.textContent;
    title.style.setProperty(
        "--collection-title-scroll-distance",
        `-${overflowWidth}px`
    );
    title.style.setProperty(
        "--collection-title-scroll-duration",
        `${Math.min(7, Math.max(2.4, overflowWidth / 45))}s`
    );
}

/** 대표 포스터 콜라주 생성 */
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
        slot.className = `collection-list-poster-slot poster-slot-${slotIndex + 1}`;

        const poster = document.createElement("img");
        poster.className = "collection-list-preview-poster";
        poster.src = resolveCollectionPosterUrl(posterUrls[posterIndex]);
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

/** 작성자 프로필 이미지 생성 */
function createAuthorAvatar(profileImgUrl, nickname) {

    const fallback = document.createElement("span");
    fallback.className = "collection-list-card-avatar collection-list-card-avatar-fallback";
    fallback.setAttribute("aria-hidden", "true");
    fallback.innerHTML = '<i class="bi bi-person-fill"></i>';

    if (!profileImgUrl) {
        return fallback;
    }

    const image = document.createElement("img");
    image.className = "collection-list-card-avatar";
    image.src = resolveCollectionProfileUrl(profileImgUrl);
    image.alt = "";
    image.loading = "lazy";
    image.decoding = "async";
    image.addEventListener("error", () => image.replaceWith(fallback));
    image.setAttribute("title", `${nickname} 프로필`);

    return image;
}

/** TMDB 포스터 URL 정규화 */
function resolveCollectionPosterUrl(posterUrl) {
    if (/^https?:\/\//i.test(posterUrl)) {
        return posterUrl;
    }

    return `${TMDB_POSTER_BASE_URL}${posterUrl}`;
}

/** 프로필 이미지 URL 정규화 */
function resolveCollectionProfileUrl(profileImgUrl) {

    try {
        return new URL(profileImgUrl, `${window.location.origin}/`).href;
    } catch {
        return profileImgUrl;
    }
}

/** 컬렉션 통계 항목 생성 */
function createStat(icon, label, count, active = false) {

    const stat = document.createElement("span");
    stat.classList.toggle("is-active", active);
    stat.innerHTML = `<i class="bi bi-${icon}" aria-hidden="true"></i>`;

    const text = document.createElement("span");
    text.textContent = `${label} ${Number(count || 0)}`;
    stat.append(text);

    return stat;
}

/** ===================================
 *  검색 결과 및 빈 상태
 *  =================================== */

/** 검색 결과 제목 갱신 */
function updateResultHeading(searchWord, totalCount) {
    document.querySelector("#collectionResultHeading").textContent = searchWord
        ? `“${searchWord}” 검색 결과`
        : "전체 컬렉션";
    document.querySelector("#resultCount").textContent = `${totalCount}개`;
}

/** 검색 결과 화면 전환 */
function setSearchResultMode(searchWord) {

    const collectionListPage = document.querySelector("#collectionListPage");
    const collectionListHero = document.querySelector(".collection-list-hero");
    const isSearchResult = Boolean(searchWord);

    collectionListPage.classList.toggle("is-search-result", isSearchResult);
    collectionListHero.classList.toggle("d-none", isSearchResult);
}

/** 빈 목록 안내 문구 갱신 */
function updateEmptyState(searchWord) {

    const title = document.querySelector("#collectionEmptyTitle");
    const description = document.querySelector("#collectionEmptyDescription");

    if (searchWord) {
        title.textContent = `“${searchWord}” 검색 결과가 없습니다.`;
        description.textContent = "다른 제목으로 다시 검색해 보세요.";
        return;
    }

    title.textContent = "아직 공개된 컬렉션이 없습니다.";
    description.textContent = "새로운 컬렉션이 만들어지면 이곳에 표시됩니다.";
}

/** ===================================
 *  페이지네이션
 *  =================================== */

/** 페이지 이동 버튼 렌더링 */
function renderPagination(page, currentPage) {

    const navigation = document.querySelector("#paginationNavigation");
    const pagination = document.querySelector("#pagination");
    const pageSize = Number(page.pageSize || 12);
    const totalCount = Number(page.totalCnt || 0);
    const totalPages = Math.ceil(totalCount / pageSize);

    pagination.replaceChildren();
    navigation.classList.toggle("d-none", totalPages <= 1);

    if (totalPages <= 1) {
        return;
    }

    const startPage = Math.floor((currentPage - 1) / PAGE_GROUP_SIZE) * PAGE_GROUP_SIZE + 1;
    const endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

    pagination.append(createPageItem("이전", startPage - 1, startPage === 1, false));

    for (let pageNo = startPage;pageNo <= endPage;pageNo += 1) {
        pagination.append(createPageItem(String(pageNo), pageNo, false, pageNo === currentPage));
    }

    pagination.append(createPageItem("다음", endPage + 1, endPage === totalPages, false));
    decorateCollectionPagination(pagination);
}

/** 활성 페이지 이동 표시선 생성 */
function decorateCollectionPagination(pagination) {

    const pageItems = Array.from(pagination.querySelectorAll(".page-item"));
    const previousButton = pageItems[0].querySelector(".page-link");
    const nextButton = pageItems[pageItems.length - 1].querySelector(".page-link");
    const numberItems = pageItems.slice(1, -1);
    const previousIcon = document.createElement("i");
    const nextIcon = document.createElement("i");

    previousIcon.className = "bi bi-chevron-left collection-page-arrow-icon";
    previousIcon.setAttribute("aria-hidden", "true");
    nextIcon.className = "bi bi-chevron-right collection-page-arrow-icon";
    nextIcon.setAttribute("aria-hidden", "true");
    previousButton.replaceChildren(previousIcon);
    previousButton.setAttribute("aria-label", "이전 페이지");
    nextButton.replaceChildren(nextIcon);
    nextButton.setAttribute("aria-label", "다음 페이지");
    numberItems.forEach((item) => item.classList.add("collection-page-number"));

    const activeIndex = numberItems.findIndex((item) => item.classList.contains("active"));

    if (activeIndex < 0) {
        return;
    }

    const track = document.createElement("li");
    const indicator = document.createElement("span");
    const firstRect = numberItems[0].getBoundingClientRect();
    const lastRect = numberItems[numberItems.length - 1].getBoundingClientRect();
    const activeRect = numberItems[activeIndex].getBoundingClientRect();
    const paginationRect = pagination.getBoundingClientRect();
    const startPage = Number(numberItems[0].querySelector(".page-link").textContent);
    // 같은 페이지 구간에서는 직전 위치를 시작점으로 사용하여 표시선이 자연스럽게 이동한다.
    const previousIndex = collectionPaginationIndicatorState?.startPage === startPage
        ? Math.min(
            collectionPaginationIndicatorState.activeIndex,
            numberItems.length - 1
        )
        : activeIndex;
    const previousRect = numberItems[previousIndex].getBoundingClientRect();

    track.className = "collection-page-indicator-track";
    track.setAttribute("aria-hidden", "true");
    track.style.left = `${firstRect.left - paginationRect.left}px`;
    track.style.width = `${lastRect.right - firstRect.left}px`;
    indicator.className = "collection-page-indicator";
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
    collectionPaginationIndicatorState = { startPage, activeIndex };
}

/** 단일 페이지 버튼 생성 */
function createPageItem(label, pageNo, disabled, active) {

    const item = document.createElement("li");
    item.className = `page-item${disabled ? " disabled" : ""}${active ? " active" : ""}`;

    const button = document.createElement("button");
    button.className = "page-link";
    button.type = "button";
    button.textContent = label;
    button.disabled = disabled;
    if (active) {
        button.setAttribute("aria-current", "page");
    }
    button.addEventListener("click", () => {
        loadCollections(pageNo);
        document.querySelector("#collectionResultHeading").scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    });

    item.append(button);

    return item;
}

/** 검색·페이지 조건의 URL 반영 */
function updateLocation(pageNo, pageSize, searchWord) {

    const query = new URLSearchParams();

    if (searchWord) {
        query.set("searchWord", searchWord);
    }
    if (pageNo > 1) {
        query.set("pageNo", String(pageNo));
    }
    if (pageSize !== DEFAULT_PAGE_SIZE) {
        query.set("pageSize", pageSize);
    }

    const queryString = query.toString();
    history.replaceState(null, "", queryString ? `/collections?${queryString}` : "/collections");
}

/** ===================================
 *  오류 상태
 *  =================================== */

/** 목록 조회 실패 상태 표시 */
function showLoadFailure(element, message) {

    document.querySelector("#collectionLoading").classList.add("d-none");
    document.querySelector("#collectionList").classList.add("d-none");
    document.querySelector("#collectionListEmpty").classList.add("d-none");
    document.querySelector("#paginationNavigation").classList.add("d-none");

    element.textContent = message || "컬렉션 목록을 불러오지 못했습니다.";
    element.classList.remove("d-none");
    element.focus();

    setSearchResultMode("");
}

/** 오류 상태 초기화 */
function hideError(element) {

    element.textContent = "";
    element.classList.add("d-none");
}
