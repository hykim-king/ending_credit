/**
 * Modification History
 * 2026. 8. 29. jinyoung - D-01 조회 전용·소유자 관리·좋아요 및 포스터 적용
 * 2026. 8. 31. jinyoung - 공개 링크 복사·코멘트 연결·작품 평균 별점 적용
 * 2026. 8. 31. jinyoung - 컬렉션 작품 카드를 영화 상세 화면과 연결
 * 2026. 9. 01. jinyoung - D-01·D-05 본문 UI와 상태 피드백 반영
 * 2026. 9. 01. jinyoung - 상세 상단 작품 포스터 콜라주 반영
 * 2026. 9. 01. jinyoung - 상세 통합 블록·보기 전환·더보기·댓글 이동 적용
 * 2026. 9. 02. jinyoung - 상세 반응형·소유자 액션·평균 및 내 평가 표시 개선
 */
// 공통 layout을 사용하므로 담당 본문 루트의 값을 상세 조회와 권한 표시에 사용한다.
const detailPage = document.querySelector("#collectionDetailPage");
const collectionId = Number(detailPage.dataset.collectionId);
const currentMemberId = Number(detailPage.dataset.currentMemberId || 0);
let isOwner = false;
let isLiked = false;
let isPublicCollection = false;
let currentItemPage = 0;
let totalItemCount = 0;
let currentViewMode = "grid";
let copyFeedbackTimer = null;

document.addEventListener("DOMContentLoaded", initializeDetail);

async function initializeDetail() {
    document.querySelector("#editLink").href = `/collections/${collectionId}/edit`;
    document.querySelector("#confirmDeleteButton").addEventListener("click", deleteCollection);
    document.querySelector("#likeButton").addEventListener("click", toggleLike);
    document.querySelector("#copyLinkButton").addEventListener("click", copyCollectionLink);
    document.querySelector("#commentsLink").addEventListener("click", scrollToComments);
    document.querySelector("#gridViewButton").addEventListener(
        "click",
        () => setMovieView("grid")
    );
    document.querySelector("#listViewButton").addEventListener(
        "click",
        () => setMovieView("list")
    );
    document.querySelector("#loadMoreButton").addEventListener("click", loadMoreItems);

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
        const description = collection.description || "";
        const descriptionElement = document.querySelector("#collectionDescription");
        descriptionElement.textContent = description;
        descriptionElement.classList.toggle("d-none", description.trim().length === 0);
        renderCollectionAuthor(collection);
        renderUpdatedDate(collection.updatedDt || collection.createdDt);
        document.querySelector("#itemResultCount").textContent =
            String(collection.itemCount || 0);
        document.querySelector("#likeCount").textContent = collection.likeCount || 0;
        document.querySelector("#commentCount").textContent = collection.commentCount || 0;
        document.querySelector("#commentSectionCount").textContent =
            collection.commentCount || 0;

        isPublicCollection = collection.isPublic === "Y";
        isOwner = currentMemberId > 0 && currentMemberId === Number(collection.memberId);
        document.querySelector("#privateBadge").classList.toggle(
            "d-none",
            isPublicCollection
        );
        applyActionVisibility();

        if (currentMemberId > 0 && !isOwner) {
            await loadLikeStatus();
        } else {
            renderLikeButton();
        }
    } catch (error) {
        showDetailError(errorMessage, error.message);
    }
}

/** 인증 여부와 소유자 여부에 따라 변경·좋아요 동작을 구분해 노출한다. */
function applyActionVisibility() {
    const likeButton = document.querySelector("#likeButton");

    document.querySelector("#ownerActions").classList.toggle("d-none", !isOwner);
    likeButton.disabled = currentMemberId <= 0;
    likeButton.title = currentMemberId <= 0
        ? "로그인 후 좋아요를 누를 수 있습니다."
        : (isOwner ? "자신의 컬렉션에는 좋아요를 누를 수 없습니다." : "");
}

/** 공개 컬렉션의 현재 상세 URL을 클립보드에 복사한다. */
async function copyCollectionLink() {
    const errorMessage = document.querySelector("#errorMessage");
    hideDetailError(errorMessage);

    if (!isPublicCollection) {
        showCopyFeedback("비공개 컬렉션은 링크를 공유할 수 없어요.");
        return;
    }

    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(window.location.href);
        } else {
            copyLinkWithTemporaryInput(window.location.href);
        }

        showCopyFeedback("링크를 복사했어요");
    } catch (error) {
        showDetailError(errorMessage, "링크를 복사하지 못했습니다.");
    }
}

