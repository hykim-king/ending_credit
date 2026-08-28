// View Controller가 전달한 컬렉션 번호와 임시 회원 번호를 상세 API 요청에 사용한다.
const collectionId = Number(document.body.dataset.collectionId);
const memberId = Number(document.body.dataset.memberId);

let collectionLiked = false;
let likeRequestPending = false;

document.addEventListener("DOMContentLoaded", () => {
    const errorMessage = document.querySelector("#errorMessage");

    document.querySelector("#editLink").href =
        `/collections/${collectionId}/edit?memberId=${memberId}`;
    document.querySelector("#collectionLikeButton")
        .addEventListener("click", toggleCollectionLike);

    if (!Number.isInteger(collectionId) || collectionId <= 0
            || !Number.isInteger(memberId) || memberId <= 0) {
        showDetailError(errorMessage, "올바른 컬렉션 번호와 회원 번호가 필요합니다.");
        return;
    }

    // 컬렉션 기본 정보, 좋아요 상태와 작품 목록은 각각 조회한다.
    loadCollection();
    loadLikeStatus();
    loadItems(1);
});

/** 컬렉션 제목, 설명, 작성자와 집계 정보를 화면에 표시한다. */
async function loadCollection() {
    const errorMessage = document.querySelector("#errorMessage");

    try {
        const collection = await requestGet(`/api/collections/${collectionId}`);

        document.title = `${collection.title} - 컬렉션`;
        document.querySelector("#collectionTitle").textContent = collection.title;
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
        publicBadge.className = collection.isPublic === "Y"
            ? "badge text-bg-success"
            : "badge text-bg-secondary";
    } catch (error) {
        showDetailError(errorMessage, error.message);
    }
}

/** 임시 회원의 현재 컬렉션 좋아요 상태를 조회한다. */
async function loadLikeStatus() {
    const errorMessage = document.querySelector("#errorMessage");
    const likeButton = document.querySelector("#collectionLikeButton");
    likeButton.disabled = true;

    try {
        const data = await requestGet(
            `/api/collections/${collectionId}/likes`,
            { memberId }
        );

        collectionLiked = data.liked === true;
        renderCollectionLikeButton();
    } catch (error) {
        showDetailError(errorMessage, error.message);
    }
}

/** 현재 좋아요 상태에 맞게 버튼 문구와 스타일을 변경한다. */
function renderCollectionLikeButton() {
    const likeButton = document.querySelector("#collectionLikeButton");

    likeButton.className = collectionLiked
        ? "btn btn-danger"
        : "btn btn-outline-danger";
    likeButton.textContent = collectionLiked
        ? "♥ 좋아요 취소"
        : "♡ 좋아요";
    likeButton.setAttribute("aria-pressed", String(collectionLiked));
    likeButton.disabled = likeRequestPending;
}

/** 임시 회원 번호로 컬렉션 좋아요를 등록하거나 취소한다. */
async function toggleCollectionLike() {
    if (likeRequestPending) {
        return;
    }

    const errorMessage = document.querySelector("#errorMessage");
    likeRequestPending = true;
    hideDetailError(errorMessage);
    renderCollectionLikeButton();

    try {
        const url = `/api/collections/${collectionId}/likes`;

        if (collectionLiked) {
            await requestDelete(url, { memberId });
            collectionLiked = false;
        } else {
            await requestPost(url, { memberId });
            collectionLiked = true;
        }

        await loadCollection();
    } catch (error) {
        showDetailError(errorMessage, error.message);
    } finally {
        likeRequestPending = false;
        renderCollectionLikeButton();
    }
}

/** 지정한 페이지의 컬렉션 작품을 조회하고 목록과 페이지 버튼을 갱신한다. */
async function loadItems(pageNo) {
    const errorMessage = document.querySelector("#errorMessage");

    try {
        const data = await requestGet(`/api/collections/${collectionId}/items`, {
            pageNo,
            pageSize: 12
        });

        renderItems(data.items || []);
        renderItemPagination(data.page || {}, pageNo);
        document.querySelector("#itemResultCount").textContent =
            `${data.page?.totalCnt || 0}개`;
    } catch (error) {
        showDetailError(errorMessage, error.message);
    }
}

function renderItems(items) {
    const itemList = document.querySelector("#itemList");
    itemList.replaceChildren();

    if (items.length === 0) {
        const empty = document.createElement("div");
        empty.className = "col-12 alert alert-secondary text-center";
        empty.textContent = "컬렉션에 추가된 작품이 없습니다.";
        itemList.append(empty);
        return;
    }

    items.forEach((item) => {
        // 서버 데이터를 innerHTML로 조합하지 않고 textContent로 넣어 안전하게 표시한다.
        const column = document.createElement("div");
        column.className = "col-12 col-sm-6 col-lg-4 col-xl-3";

        const card = document.createElement("article");
        card.className = "card h-100 border-0 shadow-sm";

        if (item.posterUrl) {
            const poster = document.createElement("img");
            poster.className = "card-img-top";
            poster.src = item.posterUrl;
            poster.alt = `${item.titleKo || item.titleOrg || "작품"} 포스터`;
            card.append(poster);
        }

        const body = document.createElement("div");
        body.className = "card-body";

        const title = document.createElement("h3");
        title.className = "h6 card-title";
        title.textContent = item.titleKo || item.titleOrg || `콘텐츠 ${item.contentId}`;

        const info = document.createElement("p");
        info.className = "card-text text-secondary small mb-0";
        info.textContent = item.releaseYear || item.titleOrg || "";

        body.append(title, info);
        card.append(body);
        column.append(card);
        itemList.append(column);
    });
}

function requestDelete(url, data = null) {
    // 공통 requestFetch를 사용하면 204 응답과 오류 JSON 처리를 다시 작성하지 않아도 된다.
    const headers = {
        "Accept": "application/json",
        ...getCsrfHeaders()
    };
    const options = {
        method: "DELETE",
        headers
    };

    if (data) {
        headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(data);
    }

    return requestFetch(url, options);
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
