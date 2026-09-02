package com.endit.controller;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.endit.domain.ContentVO;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentGenreService;
import com.endit.service.ContentImageService;
import com.endit.service.ContentService;
import com.endit.service.GenreService;

import jakarta.servlet.http.HttpSession;

// AD-02 영화 관리 목록 / AD-03 영화 등록 폼 / AD-04 영화 상세 조회 - 화면 라우팅
@Controller
public class AdminContentPageController {

	private static final String CONTENT_LIST_VIEW = "admin/movies/adminContentList";
	private static final String CONTENT_FORM_VIEW = "admin/movies/adminContentForm";
	private static final String CONTENT_DETAIL_VIEW = "admin/movies/adminContentDetail";

	private final ContentService contentService;
	private final ContentGenreService contentGenreService;
	private final ContentImageService contentImageService;
	private final ContentCreditService contentCreditService;
	private final GenreService genreService;

	public AdminContentPageController(
			ContentService contentService,
			ContentGenreService contentGenreService,
			ContentImageService contentImageService,
			ContentCreditService contentCreditService,
			GenreService genreService) {
		this.contentService = contentService;
		this.contentGenreService = contentGenreService;
		this.contentImageService = contentImageService;
		this.contentCreditService = contentCreditService;
		this.genreService = genreService;
	}

	// AD-02 목록 - 데이터는 화면이 /api/admin/movies로 직접 읽는다(AD-05와 같은 방식)
	@GetMapping("/admin/movies")
	public String list(HttpSession session) {
		NoticeSessionSupport.requireAdminId(session);

		return CONTENT_LIST_VIEW;
	}

	// AD-03 등록 폼 - 장르 마스터만 미리 실어 준다. 저장은 화면이 POST로 보낸다
	@GetMapping("/admin/movies/new")
	public String createForm(HttpSession session, Model model) {
		NoticeSessionSupport.requireAdminId(session);

		model.addAttribute("content", new ContentVO());
		model.addAttribute("genres", genreService.retrieveAll());

		return CONTENT_FORM_VIEW;
	}

	// AD-04 읽기 전용 상세 - 네 조각을 서버에서 모아 넘긴다. 쓰기 기능은 없다
	@GetMapping("/admin/movies/{contentId}")
	public String detail(@PathVariable int contentId, HttpSession session, Model model) {
		NoticeSessionSupport.requireAdminId(session);

		ContentVO content;

		try {
			content = contentService.get(contentId);
		} catch (NoSuchElementException | IllegalArgumentException e) {
			// get은 없는 콘텐츠에 NoSuchElement를, 0 이하 번호에 IllegalArgument를 던진다.
			// /admin/movies/0 도 경로에 맞아 들어오므로 둘 다 잡지 않으면 500이 난다 - 화면에서는 둘 다 404가 맞다
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 영화입니다.");
		}

		model.addAttribute("content", content);
		model.addAttribute("genres", contentGenreService.retrieveAll(contentId));
		model.addAttribute("images", contentImageService.retrieveAll(contentId));
		model.addAttribute("credits", contentCreditService.retrieveAll(contentId));

		return CONTENT_DETAIL_VIEW;
	}

}
