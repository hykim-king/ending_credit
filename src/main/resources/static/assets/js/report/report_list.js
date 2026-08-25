/**
 * report_list.js — 신고 관리 목록 (AD-09)
 * 검색구분에 따라 검색어 입력칸을 상태 select / 사유 select / 번호 입력으로 바꾼다.
 */
document.addEventListener('DOMContentLoaded', () => {

    const searchDiv = document.getElementById('searchDiv');
    searchDiv.addEventListener('change', switchWordInput);
    switchWordInput(); // 초기 표시

    // 행 클릭 → 상세·처리 화면 (AD-10)
    document.getElementById('reportTbody').addEventListener('click', (e) => {
        const tr = e.target.closest('tr');
        if (!tr || !tr.dataset.reportId) {
            return;
        }
        location.href = '/report/doSelectOne?reportId=' + tr.dataset.reportId;
    });
});

// 검색구분에 맞는 검색어 입력칸만 보여준다
function switchWordInput() {
    const div = document.getElementById('searchDiv').value;
    document.getElementById('wordStatus').classList.toggle('d-none', '10' !== div);
    document.getElementById('wordReason').classList.toggle('d-none', '20' !== div);
    document.getElementById('wordText').classList.toggle('d-none', '30' !== div);
}

// 검색 + 페이지 이동 — 보이는 입력칸의 값을 hidden searchWord에 채워 GET 제출
function doSearch(pageNo) {
    const div = document.getElementById('searchDiv').value;
    let word = '';
    if ('10' === div) {
        word = document.getElementById('wordStatus').value;
    } else if ('20' === div) {
        word = document.getElementById('wordReason').value;
    } else if ('30' === div) {
        word = document.getElementById('wordText').value.trim();
    }
    document.getElementById('searchWord').value = word;
    document.getElementById('pageNo').value = pageNo;
    document.getElementById('searchForm').submit();
}
