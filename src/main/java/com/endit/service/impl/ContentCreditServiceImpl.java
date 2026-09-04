package com.endit.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;
import com.endit.mapper.ContentCreditMapper;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentImageService;

@Service
@Transactional(readOnly = true)
public class ContentCreditServiceImpl implements ContentCreditService {

	private static final String SEARCH_BY_CONTENT = "10";
	private static final String SEARCH_BY_PERSON = "20";

	// 역할 필터는 searchWord를 주 축이 쓰고 있어 searchMap의 이 키로 받는다
	private static final String SEARCH_KEY_ROLE = "role";

	// POL-033이 정한 크레딧 역할 4종
	private static final String ROLE_DIRECTOR = "DIRECTOR";
	private static final String ROLE_ACTOR = "ACTOR";
	private static final String ROLE_WRITER = "WRITER";
	private static final String ROLE_PRODUCER = "PRODUCER";

	// getTopPerson이 IN 절에 펼칠 수 있는 최대 개수. Oracle 상한 1000보다 낮게 잡아 여유를 둔다
	private static final int MAX_TOP_PERSON_POOL = 500;

	// 페이징 없이 "전체 조회"를 흉내낼 때 쓰는 페이지 크기 - 콘텐츠 하나가 가질 수 있는 크레딧 수보다 넉넉하게 잡음
	private static final int RETRIEVE_ALL_PAGE_SIZE = 100;
	private static final int FIRST_PAGE_NO = 1;
	private static final int DEFAULT_PAGE_SIZE = 12;
	private static final int MAX_PAGE_SIZE = 100;

	private static final Logger log = LoggerFactory.getLogger(ContentCreditServiceImpl.class);

	private final ContentCreditMapper contentCreditMapper;
	private final ContentImageService contentImageService;

	public ContentCreditServiceImpl(ContentCreditMapper contentCreditMapper, ContentImageService contentImageService) {
		this.contentCreditMapper = contentCreditMapper;
		this.contentImageService = contentImageService;
	}

	// 콘텐츠 하나의 출연/제작진 목록 조회 - CONTENT_CREDIT + PERSON + CONTENT 조인, role/이름/프로필/배역/순서 반환
	@Override
	public List<ContentCreditVO> retrieve(int contentId, DTO param) {
		validateContentId(contentId);

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		normalizePaging(param);
		validateRole(param);
		param.setSearchDiv(SEARCH_BY_CONTENT);
		param.setSearchWord(String.valueOf(contentId));

		List<ContentCreditVO> credits = contentCreditMapper.doRetrieve(param);

		// doRetrieve가 CROSS JOIN으로 content_id까지 걸러낸 총건수를 각 행에 실어 준다.
		param.setTotalCnt(credits.isEmpty() ? 0 : credits.get(0).getTotalCnt());

		for (ContentCreditVO credit : credits) {
			applyFullImageUrl(credit);
		}

		return credits;
	}

	// 인물 여럿 중 감독 크레딧을 가진 인물만 추린다 - 목록 화면의 역할 표기용
	@Override
	public Set<Integer> retrieveDirectorIds(List<Integer> personIds) {
		// IN ()은 문법 오류라 빈 목록은 매퍼까지 보내지 않는다
		if (personIds == null || personIds.isEmpty()) {
			return Collections.emptySet();
		}

		List<Integer> directorIds =
				contentCreditMapper.doSelectPersonIdsByRole(personIds, ROLE_DIRECTOR);
		log.debug("retrieveDirectorIds asked={} found={}",
				personIds.size(), directorIds == null ? 0 : directorIds.size());

		if (directorIds == null || directorIds.isEmpty()) {
			return Collections.emptySet();
		}

		return new HashSet<>(directorIds);
	}

	// 인물 하나의 참여 작품 목록 조회 - 매퍼의 person_id 검색축을 쓴다
	@Override
	public List<ContentCreditVO> retrieveByPerson(int personId, DTO param) {
		validatePersonId(personId);

		if (param == null) {
			throw new IllegalArgumentException("조회 조건은 null일 수 없습니다.");
		}

		normalizePaging(param);
		validateRole(param);
		param.setSearchDiv(SEARCH_BY_PERSON);
		param.setSearchWord(String.valueOf(personId));

		List<ContentCreditVO> credits = contentCreditMapper.doRetrieve(param);

		// doRetrieve가 CROSS JOIN으로 person_id까지 걸러낸 총건수를 각 행에 실어 준다.
		param.setTotalCnt(credits.isEmpty() ? 0 : credits.get(0).getTotalCnt());

		for (ContentCreditVO credit : credits) {
			applyFullImageUrl(credit);
		}

		return credits;
	}

