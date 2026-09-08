/**
 * member_list.js — 회원 관리 목록 (AD-07)
 * 강퇴는 회원 행 삭제라 되돌릴 수 없다. 닉네임을 확인창에 보여 오조작을 막는다.
 */
document.addEventListener('DOMContentLoaded', () => {

    const tbody = document.getElementById('memberTbody');
    if (!tbody) {
        return;
    }

    // 강퇴 버튼은 행마다 있어 이벤트 위임으로 처리한다
    tbody.addEventListener('click', (e) => {
        if (!e.target.closest('.ban-btn')) {
            return;
        }
        const tr = e.target.closest('[data-member-id]');
        if (!tr) {
            return;
        }
        doWithdraw(tr.dataset.memberId, tr.dataset.nickname);
    });
});

// 검색 + 페이지 이동
function doSearch(pageNo) {
    document.getElementById('pageNo').value = pageNo;
    document.getElementById('searchForm').submit();
}

// 강퇴 — FK CASCADE로 그 회원의 코멘트·좋아요·컬렉션·평가까지 함께 삭제된다
async function doWithdraw(memberId, nickname) {
    const msg = '[' + nickname + '] 회원을 강퇴할까요?\n\n'
              + '코멘트·좋아요·컬렉션·평가 기록이 모두 함께 삭제되며 되돌릴 수 없습니다.';
    if (!confirm(msg)) {
        return;
    }

    try {
        const result = await requestPostForm('/admin/users/upWithdrawMember', { memberId: memberId });
        alert(result.message);
        if ('1' === String(result.id)) {
            doSearch(1);
        }
    } catch (e) {
        alert(e.message);
    }
}
