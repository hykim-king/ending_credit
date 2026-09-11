/**
 * Modification History
 * 2026. 8. 29. jinyoung - 상세 조회·소유자 작업·좋아요 적용
 * 2026. 8. 31. jinyoung - 링크 복사·댓글 연결·작품 평가 정보 적용
 * 2026. 9. 01. jinyoung - 포스터 콜라주·보기 전환·더보기 UI 적용
 * 2026. 9. 02. jinyoung - 반응형·소유자 작업·내 평가 표시 개선
 * 2026. 9. 05. jinyoung - 컬렉션 댓글 작성·대표 댓글·전체 댓글 모달 적용
 * 2026. 9. 07. jinyoung - 댓글 상대 작성일·좋아요·수정·삭제·신고 적용
 */
// ==================== 설정과 화면 상태 ====================

const ITEMS_PER_PAGE = 12;
const COMMENTS_PER_PAGE = 10;
const COMMENT_MAX_LENGTH = 1000;
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
let currentCommentsPage = 0;
let isCommentSubmitting = false;
let currentReportCommentId = 0;

// ==================== 화면 초기화 ====================

document.addEventListener("DOMContentLoaded", initializeDetail);

/** 상세 화면 이벤트를 연결하고 컬렉션과 작품을 차례로 불러온다. */
async function initializeDetail() {
    document.querySelector("#collectionBackLink").addEventListener("click", navigateBack);
    document.querySelector("#editLink").href = `/collections/${collectionId}/edit`;
    document.querySelector("#confirmDeleteButton").addEventListener("click", deleteCollection);
    document.querySelector("#likeButton").addEventListener("click", toggleLike);
    document.querySelector("#copyLinkButton").addEventListener("click", copyCollectionLink);
    document.querySelector("#commentsLink").addEventListener("click", scrollToComments);
    document.querySelector("#gridViewButton").addEventListener("click", () => setMovieView("grid"));
    document.querySelector("#listViewButton").addEventListener("click", () => setMovieView("list"));
    document.querySelector("#loadMoreButton").addEventListener("click", loadMoreItems);
    document.querySelector("#submitCommentReportButton").addEventListener("click", submitCommentReport);
    initializeCommentComposer();

    // 소유자 여부를 먼저 확정한 뒤 상세 화면의 버튼 노출을 결정한다.
    const collectionLoaded = await loadCollection();
    if (!collectionLoaded) {
        return;
    }
    await loadItems(1);
    await loadCommentPreview();
}

// ==================== 상세 정보·링크 공유·좋아요 ====================

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
        document.querySelector("#itemResultCount").textContent = String(collection.itemCount || 0);
        document.querySelector("#likeCount").textContent = collection.likeCount || 0;
        document.querySelector("#commentCount").textContent = collection.commentCount || 0;
        document.querySelector("#commentSectionCount").textContent = collection.commentCount || 0;

        isPublicCollection = collection.isPublic === "Y";
        isOwner = currentMemberId > 0 && currentMemberId === Number(collection.memberId);
        document.querySelector("#privateBadge").classList.toggle("d-none", isPublicCollection);
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
    likeButton.title = currentMemberId <= 0 ? "로그인 후 좋아요를 누를 수 있습니다." : (isOwner ? "자신의 컬렉션에는 좋아요를 누를 수 없습니다." : "");
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
    likeCount.textContent = String(Math.max(0, previousCount + (isLiked ? 1 : -1)));
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

// ==================== 작품 목록과 보기 방식 ====================

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
        document.querySelector("#itemResultCount").textContent = String(totalItemCount);
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
    const posterItems = items.filter((item) => item.posterUrl).slice(0, 5);

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

// ==================== 댓글 작성·목록·모달 ====================

/** 로그인 상태에 맞게 댓글 작성 영역과 이벤트를 초기화한다. */
function initializeCommentComposer() {
    const input = document.querySelector("#collectionCommentInput");
    const spoiler = document.querySelector("#collectionCommentSpoiler");

    if (currentMemberId <= 0) {
        input.readOnly = true;
        input.placeholder = "로그인 후, 컬렉션에 댓글을 남겨보세요.";
        spoiler.disabled = true;
    }

    input.addEventListener("beforeinput", guardCommentLength);
    input.addEventListener("paste", notifyLongCommentPaste);
    input.addEventListener("input", enforceCommentLength);
    document.querySelector("#submitCommentButton").addEventListener("click", submitComment);
    document.querySelector("#allCommentsButton").addEventListener("click", openAllCommentsModal);
    document.querySelector("#loadMoreCommentsButton").addEventListener("click", () => loadAllComments(currentCommentsPage + 1));
}

