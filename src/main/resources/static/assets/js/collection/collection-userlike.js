document.addEventListener('DOMContentLoaded', () => {
    // <body>는 layout.html이 소유하므로 회원 번호는 #likesRoot에서 읽는다.
    const memberId = Number(document.getElementById('likesRoot').dataset.memberId);

    const personTabButton = document.querySelector('#personTabButton');
    const collectionTabButton = document.querySelector('#collectionTabButton');

    document.querySelector('#backToProfileLink').href = `/users/${memberId}`;

    personTabButton.addEventListener('click', () => activateTab('person'));
    collectionTabButton.addEventListener('click', () => activateTab('collection'));

    loadCollectionCount();

    // U-07 개발 명세 2번 · 진입 URL 기본값이 type=person이므로 기본 탭은 인물이다.
    activateTab('person');
});

/**탭 전환 후 해당 유형의 첫 페이지를 조회한다.*/
function activateTab(type) {
    const personTabButton = document.querySelector('#personTabButton');
    const collectionTabButton = document.querySelector('#collectionTabButton');

    personTabButton.classList.toggle('active', type === 'person');
    personTabButton.setAttribute('aria-selected', String(type === 'person'));
    collectionTabButton.classList.toggle('active', type === 'collection');
    collectionTabButton.setAttribute('aria-selected', String(type === 'collection'));

    if (type === 'collection') {
        loadCollectionTab(1);
    } else {
        loadPersonTab(1);
    }
}

/** 좋아한 컬렉션 탭 수 조회 (목록 조회 시 함께 받는 totalCnt로 갱신) */
async function loadCollectionCount() {
    try {
        const memberId = Number(document.getElementById('likesRoot').dataset.memberId);
        const data = await requestGet(`/api/users/${memberId}/likes`, {
            type: 'collection',
            page: 1,
            size: 1
        });

        document.querySelector('#collectionCount').textContent = data.page?.totalCnt || 0;
    } catch (error) {
        showLikeError(error.message);
    }
}

/**좋아한 컬렉션 목록을 조회해 카드로 표시한다.*/
async function loadCollectionTab(pageNo) {
    const memberId = Number(document.getElementById('likesRoot').dataset.memberId);

    hideLikeError();

    try {
        const data = await requestGet(`/api/users/${memberId}/likes`, {
            type: 'collection',
            page: pageNo,
            size: 8
        });

        renderCollectionCards(data.items || []);
        renderLikePagination(data.page || {}, pageNo, loadCollectionTab);

        const totalCnt = data.page?.totalCnt || 0;
        document.querySelector('#collectionCount').textContent = totalCnt;
        document.querySelector('#likeResultCount').textContent = `${totalCnt}개`;
    } catch (error) {
        showLikeError(error.message);
    }
}

/** 좋아한 컬렉션 카드 목록을 렌더링한다. */
function renderCollectionCards(items) {
    const likeList = document.querySelector('#likeList');
    const likeEmpty = document.querySelector('#likeEmpty');

    likeList.replaceChildren();

    if (items.length === 0) {
        likeEmpty.classList.remove('d-none');
        return;
    }

    likeEmpty.classList.add('d-none');

    items.forEach((item) => {
        // 서버 데이터는 innerHTML이 아닌 textContent로 넣어 XSS를 방지한다.
        const column = document.createElement('div');
        column.className = 'col-6 col-md-4 col-lg-3';

        const card = document.createElement('article');
        card.className = 'like-card p-3 d-flex flex-column';

        const title = document.createElement('h2');
        title.className = 'h6';

        const link = document.createElement('a');
        link.className = 'stretched-link text-decoration-none text-dark';
        link.href = `/collections/${item.collectionId}`;
        link.textContent = item.title;
        title.append(link);

        const description = document.createElement('p');
        description.className = 'text-secondary small flex-grow-1';
        description.textContent = item.description || '';

        const visibility = document.createElement('span');
        visibility.className = item.isPublic === 'Y'
            ? 'badge text-bg-success align-self-start'
            : 'badge text-bg-secondary align-self-start';
        visibility.textContent = item.isPublic === 'Y' ? '공개' : '비공개';

        // 본인 프로필일 때만 하트 선택으로 좋아요 해제 가능
        const heartButton = document.createElement('button');
        heartButton.type = 'button';
        heartButton.className = 'like-heart-btn mt-2 align-self-start';
        heartButton.setAttribute('aria-label', '좋아요 취소');
        heartButton.innerHTML = '&#9829;';
        heartButton.addEventListener('click', (event) => {
            event.preventDefault();
            unlikeCollection(item.collectionId);
        });

        card.append(title, description, visibility, heartButton);
        column.append(card);
        likeList.append(column);
    });
}

/**카드의 하트 버튼으로 좋아요를 취소하고 현재 페이지를 다시 불러온다.*/
async function unlikeCollection(collectionId) {
    const memberId = Number(document.getElementById('likesRoot').dataset.memberId);

    try {
        await requestFetch(`/api/collections/${collectionId}/likes`, {
            method: 'DELETE',
            headers: Object.assign(
                { 'Content-Type': 'application/json' },
                getCsrfHeaders()
            ),
            body: JSON.stringify({ memberId })
        });

        loadCollectionTab(1);
    } catch (error) {
        showLikeError(error.message);
    }
}

/**좋아한 인물 탭 조회 (TODO: PersonLikeController 나오면 연동)*/
function loadPersonTab(pageNo) {
    document.querySelector('#likeList').replaceChildren();
    document.querySelector('#likeEmpty').classList.add('d-none');
    document.querySelector('#likeResultCount').textContent = '';
    document.querySelector('#likePagination').replaceChildren();
}

/**좋아요 목록 공통 페이지네이션 렌더링*/
function renderLikePagination(page, currentPage, onPageChange) {
    const pagination = document.querySelector('#likePagination');
    pagination.replaceChildren();

    const pageSize = Number(page.pageSize || 8);
    const totalCount = Number(page.totalCnt || 0);
    const totalPages = Math.ceil(totalCount / pageSize);

    if (totalPages <= 1) {
        return;
    }

    const startPage = Math.floor((currentPage - 1) / 10) * 10 + 1;
    const endPage = Math.min(startPage + 9, totalPages);

    pagination.append(createLikePageButton('이전', startPage - 1, startPage === 1, false, onPageChange));

    for (let pageNo = startPage; pageNo <= endPage; pageNo += 1) {
        pagination.append(createLikePageButton(
            String(pageNo),
            pageNo,
            false,
            pageNo === currentPage,
            onPageChange
        ));
    }

    pagination.append(createLikePageButton('다음', endPage + 1, endPage === totalPages, false, onPageChange));
}

function createLikePageButton(label, pageNo, disabled, active, onPageChange) {
    const item = document.createElement('li');
    item.className = `page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}`;

    const button = document.createElement('button');
    button.className = 'page-link';
    button.type = 'button';
    button.textContent = label;
    button.disabled = disabled;
    button.addEventListener('click', () => onPageChange(pageNo));

    item.append(button);
    return item;
}

function showLikeError(message) {
    const errorMessage = document.querySelector('#errorMessage');
    errorMessage.textContent = message;
    errorMessage.classList.remove('d-none');
}

function hideLikeError() {
    const errorMessage = document.querySelector('#errorMessage');
    errorMessage.textContent = '';
    errorMessage.classList.add('d-none');
}