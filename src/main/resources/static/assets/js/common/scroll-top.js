/*
 * 맨 위로 버튼. 홈·영화 상세·관리자 목록이 함께 쓴다 - 화면마다 다시 적지 않는다.
 * 마크업은 fragments/scroll_top.html이 그리고, 이 파일은 #scrollTopButton 하나를 찾아 동작만 건다.
 */
(function () {
    "use strict";

    // 이 아래로는 헤더가 아직 가까워 버튼이 방해만 된다
    var SCROLL_TOP_THRESHOLD = 150;

    document.addEventListener("DOMContentLoaded", function () {
        var button = document.getElementById("scrollTopButton");

        if (!button) {
            return;
        }

        function update() {
            button.hidden = window.scrollY < SCROLL_TOP_THRESHOLD;
        }

        // 스크롤마다 부르므로 passive로 둔다 - 여기서 기본 동작을 막을 일이 없다
        window.addEventListener("scroll", update, { passive: true });
        button.addEventListener("click", function () {
            window.scrollTo({ top: 0, behavior: "smooth" });
        });
        // 새로고침이 화면 중간에서 되살아나는 경우가 있어 처음에도 한 번 본다
        update();
    });
})();