/** 입력 결과가 최대 글자 수를 넘는 키 입력을 막고 토스트를 표시한다. */
function guardCommentLength(event) {
    if (event.inputType.startsWith("delete") || event.data === null) {
        return;
    }

    const input = event.currentTarget;
    const selectionLength = input.selectionEnd - input.selectionStart;
    const nextLength = input.value.length - selectionLength + event.data.length;

    if (nextLength > COMMENT_MAX_LENGTH) {
        event.preventDefault();
        showCommentToast(`댓글은 최대 ${COMMENT_MAX_LENGTH.toLocaleString()}자까지 작성 가능해요.`);
    }
}

/** 붙여넣기로 최대 글자 수를 넘길 때 안내한다. */
function notifyLongCommentPaste(event) {
    const input = event.currentTarget;
    const pastedText = event.clipboardData?.getData("text") || "";
    const selectionLength = input.selectionEnd - input.selectionStart;

    if (input.value.length - selectionLength + pastedText.length > COMMENT_MAX_LENGTH) {
        showCommentToast(`댓글은 최대 ${COMMENT_MAX_LENGTH.toLocaleString()}자까지 작성 가능해요.`);
    }
}

/** 자동완성 등으로 제한을 넘긴 입력값을 자른다. */
function enforceCommentLength(event) {
    const input = event.currentTarget;

    if (input.value.length <= COMMENT_MAX_LENGTH) {
        return;
    }

    input.value = input.value.slice(0, COMMENT_MAX_LENGTH);
    showCommentToast(`댓글은 최대 ${COMMENT_MAX_LENGTH.toLocaleString()}자까지 작성 가능해요.`);
}

/** 로그인 회원의 댓글을 등록하고 대표 댓글과 집계를 갱신한다. */
async function submitComment() {
    if (currentMemberId <= 0) {
        showCommentLoginModal();
        return;
    }

    if (isCommentSubmitting) {
        return;
    }

    const input = document.querySelector("#collectionCommentInput");
    const spoiler = document.querySelector("#collectionCommentSpoiler");
    const commentDetail = input.value.trim();

    if (!commentDetail) {
        showCommentToast("댓글 내용을 입력해 주세요.");
        input.focus();
        return;
    }

    const submitButton = document.querySelector("#submitCommentButton");
    isCommentSubmitting = true;
    submitButton.disabled = true;
    submitButton.textContent = "등록 중";

    try {
        await requestPost(`/api/collections/${collectionId}/comments`, {
            commentDetail,
            spoiler: spoiler.checked ? "Y" : "N"
        });

        input.value = "";
        spoiler.checked = false;
        await loadCommentPreview();
        showCommentToast("댓글을 등록했어요.");
    } catch (error) {
        showCommentToast(error.message);
    } finally {
        isCommentSubmitting = false;
        submitButton.disabled = false;
        submitButton.textContent = "등록";
    }
}

/** 대표 댓글 한 건과 전체 댓글 수를 조회한다. */
async function loadCommentPreview() {
    try {
        const data = await requestGet(`/api/collections/${collectionId}/comments`, {
            pageNo: 1,
            pageSize: 1
        });
        const items = data.items || [];
        const totalCount = Number(data.totalCount || 0);
        const empty = document.querySelector("#collectionCommentEmpty");
        const preview = document.querySelector("#collectionCommentPreview");

        updateCommentCounts(totalCount);
        empty.classList.toggle("d-none", totalCount > 0);
        preview.classList.toggle("d-none", items.length === 0);

        if (items.length > 0) {
            renderCommentCard(preview, items[0]);
        } else {
            preview.replaceChildren();
        }
    } catch (error) {
        showCommentToast(error.message);
    }
}

