// 등록과 수정은 필드 구성이 같으므로 하나의 form.html과 JavaScript를 재사용한다.
const formMode = document.body.dataset.formMode;
const collectionId = Number(document.body.dataset.collectionId);
const temporaryMemberId = Number(document.body.dataset.memberId);
const selectedItems = new Map();

let latestSearchResults = [];
let itemRequestPending = false;

document.addEventListener("DOMContentLoaded", () => {
    const collectionForm = document.querySelector("#collectionForm");
    const description = document.querySelector("#description");
    const movieSearchForm = document.querySelector("#movieSearchForm");
    const addItemModal = document.querySelector("#addCollectionItemModal");
    const memberIdInput = document.querySelector("#memberId");

    memberIdInput.value = Number.isInteger(temporaryMemberId) && temporaryMemberId > 0
        ? temporaryMemberId
        : 1;

    description.addEventListener("input", updateDescriptionLength);
    collectionForm.addEventListener("submit", submitCollection);
    movieSearchForm.addEventListener("submit", searchMovies);
    addItemModal.addEventListener("shown.bs.modal", () => {
        document.querySelector("#movieSearchWord").focus();
        if (latestSearchResults.length === 0) {
            loadMovieSearchResults();
        }
    });

    const deleteButton = document.querySelector("#confirmDeleteButton");
    if (deleteButton) {
        deleteButton.addEventListener("click", deleteCollection);
    }

    renderSelectedItems();

    if (formMode === "update") {
        prepareUpdateForm();
    }

    const itemError = new URLSearchParams(window.location.search).get("itemError");
    if (itemError) {
        showFormError(
            document.querySelector("#errorMessage"),
            `컬렉션은 등록됐지만 일부 작품을 추가하지 못했습니다: ${itemError}`
        );
    }
});

async function prepareUpdateForm() {
    const errorMessage = document.querySelector("#errorMessage");

    document.querySelector("#formTitle").textContent = "컬렉션 수정";
    document.querySelector("#submitButton").textContent = "수정";
    document.querySelector("#cancelLink").href =
        `/collections/${collectionId}?memberId=${temporaryMemberId}`;

    try {
        const collection = await requestGet(`/api/collections/${collectionId}`);

        // 수정 시 작성자 번호를 바꾸면 소유 관계가 달라지므로 기존 값으로 고정한다.
        const memberIdInput = document.querySelector("#memberId");
        memberIdInput.value = collection.memberId;
        memberIdInput.readOnly = true;

        document.querySelector("#title").value = collection.title || "";
        document.querySelector("#description").value = collection.description || "";
        document.querySelector("#isPublic").value = collection.isPublic || "Y";
        document.querySelector("#deleteCollectionTitle").textContent =
            collection.title || "이 컬렉션";

        updateDescriptionLength();
        await loadSelectedItems();
    } catch (error) {
        showFormError(errorMessage, error.message);
    }
}

/** 수정 화면에 이미 포함된 작품을 모두 불러온다. */
async function loadSelectedItems() {
    let pageNo = 1;
    let totalCount = 0;

    do {
        const data = await requestGet(`/api/collections/${collectionId}/items`, {
            pageNo,
            pageSize: 100
        });
        const items = data.items || [];

        items.forEach((item) => selectedItems.set(item.contentId, item));
        totalCount = Number(data.page?.totalCnt || items.length);

        if (items.length === 0) {
            break;
        }
        pageNo += 1;
    } while (selectedItems.size < totalCount);

    renderSelectedItems();
}

async function searchMovies(event) {
    event.preventDefault();
    await loadMovieSearchResults();
}

/** 한글 제목을 기준으로 영화를 검색해 MOD-07 결과 목록을 갱신한다. */
async function loadMovieSearchResults() {
    const searchButton = document.querySelector("#movieSearchButton");
    const message = document.querySelector("#movieSearchMessage");

    hideFormError(message);
    searchButton.disabled = true;

    try {
        latestSearchResults = await requestGet("/content/search", {
            searchWord: document.querySelector("#movieSearchWord").value.trim(),
            limit: 20
        });
        renderMovieSearchResults();
    } catch (error) {
        showFormError(message, error.message);
    } finally {
        searchButton.disabled = false;
    }
}

function renderMovieSearchResults() {
    const resultList = document.querySelector("#movieSearchResults");
    resultList.replaceChildren();

    if (latestSearchResults.length === 0) {
        resultList.append(createEmptyListItem("검색된 영화가 없습니다."));
        return;
    }

    latestSearchResults.forEach((content) => {
        const row = createMovieRow(content);
        const button = document.createElement("button");
        const alreadyAdded = selectedItems.has(content.contentId);

        button.className = alreadyAdded ? "btn btn-sm btn-secondary" : "btn btn-sm btn-dark";
        button.type = "button";
        button.textContent = alreadyAdded ? "추가됨" : "추가";
        button.disabled = alreadyAdded || itemRequestPending;
        button.addEventListener("click", () => addSelectedItem(content));

        row.append(button);
        resultList.append(row);
    });
}

async function addSelectedItem(content) {
    if (selectedItems.has(content.contentId) || itemRequestPending) {
        return;
    }

    const message = document.querySelector("#movieSearchMessage");
    hideFormError(message);
    itemRequestPending = true;
    renderMovieSearchResults();

    try {
        if (formMode === "update") {
            const saved = await requestPost(`/api/collections/${collectionId}/items`, {
                contentId: content.contentId
            });
            selectedItems.set(content.contentId, saved);
        } else {
            selectedItems.set(content.contentId, content);
        }

        renderSelectedItems();
    } catch (error) {
        showFormError(message, error.message);
    } finally {
        itemRequestPending = false;
        renderMovieSearchResults();
    }
}

