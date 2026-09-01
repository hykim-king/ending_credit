/**
 * Modification History
 * 2026. 8. 29. jinyoung - D-01 조회 전용·소유자 관리·좋아요 및 포스터 적용
 * 2026. 8. 31. jinyoung - 공개 링크 복사·코멘트 연결·작품 평균 별점 적용
 * 2026. 8. 31. jinyoung - 컬렉션 작품 카드를 영화 상세 화면과 연결
 * 2026. 9. 01. jinyoung - D-01·D-05 본문 UI와 상태 피드백 반영
 * 2026. 9. 01. jinyoung - 상세 상단 작품 포스터 콜라주 반영
 */
// 공통 layout을 사용하므로 담당 본문 루트의 값을 상세 조회와 권한 표시에 사용한다.
const detailPage = document.querySelector("#collectionDetailPage");
const collectionId = Number(detailPage.dataset.collectionId);
const currentMemberId = Number(detailPage.dataset.currentMemberId || 0);
let isOwner = false;
let isLiked = false;
let isPublicCollection = false;

document.addEventListener("DOMContentLoaded", initializeDetail);

async function initializeDetail() {
    document.querySelector("#editLink").href = `/collections/${collectionId}/edit`;
    document.querySelector("#commentsLink").href =
        `/comment/doRetrieve?collectionId=${collectionId}`;
    document.querySelector("#confirmDeleteButton").addEventListener("click", deleteCollection);
    document.querySelector("#likeButton").addEventListener("click", toggleLike);
    document.querySelector("#copyLinkButton").addEventListener("click", copyCollectionLink);

    // 소유자 여부를 먼저 확정한 뒤 상세 화면의 버튼 노출을 결정한다.
    await loadCollection();
    await loadItems(1);
}

/** 컬렉션 제목, 설명, 작성자와 집계 정보를 화면에 표시한다. */
async function loadCollection() {
    const errorMessage = document.querySelector("#errorMessage");

    try {
        const collection = await requestGet(`/api/collections/${collectionId}`);

        document.title = `${collection.title} - 컬렉션`;
        document.querySelector("#collectionTitle").textContent = collection.title;
        document.querySelector("#deleteCollectionTitle").textContent =
            collection.title || "이 컬렉션";
        document.querySelector("#collectionDescription").textContent =
            collection.description || "설명이 없습니다.";
        document.querySelector("#collectionAuthor").textContent =
            collection.nickname || `회원 ${collection.memberId}`;
        document.querySelector("#collectionDate").textContent =
            collection.updatedDt || collection.createdDt || "";
        document.querySelector("#itemCount").textContent = collection.itemCount || 0;
        document.querySelector("#likeCount").textContent = collection.likeCount || 0;
        document.querySelector("#commentCount").textContent = collection.commentCount || 0;

        const publicBadge = document.querySelector("#publicBadge");
        publicBadge.textContent = collection.isPublic === "Y" ? "공개" : "비공개";
        publicBadge.className = "collection-visibility-badge";

        isPublicCollection = collection.isPublic === "Y";
        isOwner = currentMemberId > 0 && currentMemberId === Number(collection.memberId);
        applyActionVisibility();

        if (currentMemberId > 0 && !isOwner) {
            await loadLikeStatus();
        }
    } catch (error) {
        showDetailError(errorMessage, error.message);
    }
}

/** 인증 여부와 소유자 여부에 따라 변경·좋아요 동작을 구분해 노출한다. */
function applyActionVisibility() {
    document.querySelector("#ownerActions").classList.toggle("d-none", !isOwner);
    document.querySelector("#copyLinkButton").classList.toggle(
        "d-none",
        !isPublicCollection
    );
    document.querySelector("#likeButton").classList.toggle(
        "d-none",
        currentMemberId <= 0 || isOwner
    );
}

/** 공개 컬렉션의 현재 상세 URL을 클립보드에 복사한다. */
async function copyCollectionLink() {
    const errorMessage = document.querySelector("#errorMessage");
    const feedback = document.querySelector("#copyLinkFeedback");
    hideDetailError(errorMessage);
    feedback.classList.add("d-none");

    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(window.location.href);
        } else {
            copyLinkWithTemporaryInput(window.location.href);
        }

        feedback.classList.remove("d-none");
        window.setTimeout(() => {
            feedback.classList.add("d-none");
        }, 2400);
    } catch (error) {
        showDetailError(errorMessage, "링크를 복사하지 못했습니다.");
    }
}