/** 전체 댓글 모달을 열고 첫 페이지를 조회한다. */
async function openAllCommentsModal() {
    currentCommentsPage = 0;
    document.querySelector("#allCommentsList").replaceChildren();
    document.querySelector("#allCommentsEmpty").classList.add("d-none");
    document.querySelector("#loadMoreCommentsButton").classList.add("d-none");

    bootstrap.Modal.getOrCreateInstance(document.querySelector("#allCommentsModal")).show();
    await loadAllComments(1);
}

/** 전체 댓글 모달에 지정한 페이지를 추가한다. */
async function loadAllComments(pageNo) {
    const loadMoreButton = document.querySelector("#loadMoreCommentsButton");
    loadMoreButton.disabled = true;
    loadMoreButton.textContent = "불러오는 중";

    try {
        const data = await requestGet(`/api/collections/${collectionId}/comments`, {
            pageNo,
            pageSize: COMMENTS_PER_PAGE
        });
        const list = document.querySelector("#allCommentsList");
        const items = data.items || [];
        const totalCount = Number(data.totalCount || 0);

        items.forEach((comment) => {
            const card = document.createElement("article");
            card.className = "collection-comment-card";
            renderCommentCard(card, comment);
            list.append(card);
        });

        currentCommentsPage = pageNo;
        updateCommentCounts(totalCount);
        document.querySelector("#allCommentsEmpty").classList.toggle("d-none", totalCount > 0);
        loadMoreButton.classList.toggle("d-none", data.hasNext !== true);
    } catch (error) {
        showCommentToast(error.message);
    } finally {
        loadMoreButton.disabled = false;
        loadMoreButton.textContent = "댓글 더보기";
    }
}

/** 댓글 한 건을 작성일·본문·좋아요·작성자별 작업과 함께 만든다. */
function renderCommentCard(card, comment) {
    const layout = document.createElement("div");
    const avatar = createCommentAvatar(comment);
    const main = document.createElement("div");
    const meta = document.createElement("div");
    const nickname = document.createElement("strong");
    const createdDate = document.createElement("time");
    const detailWrap = document.createElement("div");
    const detail = document.createElement("div");
    const side = document.createElement("div");
    const likeButton = createCommentLikeButton(comment);
    const actions = createCommentActions(card, comment);

    card.dataset.commentId = String(comment.commentId);
    layout.className = "collection-comment-layout";
    main.className = "collection-comment-main";
    meta.className = "collection-comment-meta";
    nickname.textContent = comment.nickname || `회원 ${comment.memberId}`;
    createdDate.className = "collection-comment-date";
    createdDate.textContent = formatCommentRelativeDate(comment.createdDt);
    createdDate.dateTime = toCommentDate(comment.createdDt)?.toISOString() || "";
    meta.append(nickname, createdDate);

    detail.className = "collection-comment-detail";
    detailWrap.className = "collection-comment-detail-wrap";
    detailWrap.append(detail);
    renderCommentDetail(detail, comment, () => applyCommentOverflow(detailWrap, detail));
    main.append(meta, detailWrap);
    applyCommentOverflow(detailWrap, detail);
    side.className = "collection-comment-side";
    side.append(actions, likeButton);
    layout.append(avatar, main, side);
    card.replaceChildren(layout);
}

/** 댓글 좋아요 버튼을 만들고 현재 회원의 좋아요 상태를 반영한다. */
function createCommentLikeButton(comment) {
    const button = document.createElement("button");
    const icon = document.createElement("i");
    const label = document.createElement("span");
    const count = document.createElement("span");
    const liked = comment.likedByMember === true;

    button.type = "button";
    button.className = `collection-comment-like${liked ? " is-liked" : ""}`;
    button.setAttribute("aria-pressed", String(liked));
    button.setAttribute("aria-label", liked ? "댓글 좋아요 취소" : "댓글 좋아요");
    icon.className = liked ? "bi bi-heart-fill" : "bi bi-heart";
    icon.setAttribute("aria-hidden", "true");
    label.textContent = "좋아요";
    count.className = "collection-comment-like-count";
    count.textContent = String(Number(comment.likeCnt || 0));
    button.append(icon, label, count);
    button.addEventListener("click", () => toggleCommentLike(comment.commentId, button));

    return button;
}

