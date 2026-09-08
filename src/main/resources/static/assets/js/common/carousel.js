/*
 * 가로 캐러셀 한 벌. 홈 선반·상세 갤러리·담긴 컬렉션이 같은 골격을 쓴다.
 * 세 곳에 복사돼 있던 것을 모았다 - 같은 수정을 세 번 하지 않으려는 것이 목적이므로,
 * 새 캐러셀이 생기면 여기에 옵션을 더하고 골격을 다시 베끼지 않는다.
 *
 * 칸 수와 간격은 CSS 변수가 유일한 출처다. 화면 폭에 따라 미디어쿼리가 바꾸므로
 * 호출부가 숫자를 다시 적으면 선반마다 이동량이 어긋난다.
 */
(function () {
	"use strict";

	// 폭에 따라 달라지는 CSS 변수를 읽는다. 못 읽으면 호출부가 준 폴백을 쓴다
	function readCssVar(el, name, fallback) {
		const value = parseInt(getComputedStyle(el).getPropertyValue(name), 10);

		return isNaN(value) ? fallback : value;
	}

	/*
	 * options
	 *   viewport        칸 수·간격 CSS 변수가 붙어 있는 바깥 요소
	 *   track           좌우로 밀리는 요소. 자식 하나가 한 칸이다
	 *   prevBtn/nextBtn 넘길 쪽이 없으면 hidden으로 감춘다(흐리게 두지 않는다)
	 *   perViewVar/gapVar   CSS 변수 이름
	 *   perViewFallback/gapFallback  변수를 못 읽었을 때 값
	 *   onUpdate        (viewport, track) 화살표 세로 위치처럼 캐러셀마다 다른 뒷정리
	 *
	 * 칸이 하나도 없으면 아무것도 걸지 않고 false를 돌려준다.
	 */
	function create(options) {
		const viewport = options.viewport;
		const track = options.track;
		const prevBtn = options.prevBtn;
		const nextBtn = options.nextBtn;

		if (!viewport || !track || !prevBtn || !nextBtn || track.children.length === 0) {
			return false;
		}

		const items = track.children;
		let perView = options.perViewFallback;
		let startIndex = 0;

		function update() {
			const itemWidth = items[0].getBoundingClientRect().width;

			// 창을 줄인 뒤에도 화살표가 한 번에 한 화면씩 넘어가도록 매번 다시 읽는다
			perView = readCssVar(viewport, options.perViewVar, options.perViewFallback);

			const gap = readCssVar(viewport, options.gapVar, options.gapFallback);

			// 칸이 늘어나면 마지막 화면이 앞당겨지므로 넘어가 있던 위치를 끌어온다
			startIndex = Math.min(startIndex, Math.max(0, items.length - perView));

			if (options.onUpdate) {
				options.onUpdate(viewport, track);
			}

			track.style.transform = "translateX(-" + startIndex * (itemWidth + gap) + "px)";
			// 넘어갈 쪽이 없으면(한 화면에 다 들어가는 경우 포함) 흐리게 두지 않고 감춘다
			prevBtn.hidden = startIndex === 0;
			nextBtn.hidden = startIndex + perView >= items.length;
		}

		prevBtn.addEventListener("click", () => {
			startIndex = Math.max(0, startIndex - perView);
			update();
		});
		nextBtn.addEventListener("click", () => {
			startIndex = Math.min(items.length - perView, startIndex + perView);
			update();
		});
		// 창 크기가 바뀌면 칸 너비가 달라지므로 위치를 다시 계산한다
		window.addEventListener("resize", update);

		update();

		return true;
	}

	// 화살표를 포스터 세로 중앙에 두는 뒷정리. 카드 전체 중앙은 제목·연도까지 끼어 아래로 내려간다.
	// 포스터 높이는 aspect-ratio로 정해지므로 이미지 로딩을 기다릴 필요가 없다
	function centerNavOn(selector) {
		return function (viewport) {
			const visual = viewport.querySelector(selector);

			if (!visual) {
				return;
			}

			const visualRect = visual.getBoundingClientRect();
			const viewportTop = viewport.getBoundingClientRect().top;

			viewport.style.setProperty("--nav-top",
					(visualRect.top - viewportTop + visualRect.height / 2) + "px");
		};
	}

	window.enditCarousel = {
		create: create,
		centerNavOn: centerNavOn,
		readCssVar: readCssVar
	};
})();
