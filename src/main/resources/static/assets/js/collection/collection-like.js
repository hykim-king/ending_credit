(function () {
    'use strict';

    const TEMP_MEMBER_ID = 1;

    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        const collectionId = Number(document.body.dataset.collectionId);

        if (!collectionId) {
            return;
        }

        const { button, icon } = createLikeButton();
        mountButton(button);

        const liked = await checkLiked(collectionId);
        applyLikedState(button, icon, liked);

        button.addEventListener('click', () => toggleLike(collectionId, button, icon));
    }

    /** 좋아요 버튼과 아이콘 DOM을 생성 (부트스트랩 기본 클래스만 사용) */
    function createLikeButton() {
        const button = document.createElement('button');
        button.id = 'likeButton';
        button.type = 'button';
        button.className = 'btn btn-outline-dark';
        button.setAttribute('aria-pressed', 'false');
        button.setAttribute('aria-label', '좋아요');

        const icon = document.createElement('span');
        icon.id = 'likeIcon';
        icon.setAttribute('aria-hidden', 'true');
        icon.innerHTML = '&#9825;';

        button.append(icon, document.createTextNode(' 좋아요'));

        return { button: button, icon: icon };
    }

    /** 수정 버튼(#editLink) 앞에 좋아요 버튼을 끼워 넣는다 */
    function mountButton(button) {
        const editLink = document.getElementById('editLink');

        if (editLink && editLink.parentElement) {
            editLink.parentElement.insertBefore(button, editLink);
        }
    }

    /** 회원의 좋아요한 컬렉션 목록에 현재 컬렉션이 포함되어 있는지 확인 */
    async function checkLiked(collectionId) {
        try {
            const data = await requestGet('/api/users/' + TEMP_MEMBER_ID + '/likes', {
                type: 'collection',
                page: 1,
                size: 100
            });

            return (data.items || []).some(function (item) {
                return item.collectionId === collectionId;
            });
        } catch (error) {
            return false;
        }
    }

    /** 좋아요 등록/취소 요청을 보내고 결과에 따라 화면을 갱신 */
    async function toggleLike(collectionId, button, icon) {
        const currentlyLiked = button.classList.contains('like-liked');
        const method = currentlyLiked ? 'DELETE' : 'POST';

        button.disabled = true;

        try {
            await requestFetch('/api/collections/' + collectionId + '/likes', {
                method: method,
                headers: Object.assign(
                    { 'Content-Type': 'application/json' },
                    getCsrfHeaders()
                ),
                body: JSON.stringify({ memberId: TEMP_MEMBER_ID })
            });

            applyLikedState(button, icon, !currentlyLiked);
            adjustLikeCount(currentlyLiked ? -1 : 1);
        } catch (error) {
            showError(error.message);
        } finally {
            button.disabled = false;
        }
    }

    /**
     * 버튼/아이콘을 좋아요 상태에 맞게 갱신
     * U-07 카드 하트(collection-userlike.js)와 동일하게 --endit-primary(보라색)로 통일한다.
     */
    function applyLikedState(button, icon, liked) {
        button.setAttribute('aria-pressed', String(liked));
        icon.innerHTML = liked ? '&#9829;' : '&#9825;';
        icon.style.color = liked ? 'var(--endit-primary)' : '';

        if (liked) {
            button.classList.add('like-liked');
        } else {
            button.classList.remove('like-liked');
        }
    }

    /** #likeCount 표시 값을 delta만큼 조정 */
    function adjustLikeCount(delta) {
        const likeCountEl = document.getElementById('likeCount');

        if (!likeCountEl) {
            return;
        }

        const current = parseInt(likeCountEl.textContent, 10) || 0;
        likeCountEl.textContent = String(Math.max(current + delta, 0));
    }

    /** 공통 오류 영역(#errorMessage)에 메시지 표시 */
    function showError(message) {
        const errorMessage = document.getElementById('errorMessage');

        if (!errorMessage) {
            return;
        }

        errorMessage.textContent = message;
        errorMessage.classList.remove('d-none');
    }
})();