/** 본인 댓글에는 수정·삭제, 다른 회원 댓글에는 신고 버튼을 만든다. */
function createCommentActions(card, comment) {
    const actions = document.createElement("div");
    const isMine = currentMemberId > 0 && currentMemberId === Number(comment.memberId);
    actions.className = "collection-comment-actions";

    if (isMine) {
        actions.append(
            createCommentActionButton("bi-pencil", "수정", () => renderCommentEditor(card, comment), false, true),
            createCommentActionButton("bi-trash3", "삭제", () => deleteComment(comment.commentId), true, true)
        );
    } else {
        actions.append(createCommentActionButton("bi-flag", "신고하기", () => openCommentReportModal(comment.commentId)));
    }

    return actions;
}

/** 댓글 작업 버튼의 공통 마크업을 만든다. */
function createCommentActionButton(iconClass, label, clickHandler, danger = false, iconOnly = false) {
    const button = document.createElement("button");
    const icon = document.createElement("i");

    button.type = "button";
    button.className = `collection-comment-action${danger ? " is-danger" : ""}${iconOnly ? " is-icon-only" : ""}`;
    icon.className = `bi ${iconClass}`;
    icon.setAttribute("aria-hidden", "true");
    button.append(icon);
    if (iconOnly) {
        button.setAttribute("aria-label", label);
        button.title = label;
    } else {
        button.append(document.createTextNode(label));
    }
    button.addEventListener("click", clickHandler);

    return button;
}

/** 댓글 카드를 내용과 스포일러 여부를 바꾸는 인라인 편집 상태로 전환한다. */
function renderCommentEditor(card, comment) {
    const editor = document.createElement("div");
    const textarea = document.createElement("textarea");
    const controls = document.createElement("div");
    const spoilerLabel = document.createElement("label");
    const spoiler = document.createElement("input");
    const buttons = document.createElement("div");
    const cancelButton = createCommentActionButton("bi-x-lg", "취소", () => renderCommentCard(card, comment));
    const saveButton = createCommentActionButton("bi-check-lg", "저장", () => updateComment(card, comment, textarea, spoiler));

    editor.className = "collection-comment-editor";
    textarea.maxLength = COMMENT_MAX_LENGTH;
    textarea.rows = 4;
    textarea.value = comment.commentDetail || "";
    textarea.setAttribute("aria-label", "댓글 내용 수정");
    controls.className = "collection-comment-editor-controls";
    spoilerLabel.className = "collection-comment-editor-spoiler";
    spoiler.type = "checkbox";
    spoiler.checked = comment.spoiler === "Y";
    spoilerLabel.append(spoiler, document.createTextNode("스포일러 포함"));
    buttons.className = "collection-comment-editor-buttons";
    saveButton.classList.add("is-primary");
    buttons.append(cancelButton, saveButton);
    controls.append(spoilerLabel, buttons);
    editor.append(textarea, controls);
    card.replaceChildren(editor);
    textarea.focus();
}

/** 수정된 댓글을 저장하고 현재 화면의 댓글 목록을 다시 불러온다. */
async function updateComment(card, comment, textarea, spoiler) {
    const commentDetail = textarea.value.trim();
    if (!commentDetail) {
        showCommentToast("댓글 내용을 입력해 주세요.");
        textarea.focus();
        return;
    }

    try {
        await requestJson(`/api/collections/${collectionId}/comments/${comment.commentId}`, "PUT", {
            commentDetail,
            spoiler: spoiler.checked ? "Y" : "N"
        });
        await refreshCommentLists();
        showCommentToast("댓글을 수정했어요.");
    } catch (error) {
        renderCommentCard(card, comment);
        showCommentToast(error.message);
    }
}

/** 댓글 삭제 확인 후 서버와 현재 댓글 목록을 갱신한다. */
async function deleteComment(commentId) {
    if (!window.confirm("댓글을 삭제할까요?")) {
        return;
    }

    try {
        await requestDelete(`/api/collections/${collectionId}/comments/${commentId}`);
        await refreshCommentLists();
        showCommentToast("댓글을 삭제했어요.");
    } catch (error) {
        showCommentToast(error.message);
    }
}

