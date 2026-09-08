/*
 * 서버가 넘긴 컬렉션 목록을 격자에 채운다. 검색(S-01)·상세(C-02)·담긴 컬렉션 전체보기가 함께 쓴다.
 * 카드는 3조 collection-list.js의 createCollectionCard가 그리므로 목록·검색 화면과 같은 모양이 된다.
 *
 * 목록과 회원 번호는 window.enditCollections / window.enditCollectionMemberId 한 쌍으로만 다닌다 -
 * 화면마다 다른 이름을 쓰면 createCollectionCard의 인자가 바뀔 때 고쳐야 할 곳을 grep으로 못 찾는다.
 */
(function () {
	"use strict";

	// 카드 공장이 아직 없으면(로드 실패) 빈 격자를 남기지 않도록 호출부가 판단하게 false를 준다
	function isReady() {
		return typeof createCollectionCard === "function";
	}

	// 서버가 실은 목록. 없으면 빈 배열이라 호출부가 따로 막지 않아도 된다
	function toCollections() {
		return window.enditCollections || [];
	}

	function toMemberId() {
		return window.enditCollectionMemberId;
	}

	// 카드 한 장. 캐러셀처럼 칸 요소로 감싸야 하는 화면은 이것만 받아 스스로 감싼다
	function createCard(collection) {
		return createCollectionCard(collection, toMemberId());
	}

	/*
	 * 격자에 그대로 붙인다. 캐러셀이 아닌 화면(검색 미리보기·전체보기)이 쓴다.
	 * 격자가 없거나 카드 공장이 없으면 아무것도 하지 않고 false다.
	 */
	function fill(grid) {
		if (!grid || !isReady()) {
			return false;
		}

		toCollections().forEach(function (collection) {
			grid.append(createCard(collection));
		});

		return true;
	}

	window.enditCollectionGrid = {
		isReady: isReady,
		toCollections: toCollections,
		toMemberId: toMemberId,
		createCard: createCard,
		fill: fill
	};
})();
