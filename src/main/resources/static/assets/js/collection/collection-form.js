/**
 * Modification History
 * 2026. 8. 29. jinyoung - 인증 요청·PATCH·공개 여부·contentIds 및 실제 콘텐츠 검색·선택 적용
 * 2026. 8. 31. jinyoung - D-04 모달·영화 검색 최종 API·미저장 변경 경고 적용
 * 2026. 9. 01. jinyoung - D-02~D-04 본문 UI와 컬렉션 공개 정책 반영
 */
// 등록과 수정은 필드 구성이 같으므로 하나의 form.html과 JavaScript를 재사용한다.
let selectedContentIds = [];
const selectedContentDetails = new Map();
let currentSearchContents = [];
let contentSearchInitialized = false;
let initialFormState = null;
let isFormInitialized = false;
let isSubmitting = false;
let originalIsPublic = "Y";
document.addEventListener("DOMContentLoaded", () => {
    // 공통 layout을 사용하므로 담당 본문 루트의 data-* 속성에서 화면 정보를 읽는다.
    const page = document.querySelector("#collectionFormPage");
    const formMode = page.dataset.formMode;
    const collectionId = Number(page.dataset.collectionId);
    const collectionForm = document.querySelector("#collectionForm");
    const description = document.querySelector("#description");
    const contentSearchInput = document.querySelector("#contentSearchInput");

    description.addEventListener("input", updateDescriptionLength);
    collectionForm.addEventListener("submit", submitCollection);
    document.querySelector("#cancelLink").addEventListener(
        "click",
        confirmCancelNavigation
    );
    window.addEventListener("beforeunload", warnUnsavedChanges);
    document.querySelector("#searchContentButton").addEventListener(
        "click",
        () => searchContents(1)
    );
    contentSearchInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            searchContents(1);
        }
    });
    document.querySelector("#contentSearchModal").addEventListener(
        "shown.bs.modal",
        () => {
            contentSearchInput.focus();
            if (!contentSearchInitialized) {
                searchContents(1);
            }
        }
    );

    renderSelectedContents();

    // 수정 화면에서만 기존 데이터를 API로 읽어 입력란에 채운다.
    if (formMode === "update") {
        prepareUpdateForm(collectionId);
    } else {
        initializeFormState();
    }
});

async function prepareUpdateForm(collectionId) {
    const errorMessage = document.querySelector("#errorMessage");

    document.querySelector("#formTitle").textContent = "컬렉션 수정";
    document.querySelector("#formDescription").textContent =
        "컬렉션 정보와 담긴 작품을 수정할 수 있습니다.";
    document.querySelector("#submitButton").textContent = "수정 완료";
    document.querySelector("#cancelLink").href = `/collections/${collectionId}`;

    try {
        const [collection, contents] = await Promise.all([
            requestGet(`/api/collections/${collectionId}`),
            loadAllContents(collectionId)
        ]);

        document.querySelector("#title").value = collection.title || "";
        document.querySelector("#description").value = collection.description || "";
        originalIsPublic = collection.isPublic === "N" ? "N" : "Y";
        selectedContentIds = contents.map((content) => content.contentId);
        contents.forEach((content) => {
            selectedContentDetails.set(content.contentId, content);
        });

        updateDescriptionLength();
        renderSelectedContents();
        renderContentSearchResults(currentSearchContents);
        initializeFormState();
    } catch (error) {
        showFormError(errorMessage, error.message);
    }
}

async function submitCollection(event) {
    // form의 기본 페이지 이동을 막고 Fetch로 JSON 요청을 보낸다.
    event.preventDefault();

    const errorMessage = document.querySelector("#errorMessage");
    const page = document.querySelector("#collectionFormPage");
    const formMode = page.dataset.formMode;
    const collectionId = Number(page.dataset.collectionId);
    const collectionForm = document.querySelector("#collectionForm");
    const submitButton = document.querySelector("#submitButton");

    collectionForm.classList.add("was-validated");
    if (!collectionForm.checkValidity()) {
        document.querySelector("#title").focus();
        return;
    }

    hideFormError(errorMessage);
    // 사용자가 저장 버튼을 연속 클릭해 중복 요청하는 것을 막는다.
    submitButton.disabled = true;
    submitButton.setAttribute("aria-busy", "true");
    submitButton.textContent = "저장 중...";
    isSubmitting = true;

    const data = {
        title: document.querySelector("#title").value.trim(),
        description: document.querySelector("#description").value.trim(),
        // v3.0 정책: 신규는 공개, 수정은 기존 공개 상태를 유지한다.
        isPublic: formMode === "update" ? originalIsPublic : "Y",
        contentIds: selectedContentIds
    };

    try {
        // 등록은 POST, 수정은 PATCH를 사용하지만 성공 후에는 모두 상세 화면으로 이동한다.
        const saved = formMode === "update"
            ? await requestPatch(`/api/collections/${collectionId}`, data)
            : await requestPost("/api/collections", data);

        window.location.href = `/collections/${saved.collectionId}`;
    } catch (error) {
        showFormError(errorMessage, error.message);
        submitButton.disabled = false;
        submitButton.removeAttribute("aria-busy");
        submitButton.textContent = formMode === "update"
            ? "수정 완료"
            : "컬렉션 만들기";
        isSubmitting = false;
    }
}

