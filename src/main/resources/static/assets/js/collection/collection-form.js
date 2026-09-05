/**
 * Modification History
 * 2026. 8. 29. jinyoung - 등록·수정·공개 여부·작품 검색 적용
 * 2026. 8. 31. jinyoung - 작품 추가 모달과 미저장 변경 경고 적용
 * 2026. 9. 01. jinyoung - 등록·수정 화면과 공개 정책 UI 적용
 * 2026. 9. 02. jinyoung - 길이 경고와 작품 추가·제거 흐름 개선
 * 2026. 9. 05. jinyoung - 수정 화면 컬렉션 삭제 확인·요청 추가
 */
// 등록과 수정은 필드 구성이 같으므로 하나의 form.html과 JavaScript를 재사용한다.
const UPDATE_FORM_MODE = "update";
const TITLE_MAX_LENGTH = 100;
const DESCRIPTION_MAX_LENGTH = 1000;
const CONTENT_SEARCH_DEBOUNCE_MS = 300;
const CONTENT_SEARCH_PAGE_SIZE = 10;
const CONTENT_SNAPSHOT_PAGE_SIZE = 100;
const TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w185";

const selectedContentDetails = new Map();
const pendingAdditionDetails = new Map();
const pendingRemovalIds = new Set();

let selectedContentIds = [];
let currentSearchContents = [];
let currentFormMode = "create";
let contentSearchRequestSequence = 0;
let hasContentSearchQuery = false;
let contentSearchDebounceTimer = null;
let isContentEditMode = false;
let isContentEditEntering = false;
let initialFormState = null;
let isFormInitialized = false;
let isSubmitting = false;
let isEmptyCollectionCreationConfirmed = false;

document.addEventListener("DOMContentLoaded", () => {
    const page = document.querySelector("#collectionFormPage");
    const collectionId = Number(page.dataset.collectionId);
    const collectionForm = document.querySelector("#collectionForm");
    const contentSearchInput = document.querySelector("#contentSearchInput");
    const contentSearchModal = document.querySelector("#contentSearchModal");
    const clearContentSearchButton = document.querySelector("#clearContentSearchButton");
    const confirmEmptyCollectionButton = document.querySelector(
        "#confirmEmptyCollectionButton"
    );
    const confirmDeleteCollectionButton = document.querySelector(
        "#confirmDeleteCollectionButton"
    );

    currentFormMode = page.dataset.formMode;
    collectionForm.addEventListener("submit", submitCollection);
    window.addEventListener("beforeunload", warnUnsavedChanges);

    if (confirmEmptyCollectionButton) {
        confirmEmptyCollectionButton.addEventListener(
            "click",
            confirmEmptyCollectionCreation
        );
    }
    if (confirmDeleteCollectionButton) {
        confirmDeleteCollectionButton.addEventListener(
            "click",
            deleteCollectionFromForm
        );
    }
    contentSearchInput.addEventListener("keydown", handleContentSearchKeydown);
    contentSearchInput.addEventListener("input", scheduleContentSearch);
    clearContentSearchButton.addEventListener("click", clearContentSearchInput);
    initializeCollectionFormInteractions();
    contentSearchModal.addEventListener("hidden.bs.modal", resetContentSearchModal);
    contentSearchModal.addEventListener("shown.bs.modal", () => {
        contentSearchInput.focus();
    });

    renderSelectedContents();

    if (isUpdateForm()) {
        prepareUpdateForm(collectionId);
    } else {
        initializeFormState();
    }
});

// 폼 초기화와 입력 검증

/** 현재 화면이 수정 모드인지 확인한다. */
function isUpdateForm() {
    return currentFormMode === UPDATE_FORM_MODE;
}

