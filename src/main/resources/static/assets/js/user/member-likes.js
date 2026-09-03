/**
 * Modification History
 * 2026. 8. 31. jinyoung - TMDB 상대 경로의 인물 프로필 이미지 표시 지원
 * 2026. 8. 31. jinyoung - 이미지 URL·페이지네이션 공통 UI 사용
 * 2026. 9. 01. jinyoung - U-07 인물·컬렉션 탭 전환 UI 반영
 * 2026. 9. 03. jinyoung - 인물·컬렉션 조회 카드와 고정형 페이지네이션 UI 정리
 */

/** ===================================
 *  좋아요 탭 설정
 *  =================================== */
const LIKE_TYPES = ["person", "collection"]; // 지원 탭
const LIKE_PAGE_SIZES = Object.freeze({ person: 12, collection: 6 }); // 탭별 페이지 크기
const LIKE_PAGINATION_GROUP_SIZE = 5; // 한 구간의 최대 페이지 수
const LIKE_ROLE_LABELS = Object.freeze({
    DIRECTOR: "감독",
    ACTOR: "배우"
});

/** ===================================
 *  화면 요소 및 탭별 상태
 *  =================================== */
const likesPage = document.querySelector("#memberLikesPage"); // 좋아요 화면 루트 요소
const memberId = Number(likesPage.dataset.memberId); // 조회 대상 회원 번호
const currentMemberId = Number(likesPage.dataset.currentMemberId || 0); // 현재 로그인 회원 번호
// 페이지 번호, 조회 결과, 스크롤 위치를 보관하는 탭별 상태
const likeState = {
    person: { pageNo: 1, data: null, scrollY: 0 },
    collection: { pageNo: 1, data: null, scrollY: 0 }
};
// 페이지 표시선의 이전 위치를 보관하는 탭별 상태
const likePaginationIndicatorState = Object.fromEntries(
    LIKE_TYPES.map((type) => [type, null])
);

let activeLikeType = normalizeLikeType(
    likesPage.dataset.initialType
    || new URLSearchParams(window.location.search).get("type")
); // 현재 탭

let requestSequence = 0; // 이전 비동기 응답 무시용 요청 순번

/** ===================================
 *  화면 초기화 및 이벤트 연결
 *  =================================== */
document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("#likeTabs [data-type]")
        .forEach((tabLink) => {
            tabLink.addEventListener("click", changeLikeType);
        });

    document.querySelector("#likeRetryButton")
        .addEventListener("click", () => {
            loadLikes(activeLikeType, likeState[activeLikeType].pageNo);
        });

    window.addEventListener("popstate", () => {
        switchLikeType(normalizeLikeType(
            new URLSearchParams(window.location.search).get("type")
        ), false);
    });

    configureLikeTypeView();

    if (!Number.isInteger(memberId) || memberId <= 0) {
        showLikeError("올바른 회원 번호가 필요합니다.");
        return;
    }

    loadLikes(activeLikeType, 1);
});

/** ===================================
 *  탭 전환
 *  =================================== */

/** 요청 탭 이름 정규화 */
function normalizeLikeType(type) {

    const normalizedType = String(type || "").trim().toLowerCase();

    return LIKE_TYPES.includes(normalizedType)
        ? normalizedType
        : "person";
}

/** 좋아요 탭 클릭 처리 */
function changeLikeType(event) {

    event.preventDefault();
    switchLikeType(normalizeLikeType(event.currentTarget.dataset.type), true);
}

/** 활성 좋아요 탭 전환 */
function switchLikeType(nextType, updateHistory) {

    if (nextType === activeLikeType) {
        return;
    }

    likeState[activeLikeType].scrollY = window.scrollY;
    activeLikeType = nextType;

    if (updateHistory) {
        const url = new URL(window.location.href);
        url.searchParams.set("type", activeLikeType);
        window.history.pushState({ type: activeLikeType }, "", url);
    }

    configureLikeTypeView();

    const savedState = likeState[activeLikeType];

    if (savedState.data) {
        renderLikes(activeLikeType, savedState.data);
        window.requestAnimationFrame(() => {
            window.scrollTo({ top: savedState.scrollY, behavior: "auto" });
        });

        return;
    }

    loadLikes(activeLikeType, savedState.pageNo);
}

