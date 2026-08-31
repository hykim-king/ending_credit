/**
 * Modification History
 * 2026. 8. 31. jinyoung - TMDB 상대 경로의 인물 프로필 이미지 표시 지원
 */
const LIKE_PAGE_SIZE = 12;

const memberId = Number(document.body.dataset.memberId);
const likeType = normalizeLikeType(
    document.body.dataset.initialType
    || new URLSearchParams(window.location.search).get("type")
);

let currentPage = 1;
let requestSequence = 0;

document.addEventListener("DOMContentLoaded", () => {
    configureLikeTypeView();

    document.querySelector("#likeRetryButton")
        .addEventListener("click", () => {
            loadLikes(currentPage);
        });

    if (!Number.isInteger(memberId) || memberId <= 0) {
        showLikeError("올바른 회원 번호가 필요합니다.");
        return;
    }

    loadLikes(1);
});

/** 지원하는 좋아요 유형을 보정하고 그 외의 값은 person으로 처리한다. */
function normalizeLikeType(type) {
    return String(type || "").trim().toLowerCase()
        === "collection"
        ? "collection"
        : "person";
}

/** 현재 유형에 맞는 탭과 화면 문구를 설정한다. */
function configureLikeTypeView() {
    const isCollection = likeType === "collection";
    const activeTab = document.querySelector(
        isCollection
            ? "#collectionLikeTab"
            : "#personLikeTab"
    );

    activeTab.classList.add("active");
    activeTab.setAttribute("aria-current", "page");

    document.querySelector("#likeTitle")
        .textContent = isCollection
            ? "좋아한 컬렉션"
            : "좋아한 인물";
    document.querySelector("#likeTotalCount")
        .textContent = isCollection ? "0개" : "0명";
    document.querySelector("#likeEmpty")
        .textContent = isCollection
            ? "아직 좋아요한 컬렉션이 없습니다."
            : "아직 좋아요한 인물이 없습니다.";
}

/** 지정한 페이지의 회원 좋아요 목록을 조회한다. */
async function loadLikes(pageNo) {
    const requestId = ++requestSequence;
    currentPage = pageNo;

    showLikeLoading();

    try {
        const requestParam = {
            type: likeType,
            page: pageNo,
            size: LIKE_PAGE_SIZE
        };

        if (likeType === "person") {
            requestParam.sort = "latest";
        }

        const data = await requestGet(
            `/api/users/${memberId}/likes`,
            requestParam
        );

        // 빠른 페이지 이동으로 이전 요청이 늦게 끝난 경우 화면을 덮지 않는다.
        if (requestId !== requestSequence) {
            return;
        }

        renderLikes(data);
    } catch (error) {
        if (requestId === requestSequence) {
            showLikeError(error.message);
        }
    }
}

/** 좋아요 목록, 전체 건수와 페이지 정보를 화면에 표시한다. */
function renderLikes(data) {
    const items = Array.isArray(data.items) ? data.items : [];
    const page = data.page || {};
    const totalCount = Number(page.totalCnt || 0);

    hideLikeStatus();

    document.querySelector("#likeTotalCount")
        .textContent = likeType === "collection"
            ? `${totalCount}개`
            : `${totalCount}명`;

    if (items.length === 0) {
        document.querySelector("#likeEmpty")
            .classList.remove("d-none");

        renderPagination(page, 1);
        return;
    }

    if (likeType === "collection") {
        renderCollectionCards(items);
    } else {
        renderPersonCards(items);
    }

    renderPagination(
        page,
        Number(page.pageNo || currentPage)
    );
}

/** 인물 좋아요 응답을 프로필 카드 목록으로 생성한다. */
function renderPersonCards(items) {
    const likeList = document.querySelector("#likeList");
    likeList.replaceChildren();

    items.forEach((item) => {
        const column = document.createElement("div");
        const link = document.createElement("a");
        const card = document.createElement("article");
        const body = document.createElement("div");
        const name = document.createElement("h3");
        const originalName = document.createElement("p");

        link.className = "text-decoration-none text-dark h-100";
        link.href = `/people/${item.personId}`;

        card.className = "card h-100 border-0 shadow-sm";
        body.className = "card-body text-center";
        name.className = "h6 card-title mb-1";
        originalName.className =
            "card-text text-secondary small text-truncate mb-0";

        const personName =
            item.nameKo
            || item.nameOrg
            || `인물 ${item.personId}`;

        name.textContent = personName;
        originalName.textContent =
            item.nameOrg && item.nameOrg !== item.nameKo
                ? item.nameOrg
                : "";

        const image = item.profileImageUrl
            ? createPersonImage(
                    item.profileImageUrl,
                    personName
            )
            : createPersonPlaceholder(personName);

        body.append(name, originalName);
        card.append(image, body);
        link.append(card);
        column.append(link);
        likeList.append(column);
    });

    likeList.classList.remove("d-none");
}