/** 입력 제한과 작품 편집 버튼 이벤트를 연결한다. */
function initializeCollectionFormInteractions() {
    const isPublic = document.querySelector("#isPublic");

    initializeLengthGuard(
        document.querySelector("#title"),
        TITLE_MAX_LENGTH,
        document.querySelector("#titleLengthWarning")
    );
    initializeLengthGuard(
        document.querySelector("#description"),
        DESCRIPTION_MAX_LENGTH,
        document.querySelector("#descriptionLengthWarning")
    );
    document.querySelector("#toggleContentEditButton").addEventListener(
        "click",
        beginContentEdit
    );
    document.querySelector("#cancelContentEditButton").addEventListener(
        "click",
        cancelContentEdit
    );
    document.querySelector("#confirmContentRemovalButton").addEventListener(
        "click",
        confirmContentRemoval
    );
    document.querySelector("#modalAddContentButton").addEventListener(
        "click",
        confirmPendingAdditions
    );
    isPublic.addEventListener("change", updateCollectionVisibility);
    updateCollectionVisibility();
}

/** 공개 체크 상태에 맞는 안내 문구를 표시한다. */
function updateCollectionVisibility() {
    const isPublic = document.querySelector("#isPublic");
    const visibilityText = document.querySelector("#collectionVisibilityText");

    visibilityText.textContent = isPublic.checked
        ? "공개 컬렉션"
        : "비공개 컬렉션";
}

/** 공개 체크 상태를 API 요청값 Y/N으로 바꾼다. */
function getSelectedIsPublic() {
    return document.querySelector("#isPublic").checked ? "Y" : "N";
}

/** 최대 길이를 넘는 입력은 반영하지 않고 길이 경고를 표시한다. */
function initializeLengthGuard(input, maxLength, warning) {
    input.addEventListener("beforeinput", (event) => {
        if (!event.inputType.startsWith("insert") || event.isComposing) {
            return;
        }

        const insertedText = event.data || "";
        if (!insertedText) {
            return;
        }

        if (getProspectiveLength(input, insertedText) > maxLength) {
            showLengthWarning(input, warning);
        } else {
            hideLengthWarning(input, warning);
        }
    });
    input.addEventListener("paste", (event) => {
        const pastedText = event.clipboardData?.getData("text") || "";
        if (getProspectiveLength(input, pastedText) <= maxLength) {
            hideLengthWarning(input, warning);
            return;
        }

        event.preventDefault();
        const selectionStart = input.selectionStart ?? input.value.length;
        const selectionEnd = input.selectionEnd ?? selectionStart;
        const availableLength = Math.max(
            0,
            maxLength - (input.value.length - (selectionEnd - selectionStart))
        );
        input.setRangeText(
            pastedText.slice(0, availableLength),
            selectionStart,
            selectionEnd,
            "end"
        );
        input.dispatchEvent(new Event("input", { bubbles: true }));
        showLengthWarning(input, warning);
    });
    input.addEventListener("compositionend", () => {
        if (input.value.length > maxLength) {
            input.value = input.value.slice(0, maxLength);
            input.dispatchEvent(new Event("input", { bubbles: true }));
            showLengthWarning(input, warning);
        }
    });
    input.addEventListener("input", () => {
        if (input.value.length < maxLength) {
            hideLengthWarning(input, warning);
        }
    });
}

/** 현재 선택 영역에 문자를 넣었을 때의 길이를 계산한다. */
function getProspectiveLength(input, insertedText) {
    const selectionStart = input.selectionStart ?? input.value.length;
    const selectionEnd = input.selectionEnd ?? selectionStart;
    return input.value.length
        - (selectionEnd - selectionStart)
        + insertedText.length;
}

/** 입력 요소와 길이 경고를 강조한다. */
function showLengthWarning(input, warning) {
    input.classList.add("is-length-warning");
    warning.classList.remove("d-none");
}

/** 입력 길이가 정상 범위이면 경고를 숨긴다. */
function hideLengthWarning(input, warning) {
    input.classList.remove("is-length-warning");
    warning.classList.add("d-none");
}

/** 수정할 컬렉션과 전체 작품을 불러와 폼을 채운다. */
async function prepareUpdateForm(collectionId) {
    const errorMessage = document.querySelector("#errorMessage");

    try {
        const [collection, contents] = await Promise.all([
            requestGet(`/api/collections/${collectionId}`),
            loadAllContents(collectionId)
        ]);

        document.querySelector("#title").value = collection.title || "";
        document.querySelector("#description").value = collection.description || "";
        document.querySelector("#isPublic").checked = collection.isPublic !== "N";
        updateCollectionVisibility();
        selectedContentIds = contents.map((content) => Number(content.contentId));
        contents.forEach((content) => {
            selectedContentDetails.set(Number(content.contentId), content);
        });

        renderSelectedContents();
        initializeFormState();
    } catch (error) {
        showFormError(errorMessage, error.message);
    }
}