/** 활성 탭의 제목과 접근성 상태 갱신 */
function configureLikeTypeView() {

    const isCollection = activeLikeType === "collection";

    document.querySelectorAll("#likeTabs [data-type]")
        .forEach((tabLink) => {
            const selected = tabLink.dataset.type === activeLikeType;
            tabLink.classList.toggle("active", selected);
            tabLink.setAttribute("aria-selected", String(selected));

            if (selected) {
                tabLink.setAttribute("aria-current", "page");
            } else {
                tabLink.removeAttribute("aria-current");
            }
        });

    document.querySelector("#likeTitleText").textContent = isCollection
        ? "좋아요한 컬렉션"
        : "좋아요한 인물";
    document.querySelector("#likeTotalCount").textContent = "0";
    document.querySelector("#likeEmptyMessage").textContent = isCollection
        ? "아직 좋아요한 컬렉션이 없습니다."
        : "아직 좋아요한 인물이 없습니다.";
    document.querySelector("#likeTabs").style.setProperty(
        "--active-tab-index",
        String(LIKE_TYPES.indexOf(activeLikeType))
    );
}

/** ===================================
 *  좋아요 API 조회
 *  =================================== */

/** 유형별 좋아요 목록 조회 */
async function loadLikes(type, pageNo) {

    const requestId = ++requestSequence;

    likeState[type].pageNo = pageNo;
    showLikeLoading();

    try {
        const requestParam = {
            type,
            page: pageNo,
            size: LIKE_PAGE_SIZES[type],
            sort: "latest"
        };

        const data = await requestGet(
            `/api/users/${memberId}/likes`,
            requestParam
        );

        likeState[type].data = data;

        if (requestId === requestSequence && activeLikeType === type) {
            renderLikes(type, data);
        }
    } catch (error) {
        if (requestId === requestSequence && activeLikeType === type) {
            showLikeError(error.message);
        }
    }
}

/** ===================================
 *  좋아요 결과 및 인물 카드
 *  =================================== */

/** 유형별 조회 결과 렌더링 */
function renderLikes(type, data) {

    const items = Array.isArray(data.items) ? data.items : [];
    const page = data.page || {};
    const totalCount = Number(page.totalCnt || 0);

    hideLikeStatus();
    document.querySelector("#likeTotalCount").textContent = String(totalCount);

    if (items.length === 0) {
        document.querySelector("#likeEmpty").classList.remove("d-none");
        renderPagination(type, page, 1);
        return;
    }

    if (type === "collection") {
        renderCollectionCards(items);
    } else {
        renderPersonCards(items);
    }

    appendLikePlaceholders(type, items.length, totalCount);

    renderPagination(
        type,
        page,
        Number(page.pageNo || likeState[type].pageNo)
    );
}

/** 인물 카드 목록 렌더링 */
function renderPersonCards(items) {

    const likeList = document.querySelector("#likeList");

    likeList.className = "member-card-grid member-person-like-grid";
    likeList.replaceChildren();

    items.forEach((item) => {
        const article = document.createElement("article");
        const link = document.createElement("a");
        const body = document.createElement("div");
        const name = document.createElement("h3");
        const personInfo = document.createElement("p");
        const latestContent = document.createElement("p");
        const personName = item.nameKo || item.nameOrg || `인물 ${item.personId}`;
        const roleLabel = LIKE_ROLE_LABELS[item.role] || "";
        const originalName = item.nameOrg && item.nameOrg !== personName
            ? item.nameOrg
            : "";

        article.className = "member-like-card";
        link.className = "member-person-card";
        link.href = `/people/${item.personId}`;
        body.className = "member-card-body";
        name.className = "member-card-title";
        personInfo.className = "member-card-meta member-person-info";
        latestContent.className = "member-card-meta member-person-latest";
        name.textContent = personName;
        personInfo.textContent = [roleLabel, originalName].filter(Boolean).join(" | ");
        latestContent.textContent = item.latestContentTitle || "";

        const image = item.profileImageUrl
            ? createPersonImage(item.profileImageUrl, personName)
            : createPersonPlaceholder(personName);

        body.append(name);

        if (personInfo.textContent) {
            body.append(personInfo);
        }

        if (latestContent.textContent) {
            body.append(latestContent);
        }

        link.append(image, body);
        article.append(link);
        likeList.append(article);
    });

    likeList.classList.remove("d-none");
}

/** ===================================
 *  컬렉션 카드
 *  =================================== */

/** 컬렉션 카드 목록 렌더링 */
function renderCollectionCards(items) {

    const likeList = document.querySelector("#likeList");

    likeList.className = "collection-card-grid";
    likeList.replaceChildren();

    items.forEach((collection) => {
        const article = createCollectionCard(collection);

        likeList.append(article);
    });

    likeList.classList.remove("d-none");
}

/** 마지막 페이지의 목록 높이를 유지하는 빈 카드 추가 */
function appendLikePlaceholders(type, itemCount, totalCount) {

    const pageSize = LIKE_PAGE_SIZES[type];

    if (totalCount <= pageSize || itemCount === 0 || itemCount >= pageSize) {
        return;
    }

    const likeList = document.querySelector("#likeList");
    const templates = Array.from(likeList.children);

    for (let index = itemCount;index < pageSize;index += 1) {

        const templateIndex = (index - itemCount) % templates.length;
        const placeholder = templates[templateIndex].cloneNode(true);

        placeholder.classList.add("member-like-placeholder");
        placeholder.setAttribute("aria-hidden", "true");
        placeholder.querySelectorAll("a, button").forEach((element) => {
            element.removeAttribute("href");
            element.tabIndex = -1;
        });

        likeList.append(placeholder);
    }
}