/** 댓글 좋아요를 토글하고 같은 댓글을 표시한 모든 카드의 상태를 맞춘다. */
async function toggleCommentLike(commentId, button) {
    if (currentMemberId <= 0) {
        showCommentLoginModal();
        return;
    }

    button.disabled = true;
    try {
        const result = await requestPost(`/api/collections/${collectionId}/comments/${commentId}/likes`, {});
        syncCommentLikeButtons(commentId, result.liked === true, Number(result.likeCount || 0));
    } catch (error) {
        showCommentToast(error.message);
    } finally {
        button.disabled = false;
    }
}

/** 대표 댓글과 전체 댓글에 중복 표시된 좋아요 버튼을 함께 갱신한다. */
function syncCommentLikeButtons(commentId, liked, likeCount) {
    document.querySelectorAll(`.collection-comment-card[data-comment-id="${commentId}"] .collection-comment-like`).forEach((button) => {
        button.classList.toggle("is-liked", liked);
        button.setAttribute("aria-pressed", String(liked));
        button.setAttribute("aria-label", liked ? "댓글 좋아요 취소" : "댓글 좋아요");
        button.querySelector("i").className = liked ? "bi bi-heart-fill" : "bi bi-heart";
        button.querySelector(".collection-comment-like-count").textContent = String(likeCount);
    });
}

/** 다른 회원의 댓글 신고 모달을 연다. */
function openCommentReportModal(commentId) {
    if (currentMemberId <= 0) {
        showCommentLoginModal();
        return;
    }

    currentReportCommentId = Number(commentId);
    document.querySelector("#collectionCommentReportReason").value = "SPOILER";
    document.querySelector("#collectionCommentReportDetail").value = "";
    const reportModal = bootstrap.Modal.getOrCreateInstance(document.querySelector("#collectionCommentReportModal"));
    const allCommentsElement = document.querySelector("#allCommentsModal");

    if (allCommentsElement.classList.contains("show")) {
        allCommentsElement.addEventListener("hidden.bs.modal", () => reportModal.show(), { once: true });
        bootstrap.Modal.getOrCreateInstance(allCommentsElement).hide();
        return;
    }

    reportModal.show();
}

/** 다른 댓글 모달이 열려 있으면 닫은 뒤 로그인 안내를 표시한다. */
function showCommentLoginModal() {
    const loginModal = bootstrap.Modal.getOrCreateInstance(document.querySelector("#commentLoginModal"));
    const allCommentsElement = document.querySelector("#allCommentsModal");

    if (allCommentsElement.classList.contains("show")) {
        allCommentsElement.addEventListener("hidden.bs.modal", () => loginModal.show(), { once: true });
        bootstrap.Modal.getOrCreateInstance(allCommentsElement).hide();
        return;
    }

    loginModal.show();
}

/** 선택한 사유로 다른 회원의 댓글 신고를 접수한다. */
async function submitCommentReport() {
    const reason = document.querySelector("#collectionCommentReportReason").value;
    const detail = document.querySelector("#collectionCommentReportDetail").value.trim();
    const submitButton = document.querySelector("#submitCommentReportButton");

    if (reason === "OTHER" && !detail) {
        showCommentToast("기타 신고 사유를 입력해 주세요.");
        document.querySelector("#collectionCommentReportDetail").focus();
        return;
    }

    submitButton.disabled = true;
    try {
        await requestPost(`/api/collections/${collectionId}/comments/${currentReportCommentId}/reports`, { reason, detail });
        bootstrap.Modal.getOrCreateInstance(document.querySelector("#collectionCommentReportModal")).hide();
        showCommentToast("댓글 신고를 접수했어요.");
    } catch (error) {
        showCommentToast(error.message);
    } finally {
        submitButton.disabled = false;
    }
}

/** 대표 댓글과 열려 있는 전체 댓글 목록을 서버 상태로 갱신한다. */
async function refreshCommentLists() {
    await loadCommentPreview();

    if (document.querySelector("#allCommentsModal").classList.contains("show")) {
        currentCommentsPage = 0;
        document.querySelector("#allCommentsList").replaceChildren();
        await loadAllComments(1);
    }
}

