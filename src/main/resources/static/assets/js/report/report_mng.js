/**
 * report_mng.js — 신고 상세·처리 (AD-10)
 * 반려 = /report/doUpdate(REJECTED) · 승인 = /report/upApproveReport(코멘트 삭제까지 한 트랜잭션)
 */
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('btnReject').addEventListener('click', doReject);
    document.getElementById('btnApprove').addEventListener('click', doApprove);
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
        const result = await requestPostForm('/report/doUpdate', param);
        alert(result.message);
        if ('1' === String(result.id)) {
            location.href = '/report/doRetrieve';
        }
    } catch (e) {
        alert(e.message);
    }
}

// 신고 승인 — 코멘트 삭제 + 신고 이력 CASCADE 소멸
async function doApprove() {
    const param = getProcessParam();
    if (null === param) {
        return;
    }
    if (!confirm('신고를 승인할까요? 신고된 코멘트가 삭제되고 이 신고 이력도 함께 사라집니다.')) {
        return;
    }

    try {
        const result = await requestPostForm('/report/upApproveReport', param);
        alert(result.message);
        if ('1' === String(result.id)) {
            location.href = '/report/doRetrieve';
        }
    } catch (e) {
        alert(e.message);
    }
}
