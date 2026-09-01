/**
 * Modification History
 * 2026. 8. 31. jinyoung - TMDB 상대 경로의 인물 프로필 이미지 표시 지원
 * 2026. 8. 31. jinyoung - 이미지 URL·페이지네이션 공통 UI 사용
 * 2026. 9. 01. jinyoung - U-07 인물·컬렉션 탭 전환과 본인 좋아요 취소 UI 반영
 */
const LIKE_PAGE_SIZE = 12;
const LIKE_TYPES = ["person", "collection"];
const likesPage = document.querySelector("#memberLikesPage");
const memberId = Number(likesPage.dataset.memberId);
const currentMemberId = Number(likesPage.dataset.currentMemberId || 0);
const isOwnList = memberId > 0 && memberId === currentMemberId;
const likeState = {
    person: { pageNo: 1, data: null, scrollY: 0 },
    collection: { pageNo: 1, data: null, scrollY: 0 }
};

let activeLikeType = normalizeLikeType(
    likesPage.dataset.initialType
    || new URLSearchParams(window.location.search).get("type")
);
let requestSequence = 0;

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

function normalizeLikeType(type) {
    return LIKE_TYPES.includes(String(type || "").trim().toLowerCase())
        ? String(type).trim().toLowerCase()
        : "person";
}

function changeLikeType(event) {
    event.preventDefault();
    switchLikeType(normalizeLikeType(event.currentTarget.dataset.type), true);
}

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

    document.querySelector("#likeTitle").textContent = isCollection
        ? "좋아한 컬렉션"
        : "좋아한 인물";
    document.querySelector("#likeTotalCount").textContent = isCollection
        ? "0개"
        : "0명";
    document.querySelector("#likeEmptyMessage").textContent = isCollection
        ? "아직 좋아요한 컬렉션이 없습니다."
        : "아직 좋아요한 인물이 없습니다.";
    document.title = isCollection ? "좋아한 컬렉션" : "좋아한 인물";
}

async function loadLikes(type, pageNo) {
    const requestType = type;
    const requestId = ++requestSequence;
    likeState[type].pageNo = pageNo;
    showLikeLoading();

    try {
        const requestParam = {
            type,
            page: pageNo,
            size: LIKE_PAGE_SIZE
        };

        if (type === "person") {
            requestParam.sort = "latest";
        }

        const data = await requestGet(
            `/api/users/${memberId}/likes`,
            requestParam
        );

        likeState[requestType].data = data;
        if (requestId === requestSequence && activeLikeType === requestType) {
            renderLikes(requestType, data);
        }
    } catch (error) {
        if (requestId === requestSequence && activeLikeType === requestType) {
            showLikeError(error.message);
        }
    }
}

function renderLikes(type, data) {
    const items = Array.isArray(data.items) ? data.items : [];
    const page = data.page || {};
    const totalCount = Number(page.totalCnt || 0);

    hideLikeStatus();
    document.querySelector("#likeTotalCount").textContent = type === "collection"
        ? `${totalCount}개`
        : `${totalCount}명`;

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

    renderPagination(
        type,
        page,
        Number(page.pageNo || likeState[type].pageNo)
    );
}

function renderPersonCards(items) {
    const likeList = document.querySelector("#likeList");
    likeList.replaceChildren();

    items.forEach((item) => {
        const article = document.createElement("article");
        const link = document.createElement("a");
        const body = document.createElement("div");
        const name = document.createElement("h3");
        const originalName = document.createElement("p");
        const likedDate = document.createElement("time");
        const personName = item.nameKo || item.nameOrg || `인물 ${item.personId}`;

        article.className = "member-like-card";
        link.className = "member-person-card";
        link.href = `/people/${item.personId}`;
        body.className = "member-card-body";
        name.className = "member-card-title";
        originalName.className = "member-card-meta";
        likedDate.className = "member-like-date";
        name.textContent = personName;
        originalName.textContent =
            item.nameOrg && item.nameOrg !== item.nameKo ? item.nameOrg : "";
        likedDate.textContent = formatLikedDate(item.createdDt);

        const image = item.profileImageUrl
            ? createPersonImage(item.profileImageUrl, personName)
            : createPersonPlaceholder(personName);

        body.append(name, originalName, likedDate);
        link.append(image, body);
        article.append(link);
        appendRemoveButton(article, "person", item.personId, personName, items.length);
        likeList.append(article);
    });

    likeList.classList.remove("d-none");
}