/** 서버 댓글 작성일을 브라우저에서 해석 가능한 Date로 변환한다. */
function toCommentDate(dateValue) {
    if (!dateValue) {
        return null;
    }

    const date = new Date(String(dateValue).trim().replaceAll("/", "-").replace(" ", "T"));
    return Number.isNaN(date.getTime()) ? null : date;
}

/** 댓글 작성일을 분·시간·일·달·년 단위 상대 시간으로 표시한다. */
function formatCommentRelativeDate(dateValue) {
    const createdDate = toCommentDate(dateValue);
    if (!createdDate) {
        return "";
    }

    const elapsedMilliseconds = Math.max(0, Date.now() - createdDate.getTime());
    const elapsedMinutes = Math.max(1, Math.floor(elapsedMilliseconds / (60 * 1000)));
    if (elapsedMinutes < 60) {
        return `${elapsedMinutes}분 전`;
    }

    const elapsedHours = Math.floor(elapsedMinutes / 60);
    if (elapsedHours < 24) {
        return `${elapsedHours}시간 전`;
    }

    const elapsedDays = Math.floor(elapsedHours / 24);
    if (elapsedDays < 30) {
        return `${elapsedDays}일 전`;
    }

    const elapsedMonths = Math.floor(elapsedDays / 30);
    if (elapsedMonths < 12) {
        return `${elapsedMonths}달 전`;
    }

    return `${Math.max(1, Math.floor(elapsedDays / 365))}년 전`;
}

/** 댓글 본문이 3줄을 넘을 때 더보기와 접기 버튼을 표시한다. */
function applyCommentOverflow(detailWrap, detail) {
    detailWrap.querySelector(".collection-comment-more")?.remove();
    detail.classList.remove("is-collapsed");
    detail.classList.remove("is-expanded");

    window.requestAnimationFrame(() => {
        const lineHeight = Number.parseFloat(window.getComputedStyle(detail).lineHeight);
        const threeLineHeight = lineHeight * 3;

        if (!Number.isFinite(lineHeight) || detail.scrollHeight <= threeLineHeight + 1) {
            return;
        }

        detail.classList.add("is-collapsed");
        const moreButton = document.createElement("button");
        moreButton.type = "button";
        moreButton.className = "collection-comment-more";
        moreButton.textContent = "더보기";
        moreButton.addEventListener("click", () => {
            const expanded = detail.classList.toggle("is-expanded");
            detail.classList.toggle("is-collapsed", !expanded);
            moreButton.textContent = expanded ? "접기" : "더보기";
        });
        detailWrap.append(moreButton);
    });
}

/** 신고 승인 및 스포일러 상태에 맞춰 댓글 본문을 표시한다. */
function renderCommentDetail(detail, comment, afterReveal) {
    if (comment.blindReason) {
        detail.classList.add("is-blinded");
        detail.textContent = comment.blindReason === "SPOILER" ? "스포일러 댓글입니다." : (comment.blindReason === "INAPPROPRIATE" ? "부적절한 댓글입니다."
                : "신고 승인된 댓글입니다.");
        return;
    }

    if (comment.spoiler !== "Y") {
        detail.textContent = comment.commentDetail || "";
        return;
    }

    const warning = document.createElement("span");
    const revealButton = document.createElement("button");
    warning.textContent = "스포일러가 있어요!";
    revealButton.type = "button";
    revealButton.className = "collection-comment-reveal";
    revealButton.textContent = "보기";
    revealButton.addEventListener("click", () => {
        detail.classList.remove("is-spoiler");
        detail.textContent = comment.commentDetail || "";
        afterReveal();
    });
    detail.classList.add("is-spoiler");
    detail.append(warning, revealButton);
}

/**
 * 댓글 작성자의 프로필 이미지 또는 기본 아바타를 만든다.
 * 작성자 번호가 있으면 프로필로 가는 링크로, 없으면 그냥 표시만 한다.
 * 본인 번호를 눌러도 서버가 /members/me 로 돌려보내므로 따로 가르지 않는다.
 */