	// 콘텐츠 하나의 출연/제작진 전체 목록 조회 - 역할 우선순위 정렬은 매퍼가 한다
	@Override
	public List<ContentCreditVO> retrieveAll(int contentId) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(RETRIEVE_ALL_PAGE_SIZE);
		List<ContentCreditVO> credits = retrieve(contentId, param);

		// "전체"는 실제로 한 페이지 상한이다. 초과분이 말없이 잘리지 않도록 남겨 둔다
		if (credits.size() == RETRIEVE_ALL_PAGE_SIZE) {
			log.warn("크레딧 전체 조회가 페이지 상한에 도달했습니다. 초과분이 잘렸을 수 있습니다. contentId={}, size={}",
					contentId, credits.size());
		}

		return credits;
	}

	// 크레딧 단건 조회
	@Override
	public ContentCreditVO get(int creditId) {
		ContentCreditVO item = getStored(creditId);
		applyFullImageUrl(item);

		return item;
	}

	// 화제의 감독·배우 선별 - 모수는 호출부가 정한다
	@Override
	public ContentCreditVO getTopPerson(String role, List<Integer> contentIds) {
		validateRoleCode(role);

		// IN ()은 문법 오류라 빈 목록은 매퍼까지 보내지 않는다. 순위가 아직 안 채워진 기동 직후가 이 경우다
		if (contentIds == null || contentIds.isEmpty()) {
			return null;
		}

		// 매퍼가 이 목록을 통째로 IN 절에 펼치는데 Oracle 상한이 표현식 1000개다(초과하면 ORA-01795).
		// 호출부의 순위 크기가 커져도 여기서 터지지 않도록 앞쪽만 자른다 - 앞쪽이 곧 상위 인기작이라 선별 결과도 그쪽이 지배한다
		List<Integer> pool = contentIds.size() > MAX_TOP_PERSON_POOL
				? contentIds.subList(0, MAX_TOP_PERSON_POOL)
				: contentIds;

		ContentCreditVO top = contentCreditMapper.doSelectTopPersonByRole(role, pool);
		log.debug("getTopPerson role={} pool={} picked={}",
				role, pool.size(), top == null ? null : top.getNameKo());

		return top;
	}

	// 콘텐츠에 크레딧(배우 또는 감독) 등록
	@Override
	@Transactional
	public ContentCreditVO create(int contentId, ContentCreditVO param) {
		validateContentId(contentId);

		if (param == null) {
			throw new IllegalArgumentException("등록할 크레딧 정보가 필요합니다.");
		}

		param.setContentId(contentId);

		// POL-033 - 읽기 필터에만 걸려 있던 역할 검사를 쓰기 경로에도 건다.
		// AD-06 크레딧 정정이 임의 역할을 넣을 수 있는 유일한 경로다
		validateRoleCode(param.getRole());

		int result = contentCreditMapper.doSave(param);

		if (result != 1) {
			throw new IllegalStateException("크레딧 등록에 실패했습니다.");
		}

		// doSave가 시퀀스로 채번한 creditId를 param이 그대로 들고 있으므로 재조회에 사용한다
		return get(param.getCreditId());
	}

	// 크레딧의 배역명/표시순서 등 수정
	@Override
	@Transactional
	public ContentCreditVO update(int creditId, ContentCreditVO param) {
		ContentCreditVO stored = getStored(creditId);

		if (param == null) {
			throw new IllegalArgumentException("수정할 크레딧 정보가 필요합니다.");
		}

		param.setCreditId(creditId);

		// doUpdate가 content_id·person_id를 무조건 SET하므로, 호출부가 안 채웠으면 기존 소속을 유지한다
		if (param.getContentId() <= 0) {
			param.setContentId(stored.getContentId());
		}

		if (param.getPersonId() <= 0) {
			param.setPersonId(stored.getPersonId());
		}

		// role은 NOT NULL이라 비워 두는 것이 의도일 수 없다. 안 채웠으면 기존 역할을 유지한다
		if (!StringUtils.hasText(param.getRole())) {
			param.setRole(stored.getRole());
		}

		// character(감독은 null)와 displayOrder(0이 정상값)는 "안 채움"과 "비움"을 구분할 수 없어
		// 넘어온 값으로 그대로 덮는다. 호출부가 전체 필드를 채워 보내야 한다

		// 기존 역할을 메운 뒤에 검사한다. 역할을 안 보내는 수정이 정상이기 때문이다
		validateRoleCode(param.getRole());

		int result = contentCreditMapper.doUpdate(param);

		if (result != 1) {
			throw new IllegalStateException("크레딧 수정에 실패했습니다.");
		}

		return get(creditId);
	}

	// 크레딧 삭제
	@Override
	@Transactional
	public void delete(int creditId) {
		ContentCreditVO existing = getStored(creditId);
		int result = contentCreditMapper.doDelete(existing);

		if (result != 1) {
			throw new IllegalStateException("크레딧 삭제에 실패했습니다.");
		}
	}

	// TMDB 원본 경로를 화면에 바로 쓸 수 있는 풀 URL로 완성한다.
	// 어떤 크기를 쓸지는 ContentImageServiceImpl이 정하므로 여기서는 용도만 말한다
	private void applyFullImageUrl(ContentCreditVO credit) {
		credit.setProfileImageUrl(contentImageService.toCreditProfileUrl(credit.getProfileImageUrl()));
		credit.setPosterUrl(contentImageService.toPosterUrl(credit.getPosterUrl()));
	}

	// DB에 저장된 그대로(TMDB 원본 경로) 단건 조회. 쓰기 경로가 완성 URL을 다시 쓰지 않도록 읽기용 get과 나눠 둔다
	private ContentCreditVO getStored(int creditId) {
		ContentCreditVO item = contentCreditMapper.doSelectOne(createKey(creditId));

		if (item == null) {
			throw new NoSuchElementException("존재하지 않는 크레딧입니다. creditId=" + creditId);
		}

		return item;
	}

	// 페이지 번호와 페이지 크기를 허용 범위의 기본값으로 보정
	private void normalizePaging(DTO param) {
		if (param.getPageNo() <= 0) {
			param.setPageNo(FIRST_PAGE_NO);
		}

		if (param.getPageSize() <= 0) {
			param.setPageSize(DEFAULT_PAGE_SIZE);
		} else if (param.getPageSize() > MAX_PAGE_SIZE) {
			param.setPageSize(MAX_PAGE_SIZE);
		}
	}

	// 역할 값이 POL-033 4종 안인지 확인한다 - 쓰기 3종과 getTopPerson이 함께 쓴다
	private void validateRoleCode(String role) {
		if (!StringUtils.hasText(role)) {
			throw new IllegalArgumentException("크레딧 역할이 필요합니다.");
		}

		if (!ROLE_DIRECTOR.equals(role)
				&& !ROLE_ACTOR.equals(role)
				&& !ROLE_WRITER.equals(role)
				&& !ROLE_PRODUCER.equals(role)) {
			throw new IllegalArgumentException("지원하지 않는 역할입니다. role=" + role);
		}
	}

	private void validateRole(DTO param) {
		if (param.getSearchMap() == null) {
			// 매퍼의 searchMap.role 판정이 NPE를 내지 않도록 빈 맵으로 되돌려 준다
			param.setSearchMap(new HashMap<>());
			return;
		}

		String role = param.getSearchMap().get(SEARCH_KEY_ROLE);

		if (!StringUtils.hasText(role)) {
			return;
		}

		if (!ROLE_DIRECTOR.equals(role)
				&& !ROLE_ACTOR.equals(role)
				&& !ROLE_WRITER.equals(role)
				&& !ROLE_PRODUCER.equals(role)) {
			throw new IllegalArgumentException("지원하지 않는 역할입니다. role=" + role);
		}
	}

	// 크레딧 번호를 담은 조회 키 생성
	private ContentCreditVO createKey(int creditId) {
		validateCreditId(creditId);

		ContentCreditVO key = new ContentCreditVO();
		key.setCreditId(creditId);

		return key;
	}

	// 콘텐츠 번호가 유효한 양수인지 검증
	private void validateContentId(int contentId) {
		if (contentId <= 0) {
			throw new IllegalArgumentException("올바른 콘텐츠 번호가 필요합니다.");
		}
	}

	// 인물 번호가 유효한 양수인지 검증
	private void validatePersonId(int personId) {
		if (personId <= 0) {
			throw new IllegalArgumentException("올바른 인물 번호가 필요합니다.");
		}
	}

	// 크레딧 번호가 유효한 양수인지 검증
	private void validateCreditId(int creditId) {
		if (creditId <= 0) {
			throw new IllegalArgumentException("올바른 크레딧 번호가 필요합니다.");
		}
	}

}