function requestPatch(url, data) {
    // 공통 파일에 PATCH 도우미가 없어 requestFetch를 이용해 이 화면에 필요한 요청을 구성한다.
    // CSRF 메타 태그가 존재하면 getCsrfHeaders가 자동으로 헤더를 추가한다.
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

/** 수정 화면에서 전체 스냅샷과 표시 정보를 보존하도록 기존 작품을 모든 페이지에서 읽는다. */
async function loadAllContents(collectionId) {
    const pageSize = 100;
    const contents = [];
    let pageNo = 1;
    let totalPages = 1;

    do {
        const data = await requestGet(`/api/collections/${collectionId}/items`, {
            pageNo,
            pageSize
        });

        contents.push(...(data.items || []));

        const totalCount = Number(data.page?.totalCnt || 0);
        totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
        pageNo += 1;
    } while (pageNo <= totalPages);

    return contents;
}

/** 검색 결과의 콘텐츠를 현재 선택 목록에 추가한다. */
function addContent(content) {
    const contentId = Number(content.contentId);

    if (selectedContentIds.includes(contentId)) {
        return;
    }

    selectedContentIds.push(contentId);
    selectedContentDetails.set(contentId, content);
    renderSelectedContents();
    renderContentSearchResults(currentSearchContents);
}

/** 선택한 콘텐츠 번호를 목록과 저장 스냅샷에서 제거한다. */
function removeContent(contentId) {
    selectedContentIds = selectedContentIds.filter((id) => id !== contentId);
    selectedContentDetails.delete(contentId);
    hideContentSelectionError();
    renderSelectedContents();
    renderContentSearchResults(currentSearchContents);
}

/** DB에 저장된 콘텐츠를 제목으로 검색한다. 검색어가 없으면 최신 목록을 조회한다. */
async function searchContents(pageNo) {
    const query = document.querySelector("#contentSearchInput").value.trim();
    const searchButton = document.querySelector("#searchContentButton");
    const loading = document.querySelector("#contentSearchLoading");

    hideContentSelectionError();
    searchButton.disabled = true;
    loading.classList.remove("d-none");
    document.querySelector("#contentSearchResults").replaceChildren();
    document.querySelector("#contentSearchEmpty").classList.add("d-none");
    contentSearchInitialized = true;

    try {
        const data = await requestGet("/api/search/movies", {
            query,
            page: pageNo,
            size: 10
        });

        currentSearchContents = data.items || [];
        renderContentSearchResults(currentSearchContents);
        renderContentSearchPagination(data.page || {}, pageNo);
    } catch (error) {
        currentSearchContents = [];
        renderContentSearchResults(currentSearchContents);
        renderContentSearchPagination({}, 1);
        showContentSelectionError(error.message);
    } finally {
        searchButton.disabled = false;
        loading.classList.add("d-none");
    }
}

/** 콘텐츠 검색 결과를 영화 정보와 선택 버튼으로 표시한다. */
function renderContentSearchResults(contents) {
    const results = document.querySelector("#contentSearchResults");
    const empty = document.querySelector("#contentSearchEmpty");
    results.replaceChildren();
    empty.classList.toggle("d-none", contents.length > 0);

    contents.forEach((content) => {
        const contentId = Number(content.contentId);
        const selected = selectedContentIds.includes(contentId);
        const row = document.createElement("div");
        row.className = "collection-search-item";

        appendContentPoster(row, content);

        const information = createContentInformation(content);
        information.classList.add("flex-grow-1");

        const addButton = document.createElement("button");
        addButton.className = selected
            ? "btn btn-sm btn-secondary flex-shrink-0"
            : "btn btn-sm btn-outline-primary flex-shrink-0";
        addButton.type = "button";
        addButton.disabled = selected;
        addButton.textContent = selected ? "추가됨" : "추가";
        addButton.addEventListener("click", () => addContent(content));

        row.append(information, addButton);
        results.append(row);
    });
}

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

/** 선택 작품 개수와 목록 또는 빈 상태를 화면에 표시한다. */
function renderSelectedContents() {
    const selectedContentList = document.querySelector("#selectedContentList");
    const selectedContentEmpty = document.querySelector("#selectedContentEmpty");

    document.querySelector("#selectedContentCount").textContent =
        String(selectedContentIds.length);
    document.querySelector("#modalSelectedContentCount").textContent =
        String(selectedContentIds.length);
    selectedContentList.replaceChildren();
    selectedContentEmpty.classList.toggle("d-none", selectedContentIds.length > 0);

    selectedContentIds.forEach((contentId) => {
        const content = selectedContentDetails.get(contentId) || { contentId };
        const row = document.createElement("div");
        row.className = "collection-selection-item";

        appendContentPoster(row, content);

        const information = createContentInformation(content);
        information.classList.add("flex-grow-1");

        const removeButton = document.createElement("button");
        removeButton.className = "btn btn-sm btn-outline-secondary flex-shrink-0";
        removeButton.type = "button";
        removeButton.textContent = "제거";
        removeButton.setAttribute(
            "aria-label",
            `${content.titleKo || content.titleOrg || "작품"} 제거`
        );
        removeButton.addEventListener("click", () => removeContent(contentId));

        row.append(information, removeButton);
        selectedContentList.append(row);
    });
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

/** 검색 결과와 선택 목록에서 공통으로 사용하는 제목·개봉연도 영역을 만든다. */
function createContentInformation(content) {
    const information = document.createElement("div");
    const title = document.createElement("div");
    const metadata = document.createElement("div");

    title.className = "collection-content-title";
    title.textContent = content.titleKo || content.titleOrg || "제목 정보 없음";
    metadata.className = "collection-content-meta";
    metadata.textContent = content.releaseYear
        ? String(content.releaseYear).slice(0, 4)
        : "개봉연도 정보 없음";

    information.append(title, metadata);
    return information;
}

/** DB에 저장된 TMDB 상대 경로를 브라우저에서 표시할 수 있는 포스터 URL로 변환한다. */
function resolvePosterUrl(posterUrl) {
    if (/^https?:\/\//i.test(posterUrl)) {
        return posterUrl;
    }

    return `https://image.tmdb.org/t/p/w185${posterUrl}`;
}

function showContentSelectionError(message) {
    const error = document.querySelector("#contentSelectionError");
    error.textContent = message;
    error.classList.remove("d-none");
}

function hideContentSelectionError() {
    const error = document.querySelector("#contentSelectionError");
    error.textContent = "";
    error.classList.add("d-none");
}

/** 현재 입력값과 선택 작품을 비교 가능한 문자열로 만든다. */
function serializeFormState() {
    return JSON.stringify({
        title: document.querySelector("#title").value,
        description: document.querySelector("#description").value,
        contentIds: selectedContentIds
    });
}

/** 신규 기본값 또는 수정 원본을 미저장 변경 비교 기준으로 저장한다. */
function initializeFormState() {
    initialFormState = serializeFormState();
    isFormInitialized = true;
}

function hasUnsavedChanges() {
    return isFormInitialized
        && serializeFormState() !== initialFormState;
}

/** 취소 링크는 사용자 선택을 받을 수 있도록 명시적인 확인 문구를 표시한다. */
function confirmCancelNavigation(event) {
    if (!hasUnsavedChanges()) {
        return;
    }

    const shouldLeave = window.confirm(
        "변경사항이 저장되지 않았습니다. 페이지를 나가시겠습니까?"
    );

    if (!shouldLeave) {
        event.preventDefault();
        return;
    }

    isSubmitting = true;
}

/** 새로고침·뒤로가기·다른 링크 이동은 브라우저 기본 이탈 경고로 보호한다. */
function warnUnsavedChanges(event) {
    if (isSubmitting || !hasUnsavedChanges()) {
        return;
    }

    event.preventDefault();
    event.returnValue = "";
}

function updateDescriptionLength() {
    // maxlength의 브라우저 차단과 별도로 현재 글자 수를 사용자에게 보여준다.
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