function createCommentAvatar(comment) {
    const profileImgUrl = comment.profileImgUrl;
    const nickname = comment.nickname;
    const initial = (nickname || "회").trim().charAt(0) || "회";
    const avatar = document.createElement(comment.memberId ? "a" : "span");

    avatar.className = "collection-comment-avatar";

    if (comment.memberId) {
        avatar.href = `/members/${comment.memberId}`;
        avatar.setAttribute("aria-label", `${nickname || "회원"} 프로필 보기`);
    }

    if (!profileImgUrl) {
        avatar.classList.add("is-fallback");
        avatar.textContent = initial;
        return avatar;
    }

    const image = document.createElement("img");
    image.src = resolveCollectionProfileUrl(profileImgUrl);
    image.alt = "";
    image.addEventListener("error", () => {
        avatar.replaceChildren();
        avatar.classList.add("is-fallback");
        avatar.textContent = initial;
    });
    avatar.append(image);

    return avatar;
}

/** 상세 상단·댓글 영역·전체 댓글 모달의 댓글 수를 함께 갱신한다. */
function updateCommentCounts(totalCount) {
    const normalizedCount = Math.max(0, Number(totalCount || 0));
    document.querySelector("#commentCount").textContent = String(normalizedCount);
    document.querySelector("#commentSectionCount").textContent = String(normalizedCount);
    document.querySelector("#allCommentsCount").textContent = String(normalizedCount);
}

/** 컬렉션 댓글 관련 안내를 Bootstrap 토스트로 표시한다. */
function showCommentToast(message) {
    const toast = document.querySelector("#collectionCommentToast");
    toast.querySelector(".toast-body").textContent = message;
    bootstrap.Toast.getOrCreateInstance(toast).show();
}

// ==================== 작성자와 수정 일시 표시 ====================

/**
 * 작성자 이름과 프로필 이미지를 표시한다.
 * 아바타를 누르면 작성자 프로필로 간다. 본인 번호면 서버가 /members/me 로 돌려보낸다.
 */
function renderCollectionAuthor(collection) {
    const nickname = collection.nickname || `회원 ${collection.memberId}`;
    const avatar = document.querySelector("#collectionAuthorAvatar");

    document.querySelector("#collectionAuthor").textContent = nickname;

    // 작성자 번호가 없으면 링크로 만들지 않는다.
    if (collection.memberId) {
        avatar.href = `/members/${collection.memberId}`;
        avatar.setAttribute("aria-label", `${nickname} 프로필 보기`);
    } else {
        avatar.removeAttribute("href");
        avatar.removeAttribute("aria-label");
    }

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
    const updatedDay = new Date(updatedDate.getFullYear(), updatedDate.getMonth(), updatedDate.getDate());
    const elapsedDays = Math.max(0, Math.floor((today.getTime() - updatedDay.getTime()) / MILLISECONDS_PER_DAY
        ));

    if (elapsedDays === 0) {
        return "오늘 업데이트";
    }
    if (elapsedDays < 14) {
        return `${elapsedDays}일 전 업데이트`;
    }
    if (elapsedDays < 30) {
        return `${Math.floor(elapsedDays / 7)}주 전 업데이트`;
    }

    let elapsedMonths = (now.getFullYear() - updatedDate.getFullYear()) * 12 + now.getMonth() - updatedDate.getMonth();
    if (now.getDate() < updatedDate.getDate()) {
        elapsedMonths -= 1;
    }
    elapsedMonths = Math.max(1, elapsedMonths);

    if (elapsedMonths < 12) {
        return `${elapsedMonths}달 전 업데이트`;
    }

    let elapsedYears = now.getFullYear() - updatedDate.getFullYear();
    if (now.getMonth() < updatedDate.getMonth() || (now.getMonth() === updatedDate.getMonth()
                && now.getDate() < updatedDate.getDate())) {
        elapsedYears -= 1;
    }

    return `${Math.max(1, elapsedYears)}년 전 업데이트`;
}

// ==================== 삭제 요청과 표시 도우미 ====================

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
        window.location.href = currentMemberId > 0 ? "/members/records?tab=collections" : "/collections";
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

/** JSON 본문을 사용하는 수정 요청을 보낸다. */
function requestJson(url, method, data) {
    return requestFetch(url, {
        method,
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json",
            ...getCsrfHeaders()
        },
        body: JSON.stringify(data)
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