/** 컬렉션 링크 카드 생성 */
function createCollectionCard(collection) {

    const article = document.createElement("article");
    const link = document.createElement("a");
    const visual = createCollectionVisual(collection);
    const body = createCollectionBody(collection);
    const collectionTitle = collection.title || `컬렉션 ${collection.collectionId}`;

    article.className = "member-like-card collection-list-card";

    link.className = "collection-list-card-link";
    link.href = `/collections/${collection.collectionId}`;
    link.setAttribute("aria-label", `${collectionTitle} 컬렉션 보기`);
    link.append(visual, body);

    article.append(link);

    return article;
}

/** 컬렉션 대표 이미지 영역 생성 */
function createCollectionVisual(collection) {

    const visual = document.createElement("div");
    const previewPosters = [
        collection.previewPosterUrl1,
        collection.previewPosterUrl2,
        collection.previewPosterUrl3,
        collection.previewPosterUrl4,
        collection.previewPosterUrl5
    ].filter(Boolean);

    visual.className = "collection-list-card-visual";

    if (previewPosters.length > 0) {
        visual.classList.add("has-posters");
        visual.append(createCollectionPosterCollage(previewPosters, visual));
    }

    const isOwnedByCurrentMember = currentMemberId > 0
        && Number(collection.memberId) === currentMemberId;
    const label = document.createElement("span");
    const symbol = document.createElement("span");
    const visualCount = document.createElement("span");

    label.className = isOwnedByCurrentMember
        ? "collection-list-card-owner-badge"
        : "collection-list-card-label";
    label.textContent = isOwnedByCurrentMember ? "내 컬렉션" : "COLLECTION";
    symbol.className = "collection-list-card-symbol";
    symbol.setAttribute("aria-hidden", "true");
    symbol.innerHTML = '<i class="bi bi-collection-play"></i>';
    visualCount.className = "collection-list-card-visual-count";
    visualCount.textContent = `작품 ${Number(collection.itemCount || 0)}`;
    visual.append(label, symbol, visualCount);

    return visual;
}

/** 컬렉션 제목·작성자·통계 영역 생성 */
function createCollectionBody(collection) {

    const body = document.createElement("div");
    const title = document.createElement("h3");
    const titleText = document.createElement("span");
    const descriptionText = (collection.description || "").trim();
    const author = document.createElement("div");
    const nickname = collection.nickname || `회원 ${collection.memberId}`;
    const authorName = document.createElement("span");
    const stats = document.createElement("div");

    body.className = "collection-list-card-body";
    title.className = "collection-list-card-title";
    titleText.className = "collection-list-card-title-text";
    titleText.textContent = collection.title || `컬렉션 ${collection.collectionId}`;
    title.append(titleText);
    body.append(title);

    if (descriptionText) {
        const description = document.createElement("p");
        description.className = "collection-list-card-description";
        description.textContent = descriptionText;
        body.append(description);
    }

    author.className = "collection-list-card-author";
    authorName.textContent = nickname;
    author.append(
        createCollectionAuthorAvatar(collection.profileImgUrl, nickname),
        authorName
    );

    stats.className = "collection-list-card-stats";
    stats.append(
        createCollectionStat(
            collection.likedByCurrentMember ? "heart-fill" : "heart",
            "좋아요",
            collection.likeCount,
            collection.likedByCurrentMember
        ),
        createCollectionStat("chat", "코멘트", collection.commentCount)
    );
    body.append(author, stats);

    window.requestAnimationFrame(() => {
        configureScrollableCollectionTitle(title, titleText);
    });

    return body;
}

