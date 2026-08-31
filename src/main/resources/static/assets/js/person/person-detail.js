const personId = Number(document.body.dataset.personId);

document.addEventListener("DOMContentLoaded", () => {
    if (!personId) {
        showDetailError(document.querySelector("#errorMessage"), "잘못된 인물 번호입니다.");
        return;
    }
    loadPersonDetail();
});

/** 인물 상세 + 참여 작품 조회 */
async function loadPersonDetail() {
    const errorMessage = document.querySelector("#errorMessage");

    try {
        const data = await requestGet(`/api/people/${personId}`);
        renderPerson(data.person);
        renderFilmography(data.filmography || []);
    } catch (error) {
        showDetailError(errorMessage, error.message || "인물 정보를 불러오지 못했습니다.");
    }
}

/** 프로필 영역 렌더링 */
function renderPerson(person) {
    const displayName = resolveDisplayName(person);

    document.title = `${displayName} | ENDIT`;
    document.querySelector("#personName").textContent = displayName;

    const nameOrgEl = document.querySelector("#personNameOrg");
    if (person.nameOrg && person.nameOrg !== person.nameKo) {
        nameOrgEl.textContent = person.nameOrg;
        nameOrgEl.classList.remove("d-none");
    } else {
        nameOrgEl.classList.add("d-none");
    }

    const profileImage = document.querySelector("#profileImage");
    const profilePlaceholder = document.querySelector("#profilePlaceholder");

    if (person.profileImageUrl) {
        profileImage.src = person.profileImageUrl;
        profileImage.alt = `${displayName} 프로필`;
        profileImage.classList.remove("d-none");
        profilePlaceholder.classList.add("d-none");
    } else {
        profileImage.classList.add("d-none");
        profilePlaceholder.classList.remove("d-none");
    }
}

/** 참여 작품 목록 렌더링 */
function renderFilmography(items) {
    const listEl = document.querySelector("#filmographyList");
    const emptyEl = document.querySelector("#filmographyEmpty");
    const countEl = document.querySelector("#filmographyCount");

    countEl.textContent = `${items.length}편`;
    listEl.innerHTML = "";

    if (items.length === 0) {
        emptyEl.classList.remove("d-none");
        return;
    }

    emptyEl.classList.add("d-none");

    items.forEach((item) => {
        listEl.appendChild(createFilmographyCard(item));
    });
}

/** 참여 작품 카드 1건 */
function createFilmographyCard(item) {
    const col = document.createElement("div");
    col.className = "col";

    const title = item.titleKo || "제목 없음";
    const roleText = formatRole(item.role, item.character);
    const year = item.releaseYear ? item.releaseYear.substring(0, 4) : "";

    col.innerHTML = `
        <article class="filmography-card h-100">
            <div class="filmography-poster-wrap">
                ${item.posterUrl
                    ? `<img class="filmography-poster" src="${escapeHtml(item.posterUrl)}" alt="${escapeHtml(title)} 포스터">`
                    : `<div class="filmography-poster-placeholder"><i class="bi bi-film"></i></div>`}
            </div>
            <div class="filmography-body">
                <h3 class="filmography-title">${escapeHtml(title)}</h3>
                <p class="filmography-meta mb-0">${escapeHtml(roleText)}${year ? ` · ${escapeHtml(year)}` : ""}</p>
            </div>
        </article>
    `;

    return col;
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

function formatRole(role, character) {
    const roleLabel = role === "DIRECTOR" ? "감독" : role === "ACTOR" ? "배우" : (role || "참여");

    if (character && character.trim()) {
        return `${roleLabel} · ${character.trim()}`;
    }
    return roleLabel;
}

function showDetailError(errorMessage, message) {
    errorMessage.textContent = message;
    errorMessage.classList.remove("d-none");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}
