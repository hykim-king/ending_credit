/**
 * Modification History
 * 2026. 8. 29. jinyoung - 상세 조회·소유자 작업·좋아요 적용
 * 2026. 8. 31. jinyoung - 링크 복사·댓글 연결·작품 평가 정보 적용
 * 2026. 9. 01. jinyoung - 포스터 콜라주·보기 전환·더보기 UI 적용
 * 2026. 9. 02. jinyoung - 반응형·소유자 작업·내 평가 표시 개선
 */
const ITEMS_PER_PAGE = 12;
const COPY_FEEDBACK_DURATION_MS = 2400;
const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;
const TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";

// 공통 레이아웃의 본문 루트에 서버가 기록한 식별값을 사용한다.
const detailPage = document.querySelector("#collectionDetailPage");
const collectionId = Number(detailPage.dataset.collectionId);
const currentMemberId = Number(detailPage.dataset.currentMemberId || 0);

let currentItemPage = 0;
let totalItemCount = 0;
let isOwner = false;
let isLiked = false;
let isPublicCollection = false;
let copyFeedbackTimer = null;

document.addEventListener("DOMContentLoaded", initializeDetail);

/** 상세 화면 이벤트를 연결하고 컬렉션과 작품을 차례로 불러온다. */
async function initializeDetail() {
    document.querySelector("#collectionBackLink").addEventListener("click", navigateBack);
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
    const collectionLoaded = await loadCollection();
    if (!collectionLoaded) {
        return;
    }
    await loadItems(1);
}

// 상세 정보와 사용자 동작

/** 방문 기록이 있으면 실제 이전 페이지로, 없으면 컬렉션 목록으로 이동한다. */
function navigateBack(event) {
    event.preventDefault();

    if (window.history.length > 1) {
        window.history.back();
        return;
    }

    window.location.href = event.currentTarget.href;
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

        return true;
    } catch (error) {
        showDetailError(errorMessage, error.message);
        return false;
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

/** 링크 복사 결과를 잠시 표시한다. */
function showCopyFeedback(message) {
    const feedback = document.querySelector("#copyLinkFeedback");
    feedback.textContent = message;
    window.clearTimeout(copyFeedbackTimer);
    feedback.classList.remove("is-visible");
    window.requestAnimationFrame(() => feedback.classList.add("is-visible"));
    copyFeedbackTimer = window.setTimeout(() => {
        feedback.classList.remove("is-visible");
    }, COPY_FEEDBACK_DURATION_MS);
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

/** 현재 좋아요 상태를 버튼 아이콘과 접근성 속성에 반영한다. */
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

// 작품 목록과 보기 방식

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
            pageSize: ITEMS_PER_PAGE
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

/** 첫 작품 목록의 포스터로 상세 상단 콜라주를 만든다. */
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
            collage.className =
                `collection-detail-poster-collage poster-count-${remainingPosters || 1}`;
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

/** 작품 목록을 기존 목록에 추가하거나 새로 그린다. */
function renderItems(items, appendResults) {
    const itemList = document.querySelector("#itemList");
    const itemEmpty = document.querySelector("#itemEmpty");

    if (!appendResults) {
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
    if (currentItemPage * ITEMS_PER_PAGE >= totalItemCount) {
        return;
    }

    loadItems(currentItemPage + 1);
}

/** 다음 작품 페이지가 있을 때만 더보기 버튼을 표시한다. */
function updateLoadMoreButton() {
    const loadMoreButton = document.querySelector("#loadMoreButton");
    const hasMore = currentItemPage * ITEMS_PER_PAGE < totalItemCount;

    restoreLoadMoreButton();
    loadMoreButton.classList.toggle("d-none", !hasMore);
}

/** 더보기 버튼을 기본 상태로 되돌린다. */
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

    const normalizedViewMode = viewMode === "list" ? "list" : "grid";
    const isGridView = normalizedViewMode === "grid";

    itemList.classList.toggle("is-grid-view", isGridView);
    itemList.classList.toggle("is-list-view", !isGridView);
    gridViewButton.classList.toggle("is-active", isGridView);
    listViewButton.classList.toggle("is-active", !isGridView);
    gridViewButton.setAttribute("aria-pressed", String(isGridView));
    listViewButton.setAttribute("aria-pressed", String(!isGridView));
}

/** 댓글 입력 영역으로 이동하고 키보드 초점을 맞춘다. */
function scrollToComments() {
    const comments = document.querySelector("#collectionComments");
    comments.scrollIntoView({ behavior: "smooth", block: "start" });
    window.setTimeout(() => comments.focus({ preventScroll: true }), 450);
}

/** 작성자 이름과 프로필 이미지를 표시한다. */
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

/** 수정 일시를 상대 시간 문구와 time 속성으로 표시한다. */
function renderUpdatedDate(dateValue) {
    const dateElement = document.querySelector("#collectionDate");
    dateElement.textContent = formatRelativeUpdate(dateValue);
    dateElement.dateTime = dateValue ? dateValue.replace(" ", "T") : "";
}

/** 저장된 날짜를 오늘·일·주·월·년 단위의 상대 시간으로 바꾼다. */
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
        Math.floor(
            (today.getTime() - updatedDay.getTime()) / MILLISECONDS_PER_DAY
        )
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

// 삭제와 공통 표시 도우미

/** 포스터가 없는 작품에 사용할 기본 영역을 만든다. */
function createDetailPosterPlaceholder() {
    const placeholder = document.createElement("div");
    const icon = document.createElement("i");

    placeholder.className = "collection-movie-poster-placeholder";
    icon.className = "bi bi-film";
    icon.setAttribute("aria-hidden", "true");
    placeholder.append(icon);

    return placeholder;
}

/** 삭제 요청이 성공하면 회원 컬렉션 목록으로 이동한다. */
async function deleteCollection() {
    const errorMessage = document.querySelector("#errorMessage");
    const deleteButton = document.querySelector("#confirmDeleteButton");

    // 모달의 삭제 버튼을 잠가 같은 DELETE 요청이 중복 전송되는 것을 막는다.
    deleteButton.disabled = true;
    deleteButton.textContent = "삭제 중...";

    try {
        await requestDelete(`/api/collections/${collectionId}`);
        window.location.href = currentMemberId > 0
            ? "/members/records?tab=collections"
            : "/collections";
    } catch (error) {
        showDetailError(errorMessage, error.message);
        deleteButton.disabled = false;
        deleteButton.textContent = "삭제";
    }
}

/** 공통 요청 함수로 DELETE 요청을 보낸다. */
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

/** 상세 오류를 표시하고 화면 상단으로 이동한다. */
function showDetailError(element, message) {
    element.textContent = message;
    element.classList.remove("d-none");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

/** 상세 오류 메시지를 숨긴다. */
function hideDetailError(element) {
    element.textContent = "";
    element.classList.add("d-none");
}

/** DB의 TMDB 상대 경로와 완전한 외부 URL을 모두 포스터로 표시한다. */
function resolveDetailPosterUrl(posterUrl) {
    if (/^https?:\/\//i.test(posterUrl)) {
        return posterUrl;
    }

    return `${TMDB_POSTER_BASE_URL}${posterUrl}`;
}

/** 프로필 이미지 경로를 현재 사이트 기준의 절대 URL로 바꾼다. */
function resolveCollectionProfileUrl(profileImgUrl) {
    try {
        return new URL(profileImgUrl, `${window.location.origin}/`).href;
    } catch {
        return profileImgUrl;
    }
}
