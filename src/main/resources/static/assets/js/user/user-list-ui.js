/**
 * Modification History
 * 2026. 8. 31. jinyoung - 회원 목록 화면의 이미지 URL과 페이지네이션 공통 처리 분리
 * 2026. 9. 03. jinyoung - 표시 페이지 수 옵션 설정 추가
 */
(() => {
    /** ===================================
     *  이미지 URL 처리
     *  =================================== */

    /** TMDB 이미지 URL 정규화 */
    function resolveTmdbImageUrl(imageUrl, size) {
        if (/^https?:\/\//i.test(imageUrl)) {
            return imageUrl;
        }

        return `https://image.tmdb.org/t/p/${size}${imageUrl}`;
    }

    /** ===================================
     *  공통 페이지네이션
     *  =================================== */

    /** 공통 목록 페이지네이션 렌더링 */
    function renderPagination(options) {
        const {
            container,
            page,
            currentPage,
            defaultPageSize,
            maxVisiblePages = 10,
            onPageChange
        } = options;
        const pageSize = Number(page.pageSize || defaultPageSize); // 페이지 크기
        const totalCount = Number(page.totalCnt || 0); // 전체 항목 수
        const totalPages = Math.ceil(totalCount / pageSize); // 전체 페이지 수

        container.replaceChildren();

        if (totalPages <= 1) {
            return;
        }

        const pageBlockSize = Math.max(1, Math.trunc(Number(maxVisiblePages) || 10)); // 한 구간의 페이지 수
        // 현재 페이지가 속한 구간의 시작과 끝 번호만 화면에 표시한다.
        const startPage = Math.floor((currentPage - 1) / pageBlockSize) * pageBlockSize + 1;
        const endPage = Math.min(startPage + pageBlockSize - 1, totalPages);

        container.append(createPageButton("이전", startPage - 1, startPage === 1, false, onPageChange));

        for (let pageNo = startPage;pageNo <= endPage;pageNo += 1) {
            container.append(createPageButton(String(pageNo), pageNo, false, pageNo === currentPage, onPageChange));
        }

        container.append(createPageButton("다음", endPage + 1, endPage === totalPages, false, onPageChange));
    }

    /** 단일 페이지 버튼 생성 */
    function createPageButton(label, pageNo, disabled, selected, onPageChange) {

        const item = document.createElement("li");
        const button = document.createElement("button");

        item.className = `page-item${disabled ? " disabled" : ""}${selected ? " active" : ""}`;
        button.className = "page-link";
        button.type = "button";
        button.textContent = label;
        button.disabled = disabled;

        if (selected) {
            button.setAttribute("aria-current", "page");
        }

        button.addEventListener("click", () => onPageChange(pageNo));
        item.append(button);

        return item;
    }

    /** ===================================
     *  공통 기능 공개
     *  =================================== */

    window.UserListUi = Object.freeze({ renderPagination, resolveTmdbImageUrl });
})();