/** Clipboard API를 사용할 수 없는 환경을 위한 복사 대체 처리다. */
function copyLinkWithTemporaryInput(link) {
    const input = document.createElement("textarea");
    input.value = link;
    input.setAttribute("readonly", "");
    input.style.position = "fixed";
    input.style.opacity = "0";
    document.body.append(input);
    input.select();

    const copied = document.execCommand("copy");
    input.remove();

    if (!copied) {
        throw new Error("클립보드 복사 실패");
    }
}

/** 현재 회원의 좋아요 여부를 조회해 토글 버튼 상태를 맞춘다. */
async function loadLikeStatus() {
    const status = await requestGet(`/api/collections/${collectionId}/likes`);
    isLiked = status.liked === true;
    renderLikeButton();
}

function renderLikeButton() {
    const likeButton = document.querySelector("#likeButton");
    const icon = likeButton.querySelector("i");
    const label = document.querySelector("#likeButtonLabel");

    label.textContent = isLiked ? "좋아요 취소" : "좋아요";
    icon.className = isLiked ? "bi bi-heart-fill" : "bi bi-heart";
    likeButton.setAttribute("aria-pressed", String(isLiked));
}

/** 현재 상태에 따라 좋아요 등록 또는 취소 요청을 전송한다. */
async function toggleLike() {
    const errorMessage = document.querySelector("#errorMessage");
    const likeButton = document.querySelector("#likeButton");
    const likeCount = document.querySelector("#likeCount");
    const previousLiked = isLiked;
    const previousCount = Number(likeCount.textContent || 0);

    likeButton.disabled = true;
    hideDetailError(errorMessage);
    isLiked = !previousLiked;
    likeCount.textContent = String(
        Math.max(0, previousCount + (isLiked ? 1 : -1))
    );
    renderLikeButton();

    try {
        if (previousLiked) {
            await requestDelete(`/api/collections/${collectionId}/likes`);
        } else {
            await requestPost(`/api/collections/${collectionId}/likes`, {});
        }
    } catch (error) {
        isLiked = previousLiked;
        likeCount.textContent = String(previousCount);
        renderLikeButton();
        showDetailError(errorMessage, error.message);
    } finally {
        likeButton.disabled = false;
    }
}

/** 지정한 페이지의 컬렉션 작품을 조회하고 목록과 페이지 버튼을 갱신한다. */
async function loadItems(pageNo) {
    const errorMessage = document.querySelector("#errorMessage");
    const loading = document.querySelector("#itemLoading");

    loading.classList.remove("d-none");
    document.querySelector("#itemList").classList.add("d-none");
    document.querySelector("#itemEmpty").classList.add("d-none");

    try {
        const data = await requestGet(`/api/collections/${collectionId}/items`, {
            pageNo,
            pageSize: 12
        });

        const items = data.items || [];
        if (pageNo === 1) {
            renderDetailCover(items);
        }
        renderItems(items);
        renderItemPagination(data.page || {}, pageNo);
        document.querySelector("#itemResultCount").textContent =
            `${data.page?.totalCnt || 0}개`;
    } catch (error) {
        loading.classList.add("d-none");
        showDetailError(errorMessage, error.message);
    }
}

function renderDetailCover(items) {
    const cover = document.querySelector("#collectionDetailCover");
    const fallback = document.querySelector("#collectionDetailCoverFallback");
    const posterItems = items
        .filter((item) => item.posterUrl)
        .slice(0, 5);

    cover.querySelector(".collection-detail-poster-collage")?.remove();
    fallback.classList.toggle("d-none", posterItems.length > 0);

    if (posterItems.length === 0) {
        cover.classList.remove("has-posters");
        return;
    }

    const collage = document.createElement("div");
    collage.className = "collection-detail-poster-collage";
    collage.style.setProperty("--poster-count", String(posterItems.length));

    posterItems.forEach((item) => {
        const poster = document.createElement("img");
        poster.src = resolveDetailPosterUrl(item.posterUrl);
        poster.alt = "";
        poster.addEventListener("error", () => {
            poster.remove();
            const remainingPosters = collage.querySelectorAll("img").length;
            collage.style.setProperty("--poster-count", String(remainingPosters || 1));
            if (remainingPosters === 0) {
                collage.remove();
                cover.classList.remove("has-posters");
                fallback.classList.remove("d-none");
            }
        });
        collage.append(poster);
    });

    cover.classList.add("has-posters");
    cover.append(collage);
}