// 저장과 미저장 변경 감지

/** 폼을 검증하고 등록 또는 수정 요청을 보낸다. */
async function submitCollection(event) {
    event.preventDefault();

    if (isSubmitting) {
        return;
    }

    const errorMessage = document.querySelector("#errorMessage");
    const collectionId = Number(
        document.querySelector("#collectionFormPage").dataset.collectionId
    );
    const collectionForm = document.querySelector("#collectionForm");
    const submitButton = document.querySelector("#submitButton");

    collectionForm.classList.add("was-validated");
    if (!collectionForm.checkValidity()) {
        document.querySelector("#title").focus();
        return;
    }

    if (!isUpdateForm()
        && selectedContentIds.length === 0
        && !isEmptyCollectionCreationConfirmed) {
        bootstrap.Modal.getOrCreateInstance(
            document.querySelector("#emptyCollectionConfirmModal")
        ).show();
        return;
    }
    isEmptyCollectionCreationConfirmed = false;

    hideFormError(errorMessage);
    submitButton.disabled = true;
    submitButton.setAttribute("aria-busy", "true");
    submitButton.textContent = "저장 중...";
    isSubmitting = true;

    const data = {
        title: document.querySelector("#title").value.trim(),
        description: document.querySelector("#description").value.trim(),
        isPublic: getSelectedIsPublic(),
        contentIds: selectedContentIds
    };

    try {
        const saved = isUpdateForm()
            ? await requestPatch(`/api/collections/${collectionId}`, data)
            : await requestPost("/api/collections", data);

        window.location.href = `/collections/${saved.collectionId}`;
    } catch (error) {
        showFormError(errorMessage, error.message);
        submitButton.disabled = false;
        submitButton.removeAttribute("aria-busy");
        submitButton.textContent = getSubmitButtonLabel();
        isSubmitting = false;
    }
}

/** 현재 폼 모드에 맞는 저장 버튼 문구를 반환한다. */
function getSubmitButtonLabel() {
    return isUpdateForm() ? "수정 완료" : "만들기";
}

/** 빈 컬렉션 등록 확인 후 폼 제출을 다시 요청한다. */
function confirmEmptyCollectionCreation() {
    isEmptyCollectionCreationConfirmed = true;
    bootstrap.Modal.getOrCreateInstance(
        document.querySelector("#emptyCollectionConfirmModal")
    ).hide();
    document.querySelector("#collectionForm").requestSubmit();
}

/** 공통 요청 함수로 PATCH 요청을 보낸다. */
function requestPatch(url, data) {
    return requestFetch(url, {
        method: "PATCH",
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json",
            ...getCsrfHeaders()
        },
        body: JSON.stringify(data)
    });
}

/** 수정 화면에서 컬렉션 삭제 후 내 컬렉션 기록으로 이동한다. */
async function deleteCollectionFromForm() {
    const page = document.querySelector("#collectionFormPage");
    const collectionId = Number(page.dataset.collectionId);
    const deleteButton = document.querySelector("#confirmDeleteCollectionButton");
    const errorMessage = document.querySelector("#errorMessage");

    deleteButton.disabled = true;
    deleteButton.textContent = "삭제 중...";
    isSubmitting = true;

    try {
        await requestDelete(`/api/collections/${collectionId}`);
        window.location.href = "/members/records?tab=collections";
    } catch (error) {
        bootstrap.Modal.getInstance(
            document.querySelector("#deleteCollectionFormModal")
        )?.hide();
        showFormError(errorMessage, error.message);
        deleteButton.disabled = false;
        deleteButton.textContent = "삭제";
        isSubmitting = false;
    }
}

/** 공통 요청 함수로 DELETE 요청을 보낸다. */
function requestDelete(url) {
    return requestFetch(url, {
        method: "DELETE",
        headers: {
            "Accept": "application/json",
            ...getCsrfHeaders()
        }
    });
}

