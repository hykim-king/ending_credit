/**
 * Modification History
 * 2026. 8. 31. jinyoung - 컬렉션 목록 검색을 제목 전용으로 단일화
 * 2026. 9. 01. jinyoung - 컬렉션 검색 결과 카드·상태·반응형 본문 UI 반영
 * 2026. 9. 01. jinyoung - 컬렉션 내부 작품 포스터 콜라주 적용
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

        renderCollections(collections, searchWord);
        renderPagination(data.page || {}, pageNo);
        updateResultHeading(searchWord, totalCount);
        updateLocation(pageNo, pageSize, searchWord);
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

function renderCollections(collections, searchWord) {
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
        collectionList.append(createCollectionCard(collection));
    });
    collectionList.classList.remove("d-none");
}

function createCollectionCard(collection) {
    const article = document.createElement("article");
    article.className = "collection-list-card";

    const link = document.createElement("a");
    link.className = "collection-list-card-link";
    link.href = `/collections/${collection.collectionId}`;
    link.setAttribute("aria-label", `${collection.title} 컬렉션 보기`);

    const visual = document.createElement("div");
    const visualVariant = Math.abs(Number(collection.collectionId || 0)) % 4;
    visual.className = `collection-list-card-visual collection-visual-${visualVariant}`;

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

    const body = document.createElement("div");
    body.className = "collection-list-card-body";

    const title = document.createElement("h3");
    title.className = "collection-list-card-title";
    title.textContent = collection.title;

    const description = document.createElement("p");
    description.className = "collection-list-card-description";
    description.textContent = collection.description || "작성자가 남긴 설명이 없습니다.";

    const author = document.createElement("p");
    author.className = "collection-list-card-author";
    author.textContent = collection.nickname || `회원 ${collection.memberId}`;

    const stats = document.createElement("div");
    stats.className = "collection-list-card-stats";
    stats.append(
        createStat("heart", "좋아요", collection.likeCount),
        createStat("chat", "코멘트", collection.commentCount)
    );

    body.append(title, description, author, stats);
    link.append(visual, body);
    article.append(link);
    return article;
}

function createCollectionPosterCollage(posterUrls, visual) {
    const collage = document.createElement("div");
    collage.className = `collection-list-poster-collage poster-count-${posterUrls.length}`;

    posterUrls.forEach((posterUrl, index) => {
        const poster = document.createElement("img");
        poster.className = `collection-list-preview-poster poster-${index + 1}`;
        poster.src = resolveCollectionPosterUrl(posterUrl);
        poster.alt = "";
        poster.loading = "lazy";
        poster.addEventListener("error", () => {
            poster.remove();
            if (!collage.querySelector("img")) {
                collage.remove();
                visual.classList.remove("has-posters");
            }
        });
        collage.append(poster);
    });

    return collage;
}

function resolveCollectionPosterUrl(posterUrl) {
    if (/^https?:\/\//i.test(posterUrl)) {
        return posterUrl;
    }

    return `https://image.tmdb.org/t/p/w342${posterUrl}`;
}

function createStat(icon, label, count) {
    const stat = document.createElement("span");
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
}

function hideError(element) {
    element.textContent = "";
    element.classList.add("d-none");
}
