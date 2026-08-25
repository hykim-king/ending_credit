/**
 * comment_list.js — 코멘트 목록/작성·수정/삭제/좋아요 토글/신고 (학원 fetch 공통 사용)
 * 회원 인증(2조) 연동 전이라 회원ID는 화면 상단 테스트 입력값을 쓴다.
 */
let commentModal;
let reportModal;

document.addEventListener('DOMContentLoaded', () => {

    commentModal = new bootstrap.Modal(document.getElementById('commentModal'));
    reportModal = new bootstrap.Modal(document.getElementById('reportModal'));

    // 글자 수 카운터 (1,000자)
    const cmDetail = document.getElementById('cmDetail');
    cmDetail.addEventListener('input', () => {
        document.getElementById('cmCount').textContent = cmDetail.value.length;
    });

    document.getElementById('btnOpenSave').addEventListener('click', openSaveModal);
    document.getElementById('btnCmSave').addEventListener('click', doSave);
    document.getElementById('btnCmDelete').addEventListener('click', doDeleteInModal);
    document.getElementById('btnRpSave').addEventListener('click', doReport);

    // 카드 버튼은 이벤트 위임으로 처리 (closest/dataset — 학원 모달 패턴, CommentCard)
    document.getElementById('commentTbody').addEventListener('click', (e) => {
        const card = e.target.closest('[data-comment-id]');
        if (!card) {
            return;
        }
        if (e.target.closest('.spoiler-btn')) {
            toggleSpoiler(e.target.closest('.spoiler-guard'), e.target.closest('.spoiler-btn'));
            return;
        }
        if (e.target.closest('.like-btn')) {
            doToggleLike(card, e.target.closest('.like-btn'));
            return;
        }
        if (e.target.closest('.edit-btn')) {
            openEditModal(card);
            return;
        }
        if (e.target.closest('.del-btn')) {
            doDeleteRow(card);
            return;
        }
        if (e.target.closest('.report-btn')) {
            openReportModal(card);
        }
    });
});

// 검색 + 페이지 이동 (hidden pageNo 채우고 GET 폼 제출)
function doSearch(pageNo) {
    document.getElementById('pageNo').value = pageNo;
    document.getElementById('searchForm').submit();
}

// 테스트용 회원ID (로그인 연동 전)
function getMemberId() {
    const memberInput = document.getElementById('memberId');
    if (isEmpty(memberInput, '테스트용 회원ID를 입력하세요.')) {
        return null;
    }
    return memberInput.value.trim();
}

// 작성 모달 열기 (신규)
function openSaveModal() {
    document.getElementById('commentModalTitle').textContent = '코멘트 작성';
    document.getElementById('cmMode').value = 'save';
    document.getElementById('cmCommentId').value = '0';
    document.getElementById('cmTargetType').disabled = false;
    document.getElementById('cmTargetId').disabled = false;
    document.getElementById('cmTargetId').value = '';
    document.getElementById('cmDetail').value = '';
    document.getElementById('cmCount').textContent = '0';
    document.getElementById('cmSpoiler').checked = false;
    document.getElementById('btnCmDelete').classList.add('d-none');
    commentModal.show();
}

// 수정 모달 열기 — 행의 data-* 값으로 채운다. 대상(영화/컬렉션)은 수정 불가
function openEditModal(card) {
    document.getElementById('commentModalTitle').textContent = '코멘트 수정';
    document.getElementById('cmMode').value = 'update';
    document.getElementById('cmCommentId').value = card.dataset.commentId;

    const targetType = document.getElementById('cmTargetType');
    const targetId = document.getElementById('cmTargetId');
    if (card.dataset.contentId) {
        targetType.value = 'content';
        targetId.value = card.dataset.contentId;
    } else {
        targetType.value = 'collection';
        targetId.value = card.dataset.collectionId;
    }
    targetType.disabled = true;
    targetId.disabled = true;

    document.getElementById('cmDetail').value = card.dataset.detail;
    document.getElementById('cmCount').textContent = card.dataset.detail.length;
    document.getElementById('cmSpoiler').checked = ('Y' === card.dataset.spoiler);
    document.getElementById('btnCmDelete').classList.remove('d-none');
    commentModal.show();
}