/** 수정 화면에서 전체 스냅샷과 표시 정보를 보존하도록 기존 작품을 모든 페이지에서 읽는다. */
async function loadAllContents(collectionId) {
    const contents = [];
    let pageNo = 1;
    let totalPages = 1;

    do {
        const data = await requestGet(`/api/collections/${collectionId}/items`, {
            pageNo,
            pageSize: CONTENT_SNAPSHOT_PAGE_SIZE
        });

        contents.push(...(data.items || []));

        const totalCount = Number(data.page?.totalCnt || 0);
        totalPages = Math.max(
            1,
            Math.ceil(totalCount / CONTENT_SNAPSHOT_PAGE_SIZE)
        );
        pageNo += 1;
    } while (pageNo <= totalPages);

    return contents;
}

// 작품 검색과 추가

/** 검색 입력에서 Enter를 누르면 즉시 첫 페이지를 조회한다. */
function handleContentSearchKeydown(event) {
    if (event.key !== "Enter") {
        return;
    }

    event.preventDefault();
    window.clearTimeout(contentSearchDebounceTimer);
    searchContents(1);
}

/** 입력이 멈춘 뒤 검색하도록 요청 시간을 조절한다. */
function scheduleContentSearch() {
    window.clearTimeout(contentSearchDebounceTimer);
    updateContentSearchClearButton();

    if (!document.querySelector("#contentSearchInput").value.trim()) {
        clearContentSearchResults();
        return;
    }

    contentSearchDebounceTimer = window.setTimeout(
        () => searchContents(1),
        CONTENT_SEARCH_DEBOUNCE_MS
    );
}

/** 검색어와 결과를 지우고 검색 입력에 초점을 돌린다. */
function clearContentSearchInput() {
    const contentSearchInput = document.querySelector("#contentSearchInput");
    contentSearchInput.value = "";
    updateContentSearchClearButton();
    clearContentSearchResults();
    contentSearchInput.focus();
}

/** 검색어가 있을 때만 지우기 버튼을 표시한다. */
function updateContentSearchClearButton() {
    const clearButton = document.querySelector("#clearContentSearchButton");
    clearButton.classList.toggle(
        "d-none",
        !document.querySelector("#contentSearchInput").value
    );
}

/** DB에 저장된 콘텐츠를 제목으로 검색한다. 빈 검색어일 때는 조회하지 않는다. */
async function searchContents(pageNo) {
    const query = document.querySelector("#contentSearchInput").value.trim();
    const loading = document.querySelector("#contentSearchLoading");

    if (!query) {
        clearContentSearchResults();
        return;
    }

    const requestSequence = ++contentSearchRequestSequence;
    hideContentSelectionError();
    loading.classList.remove("d-none");
    document.querySelector("#contentSearchResults").replaceChildren();
    document.querySelector("#contentSearchEmpty").classList.add("d-none");
    hasContentSearchQuery = true;

    try {
        const data = await requestGet("/api/search/movies", {
            query,
            page: pageNo,
            size: CONTENT_SEARCH_PAGE_SIZE
        });

        if (requestSequence !== contentSearchRequestSequence) {
            return;
        }
        currentSearchContents = data.items || [];
        renderContentSearchResults(currentSearchContents);
        renderContentSearchPagination(data.page || {}, pageNo);
    } catch (error) {
        if (requestSequence !== contentSearchRequestSequence) {
            return;
        }
        currentSearchContents = [];
        renderContentSearchResults(currentSearchContents);
        renderContentSearchPagination({}, 1);
        showContentSelectionError(error.message);
    } finally {
        if (requestSequence === contentSearchRequestSequence) {
            loading.classList.add("d-none");
        }
    }
}

/** 진행 중인 검색을 무효화하고 검색 결과 영역을 비운다. */
function clearContentSearchResults() {
    contentSearchRequestSequence += 1;
    currentSearchContents = [];
    hasContentSearchQuery = false;
    document.querySelector("#contentSearchLoading").classList.add("d-none");
    document.querySelector("#contentSearchResults").replaceChildren();
    document.querySelector("#contentSearchEmpty").classList.add("d-none");
    document.querySelector("#contentSearchPagination").replaceChildren();
    hideContentSelectionError();
}