function renderCollectionCards(items) {
    const likeList = document.querySelector("#likeList");
    likeList.replaceChildren();

    items.forEach((item) => {
        const article = document.createElement("article");
        const link = document.createElement("a");
        const cover = document.createElement("div");
        const icon = document.createElement("i");
        const title = document.createElement("strong");
        const body = document.createElement("div");
        const description = document.createElement("p");
        const likedDate = document.createElement("time");
        const collectionTitle = item.title || `컬렉션 ${item.collectionId}`;

        article.className = "member-like-card";
        link.className = "member-liked-collection-card";
        link.href = `/collections/${item.collectionId}`;
        cover.className = "member-collection-cover";
        icon.className = "bi bi-collection";
        icon.setAttribute("aria-hidden", "true");
        title.textContent = collectionTitle;
        body.className = "member-card-body";
        description.className = "member-card-meta";
        description.textContent = item.description || "설명이 없습니다.";
        likedDate.className = "member-like-date";
        likedDate.textContent = formatLikedDate(item.likedDt);

        cover.append(icon, title);
        body.append(description, likedDate);
        link.append(cover, body);
        article.append(link);
        appendRemoveButton(
            article,
            "collection",
            item.collectionId,
            collectionTitle,
            items.length
        );
        likeList.append(article);
    });

    likeList.classList.remove("d-none");
}

function appendRemoveButton(container, type, targetId, targetName, itemCount) {
    if (!isOwnList) {
        return;
    }

    const button = document.createElement("button");
    const icon = document.createElement("i");

    button.className = "member-like-remove";
    button.type = "button";
    button.setAttribute("aria-label", `${targetName} 좋아요 취소`);
    icon.className = "bi bi-heart-fill";
    icon.setAttribute("aria-hidden", "true");
    button.append(icon);
    button.addEventListener("click", () => {
        removeLike(type, targetId, itemCount, button);
    });
    container.append(button);
}

async function removeLike(type, targetId, itemCount, button) {
    button.disabled = true;
    hideLikeError();

    try {
        const url = type === "person"
            ? `/api/people/${targetId}/likes`
            : `/api/collections/${targetId}/likes`;
        const headers = {
            "Accept": "application/json",
            ...getCsrfHeaders()
        };

        if (type === "person") {
            // 실제 인증 병합 전 PersonLikeController가 사용하는 임시 계약이다.
            headers["X-Member-Id"] = String(currentMemberId);
        }

        await requestFetch(url, { method: "DELETE", headers });

        likeState[type].data = null;
        const nextPage = itemCount === 1 && likeState[type].pageNo > 1
            ? likeState[type].pageNo - 1
            : likeState[type].pageNo;
        await loadLikes(type, nextPage);
    } catch (error) {
        button.disabled = false;
        showLikeError(error.message);
    }
}

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

function createPersonPlaceholder(personName) {
    const placeholder = document.createElement("div");
    placeholder.className = "member-person-placeholder";
    placeholder.textContent = personName;
    return placeholder;
}

function formatLikedDate(value) {
    return value ? `좋아요 ${String(value).substring(0, 10)}` : "";
}

function renderPagination(type, page, selectedPage) {
    UserListUi.renderPagination({
        container: document.querySelector("#likePagination"),
        page,
        currentPage: selectedPage,
        defaultPageSize: LIKE_PAGE_SIZE,
        onPageChange: (pageNo) => {
            loadLikes(type, pageNo);
            document.querySelector("#likeTitle").scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }
    });
}

function showLikeLoading() {
    hideLikeStatus();
    document.querySelector("#likeLoading").classList.remove("d-none");
}

function showLikeError(message) {
    hideLikeStatus();
    document.querySelector("#likeErrorMessage").textContent = message;
    document.querySelector("#likeError").classList.remove("d-none");
}

function hideLikeError() {
    document.querySelector("#likeError").classList.add("d-none");
}

function hideLikeStatus() {
    document.querySelector("#likeLoading").classList.add("d-none");
    document.querySelector("#likeError").classList.add("d-none");
    document.querySelector("#likeEmpty").classList.add("d-none");
    document.querySelector("#likeList").classList.add("d-none");
    document.querySelector("#likePagination").replaceChildren();
}