// 저장 (신규=doSave / 수정=doUpdate)
async function doSave() {
    const detailInput = document.getElementById('cmDetail');
    if (isEmpty(detailInput, '내용을 입력하세요.')) {
        return;
    }
    const mode = document.getElementById('cmMode').value;
    const spoiler = document.getElementById('cmSpoiler').checked ? 'Y' : 'N';

    try {
        let result;
        if ('save' === mode) {
            const memberId = getMemberId();
            if (null === memberId) {
                return;
            }
            const targetIdInput = document.getElementById('cmTargetId');
            if (isEmpty(targetIdInput, '대상 ID를 입력하세요.')) {
                return;
            }
            const data = {
                memberId: memberId,
                commentDetail: detailInput.value.trim(),
                spoiler: spoiler
            };
            if ('content' === document.getElementById('cmTargetType').value) {
                data.contentId = targetIdInput.value.trim();
            } else {
                data.collectionId = targetIdInput.value.trim();
            }
            result = await requestPostForm('/comment/doSave', data);
        } else {
            result = await requestPostForm('/comment/doUpdate', {
                commentId: document.getElementById('cmCommentId').value,
                commentDetail: detailInput.value.trim(),
                spoiler: spoiler
            });
        }

        alert(result.message);
        if ('1' === String(result.id)) {
            commentModal.hide();
            doSearch(1);
        }
    } catch (e) {
        alert(e.message);
    }
}

// 삭제 (수정 모달 안 — MOD-03)
async function doDeleteInModal() {
    await deleteComment(document.getElementById('cmCommentId').value, () => commentModal.hide());
}

// 삭제 (목록 행)
async function doDeleteRow(card) {
    await deleteComment(card.dataset.commentId, null);
}

async function deleteComment(commentId, afterHide) {
    if (!confirm('코멘트를 삭제할까요? 좋아요·신고도 함께 삭제됩니다.')) {
        return;
    }
    try {
        const result = await requestGet('/comment/doDelete', { commentId: commentId });
        alert(result.message);
        if ('1' === String(result.id)) {
            if (afterHide) {
                afterHide();
            }
            doSearch(1);
        }
    } catch (e) {
        alert(e.message);
    }
}

// 좋아요 토글 — 응답 detailMessage에 토글 후 좋아요 수가 실려 온다
async function doToggleLike(card, btn) {
    const memberId = getMemberId();
    if (null === memberId) {
        return;
    }
    try {
        const result = await requestPostForm('/commentLike/upToggleLike', {
            memberId: memberId,
            commentId: card.dataset.commentId
        });
        btn.querySelector('.like-cnt').textContent = result.detailMessage;
    } catch (e) {
        alert(e.message);
    }
}

// 스포일러 가림·해제 (C-08 — 해당 카드만, 새로고침 시 초기화)
function toggleSpoiler(guard, btn) {
    const real = guard.querySelector('.real-detail');
    const notice = guard.querySelector('.text-muted');
    const hidden = real.classList.contains('d-none');

    real.classList.toggle('d-none', !hidden);
    notice.classList.toggle('d-none', hidden);
    btn.textContent = hidden ? '다시 가리기' : '내용 보기';
}

// 신고 모달 (MOD-04)
function openReportModal(card) {
    document.getElementById('rpCommentId').value = card.dataset.commentId;
    document.getElementById('rpDetail').value = '';
    reportModal.show();
}

async function doReport() {
    const memberId = getMemberId();
    if (null === memberId) {
        return;
    }
    const reason = document.getElementById('rpReason').value;
    const detailInput = document.getElementById('rpDetail');

    // 기타(OTHER) 사유는 상세 필수 (CK_REPORT_OTHER_DETAIL)
    if ('OTHER' === reason && isEmpty(detailInput, '기타 사유는 상세 내용을 입력해야 합니다.')) {
        return;
    }

    try {
        const result = await requestPostForm('/report/doSave', {
            reportMemberId: memberId,
            commentId: document.getElementById('rpCommentId').value,
            reason: reason,
            detail: detailInput.value.trim()
        });
        alert(result.message);
        if ('1' === String(result.id)) {
            reportModal.hide();
        }
    } catch (e) {
        alert(e.message);
    }
}