/** 검색된 작품 목록이나 빈 결과를 표시한다. */
function renderContentSearchResults(contents) {
    const results = document.querySelector("#contentSearchResults");
    const empty = document.querySelector("#contentSearchEmpty");
    results.replaceChildren();
    empty.classList.toggle("d-none", !hasContentSearchQuery || contents.length > 0);

    contents.forEach((content) => {
        results.append(createContentSearchItem(content));
    });
}

/** 검색 결과 한 건을 선택 가능한 작품 행으로 만든다. */
function createContentSearchItem(content) {
    const contentId = Number(content.contentId);
    const alreadyAdded = selectedContentIds.includes(contentId);
    const pending = pendingAdditionDetails.has(contentId);
    const row = document.createElement("div");
    const check = document.createElement("span");
    const icon = document.createElement("i");

    row.className = "collection-search-item collection-update-search-item";
    row.classList.toggle("is-selected", pending);
    row.classList.toggle("is-added", alreadyAdded);
    check.className = "collection-search-check";
    check.classList.toggle("is-checked", alreadyAdded || pending);
    check.setAttribute("aria-hidden", "true");
    icon.className = "bi bi-check-lg";
    icon.setAttribute("aria-hidden", "true");
    check.append(icon);
    if (!alreadyAdded) {
        row.classList.add("is-selectable");
        row.setAttribute("role", "checkbox");
        row.setAttribute("tabindex", "0");
        row.setAttribute("aria-checked", String(pending));
        row.setAttribute("aria-label", getContentTitle(content));
        row.addEventListener("click", () => togglePendingAddition(content));
        row.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                togglePendingAddition(content);
            }
        });
    } else {
        row.setAttribute("aria-disabled", "true");
    }

    row.append(check);
    appendContentPoster(row, content);

    const body = document.createElement("div");
    const information = createContentInformation(content);
    information.classList.add("flex-grow-1");
    body.className = "collection-search-item-body";
    body.append(information);

    if (alreadyAdded) {
        const status = document.createElement("span");
        status.className = "collection-search-added-status";
        status.textContent = "추가됨";
        body.append(status);
    }
    row.append(body);
    return row;
}

/** 작품을 추가 대기 목록에 넣거나 제외한다. */
function togglePendingAddition(content) {
    const contentId = Number(content.contentId);

    if (pendingAdditionDetails.has(contentId)) {
        pendingAdditionDetails.delete(contentId);
    } else {
        pendingAdditionDetails.set(contentId, content);
    }

    updatePendingAdditionAction();
    renderContentSearchResults(currentSearchContents);
}

/** 추가 대기 건수와 추가 버튼 상태를 갱신한다. */
function updatePendingAdditionAction() {
    const count = pendingAdditionDetails.size;
    document.querySelector("#pendingAdditionCount").textContent = String(count);
    document.querySelector("#modalAddContentButton").disabled = count === 0;
}

/** 선택한 작품을 폼 목록에 반영하고 검색 모달을 닫는다. */
function confirmPendingAdditions() {
    if (pendingAdditionDetails.size === 0) {
        return;
    }

    pendingAdditionDetails.forEach((content, contentId) => {
        if (!selectedContentIds.includes(contentId)) {
            selectedContentIds.push(contentId);
            selectedContentDetails.set(contentId, content);
        }
    });
    pendingAdditionDetails.clear();
    renderSelectedContents();
    showSaveNotice();
    bootstrap.Modal.getOrCreateInstance(
        document.querySelector("#contentSearchModal")
    ).hide();
}

/** 검색 모달의 입력값과 임시 선택 상태를 초기화한다. */
function resetContentSearchModal() {
    window.clearTimeout(contentSearchDebounceTimer);
    document.querySelector("#contentSearchInput").value = "";
    updateContentSearchClearButton();
    pendingAdditionDetails.clear();
    updatePendingAdditionAction();
    clearContentSearchResults();
}

