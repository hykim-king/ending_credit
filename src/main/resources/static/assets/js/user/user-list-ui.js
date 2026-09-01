/**
 * Modification History
 * 2026. 8. 31. jinyoung - 회원 목록 화면의 이미지 URL과 페이지네이션 공통 처리 분리
 */
(() => {
    /** TMDB 상대 경로와 이미 완성된 외부 이미지 URL을 모두 표시한다. */
    function resolveTmdbImageUrl(imageUrl, size) {
        if (/^https?:\/\//i.test(imageUrl)) {
            return imageUrl;
        }

        return `https://image.tmdb.org/t/p/${size}${imageUrl}`;
    }

    /** 목록 응답의 전체 건수와 페이지 크기로 페이지 이동 버튼을 생성한다. */
    function renderPagination(options) {
        const {
            container,
            page,
            currentPage,
            defaultPageSize,
            onPageChange
        } = options;
        const pageSize = Number(page.pageSize || defaultPageSize);
        const totalCount = Number(page.totalCnt || 0);
        const totalPages = Math.ceil(totalCount / pageSize);

        container.replaceChildren();

        if (totalPages <= 1) {
            return;
        }

        const startPage = Math.floor((currentPage - 1) / 10) * 10 + 1;
        const endPage = Math.min(startPage + 9, totalPages);

        container.append(
            createPageButton("이전", startPage - 1, startPage === 1, false, onPageChange)
        );

        for (let pageNo = startPage; pageNo <= endPage; pageNo += 1) {
            container.append(
                createPageButton(
                    String(pageNo),
                    pageNo,
                    false,
                    pageNo === currentPage,
                    onPageChange
                )
            );
        }

        container.append(
            createPageButton("다음", endPage + 1, endPage === totalPages, false, onPageChange)
        );
    }

    function createPageButton(label, pageNo, disabled, selected, onPageChange) {
        const item = document.createElement("li");
        const button = document.createElement("button");

        item.className =
            `page-item${disabled ? " disabled" : ""}`
            + `${selected ? " active" : ""}`;
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

    window.UserListUi = Object.freeze({
        renderPagination,
        resolveTmdbImageUrl
    });
})();