function renderItems(items) {
    const itemList = document.querySelector("#itemList");
    const itemEmpty = document.querySelector("#itemEmpty");

    itemList.replaceChildren();
    document.querySelector("#itemLoading").classList.add("d-none");
    itemEmpty.classList.toggle("d-none", items.length > 0);
    itemList.classList.toggle("d-none", items.length === 0);

    if (items.length === 0) {
        return;
    }

    items.forEach((item) => {
        // 서버 데이터를 innerHTML로 조합하지 않고 textContent로 넣어 안전하게 표시한다.
        const link = document.createElement("a");
        link.className = "collection-movie-card";
        link.href = `/movies/${item.contentId}`;

        if (item.posterUrl) {
            const poster = document.createElement("img");
            poster.className = "collection-movie-poster";
            poster.src = resolveDetailPosterUrl(item.posterUrl);
            poster.alt = `${item.titleKo || item.titleOrg || "작품"} 포스터`;
            poster.addEventListener("error", () => {
                poster.replaceWith(createDetailPosterPlaceholder());
            });
            link.append(poster);
        } else {
            link.append(createDetailPosterPlaceholder());
        }

        const body = document.createElement("div");
        body.className = "collection-movie-info";

        const title = document.createElement("h3");
        title.className = "collection-movie-title";
        title.textContent = item.titleKo || item.titleOrg || `콘텐츠 ${item.contentId}`;

        const info = document.createElement("p");
        info.className = "collection-movie-meta";
        const releaseYear = item.releaseYear || "개봉연도 정보 없음";
        const averageRating = item.averageRating == null
            ? "평가 없음"
            : `평균 ★${Number(item.averageRating).toFixed(1)}`;
        info.textContent = `${releaseYear} · ${averageRating}`;

        body.append(title, info);
        link.append(body);
        itemList.append(link);
    });
}

function createDetailPosterPlaceholder() {
    const placeholder = document.createElement("div");
    const icon = document.createElement("i");

    placeholder.className = "collection-movie-poster-placeholder";
    icon.className = "bi bi-film";
    icon.setAttribute("aria-hidden", "true");
    placeholder.append(icon);

    return placeholder;
}

async function deleteCollection() {
    const errorMessage = document.querySelector("#errorMessage");
    const deleteButton = document.querySelector("#confirmDeleteButton");

    // 모달의 삭제 버튼을 잠가 같은 DELETE 요청이 중복 전송되는 것을 막는다.
    deleteButton.disabled = true;
    deleteButton.textContent = "삭제 중...";

    try {
        await requestDelete(`/api/collections/${collectionId}`);
        window.location.href = currentMemberId > 0
            ? `/users/${currentMemberId}/records?tab=collections`
            : "/collections";
    } catch (error) {
        showDetailError(errorMessage, error.message);
        deleteButton.disabled = false;
        deleteButton.textContent = "삭제";
    }
}

function requestDelete(url) {
    // 공통 requestFetch를 사용하면 204 응답과 오류 JSON 처리를 다시 작성하지 않아도 된다.
    return requestFetch(url, {
        method: "DELETE",
        headers: {
            "Accept": "application/json",
            ...getCsrfHeaders()
        }
    });
}

function renderItemPagination(page, currentPage) {
    const pagination = document.querySelector("#itemPagination");
    pagination.replaceChildren();

    const pageSize = Number(page.pageSize || 12);
    const totalCount = Number(page.totalCnt || 0);
    const totalPages = Math.ceil(totalCount / pageSize);

    if (totalPages <= 1) {
        return;
    }

    // 목록 화면과 동일하게 열 개 단위의 페이지 버튼 블록을 구성한다.
    const startPage = Math.floor((currentPage - 1) / 10) * 10 + 1;
    const endPage = Math.min(startPage + 9, totalPages);

    pagination.append(createItemPageButton("이전", startPage - 1, startPage === 1, false));

    for (let pageNo = startPage; pageNo <= endPage; pageNo += 1) {
        pagination.append(createItemPageButton(
            String(pageNo),
            pageNo,
            false,
            pageNo === currentPage
        ));
    }

    pagination.append(createItemPageButton("다음", endPage + 1, endPage === totalPages, false));
}

function createItemPageButton(label, pageNo, disabled, active) {
    const item = document.createElement("li");
    item.className = `page-item${disabled ? " disabled" : ""}${active ? " active" : ""}`;

    const button = document.createElement("button");
    button.className = "page-link";
    button.type = "button";
    button.textContent = label;
    button.disabled = disabled;
    button.addEventListener("click", () => loadItems(pageNo));

    item.append(button);
    return item;
}

function showDetailError(element, message) {
    element.textContent = message;
    element.classList.remove("d-none");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function hideDetailError(element) {
    element.textContent = "";
    element.classList.add("d-none");
}

/** DB의 TMDB 상대 경로와 완전한 외부 URL을 모두 포스터로 표시한다. */
function resolveDetailPosterUrl(posterUrl) {
    if (/^https?:\/\//i.test(posterUrl)) {
        return posterUrl;
    }

    return `https://image.tmdb.org/t/p/w500${posterUrl}`;
}
