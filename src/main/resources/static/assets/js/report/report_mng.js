/**
 * report_mng.js — 신고 상세·처리 (AD-10)
 * 반려 = /admin/report/doUpdate(REJECTED) · 승인 = /admin/report/upApproveReport(상태만 ACCEPTED — 삭제 없음, 팀 결정)
 */
document.addEventListener('DOMContentLoaded', () => {
    const btnReject = document.getElementById('btnReject');
    const btnApprove = document.getElementById('btnApprove');
    if (null === btnReject || null === btnApprove) {
        return; // 완료된 신고 — 처리폼이 없다 (이력만 표시)
    }
    btnReject.addEventListener('click', doReject);
    btnApprove.addEventListener('click', doApprove);
});

// 완료 처리는 처리자·처리 내용이 필수 (CK_REPORT_PROCESS_COMPLETE)
function getProcessParam() {
    const processorInput = document.getElementById('processorId');
    if (isEmpty(processorInput, '처리 관리자 회원ID를 입력하세요.')) {
        return null;
    }
    const noteInput = document.getElementById('processNote');
    if (isEmpty(noteInput, '처리 내용을 입력하세요.')) {
        return null;
    }
    return {
        reportId: document.getElementById('reportId').value,
        processedByMemberId: processorInput.value.trim(),
        processNote: noteInput.value.trim()
    };
}

// 신고 반려 (REJECTED — 코멘트는 유지)
async function doReject() {
    const param = getProcessParam();
    if (null === param) {
        return;
    }
    if (!confirm('신고를 반려할까요? 코멘트는 유지됩니다.')) {
        return;
    }
    param.status = 'REJECTED';

    try {
        const result = await requestPostForm('/admin/report/doUpdate', param);
        alert(result.message);
        if ('1' === String(result.id)) {
            location.href = '/admin/report/doRetrieve';
        }
    } catch (e) {
        alert(e.message);
    }
}

// 신고 승인 — 신고 상태만 ACCEPTED로 저장(삭제 없음), 댓글은 목록에서 안내 문구로 가려진다
async function doApprove() {
    const param = getProcessParam();
    if (null === param) {
        return;
    }
    if (!confirm('신고를 승인할까요? 댓글은 삭제되지 않고, 목록에서 안내 문구로 가려집니다.')) {
        return;
    }

    try {
        const result = await requestPostForm('/admin/report/upApproveReport', param);
        alert(result.message);
        if ('1' === String(result.id)) {
            location.href = '/admin/report/doRetrieve';
        }
    } catch (e) {
        alert(e.message);
    }
}
