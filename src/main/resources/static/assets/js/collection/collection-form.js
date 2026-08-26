// 등록과 수정은 필드 구성이 같으므로 하나의 form.html과 JavaScript를 재사용한다.
document.addEventListener("DOMContentLoaded", () => {
    // View Controller가 body의 data-* 속성에 넣은 모드와 컬렉션 번호를 읽는다.
    const formMode = document.body.dataset.formMode;
    const collectionId = Number(document.body.dataset.collectionId);
    const collectionForm = document.querySelector("#collectionForm");
    const description = document.querySelector("#description");

    description.addEventListener("input", updateDescriptionLength);
    collectionForm.addEventListener("submit", submitCollection);

    // 수정 화면에서만 기존 데이터를 API로 읽어 입력란에 채운다.
    if (formMode === "update") {
        prepareUpdateForm(collectionId);
    }
});

async function prepareUpdateForm(collectionId) {
    const errorMessage = document.querySelector("#errorMessage");

    document.querySelector("#formTitle").textContent = "컬렉션 수정";
    document.querySelector("#submitButton").textContent = "수정";
    document.querySelector("#cancelLink").href = `/collections/${collectionId}`;

    try {
        const collection = await requestGet(`/api/collections/${collectionId}`);

		// 수정 시 작성자 번호를 바꾸면 소유 관계가 달라지므로 기존 값으로 고정한다.
		const memberId = document.querySelector("#memberId");
		memberId.value = collection.memberId;
		memberId.readOnly = true;

        document.querySelector("#title").value = collection.title || "";
        document.querySelector("#description").value = collection.description || "";
        document.querySelector("#isPublic").value = collection.isPublic || "Y";

        updateDescriptionLength();
    } catch (error) {
        showFormError(errorMessage, error.message);
    }
}

async function submitCollection(event) {
    // form의 기본 페이지 이동을 막고 Fetch로 JSON 요청을 보낸다.
    event.preventDefault();

    const errorMessage = document.querySelector("#errorMessage");
    const formMode = document.body.dataset.formMode;
    const collectionId = Number(document.body.dataset.collectionId);
    const submitButton = document.querySelector("#submitButton");

    hideFormError(errorMessage);
    // 사용자가 저장 버튼을 연속 클릭해 중복 요청하는 것을 막는다.
    submitButton.disabled = true;

	// 로그인 기능 병합 전에는 화면에서 입력한 DB 회원 번호를 요청에 포함한다.
    const data = {
		memberId: Number(document.querySelector("#memberId").value),
        title: document.querySelector("#title").value.trim(),
        description: document.querySelector("#description").value.trim(),
        isPublic: document.querySelector("#isPublic").value
    };

    try {
        // 등록은 POST, 수정은 PUT을 사용하지만 성공 후에는 모두 상세 화면으로 이동한다.
        const saved = formMode === "update"
            ? await requestPut(`/api/collections/${collectionId}`, data)
            : await requestPost("/api/collections", data);

        window.location.href = `/collections/${saved.collectionId}`;
    } catch (error) {
        showFormError(errorMessage, error.message);
        submitButton.disabled = false;
    }
}

function requestPut(url, data) {
    // 공통 파일에 PUT 도우미가 없어 requestFetch를 이용해 이 화면에 필요한 요청을 구성한다.
    // CSRF 메타 태그가 존재하면 getCsrfHeaders가 자동으로 헤더를 추가한다.
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