/** 작품 검색 결과의 이전·현재·다음 페이지 버튼을 만든다. */
function renderContentSearchPagination(page, currentPage) {
    const pagination = document.querySelector("#contentSearchPagination");
    const pageSize = Number(page.pageSize || 10);
    const totalCount = Number(page.totalCnt || 0);
    const totalPages = Math.ceil(totalCount / pageSize);
    pagination.replaceChildren();

    if (totalPages <= 1) {
        return;
    }

    pagination.append(createContentPageButton("이전", currentPage - 1, currentPage <= 1));
    pagination.append(createContentPageButton(
        `${currentPage} / ${totalPages}`,
        currentPage,
        true
    ));
    pagination.append(createContentPageButton(
        "다음",
        currentPage + 1,
        currentPage >= totalPages
    ));
}

/** 작품 검색 페이지 버튼 한 개를 만든다. */
function createContentPageButton(label, pageNo, disabled) {
    const item = document.createElement("li");
    item.className = `page-item${disabled ? " disabled" : ""}`;

    const button = document.createElement("button");
    button.className = "page-link";
    button.type = "button";
    button.textContent = label;
    button.disabled = disabled;
    button.addEventListener("click", () => searchContents(pageNo));

    item.append(button);
    return item;
}

// 선택 작품 편집

/** 선택 작품 개수와 작품 그리드를 화면에 표시한다. */
function renderSelectedContents() {
    const selectedContentList = document.querySelector("#selectedContentList");
    const selectedContentCount = document.querySelector("#selectedContentCount");

    selectedContentCount.textContent = String(selectedContentIds.length);
    selectedContentList.replaceChildren();
    selectedContentList.classList.toggle("is-editing", isContentEditMode);
    selectedContentList.classList.toggle("is-entering-edit", isContentEditEntering);
    renderSelectedContentGrid(selectedContentList);
    updateContentEditControls();
}

/** 작품 추가 카드와 선택된 작품 카드를 순서대로 배치한다. */
function renderSelectedContentGrid(selectedContentList) {
    selectedContentList.append(createAddContentCard());
    selectedContentIds.forEach((contentId) => {
        const content = selectedContentDetails.get(contentId) || { contentId };
        selectedContentList.append(createContentGridCard(contentId, content));
    });
}

/** 작품 검색 모달을 여는 추가 카드를 만든다. */
function createAddContentCard() {
    const button = document.createElement("button");
    const visual = document.createElement("span");
    const icon = document.createElement("i");
    const label = document.createElement("span");

    button.className = "collection-grid-add-card";
    button.type = "button";
    button.disabled = isContentEditMode;
    button.setAttribute("data-bs-toggle", "modal");
    button.setAttribute("data-bs-target", "#contentSearchModal");
    visual.className = "collection-grid-add-visual";
    icon.className = "bi bi-plus-lg";
    icon.setAttribute("aria-hidden", "true");
    label.textContent = "작품 추가";
    visual.append(icon, label);
    button.append(visual);
    return button;
}

/** 선택된 작품 한 건을 포스터 카드로 만든다. */
function createContentGridCard(contentId, content) {
    const card = document.createElement("article");
    const posterFrame = document.createElement("div");
    const title = document.createElement("div");

    card.className = "collection-grid-content-card";
    card.dataset.contentId = String(contentId);
    card.classList.toggle("is-pending-removal", pendingRemovalIds.has(contentId));
    posterFrame.className = "collection-grid-poster-frame";
    posterFrame.append(createGridPoster(content));

    if (isContentEditMode) {
        const removeButton = document.createElement("button");
        const icon = document.createElement("i");
        const pending = pendingRemovalIds.has(contentId);

        removeButton.className = "collection-grid-remove-button";
        removeButton.type = "button";
        removeButton.setAttribute("aria-pressed", String(pending));
        removeButton.setAttribute(
            "aria-label",
            `${getContentTitle(content)} ${pending ? "제거 선택 해제" : "제거 선택"}`
        );
        removeButton.classList.toggle("is-selected", pending);
        icon.className = pending ? "bi bi-check-lg" : "bi bi-dash-lg";
        icon.setAttribute("aria-hidden", "true");
        removeButton.append(icon);
        removeButton.addEventListener("click", () => togglePendingRemoval(contentId));
        posterFrame.append(removeButton);
    }

    title.className = "collection-grid-content-title";
    title.textContent = getContentTitle(content);
    title.title = getContentTitle(content);
    card.append(posterFrame, title);
    return card;
}