function renderSelectedItems() {
    const itemList = document.querySelector("#selectedItemList");
    itemList.replaceChildren();
    document.querySelector("#selectedItemCount").textContent = `${selectedItems.size}개`;

    if (selectedItems.size === 0) {
        itemList.append(createEmptyListItem("선택한 작품이 없습니다."));
        return;
    }

    selectedItems.forEach((content) => {
        const row = createMovieRow(content);
        const removeButton = document.createElement("button");

        removeButton.className = "btn btn-sm btn-outline-danger";
        removeButton.type = "button";
        removeButton.textContent = "제거";
        removeButton.disabled = itemRequestPending;
        removeButton.addEventListener("click", () => removeSelectedItem(content.contentId));

        row.append(removeButton);
        itemList.append(row);
    });
}

async function removeSelectedItem(contentId) {
    if (itemRequestPending) {
        return;
    }

    const message = document.querySelector("#selectedItemMessage");
    hideFormError(message);
    itemRequestPending = true;
    renderSelectedItems();

    try {
        if (formMode === "update") {
            await requestDelete(`/api/collections/${collectionId}/items/${contentId}`);
        }
        selectedItems.delete(contentId);
    } catch (error) {
        showFormError(message, error.message);
    } finally {
        itemRequestPending = false;
        renderSelectedItems();
        renderMovieSearchResults();
    }
}

function createMovieRow(content) {
    const row = document.createElement("div");
    row.className = "list-group-item d-flex align-items-center gap-3";

    if (content.posterUrl) {
        const poster = document.createElement("img");
        poster.src = content.posterUrl;
        poster.alt = `${content.titleKo || content.titleOrg || "영화"} 포스터`;
        poster.width = 48;
        poster.height = 72;
        poster.className = "rounded object-fit-cover flex-shrink-0";
        row.append(poster);
    }

    const info = document.createElement("div");
    info.className = "flex-grow-1 overflow-hidden";

    const title = document.createElement("strong");
    title.className = "d-block text-truncate";
    title.textContent = content.titleKo || content.titleOrg || `콘텐츠 ${content.contentId}`;

    const meta = document.createElement("span");
    meta.className = "text-secondary small d-block text-truncate";
    meta.textContent = [content.titleOrg, content.releaseYear]
        .filter(Boolean)
        .join(" · ");

    info.append(title, meta);
    row.append(info);
    return row;
}

function createEmptyListItem(message) {
    const empty = document.createElement("div");
    empty.className = "list-group-item text-secondary text-center py-4";
    empty.textContent = message;
    return empty;
}

async function submitCollection(event) {
    event.preventDefault();

    const errorMessage = document.querySelector("#errorMessage");
    const submitButton = document.querySelector("#submitButton");

    hideFormError(errorMessage);
    submitButton.disabled = true;

    // 로그인 기능 병합 전에는 화면에서 입력한 DB 회원 번호를 요청에 포함한다.
    const data = {
        memberId: Number(document.querySelector("#memberId").value),
        title: document.querySelector("#title").value.trim(),
        description: document.querySelector("#description").value.trim(),
        isPublic: document.querySelector("#isPublic").value
    };

    try {
        const saved = formMode === "update"
            ? await requestPut(`/api/collections/${collectionId}`, data)
            : await requestPost("/api/collections", data);

        if (formMode === "create") {
            try {
                for (const contentId of selectedItems.keys()) {
                    await requestPost(`/api/collections/${saved.collectionId}/items`, { contentId });
                }
            } catch (itemError) {
                window.location.href =
                    `/collections/${saved.collectionId}/edit?memberId=${data.memberId}`
                    + `&itemError=${encodeURIComponent(itemError.message)}`;
                return;
            }
        }

        window.location.href =
            `/collections/${saved.collectionId}?memberId=${data.memberId}`;
    } catch (error) {
        showFormError(errorMessage, error.message);
        submitButton.disabled = false;
    }
}

async function deleteCollection() {
    const errorMessage = document.querySelector("#errorMessage");
    const deleteButton = document.querySelector("#confirmDeleteButton");
    const ownerId = Number(document.querySelector("#memberId").value);

    deleteButton.disabled = true;
    hideFormError(errorMessage);

    try {
        await requestDelete(`/api/collections/${collectionId}`);
        window.location.href =
            `/collections?searchDiv=20&searchWord=${ownerId}`;
    } catch (error) {
        showFormError(errorMessage, error.message);
        deleteButton.disabled = false;
    }
}

function requestPut(url, data) {
    return requestFetch(url, {
        method: "PUT",
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json",
            ...getCsrfHeaders()
        },
        body: JSON.stringify(data)
    });
}

function requestDelete(url) {
    return requestFetch(url, {
        method: "DELETE",
        headers: {
            "Accept": "application/json",
            ...getCsrfHeaders()
        }
    });
}

function updateDescriptionLength() {
    const description = document.querySelector("#description");
    document.querySelector("#descriptionLength").textContent = description.value.length;
}

function showFormError(element, message) {
    element.textContent = message;
    element.classList.remove("d-none");
}

function hideFormError(element) {
    element.textContent = "";
    element.classList.add("d-none");
}
