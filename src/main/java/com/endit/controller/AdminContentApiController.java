package com.endit.controller;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.AdminContentCreateRequest;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.ContentImageVO;
import com.endit.domain.ContentVO;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentGenreService;
import com.endit.service.ContentImageService;
import com.endit.service.ContentService;

import jakarta.servlet.http.HttpSession;

// AD-02 영화 목록 / AD-03 영화 등록 - 콘텐츠 REST API (API-051·052, ACT-AD-004)
@RestController
@RequestMapping("/api/admin/movies")
public class AdminContentApiController {

	private static final Logger log = LoggerFactory.getLogger(AdminContentApiController.class);

	// AD-02 검색 2축 - 제목(국문/원제 동시 부분일치)과 외부 ID(등호).
	// 정의서의 외부 출처(EXTERNAL_SOURCE)는 스키마에 컬럼이 없어 외부 ID 단독으로 확정했다
	private static final String SEARCH_BY_TITLE = "50";
	private static final String SEARCH_BY_EXTERNAL_ID = "40";

	// 정렬 축 - searchWord를 검색 조건이 쓰고 있어 searchMap의 이 키로 받는다
	private static final String SEARCH_KEY_SORT = "sort";
	// 등록일 내림차순(정의서 AD-02 기본 정렬).
	// created_dt가 초 단위라 같은 초에 적재된 덩어리 안에서는 콘텐츠 번호가 오름차순이 된다 - known-issues 참고
	private static final String SORT_REGISTERED = "registered";

	private static final String FIRST_PAGE_NO_TEXT = "1";
	private static final String DEFAULT_PAGE_SIZE_TEXT = "20";

	private final ContentService contentService;
	private final ContentGenreService contentGenreService;
	private final ContentImageService contentImageService;
	private final ContentCreditService contentCreditService;

	public AdminContentApiController(
			ContentService contentService,
			ContentGenreService contentGenreService,
			ContentImageService contentImageService,
			ContentCreditService contentCreditService) {
		this.contentService = contentService;
		this.contentGenreService = contentGenreService;
		this.contentImageService = contentImageService;
		this.contentCreditService = contentCreditService;
	}

	// AD-02 검색·목록 - searchDiv 50 제목 / 40 외부ID
	@GetMapping
	public ResponseEntity<Map<String, Object>> retrieve(
			@RequestParam(defaultValue = "") String searchWord,
			@RequestParam(defaultValue = SEARCH_BY_TITLE) String searchDiv,
			@RequestParam(name = "page", defaultValue = FIRST_PAGE_NO_TEXT) int pageNo,
			@RequestParam(name = "size", defaultValue = DEFAULT_PAGE_SIZE_TEXT) int pageSize,
			HttpSession session) {

		NoticeSessionSupport.requireAdminId(session);

		DTO param = new DTO();
		param.setSearchWord(searchWord.trim());
		param.setSearchDiv(toAllowedSearchDiv(searchDiv));
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);
		param.getSearchMap().put(SEARCH_KEY_SORT, SORT_REGISTERED);

		List<ContentVO> items = contentService.retrieve(param);

