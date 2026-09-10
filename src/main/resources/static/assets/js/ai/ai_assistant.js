/**
 * AI 도우미 - 떠 있는 버튼 + 검색 패널 + 선제 말풍선
 *
 * 서버는 /api/ai/search 하나만 부른다. 대화는 하지 않는다(되묻기 없음).
 * 검색 결과가 0건인 화면에서는 말풍선이 먼저 말을 건다.
 */
(function () {
    'use strict';

    var SEARCH_API = '/api/ai/search';

    /** 말풍선이 뜨기까지 기다리는 시간. 바로 뜨면 놀란다 */
    var TEASER_DELAY_MS = 1800;

    /** 홈에서는 조금 더 여유를 두고 말을 건다 */
    var HOME_TEASER_DELAY_MS = 3000;

    /** 닫은 뒤 다시 안 띄우려고 쓰는 표시 */
    var TEASER_DISMISSED_KEY = 'endit.ai.teaserDismissed';

    var fab, panel, teaser, queryInput, statusBox, msgBox, resultBox;
    var teaserTimer = null;

    document.addEventListener('DOMContentLoaded', function () {
        fab = document.getElementById('aiFab');
        if (null === fab) {
            return;                       // 조각이 없는 화면이면 아무것도 하지 않는다
        }

        panel = document.getElementById('aiPanel');
        teaser = document.getElementById('aiTeaser');
        queryInput = document.getElementById('aiQuery');
        statusBox = document.getElementById('aiStatus');
        msgBox = document.getElementById('aiMsg');
        resultBox = document.getElementById('aiResults');

        bindEvents();
        scheduleTeaser();
    });

    function bindEvents() {
        fab.addEventListener('click', togglePanel);
        document.getElementById('aiPanelClose').addEventListener('click', closePanel);
        document.getElementById('aiSubmit').addEventListener('click', doSearch);
        document.getElementById('aiTeaserClose').addEventListener('click', dismissTeaser);

        // 말풍선을 누르면 그 자리에서 패널이 열린다
        teaser.addEventListener('click', function (e) {
            if (e.target.id !== 'aiTeaserClose') {
                hideTeaser();
                openPanel();
            }
        });

        queryInput.addEventListener('keydown', function (e) {
            if ('Enter' === e.key) {
                doSearch();
            }
        });

        document.addEventListener('keydown', function (e) {
            if ('Escape' === e.key && false === panel.classList.contains('d-none')) {
                closePanel();
            }
        });
    }

    /* -- 패널 여닫기 ---------------------------------------------------- */

    function togglePanel() {
        if (panel.classList.contains('d-none')) {
            openPanel();
        } else {
            closePanel();
        }
    }

    function openPanel() {
        hideTeaser();
        panel.classList.remove('d-none');
        fab.setAttribute('aria-expanded', 'true');

        // 검색 화면에서 열었으면 그때 친 검색어를 미리 채워 둔다
        if ('' === queryInput.value) {
            var q = new URLSearchParams(location.search).get('query');
            if (q) {
                queryInput.value = q;
            }
        }
        queryInput.focus();
    }

    function closePanel() {
        panel.classList.add('d-none');
        fab.setAttribute('aria-expanded', 'false');
        fab.focus();
    }

    /* -- 선제 말풍선 ---------------------------------------------------- */

    /**
     * 말풍선을 예약한다.
     *   - 홈: 3초 뒤 "제목을 몰라도 찾을 수 있다"고 알린다
     *   - 검색 0건: 1.8초 뒤 "줄거리로 찾아주겠다"고 말을 건다
     * 화면(1조 파일)을 고치지 않고 렌더된 결과를 읽어 판단한다.
     */
    function scheduleTeaser() {
        if ('1' === sessionStorage.getItem(TEASER_DISMISSED_KEY)) {
            return;                       // 이미 닫았으면 이 세션에서는 다시 안 띄운다
        }

        var delay;

        if (isEmptySearchResult()) {
            delay = TEASER_DELAY_MS;      // 문구는 조각의 기본값(검색용) 그대로
        } else if ('/' === location.pathname) {
            delay = HOME_TEASER_DELAY_MS;
            document.getElementById('aiTeaserTitle').textContent = '제목이 기억나지 않으세요?';
            document.getElementById('aiTeaserBody').textContent = '떠오르는 대로 적으면 찾아드려요.';
        } else {
            return;
        }

        teaserTimer = setTimeout(function () {
            teaser.classList.remove('d-none');
        }, delay);
    }

    /** 지금 화면이 "검색했는데 결과가 0건"인 상태인가 */
    function isEmptySearchResult() {
        if (0 !== location.pathname.indexOf('/search')) {
            return false;
        }

        var query = new URLSearchParams(location.search).get('query');
        if (null === query || '' === query.trim()) {
            return false;                 // 검색어 없이 들어온 시작 화면
        }

        // 결과가 하나도 없으면 화면이 안내문만 그린다(.section-count 가 아예 없다)
        if (null !== document.querySelector('.search-empty')) {
            return true;
        }

        var counts = document.querySelectorAll('.section-count');
        if (0 === counts.length) {
            return false;
        }

        // 한 건이라도 찾았으면 굳이 말을 걸지 않는다
        for (var i = 0; i < counts.length; i++) {
            if ('0건' !== counts[i].textContent.trim()) {
                return false;
            }
        }
        return true;
    }

    function hideTeaser() {
        if (null !== teaserTimer) {
            clearTimeout(teaserTimer);
            teaserTimer = null;
        }
        teaser.classList.add('d-none');
    }

    function dismissTeaser(e) {
        e.stopPropagation();
        hideTeaser();
        sessionStorage.setItem(TEASER_DISMISSED_KEY, '1');
    }

    /* -- 검색 ----------------------------------------------------------- */

    function doSearch() {
        var query = queryInput.value.trim();
        if ('' === query) {
            queryInput.focus();
            return;
        }

        showStatus(true);
        clearResult();

        fetch(SEARCH_API + '?query=' + encodeURIComponent(query))
            .then(function (res) {
                if (false === res.ok) {
                    throw new Error('검색에 실패했습니다.');
                }
                return res.json();
            })
            .then(render)
            .catch(function (err) {
                showMessage(err.message);
            })
            .finally(function () {
                showStatus(false);
            });
    }

    function render(data) {
        if (data.message) {
            showMessage(data.message);
        }

        // 사이트 사용법 안내 - 본문은 서버가 보관한 원문 그대로
        if (data.help) {
            resultBox.appendChild(buildHelpCard(data.help));
            return;
        }

        // 공지 검색 결과
        var notices = data.notices || [];
        if (notices.length > 0) {
            notices.forEach(function (n) {
                resultBox.appendChild(buildNoticeRow(n));
            });
            return;
        }

        var items = data.items || [];
        if (0 === items.length) {
            if (!data.message) {
                showMessage('조건에 맞는 영화가 없습니다. 다른 낱말로 찾아보세요.');
            }
            return;
        }

        items.forEach(function (item) {
            resultBox.appendChild(buildResultRow(item));
        });
    }

    /** 사용법 안내 카드 - 제목 + 본문 + (있으면) 바로가기 */
    function buildHelpCard(help) {
        var box = document.createElement('div');
        box.className = 'ai-help';

        var title = document.createElement('div');
        title.className = 'ai-help-title';
        title.textContent = help.title;

        var body = document.createElement('div');
        body.className = 'ai-help-body';
        body.textContent = help.body;

        box.appendChild(title);
        box.appendChild(body);

        // 링크는 우리 사이트 안 경로만 단다. FaqAnswers 가 내부 경로만 주지만,
        // 나중에 출처가 바뀌어도 javascript: 같은 스킴이 끼어들지 못하게 막는다
        if (isInternalPath(help.linkUrl)) {
            var a = document.createElement('a');
            a.className = 'btn btn-sm btn-outline-primary mt-2';
            a.href = help.linkUrl;
            a.textContent = help.linkLabel || '바로가기';
            box.appendChild(a);
        }
        return box;
    }

    /** "/경로" 꼴의 사이트 내부 주소인가 ("//다른사이트" 는 아님) */
    function isInternalPath(url) {
        return 'string' === typeof url
            && 0 === url.indexOf('/')
            && 0 !== url.indexOf('//');
    }

    /** 공지 한 줄 - 누르면 공지 상세로 */
    function buildNoticeRow(n) {
        var a = document.createElement('a');
        a.className = 'ai-result';
        a.href = '/notices/' + n.noticeId;

        var icon = document.createElement('span');
        icon.className = 'ai-notice-icon bi bi-megaphone';

        var box = document.createElement('span');

        var title = document.createElement('span');
        title.className = 'ai-result-title d-block';
        title.textContent = n.title;

        var meta = document.createElement('span');
        meta.className = 'ai-result-meta';
        meta.textContent = n.createdDt || '';

        box.appendChild(title);
        box.appendChild(meta);
        a.appendChild(icon);
        a.appendChild(box);
        return a;
    }

    function buildResultRow(item) {
        var a = document.createElement('a');
        a.className = 'ai-result';
        a.href = '/movies/' + item.contentId;

        var poster = document.createElement('span');
        poster.className = 'ai-poster';
        if (item.posterUrl) {
            poster.style.backgroundImage = 'url("' + item.posterUrl + '")';
        }

        var box = document.createElement('span');

        var title = document.createElement('span');
        title.className = 'ai-result-title d-block';
        title.textContent = item.titleKo || item.titleOrg || '(제목 없음)';

        var meta = document.createElement('span');
        meta.className = 'ai-result-meta';
        meta.textContent = buildMeta(item);

        box.appendChild(title);
        box.appendChild(meta);
        a.appendChild(poster);
        a.appendChild(box);

        return a;
    }

    function buildMeta(item) {
        var parts = [];
        if (item.releaseYear) {
            parts.push(item.releaseYear);
        }
        if (item.avgRating) {
            parts.push('별점 ' + item.avgRating);
        }
        if (item.commentCnt > 0) {
            parts.push('코멘트 ' + item.commentCnt);
        }
        return parts.join(' · ');
    }

    /* -- 표시 도우미 ---------------------------------------------------- */

    function showStatus(on) {
        statusBox.classList.toggle('d-none', false === on);
    }

    function showMessage(text) {
        msgBox.textContent = text;
        msgBox.classList.remove('d-none');
    }

    function clearResult() {
        resultBox.replaceChildren();
        msgBox.classList.add('d-none');
    }

})();
