const initialSearchWord = document.body.dataset.searchWord || "";
const initialSearchDiv = document.body.dataset.searchDiv || "";
const initialPageNo = Number(document.body.dataset.pageNo || 1);
const initialPageSize = Number(document.body.dataset.pageSize || 12);

document.addEventListener("DOMContentLoaded", () => {
    document.querySelector("#searchWord").value = initialSearchWord;
    document.querySelector("#searchDiv").value = initialSearchDiv;
    document.querySelector("#pageSize").value = String(initialPageSize);

    document.querySelector("#searchForm").addEventListener("submit", (event) => {
        event.preventDefault();
        loadPeople(1);
    });

    document.querySelector("#pageSize").addEventListener("change", () => loadPeople(1));

    loadPeople(initialPageNo > 0 ? initialPageNo : 1);
});

/** 인물 목록 조회 */
async function loadPeople(pageNo) {
    const errorMessage = document.querySelector("#errorMessage");
    hideError(errorMessage);

    const searchWord = document.querySelector("#searchWord").value.trim();
    const searchDiv = document.querySelector("#searchDiv").value;
    const pageSize = document.querySelector("#pageSize").value;

    updateBrowserUrl(pageNo, searchWord, searchDiv, pageSize);

    try {
        const data = await requestGet("/api/people", {
            searchWord,
            searchDiv,
            pageNo,
            pageSize
        });

        renderPeople(data.items || []);
        renderPagination(data.page || {}, pageNo);

        const totalCount = data.page?.totalCnt || 0;
        document.querySelector("#resultCount").textContent = `${totalCount}명`;
        document.title = searchWord
            ? `${searchWord} 인물 검색 | ENDIT`
            : "인물 검색 | ENDIT";
    } catch (error) {
        showError(errorMessage, error.message || "인물 목록을 불러오지 못했습니다.");
    }
}

/** 인물 카드 목록 렌더링 */
function renderPeople(people) {
    const listEl = document.querySelector("#personList");
    listEl.replaceChildren();

    if (people.length === 0) {
        const empty = document.createElement("div");
        empty.className = "col-12 comment-card text-center text-muted py-5";
        empty.textContent = "검색된 인물이 없습니다.";
        listEl.append(empty);
        return;
    }

    people.forEach((person) => {
        listEl.append(createPersonCard(person));
    });
}

/** 인물 카드 1건 */
function createPersonCard(person) {
    const col = document.createElement("div");
    col.className = "col";

    const displayName = resolveDisplayName(person);
    const subName = person.nameOrg && person.nameOrg !== person.nameKo
        ? person.nameOrg
        : "";

    const card = document.createElement("article");
    card.className = "person-card h-100";

    const imageWrap = document.createElement("div");
    imageWrap.className = "person-card-image-wrap";

    if (person.profileImageUrl) {
        const image = document.createElement("img");
        image.className = "person-card-image";
        image.src = person.profileImageUrl;
        image.alt = `${displayName} 프로필`;
        imageWrap.append(image);
    } else {
        const placeholder = document.createElement("div");
        placeholder.className = "person-card-placeholder";
        placeholder.innerHTML = '<i class="bi bi-person-fill"></i>';
        imageWrap.append(placeholder);
    }

    const body = document.createElement("div");
    body.className = "person-card-body";

    const title = document.createElement("h2");
    title.className = "person-card-title";

    const link = document.createElement("a");
    link.className = "stretched-link text-decoration-none text-dark";
    link.href = `/people/${person.personId}`;
    link.textContent = displayName;
    title.append(link);

    const meta = document.createElement("p");
    meta.className = "person-card-meta mb-0";
    meta.textContent = subName;

    body.append(title, meta);
    card.append(imageWrap, body);
    col.append(card);

    return col;
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

    const startPage = Math.floor((currentPage - 1) / 10) * 10 + 1;
    const endPage = Math.min(startPage + 9, totalPages);

    pagination.append(createPageItem("이전", startPage - 1, startPage === 1, false));

    for (let pageNo = startPage; pageNo <= endPage; pageNo += 1) {
        pagination.append(createPageItem(String(pageNo), pageNo, false, pageNo === currentPage));
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
    button.addEventListener("click", () => loadPeople(pageNo));

    item.append(button);
    return item;
}

function updateBrowserUrl(pageNo, searchWord, searchDiv, pageSize) {
    const params = new URLSearchParams();
    if (searchWord) {
        params.set("searchWord", searchWord);
    }
    if (searchDiv) {
        params.set("searchDiv", searchDiv);
    }
    params.set("pageNo", String(pageNo));
    params.set("pageSize", pageSize);

    const nextUrl = `${window.location.pathname}?${params.toString()}`;
    window.history.replaceState({}, "", nextUrl);
}

function resolveDisplayName(person) {
    if (person.nameKo && person.nameKo.trim()) {
        return person.nameKo.trim();
    }
    if (person.nameOrg && person.nameOrg.trim()) {
        return person.nameOrg.trim();
    }
    return "이름 없음";
}

function showError(element, message) {
    element.textContent = message;
    element.classList.remove("d-none");
}

function hideError(element) {
    element.textContent = "";
    element.classList.add("d-none");
}
