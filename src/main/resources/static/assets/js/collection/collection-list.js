// HTML 문서가 모두 만들어진 뒤 이벤트를 연결해야 querySelector 결과가 null이 되지 않는다.
document.addEventListener("DOMContentLoaded", () => {
    const searchForm = document.querySelector("#searchForm");
    const pageSize = document.querySelector("#pageSize");
    const query = new URLSearchParams(window.location.search);

    // 삭제 완료 후 작성자의 컬렉션 목록으로 돌아올 수 있도록 URL 검색 조건을 반영한다.
    if (query.has("searchDiv")) {
        document.querySelector("#searchDiv").value = query.get("searchDiv");
    }
    if (query.has("searchWord")) {
        document.querySelector("#searchWord").value = query.get("searchWord");
    }

    searchForm.addEventListener("submit", (event) => {
        event.preventDefault();
        loadCollections(1);
    });

    pageSize.addEventListener("change", () => loadCollections(1));

    // 최초 진입 시 첫 페이지를 자동 조회한다.
    loadCollections(1);
});

/**
 * REST API에서 컬렉션 목록을 가져와 목록과 페이지 버튼을 함께 갱신한다.
 * 검색 조건을 URL 쿼리스트링으로 전달하므로 새로고침이나 API 확인이 쉽다.
 */
async function loadCollections(pageNo) {
    const errorMessage = document.querySelector("#errorMessage");

    hideError(errorMessage);

    try {
        // 공통 requestGet이 객체를 URLSearchParams로 변환해 GET 요청을 보낸다.
        const data = await requestGet("/api/collections", {
            pageNo,
            pageSize: document.querySelector("#pageSize").value,
            searchDiv: document.querySelector("#searchDiv").value,
            searchWord: document.querySelector("#searchWord").value.trim()
        });

        renderCollections(data.items || []);
        renderPagination(data.page || {}, pageNo);

        // ?.는 page가 없을 때 오류를 막고, || 0은 화면 기본값을 보장한다.
        const totalCount = data.page?.totalCnt || 0;
        document.querySelector("#resultCount").textContent = `${totalCount}개`;
    } catch (error) {
        showError(errorMessage, error.message);
    }
}

function renderCollections(collections) {
    const collectionList = document.querySelector("#collectionList");
    collectionList.replaceChildren();

    if (collections.length === 0) {
        const empty = document.createElement("div");
        empty.className = "col-12 alert alert-secondary text-center";
        empty.textContent = "조회된 컬렉션이 없습니다.";
        collectionList.append(empty);
        return;
    }

    collections.forEach((collection) => {
        // innerHTML 문자열 대신 DOM API와 textContent를 사용해
        // 제목·설명에 HTML이 들어와도 스크립트로 실행되지 않게 한다.
        const column = document.createElement("div");
        column.className = "col-12 col-md-6 col-xl-4";

        const card = document.createElement("article");
        card.className = "card h-100 border-0 shadow-sm";

        const body = document.createElement("div");
        body.className = "card-body d-flex flex-column";

        const heading = document.createElement("h2");
        heading.className = "h5 card-title";

        const link = document.createElement("a");
        link.className = "stretched-link text-decoration-none text-dark";
        link.href = `/collections/${collection.collectionId}`;
        link.textContent = collection.title;
        heading.append(link);

        const description = document.createElement("p");
        description.className = "card-text text-secondary flex-grow-1";
        description.textContent = collection.description || "설명이 없습니다.";

        const meta = document.createElement("div");
        meta.className = "small text-secondary";
        meta.textContent = `${collection.nickname || `회원 ${collection.memberId}`} · 작품 ${collection.itemCount || 0} · 좋아요 ${collection.likeCount || 0}`;

        const visibility = document.createElement("span");
        visibility.className = collection.isPublic === "Y"
            ? "badge text-bg-success mt-3 align-self-start"
            : "badge text-bg-secondary mt-3 align-self-start";
        visibility.textContent = collection.isPublic === "Y" ? "공개" : "비공개";

        body.append(heading, description, meta, visibility);
        card.append(body);
        column.append(card);
        collectionList.append(column);
    });
}

function renderPagination(page, currentPage) {
    const pagination = document.querySelector("#pagination");
    pagination.replaceChildren();

    const pageSize = Number(page.pageSize || 12);
    const totalCount = Number(page.totalCnt || 0);
    const totalPages = Math.ceil(totalCount / pageSize);

    if (totalPages <= 1) {
        return;
    }

    // 페이지 버튼을 1~10, 11~20처럼 열 개 단위 블록으로 만든다.
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
    // 이전·숫자·다음 버튼의 중복 DOM 생성 코드를 한 함수로 모았다.
    const item = document.createElement("li");
    item.className = `page-item${disabled ? " disabled" : ""}${active ? " active" : ""}`;

    const button = document.createElement("button");
    button.className = "page-link";
    button.type = "button";
    button.textContent = label;
    button.disabled = disabled;
    button.addEventListener("click", () => loadCollections(pageNo));

    item.append(button);
    return item;
}

function showError(element, message) {
    // API 오류 메시지를 alert 대신 화면에 남겨 사용자가 원인을 다시 볼 수 있게 한다.
    element.textContent = message;
    element.classList.remove("d-none");
}

function hideError(element) {
    element.textContent = "";
    element.classList.add("d-none");
}