/** 컬렉션 좋아요 응답을 컬렉션 카드 목록으로 생성한다. */
function renderCollectionCards(items) {
    const likeList = document.querySelector("#likeList");
    likeList.replaceChildren();

    items.forEach((item) => {
        const column = document.createElement("div");
        const link = document.createElement("a");
        const card = document.createElement("article");
        const body = document.createElement("div");
        const header = document.createElement("div");
        const title = document.createElement("h3");
        const visibility = document.createElement("span");
        const description = document.createElement("p");
        const likedDate = document.createElement("p");

        link.className = "text-decoration-none text-dark h-100";
        link.href =
            `/collections/${item.collectionId}?memberId=${memberId}`;

        card.className = "card h-100 border-0 shadow-sm";
        body.className = "card-body d-flex flex-column";
        header.className =
            "d-flex justify-content-between align-items-start gap-2";
        title.className = "h6 card-title mb-2";
        visibility.className = "badge text-bg-light border";
        description.className =
            "card-text text-secondary small mb-3";
        likedDate.className =
            "card-text text-secondary small mt-auto mb-0";

        title.textContent =
            item.title || `컬렉션 ${item.collectionId}`;
        visibility.textContent =
            item.isPublic === "Y" ? "공개" : "비공개";
        description.textContent =
            item.description || "설명이 없습니다.";
        likedDate.textContent = item.likedDt
            ? `좋아요 ${item.likedDt}`
            : "";

        header.append(title, visibility);
        body.append(header, description, likedDate);
        card.append(body);
        link.append(card);
        column.append(link);
        likeList.append(column);
    });

    likeList.classList.remove("d-none");
}

/** 인물 프로필 이미지를 생성하고 실패 시 대체 영역으로 변경한다. */
function createPersonImage(imageUrl, personName) {
    const image = document.createElement("img");

    image.className = "card-img-top bg-secondary-subtle";
    image.src = resolveTmdbImageUrl(imageUrl, "w500");
    image.alt = `${personName} 프로필`;
    image.style.aspectRatio = "1 / 1";
    image.style.objectFit = "cover";

    image.addEventListener("error", () => {
        image.replaceWith(
            createPersonPlaceholder(personName)
        );
    });

    return image;
}

/** TMDB 상대 경로와 이미 완성된 외부 이미지 URL을 모두 표시한다. */
function resolveTmdbImageUrl(imageUrl, size) {
    if (/^https?:\/\//i.test(imageUrl)) {
        return imageUrl;
    }

    return `https://image.tmdb.org/t/p/${size}${imageUrl}`;
}

/** 프로필 이미지가 없는 인물의 대체 영역을 생성한다. */
function createPersonPlaceholder(personName) {
    const placeholder = document.createElement("div");

    placeholder.className =
        "card-img-top bg-secondary-subtle d-flex "
        + "align-items-center justify-content-center "
        + "text-secondary fw-semibold p-3";
    placeholder.style.aspectRatio = "1 / 1";
    placeholder.textContent = personName;

    return placeholder;
}

/** 전체 건수와 페이지 크기를 이용해 페이지 버튼을 생성한다. */
function renderPagination(page, selectedPage) {
    const pagination =
        document.querySelector("#likePagination");
    const pageSize =
        Number(page.pageSize || LIKE_PAGE_SIZE);
    const totalCount =
        Number(page.totalCnt || 0);
    const totalPages =
        Math.ceil(totalCount / pageSize);

    pagination.replaceChildren();

    if (totalPages <= 1) {
        return;
    }

    const startPage =
        Math.floor((selectedPage - 1) / 10) * 10 + 1;
    const endPage =
        Math.min(startPage + 9, totalPages);

    pagination.append(
        createPageButton(
            "이전",
            startPage - 1,
            startPage === 1,
            false
        )
    );

    for (
        let pageNo = startPage;
        pageNo <= endPage;
        pageNo += 1
    ) {
        pagination.append(
            createPageButton(
                String(pageNo),
                pageNo,
                false,
                pageNo === selectedPage
            )
        );
    }

    pagination.append(
        createPageButton(
            "다음",
            endPage + 1,
            endPage === totalPages,
            false
        )
    );
}

/** 좋아요 목록의 단일 페이지 버튼을 생성한다. */
function createPageButton(
    label,
    pageNo,
    disabled,
    selected
) {
    const item = document.createElement("li");
    const button = document.createElement("button");

    item.className =
        `page-item${disabled ? " disabled" : ""}`
        + `${selected ? " active" : ""}`;

    button.className = "page-link";
    button.type = "button";
    button.textContent = label;
    button.disabled = disabled;

    if (selected) {
        button.setAttribute("aria-current", "page");
    }

    button.addEventListener("click", () => {
        loadLikes(pageNo);

        document.querySelector("#likeTitle")
            .scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
    });

    item.append(button);

    return item;
}

/** 좋아요 목록 요청 중 상태를 표시한다. */
function showLikeLoading() {
    hideLikeStatus();

    document.querySelector("#likeLoading")
        .classList.remove("d-none");
}

/** 좋아요 목록 조회 오류를 표시한다. */
function showLikeError(message) {
    hideLikeStatus();

    document.querySelector("#likeErrorMessage")
        .textContent = message;
    document.querySelector("#likeError")
        .classList.remove("d-none");
}

/** loading, empty, error, list 상태를 모두 숨긴다. */
function hideLikeStatus() {
    document.querySelector("#likeLoading")
        .classList.add("d-none");
    document.querySelector("#likeError")
        .classList.add("d-none");
    document.querySelector("#likeEmpty")
        .classList.add("d-none");
    document.querySelector("#likeList")
        .classList.add("d-none");
    document.querySelector("#likePagination")
        .replaceChildren();
}