/** 작품 포스터 또는 기본 포스터 영역을 만든다. */
function createGridPoster(content) {
    if (!content.posterUrl) {
        const placeholder = document.createElement("div");
        const icon = document.createElement("i");
        placeholder.className = "collection-grid-poster collection-grid-poster-placeholder";
        icon.className = "bi bi-film";
        icon.setAttribute("aria-hidden", "true");
        placeholder.append(icon);
        return placeholder;
    }

    const poster = document.createElement("img");
    poster.src = resolvePosterUrl(content.posterUrl);
    poster.alt = "";
    poster.className = "collection-grid-poster";
    poster.addEventListener("error", () => {
        poster.replaceWith(createGridPoster({}));
    });
    return poster;
}

/** 작품 제거 선택 모드로 전환한다. */
function beginContentEdit() {
    if (selectedContentIds.length === 0) {
        return;
    }

    isContentEditMode = true;
    isContentEditEntering = true;
    pendingRemovalIds.clear();
    renderSelectedContents();
    isContentEditEntering = false;
}

/** 작품 한 건의 제거 선택 상태를 바꾼다. */
function togglePendingRemoval(contentId) {
    if (pendingRemovalIds.has(contentId)) {
        pendingRemovalIds.delete(contentId);
    } else {
        pendingRemovalIds.add(contentId);
    }

    const pending = pendingRemovalIds.has(contentId);
    const card = document.querySelector(
        `.collection-grid-content-card[data-content-id="${contentId}"]`
    );
    const removeButton = card.querySelector(".collection-grid-remove-button");
    const icon = removeButton.querySelector("i");
    card.classList.toggle("is-pending-removal", pending);
    removeButton.classList.toggle("is-selected", pending);
    removeButton.setAttribute("aria-pressed", String(pending));
    removeButton.setAttribute(
        "aria-label",
        `${card.querySelector(".collection-grid-content-title").textContent} ${pending ? "제거 선택 해제" : "제거 선택"}`
    );
    icon.className = pending ? "bi bi-check-lg" : "bi bi-dash-lg";
    updateContentEditControls();
}

/** 제거 선택을 취소하고 일반 보기로 돌아간다. */
function cancelContentEdit() {
    finishContentEdit();
    renderSelectedContents();
}

/** 제거 대상으로 선택한 작품을 폼 목록에서 제외한다. */
function confirmContentRemoval() {
    if (pendingRemovalIds.size === 0) {
        return;
    }

    selectedContentIds = selectedContentIds.filter(
        (contentId) => !pendingRemovalIds.has(contentId)
    );
    pendingRemovalIds.forEach((contentId) => {
        selectedContentDetails.delete(contentId);
    });
    finishContentEdit();
    renderSelectedContents();
    showSaveNotice();
}

/** 작품 편집 상태와 임시 제거 목록을 초기화한다. */
function finishContentEdit() {
    isContentEditMode = false;
    isContentEditEntering = false;
    pendingRemovalIds.clear();
}

/** 작품 수와 편집 상태에 맞춰 버튼을 표시한다. */
function updateContentEditControls() {
    const toggleButton = document.querySelector("#toggleContentEditButton");
    const editActions = document.querySelector("#contentEditActions");
    const confirmButton = document.querySelector("#confirmContentRemovalButton");

    toggleButton.classList.toggle("d-none", isContentEditMode);
    toggleButton.disabled = selectedContentIds.length === 0;
    editActions.classList.toggle("d-none", !isContentEditMode);
    document.querySelector("#pendingRemovalCount").textContent =
        String(pendingRemovalIds.size);
    confirmButton.disabled = pendingRemovalIds.size === 0;
}