		return ResponseEntity.ok(toListResponse(items, param));
	}

	// AD-03 외부 ID 중복 검사(ACT-AD-004) - 저장 전에 화면이 먼저 물어본다. POL-030 UK_CONTENT_EXTERNAL
	@GetMapping("/check")
	public ResponseEntity<Map<String, Object>> has(
			@RequestParam String externalId,
			HttpSession session) {

		NoticeSessionSupport.requireAdminId(session);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("externalId", externalId);
		body.put("duplicated", contentService.hasExternalId(externalId));

		return ResponseEntity.ok(body);
	}

	/*
	 * AD-03 영화 등록(API-052) - CONTENT·CONTENT_GENRE·CONTENT_IMAGE·CONTENT_CREDIT 네 테이블을 한 번에 기록한다.
	 *
	 * 트랜잭션 경계를 여기에 두는 것은 합의된 결정이다. 네 서비스를 묶는 상위 서비스를 새로 만들면
	 * 서비스 카탈로그가 이 화면 하나 때문에 늘어나므로, 조율만 하는 이 핸들러가 경계를 진다.
	 * 하위 중 하나라도 실패하면 콘텐츠까지 통째로 롤백된다 - 장르만 남은 영화가 생기지 않는다.
	 */
	@PostMapping
	@Transactional
	public ResponseEntity<ContentVO> create(
			@RequestBody AdminContentCreateRequest param,
			HttpSession session) {

		NoticeSessionSupport.requireAdminId(session);

		if (param == null || param.getContent() == null) {
			throw new IllegalArgumentException("등록할 영화 정보가 필요합니다.");
		}

		ContentVO content = param.getContent();

		// 화면이 TMDB 전체 URL을 붙여 넣어도 DB에는 경로만 남긴다
		content.setPosterUrl(toImagePath(content.getPosterUrl(), "포스터"));
		content.setBackdropUrl(toImagePath(content.getBackdropUrl(), "배경 이미지"));

		// 제목·외부 ID 필수값과 중복 검사는 create가 맡는다
		ContentVO saved = contentService.create(content);
		int contentId = saved.getContentId();

		createGenres(contentId, param.getGenreIds());
		createImages(contentId, param.getImageUrls());
		createCredits(contentId, param.getCredits());

		log.debug("create contentId={}, externalId={}", contentId, saved.getExternalId());

		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	/*
	 * TMDB 이미지만 받는다.
	 *
	 * toStoredPath는 TMDB URL이 아니면 값을 그대로 돌려주고, 조회할 때 toFullImageUrl이
	 * 무조건 TMDB 주소를 앞에 붙인다. 그래서 다른 호스트의 URL을 그냥 저장하면
	 * "https://image.tmdb.org/t/p/w500https://..." 같은 주소가 조용히 만들어져
	 * 관리자 상세와 사용자 상세 양쪽에서 깨진 이미지로만 드러난다.
	 * 저장 시점에 막는 것이 유일하게 값싼 자리다.
	 */
	private String toImagePath(String url, String fieldName) {
		if (!StringUtils.hasText(url)) {
			return null;
		}

		String storedPath = contentImageService.toStoredPath(url.trim());

		// TMDB URL이면 경로만 남아 '/'로 시작한다. 경로를 그대로 넣은 경우도 여기에 걸린다
		if (!storedPath.startsWith("/")) {
			throw new IllegalArgumentException(
					fieldName + "은(는) TMDB 이미지 주소이거나 /로 시작하는 경로여야 합니다.");
		}

		return storedPath;
	}

	// 정의서 2축 밖의 검색 구분은 받지 않는다. 서비스가 허용하는 나머지 축은 이 화면의 것이 아니다
	private String toAllowedSearchDiv(String searchDiv) {
		if (!StringUtils.hasText(searchDiv)) {
			return SEARCH_BY_TITLE;
		}

		if (!SEARCH_BY_TITLE.equals(searchDiv) && !SEARCH_BY_EXTERNAL_ID.equals(searchDiv)) {
			throw new IllegalArgumentException("제목 또는 외부 ID로만 검색할 수 있습니다.");
		}

		return searchDiv;
	}

	// 같은 장르를 두 번 담아 보내도 한 번만 연결한다 - has로 걸러야 중복 하나가 등록 전체를 되돌리지 않는다
	private void createGenres(int contentId, List<Integer> genreIds) {
		if (genreIds == null) {
			return;
		}

		for (Integer genreId : genreIds) {
			if (genreId == null || genreId <= 0) {
				throw new IllegalArgumentException("올바른 장르 번호가 필요합니다.");
			}

			if (contentGenreService.has(contentId, genreId)) {
				continue;
			}

			contentGenreService.create(contentId, genreId);
		}
	}

	// 폼에서 비워 둔 이미지 줄은 건너뛴다. 빈 칸까지 저장하면 상세 화면에 빈 이미지가 남는다
	private void createImages(int contentId, List<String> imageUrls) {
		if (imageUrls == null) {
			return;
		}

		for (String imageUrl : imageUrls) {
			if (!StringUtils.hasText(imageUrl)) {
				continue;
			}

			ContentImageVO image = new ContentImageVO();
			image.setImageUrl(toImagePath(imageUrl, "갤러리 이미지"));

			contentImageService.create(contentId, image);
		}
	}

	/*
	 * 역할(POL-033) 검사는 서비스가 맡고, 여기서는 인물이 지정됐는지와 중복인지만 본다.
	 *
	 * 같은 인물을 같은 역할로 두 번 담아 보내도 한 번만 넣는다 - 장르와 같은 이유다.
	 * 콘텐츠는 이 요청에서 막 만들어졌으므로 중복은 페이로드 안에서만 생길 수 있고,
	 * 거르지 않으면 상세에 같은 줄이 두 번 뜨거나 제약 위반 하나가 등록 전체를 되돌린다.
	 */
	private void createCredits(int contentId, List<ContentCreditVO> credits) {
		if (credits == null) {
			return;
		}

		Set<String> seen = new HashSet<>();

		for (ContentCreditVO credit : credits) {
			if (credit == null) {
				continue;
			}

			// 기존 인물 선택만 허용한다 - 등록 폼에서 인물까지 만들면 AD-06과 책임이 겹친다
			if (credit.getPersonId() <= 0) {
				throw new IllegalArgumentException("크레딧에 연결할 인물을 선택해 주세요.");
			}

			if (!seen.add(credit.getPersonId() + ":" + credit.getRole())) {
				continue;
			}

			contentCreditService.create(contentId, credit);
		}
	}

	// 목록 + 페이징 공통 응답 구조 - AdminPersonApiController와 같은 모양으로 맞춘다
	private Map<String, Object> toListResponse(List<ContentVO> items, DTO param) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return response;
	}

	// 필드 오류(제목·외부ID 누락, 허용 밖 검색축·역할) → 400
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(IllegalArgumentException exception) {
		return toResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), "입력값을 확인해 주세요.");
	}

	// 없는 콘텐츠·장르·크레딧 → 404
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<MessageVO> handleNotFound(NoSuchElementException exception) {
		return toResponse(HttpStatus.NOT_FOUND, exception.getMessage(), "대상을 찾을 수 없습니다.");
	}

	// 외부ID 중복(UK_CONTENT_EXTERNAL)·저장 실패 → 409
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<MessageVO> handleConflict(IllegalStateException exception) {
		return toResponse(HttpStatus.CONFLICT, exception.getMessage(), "저장할 수 없는 상태입니다.");
	}

	// 참조 관계·제약 위반 → 400
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<MessageVO> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		log.warn("영화 저장의 제약 조건을 위반했습니다.", exception);

		return toResponse(HttpStatus.BAD_REQUEST,
				"존재하는 장르와 인물 번호를 입력해 주세요.",
				"데이터의 참조 관계를 확인해 주세요.");
	}

	private ResponseEntity<MessageVO> toResponse(HttpStatus status, String message, String detail) {
		MessageVO body = new MessageVO(String.valueOf(status.value()), message, detail);

		return ResponseEntity.status(status).body(body);
	}

}