/** 컬렉션 대표 포스터 콜라주 생성 */
function createCollectionPosterCollage(posterUrls, visual) {

    const collage = document.createElement("div");
    const usesSevenSlotLayout = posterUrls.length === 5;
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

/** 컬렉션 작성자 프로필 요소 생성 */
function createCollectionAuthorAvatar(profileImgUrl, nickname) {

    const fallback = document.createElement("span");

    fallback.className = "collection-list-card-avatar collection-list-card-avatar-fallback";
    fallback.setAttribute("aria-hidden", "true");
    fallback.innerHTML = '<i class="bi bi-person-fill"></i>';

    if (!profileImgUrl) {
        return fallback;
    }

    const image = document.createElement("img");

    image.className = "collection-list-card-avatar";
    image.src = resolveProfileImageUrl(profileImgUrl);
    image.alt = "";
    image.loading = "lazy";
    image.decoding = "async";
    image.setAttribute("title", `${nickname} 프로필`);
    image.addEventListener("error", () => image.replaceWith(fallback));

    return image;
}

/** 컬렉션 통계 항목 생성 */
function createCollectionStat(icon, label, count, active = false) {

    const stat = document.createElement("span");
    const text = document.createElement("span");

    stat.classList.toggle("is-active", active);
    stat.innerHTML = `<i class="bi bi-${icon}" aria-hidden="true"></i>`;
    text.textContent = `${label} ${Number(count || 0)}`;
    stat.append(text);

    return stat;
}

/** 긴 컬렉션 제목의 이동 거리 계산 */
function configureScrollableCollectionTitle(title, titleText) {

    const overflowWidth = Math.ceil(
        titleText.getBoundingClientRect().width - title.clientWidth
    );
    const isOverflowing = overflowWidth > 0;

    title.classList.toggle("is-overflowing", isOverflowing);
    title.toggleAttribute("title", isOverflowing);

    if (!isOverflowing) {
        title.style.removeProperty("--collection-title-scroll-distance");
        title.style.removeProperty("--collection-title-scroll-duration");
        return;
    }

    title.title = titleText.textContent;
    title.style.setProperty("--collection-title-scroll-distance", `-${overflowWidth}px`);
    title.style.setProperty(
        "--collection-title-scroll-duration",
        `${Math.min(7, Math.max(2.4, overflowWidth / 45))}s`
    );
}

/** 컬렉션 작성자 프로필 이미지 주소 보정 */
function resolveProfileImageUrl(profileImgUrl) {

    try {
        return new URL(profileImgUrl, `${window.location.origin}/`).href;
    } catch {
        return profileImgUrl;
    }
}

/** ===================================
 *  인물 프로필 이미지
 *  =================================== */

/** 인물 프로필 이미지 생성 */
function createPersonImage(imageUrl, personName) {

    const image = document.createElement("img");

    image.className = "member-person-image";
    image.src = UserListUi.resolveTmdbImageUrl(imageUrl, "w500");
    image.alt = `${personName} 프로필`;
    image.addEventListener("error", () => {
        image.replaceWith(createPersonPlaceholder(personName));
    });

    return image;
}

/** 인물 프로필 대체 요소 생성 */
function createPersonPlaceholder(personName) {

    const placeholder = document.createElement("div");
    placeholder.className = "member-person-placeholder";
    placeholder.textContent = personName;

    return placeholder;
}

/** ===================================
 *  페이지네이션
 *  =================================== */

/** 좋아요 페이지네이션 렌더링 */
function renderPagination(type, page, selectedPage) {

    const navigation = document.querySelector("#likePaginationNavigation");
    const pagination = document.querySelector("#likePagination");

    UserListUi.renderPagination({
        container: pagination,
        page,
        currentPage: selectedPage,
        defaultPageSize: LIKE_PAGE_SIZES[type],
        maxVisiblePages: LIKE_PAGINATION_GROUP_SIZE,
        onPageChange: (pageNo) => {
            loadLikes(type, pageNo);
            document.querySelector("#likeTitle").scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }
    });

    const pageItems = pagination.querySelectorAll(".page-item");
    const hasPagination = pageItems.length >= 2;
    navigation.classList.toggle("d-none", !hasPagination);

    if (!hasPagination) {
        return;
    }

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
    renderPaginationIndicator(type, pagination, numberItems);
}

/** 활성 페이지 이동 표시선 생성 */
function renderPaginationIndicator(type, pagination, numberItems) {

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
    const previousState = likePaginationIndicatorState[type];
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

    void indicator.offsetWidth;
    window.requestAnimationFrame(() => {
        indicator.style.transform = `translateX(${activeRect.left - firstRect.left}px)`;
    });

    likePaginationIndicatorState[type] = { startPage, activeIndex };
}

/** ===================================
 *  화면 상태
 *  =================================== */

/** 좋아요 로딩 상태 표시 */
function showLikeLoading() {

    hideLikeStatus();
    document.querySelector("#likeLoading").classList.remove("d-none");
}

/** 좋아요 오류 상태 표시 */
function showLikeError(message) {

    hideLikeStatus();
    document.querySelector("#likeErrorMessage").textContent = message;
    document.querySelector("#likeError").classList.remove("d-none");
}

/** 좋아요 조회 상태 초기화 */
function hideLikeStatus() {

    document.querySelector("#likeLoading").classList.add("d-none");
    document.querySelector("#likeError").classList.add("d-none");
    document.querySelector("#likeEmpty").classList.add("d-none");
    document.querySelector("#likeList").classList.add("d-none");
    document.querySelector("#likePaginationNavigation").classList.add("d-none");
    document.querySelector("#likePagination").replaceChildren();
}
