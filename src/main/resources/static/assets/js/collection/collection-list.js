/**
 * Modification History
 * 2026. 8. 31. jinyoung - 컬렉션 목록 검색을 제목 전용으로 단일화
 * 2026. 9. 01. jinyoung - 컬렉션 검색 결과 카드·상태·반응형 본문 UI 반영
 * 2026. 9. 01. jinyoung - 컬렉션 내부 작품 포스터 콜라주 적용
 * 2026. 9. 01. jinyoung - 검색 결과 모드·작성자 프로필·7슬롯 포스터 모자이크 적용
 * 2026. 9. 02. jinyoung - 빈 설명 생략 및 현재 회원 소유 컬렉션 배지 적용
 */
let currentCollectionPage = 1;

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

    currentCollectionPage = Math.max(1, Number(query.get("pageNo")) || 1);
    setSearchResultMode((query.get("searchWord") || "").trim());

    searchForm.addEventListener("submit", (event) => {
        event.preventDefault();
        loadCollections(1);
    });

    pageSize.addEventListener("change", () => loadCollections(1));
    loadCollections(currentCollectionPage);
});

/** REST API에서 컬렉션 목록을 가져와 카드와 페이지 버튼을 갱신한다. */
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

        currentCollectionPage = pageNo;
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

function showLoading() {
    document.querySelector("#collectionLoading").classList.remove("d-none");
    document.querySelector("#collectionList").classList.add("d-none");
    document.querySelector("#collectionListEmpty").classList.add("d-none");
    document.querySelector("#paginationNavigation").classList.add("d-none");
}

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

    const label = document.createElement("span");
    label.className = "collection-list-card-label";
    label.textContent = "COLLECTION";

    const symbol = document.createElement("span");
    symbol.className = "collection-list-card-symbol";
    symbol.setAttribute("aria-hidden", "true");
    symbol.innerHTML = '<i class="bi bi-collection-play"></i>';

    const visualCount = document.createElement("span");
    visualCount.className = "collection-list-card-visual-count";
    visualCount.textContent = `작품 ${Number(collection.itemCount || 0)}`;

    visual.append(label, symbol, visualCount);

    if (Number(currentMemberId) > 0
            && Number(collection.memberId) === Number(currentMemberId)) {
        const ownerBadge = document.createElement("span");
        ownerBadge.className = "collection-list-card-owner-badge";
        ownerBadge.textContent = "내 컬렉션";
        label.remove();
        visual.append(ownerBadge);
    }

    const body = document.createElement("div");
    body.className = "collection-list-card-body";

    const title = document.createElement("h3");
    title.className = "collection-list-card-title";
    const titleText = document.createElement("span");
    titleText.className = "collection-list-card-title-text";
    titleText.textContent = collection.title;
    title.append(titleText);

    const description = document.createElement("p");
    description.className = "collection-list-card-description";
    description.textContent = (collection.description || "").trim();

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

    body.append(title, description, author, stats);
    link.append(visual, body);
    article.append(link);

    requestAnimationFrame(() => configureScrollableTitle(title, titleText));
    return article;
}

function configureScrollableTitle(title, titleText) {
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
    title.style.setProperty(
        "--collection-title-scroll-distance",
        `-${overflowWidth}px`
    );
    title.style.setProperty(
        "--collection-title-scroll-duration",
        `${Math.min(7, Math.max(2.4, overflowWidth / 45))}s`
    );
}

function createCollectionPosterCollage(posterUrls, visual) {
    const collage = document.createElement("div");
    const usesSevenSlotLayout = posterUrls.length === 5;
    const posterIndexes = usesSevenSlotLayout
        ? [0, 1, 2, 3, 3, 4, 4]
        : posterUrls.map((posterUrl, index) => index);

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

function resolveCollectionPosterUrl(posterUrl) {
    if (/^https?:\/\//i.test(posterUrl)) {
        return posterUrl;
    }

    return `https://image.tmdb.org/t/p/w342${posterUrl}`;
}

function resolveCollectionProfileUrl(profileImgUrl) {
    try {
        return new URL(profileImgUrl, `${window.location.origin}/`).href;
    } catch (error) {
        return profileImgUrl;
    }
}

function createStat(icon, label, count, active = false) {
    const stat = document.createElement("span");
    stat.classList.toggle("is-active", active);
    stat.innerHTML = `<i class="bi bi-${icon}" aria-hidden="true"></i>`;

    const text = document.createElement("span");
    text.textContent = `${label} ${Number(count || 0)}`;
    stat.append(text);
    return stat;
}

function updateResultHeading(searchWord, totalCount) {
    document.querySelector("#collectionResultHeading").textContent = searchWord
        ? `“${searchWord}” 검색 결과`
        : "전체 컬렉션";
    document.querySelector("#resultCount").textContent = `${totalCount}개`;
}

function setSearchResultMode(searchWord) {
    const collectionListPage = document.querySelector("#collectionListPage");
    const collectionListHero = document.querySelector(".collection-list-hero");
    const isSearchResult = Boolean(searchWord);

    collectionListPage.classList.toggle("is-search-result", isSearchResult);
    collectionListHero.classList.toggle("d-none", isSearchResult);
}

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

    const startPage = Math.floor((currentPage - 1) / 10) * 10 + 1;
    const endPage = Math.min(startPage + 9, totalPages);

    pagination.append(createPageItem("이전", startPage - 1, startPage === 1, false));

    for (let pageNo = startPage; pageNo <= endPage; pageNo += 1) {
        pagination.append(createPageItem(
            String(pageNo),
            pageNo,
            false,
            pageNo === currentPage
        ));
    }

    pagination.append(createPageItem("다음", endPage + 1, endPage === totalPages, false));
}

function createPageItem(label, pageNo, disabled, active) {
    const item = document.createElement("li");
    item.className = `page-item${disabled ? " disabled" : ""}${active ? " active" : ""}`;

    const button = document.createElement("button");
    button.className = "page-link";
    button.type = "button";
    button.textContent = label;
    button.disabled = disabled;
    button.setAttribute("aria-current", active ? "page" : "false");
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

function updateLocation(pageNo, pageSize, searchWord) {
    const query = new URLSearchParams();

    if (searchWord) {
        query.set("searchWord", searchWord);
    }
    if (pageNo > 1) {
        query.set("pageNo", String(pageNo));
    }
    if (pageSize !== "12") {
        query.set("pageSize", pageSize);
    }

    const queryString = query.toString();
    history.replaceState(null, "", queryString ? `/collections?${queryString}` : "/collections");
}

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

function hideError(element) {
    element.textContent = "";
    element.classList.add("d-none");
}