function showCopyFeedback(message) {
    const feedback = document.querySelector("#copyLinkFeedback");
    feedback.textContent = message;
    window.clearTimeout(copyFeedbackTimer);
    feedback.classList.remove("is-visible");
    window.requestAnimationFrame(() => feedback.classList.add("is-visible"));
    copyFeedbackTimer = window.setTimeout(() => {
        feedback.classList.remove("is-visible");
    }, 2400);
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

    label.textContent = "좋아요";
    icon.className = isLiked ? "bi bi-heart-fill" : "bi bi-heart";
    likeButton.setAttribute("aria-pressed", String(isLiked));
    likeButton.setAttribute("aria-label", isLiked ? "좋아요 취소" : "좋아요");
}

/** 현재 상태에 따라 좋아요 등록 또는 취소 요청을 전송한다. */
async function toggleLike() {
    const errorMessage = document.querySelector("#errorMessage");
    const likeButton = document.querySelector("#likeButton");
    const likeCount = document.querySelector("#likeCount");

    if (isOwner) {
        showCopyFeedback("내 컬렉션에는 좋아요를 누를 수 없어요.");
        return;
    }

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

/** 지정한 페이지의 컬렉션 작품을 조회해 기존 목록 뒤에 이어 붙인다. */
async function loadItems(pageNo) {
    const errorMessage = document.querySelector("#errorMessage");
    const loading = document.querySelector("#itemLoading");
    const itemList = document.querySelector("#itemList");
    const loadMoreButton = document.querySelector("#loadMoreButton");

    if (pageNo === 1) {
        currentItemPage = 0;
        totalItemCount = 0;
        itemList.replaceChildren();
        itemList.classList.add("d-none");
        loading.classList.remove("d-none");
        document.querySelector("#itemEmpty").classList.add("d-none");
        loadMoreButton.classList.add("d-none");
    } else {
        loadMoreButton.disabled = true;
        document.querySelector("#loadMoreLabel").textContent = "불러오는 중";
    }

    try {
        const data = await requestGet(`/api/collections/${collectionId}/items`, {
            pageNo,
            pageSize: 12
        });

        const items = data.items || [];
        if (pageNo === 1) {
            renderDetailCover(items);
        }
        renderItems(items, pageNo > 1);
        currentItemPage = pageNo;
        totalItemCount = Number(data.page?.totalCnt || 0);
        document.querySelector("#itemResultCount").textContent =
            String(totalItemCount);
        updateLoadMoreButton();
    } catch (error) {
        if (pageNo === 1) {
            loading.classList.add("d-none");
        } else {
            restoreLoadMoreButton();
        }
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
    collage.className =
        `collection-detail-poster-collage poster-count-${posterItems.length}`;
    posterItems.forEach((item) => {
        const poster = document.createElement("img");
        poster.src = resolveDetailPosterUrl(item.posterUrl);
        poster.alt = "";
        poster.addEventListener("error", () => {
            poster.remove();
            const remainingPosters = collage.querySelectorAll("img").length;
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

function renderItems(items, append) {
    const itemList = document.querySelector("#itemList");
    const itemEmpty = document.querySelector("#itemEmpty");

    if (!append) {
        itemList.replaceChildren();
    }
    document.querySelector("#itemLoading").classList.add("d-none");

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
        const metadata = [releaseYear];
        if (item.averageRating != null) {
            metadata.push(`평균 ★${Number(item.averageRating).toFixed(1)}`);
        }
        if (item.myRating != null) {
            metadata.push(`내 평가 ★${Number(item.myRating)}`);
        }
        info.textContent = metadata.join(" · ");

        body.append(title, info);
        link.append(body);
        itemList.append(link);
    });

    const hasItems = itemList.childElementCount > 0;
    itemEmpty.classList.toggle("d-none", hasItems);
    itemList.classList.toggle("d-none", !hasItems);
}

/** 현재까지 불러온 작품 뒤에 다음 12개를 추가한다. */
function loadMoreItems() {
    if (currentItemPage * 12 >= totalItemCount) {
        return;
    }

    loadItems(currentItemPage + 1);
}

function updateLoadMoreButton() {
    const loadMoreButton = document.querySelector("#loadMoreButton");
    const hasMore = currentItemPage * 12 < totalItemCount;

    restoreLoadMoreButton();
    loadMoreButton.classList.toggle("d-none", !hasMore);
}

function restoreLoadMoreButton() {
    const loadMoreButton = document.querySelector("#loadMoreButton");
    loadMoreButton.disabled = false;
    document.querySelector("#loadMoreLabel").textContent = "더보기";
}

/** 포스터형과 리스트형은 동일한 작품 정보를 레이아웃만 바꿔 표시한다. */
function setMovieView(viewMode) {
    const itemList = document.querySelector("#itemList");
    const gridViewButton = document.querySelector("#gridViewButton");
    const listViewButton = document.querySelector("#listViewButton");

    currentViewMode = viewMode === "list" ? "list" : "grid";
    itemList.classList.toggle("is-grid-view", currentViewMode === "grid");
    itemList.classList.toggle("is-list-view", currentViewMode === "list");
    gridViewButton.classList.toggle("is-active", currentViewMode === "grid");
    listViewButton.classList.toggle("is-active", currentViewMode === "list");
    gridViewButton.setAttribute("aria-pressed", String(currentViewMode === "grid"));
    listViewButton.setAttribute("aria-pressed", String(currentViewMode === "list"));
}

function scrollToComments() {
    const comments = document.querySelector("#collectionComments");
    comments.scrollIntoView({ behavior: "smooth", block: "start" });
    window.setTimeout(() => comments.focus({ preventScroll: true }), 450);
}

function renderCollectionAuthor(collection) {
    const nickname = collection.nickname || `회원 ${collection.memberId}`;
    const avatar = document.querySelector("#collectionAuthorAvatar");

    document.querySelector("#collectionAuthor").textContent = nickname;
    avatar.replaceChildren();
    avatar.className = "collection-cover-avatar collection-cover-avatar-fallback";

    if (!collection.profileImgUrl) {
        avatar.innerHTML = '<i class="bi bi-person-fill"></i>';
        return;
    }

    const image = document.createElement("img");
    image.src = resolveCollectionProfileUrl(collection.profileImgUrl);
    image.alt = "";
    image.addEventListener("error", () => {
        avatar.className = "collection-cover-avatar collection-cover-avatar-fallback";
        avatar.innerHTML = '<i class="bi bi-person-fill"></i>';
    });
    avatar.className = "collection-cover-avatar has-image";
    avatar.append(image);
}

function renderUpdatedDate(dateValue) {
    const dateElement = document.querySelector("#collectionDate");
    dateElement.textContent = formatRelativeUpdate(dateValue);
    dateElement.dateTime = dateValue ? dateValue.replace(" ", "T") : "";
}

function formatRelativeUpdate(dateValue) {
    if (!dateValue) {
        return "";
    }

    const updatedDate = new Date(dateValue.replace(" ", "T"));
    if (Number.isNaN(updatedDate.getTime())) {
        return "";
    }

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const updatedDay = new Date(
        updatedDate.getFullYear(),
        updatedDate.getMonth(),
        updatedDate.getDate()
    );
    const elapsedDays = Math.max(
        0,
        Math.floor((today.getTime() - updatedDay.getTime()) / 86400000)
    );

    if (elapsedDays === 0) {
        return "오늘 업데이트";
    }
    if (elapsedDays < 14) {
        return `${elapsedDays}일 전 업데이트`;
    }
    if (elapsedDays < 30) {
        return `${Math.floor(elapsedDays / 7)}주 전 업데이트`;
    }

    let elapsedMonths = (now.getFullYear() - updatedDate.getFullYear()) * 12
        + now.getMonth() - updatedDate.getMonth();
    if (now.getDate() < updatedDate.getDate()) {
        elapsedMonths -= 1;
    }
    elapsedMonths = Math.max(1, elapsedMonths);

    if (elapsedMonths < 12) {
        return `${elapsedMonths}달 전 업데이트`;
    }

    let elapsedYears = now.getFullYear() - updatedDate.getFullYear();
    if (now.getMonth() < updatedDate.getMonth()
            || (now.getMonth() === updatedDate.getMonth()
                && now.getDate() < updatedDate.getDate())) {
        elapsedYears -= 1;
    }

    return `${Math.max(1, elapsedYears)}년 전 업데이트`;
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

function resolveCollectionProfileUrl(profileImgUrl) {
    try {
        return new URL(profileImgUrl, `${window.location.origin}/`).href;
    } catch (error) {
        return profileImgUrl;
    }
}