/** 콘텐츠 포스터가 있으면 목록 행 앞에 작은 이미지로 추가한다. */
function appendContentPoster(row, content) {
    if (!content.posterUrl) {
        row.append(createPosterPlaceholder());
        return;
    }

    const poster = document.createElement("img");
    poster.src = resolvePosterUrl(content.posterUrl);
    poster.alt = "";
    poster.className = "collection-selection-poster";
    poster.addEventListener("error", () => {
        poster.replaceWith(createPosterPlaceholder());
    });
    row.append(poster);
}

/** 검색 결과에서 사용할 기본 포스터 영역을 만든다. */
function createPosterPlaceholder() {
    const placeholder = document.createElement("div");
    const icon = document.createElement("i");

    placeholder.className =
        "collection-selection-poster collection-selection-poster-placeholder";
    icon.className = "bi bi-film";
    icon.setAttribute("aria-hidden", "true");
    placeholder.append(icon);
    return placeholder;
}

/** 검색 결과와 등록 목록에서 공통으로 사용하는 제목·개봉연도 영역을 만든다. */
function createContentInformation(content) {
    const information = document.createElement("div");
    const title = document.createElement("div");
    const metadata = document.createElement("div");

    title.className = "collection-content-title";
    title.textContent = getContentTitle(content);
    metadata.className = "collection-content-meta";
    metadata.textContent = content.releaseYear
        ? String(content.releaseYear).slice(0, 4)
        : "개봉연도 정보 없음";

    information.append(title, metadata);
    return information;
}

/** 작품의 한글 제목, 원제, 기본 문구 순으로 제목을 선택한다. */
function getContentTitle(content) {
    return content.titleKo || content.titleOrg || "제목 정보 없음";
}

/** TMDB 상대 포스터 경로를 화면에서 사용할 URL로 바꾼다. */
function resolvePosterUrl(posterUrl) {
    if (/^https?:\/\//i.test(posterUrl)) {
        return posterUrl;
    }
    return `${TMDB_POSTER_BASE_URL}${posterUrl}`;
}

// 안내와 폼 상태 관리

/** 작품 변경 후 최종 저장이 필요하다는 안내를 표시한다. */
function showSaveNotice() {
    const toastElement = document.querySelector("#collectionSaveNoticeToast");
    const actionName = isUpdateForm() ? "수정 완료" : "컬렉션 만들기";
    toastElement.querySelector(".toast-body").textContent =
        `상단에 ‘${actionName}’ 버튼을 눌러야 저장이 반영돼요.`;
    bootstrap.Toast.getOrCreateInstance(toastElement).show();
}

/** 작품 검색 오류를 표시한다. */
function showContentSelectionError(message) {
    const error = document.querySelector("#contentSelectionError");
    error.textContent = message;
    error.classList.remove("d-none");
}

/** 작품 검색 오류를 숨긴다. */
function hideContentSelectionError() {
    const error = document.querySelector("#contentSelectionError");
    error.textContent = "";
    error.classList.add("d-none");
}

/** 미저장 변경 비교에 사용할 현재 폼 상태를 문자열로 만든다. */
function serializeFormState() {
    return JSON.stringify({
        title: document.querySelector("#title").value,
        description: document.querySelector("#description").value,
        isPublic: getSelectedIsPublic(),
        contentIds: selectedContentIds
    });
}

/** 현재 폼 상태를 최초 상태로 저장한다. */
function initializeFormState() {
    initialFormState = serializeFormState();
    isFormInitialized = true;
}

/** 최초 상태와 현재 상태를 비교해 변경 여부를 확인한다. */
function hasUnsavedChanges() {
    return isFormInitialized
        && serializeFormState() !== initialFormState;
}

/** 저장하지 않은 변경이 있으면 페이지 이탈 경고를 요청한다. */
function warnUnsavedChanges(event) {
    if (isSubmitting || !hasUnsavedChanges()) {
        return;
    }
    event.preventDefault();
    event.returnValue = "";
}

/** 폼 저장 오류를 표시한다. */
function showFormError(element, message) {
    element.textContent = message;
    element.classList.remove("d-none");
}

/** 폼 저장 오류를 숨긴다. */
function hideFormError(element) {
    element.textContent = "";
    element.classList.add("d-none");
}
