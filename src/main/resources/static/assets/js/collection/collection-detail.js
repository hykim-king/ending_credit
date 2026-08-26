// View Controller가 body의 data-collection-id에 넣은 값을 모든 상세 API 요청에 사용한다.
const collectionId = Number(document.body.dataset.collectionId);

document.addEventListener("DOMContentLoaded", () => {
    document.querySelector("#editLink").href = `/collections/${collectionId}/edit`;
    document.querySelector("#addItemForm").addEventListener("submit", addItem);
    document.querySelector("#confirmDeleteButton").addEventListener("click", deleteCollection);

    // 컬렉션 기본 정보와 작품 목록은 서로 다른 API이므로 각각 조회한다.
    loadCollection();
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
        body.className = "card-body d-flex flex-column";

        const title = document.createElement("h3");
        title.className = "h6 card-title";
        title.textContent = item.titleKo || item.titleOrg || `콘텐츠 ${item.contentId}`;

        const info = document.createElement("p");
        info.className = "card-text text-secondary small flex-grow-1";
        info.textContent = item.releaseYear || item.titleOrg || "";

        const deleteButton = document.createElement("button");
        deleteButton.className = "btn btn-sm btn-outline-danger align-self-end";
        deleteButton.type = "button";
        deleteButton.textContent = "삭제";
        deleteButton.addEventListener("click", () => deleteItem(item.contentId));

        body.append(title, info, deleteButton);
        card.append(body);
        column.append(card);
        itemList.append(column);
    });
}

async function addItem(event) {
    // 입력 폼의 기본 submit을 막고 contentId를 JSON으로 전송한다.
    event.preventDefault();

    const errorMessage = document.querySelector("#errorMessage");
    const contentIdInput = document.querySelector("#contentId");

    hideDetailError(errorMessage);

    try {
        await requestPost(`/api/collections/${collectionId}/items`, {
            contentId: Number(contentIdInput.value)
        });

        contentIdInput.value = "";
        // 작품 추가 후 상세 집계(itemCount)와 작품 목록을 동시에 새로고침한다.
        await Promise.all([loadCollection(), loadItems(1)]);
    } catch (error) {
        showDetailError(errorMessage, error.message);
    }
}

async function deleteItem(contentId) {
    // 작품 삭제는 즉시 되돌릴 수 없으므로 브라우저 확인창을 한 번 거친다.
    if (!window.confirm("컬렉션에서 이 작품을 삭제하시겠습니까?")) {
        return;
    }

    const errorMessage = document.querySelector("#errorMessage");
    hideDetailError(errorMessage);

    try {
        await requestDelete(`/api/collections/${collectionId}/items/${contentId}`);
        // 삭제 후 작품 수와 목록을 함께 갱신해 화면과 DB 상태를 맞춘다.
        await Promise.all([loadCollection(), loadItems(1)]);
    } catch (error) {
        showDetailError(errorMessage, error.message);
    }
}

async function deleteCollection() {
    const errorMessage = document.querySelector("#errorMessage");
    const deleteButton = document.querySelector("#confirmDeleteButton");

    // 모달의 삭제 버튼을 잠가 같은 DELETE 요청이 중복 전송되는 것을 막는다.
    deleteButton.disabled = true;

    try {
        await requestDelete(`/api/collections/${collectionId}`);
        window.location.href = "/collections";
    } catch (error) {
        showDetailError(errorMessage, error.message);
        deleteButton.disabled = false;
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
