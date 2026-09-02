package com.endit.support;

import java.util.List;

import com.endit.domain.CollectionCreateRequest;
import com.endit.domain.CollectionUpdateRequest;

/**
 * <pre>
 * Class Name  : CollectionRequestFixtures
 * Description : 컬렉션 Controller·Service 테스트용 요청 DTO 픽스처
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 31. jinyoung    중복 요청 DTO 생성 코드를 공통 픽스처로 분리
 * ------------------------------------------------------------
 * </pre>
 */
public final class CollectionRequestFixtures {

	private CollectionRequestFixtures() {
	}

	public static CollectionCreateRequest createRequest(
			String title,
			String description,
			List<Integer> contentIds) {

		return createRequest(title, description, null, contentIds);
	}

	public static CollectionCreateRequest createRequest(
			String title,
			String description,
			String isPublic,
			List<Integer> contentIds) {

		CollectionCreateRequest request = new CollectionCreateRequest();
		request.setTitle(title);
		request.setDescription(description);
		request.setIsPublic(isPublic);
		request.setContentIds(contentIds);
		return request;
	}

	public static CollectionUpdateRequest updateRequest(
			String title,
			String description,
			List<Integer> contentIds) {

		return updateRequest(title, description, null, contentIds);
	}

	public static CollectionUpdateRequest updateRequest(
			String title,
			String description,
			String isPublic,
			List<Integer> contentIds) {

		CollectionUpdateRequest request = new CollectionUpdateRequest();
		request.setTitle(title);
		request.setDescription(description);
		request.setIsPublic(isPublic);
		request.setContentIds(contentIds);
		return request;
	}
}
