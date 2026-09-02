package com.endit.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.endit.domain.ContentCreditVO;
import com.endit.domain.ContentGenreVO;
import com.endit.domain.ContentImageVO;
import com.endit.domain.ContentVO;
import com.endit.service.ContentCreditService;
import com.endit.service.ContentGenreService;
import com.endit.service.ContentImageService;
import com.endit.service.ContentService;

/**
 * 영화 상세 화면(C-01 영화 상세 페이지 + C-02 출연/제작·갤러리)의 경로를 처리하는 Controller
 */
@Controller
public class ContentViewController {

	private final ContentService contentService;
	private final ContentGenreService contentGenreService;
	private final ContentCreditService contentCreditService;
	private final ContentImageService contentImageService;

	public ContentViewController(
			ContentService contentService,
			ContentGenreService contentGenreService,
			ContentCreditService contentCreditService,
			ContentImageService contentImageService) {
		this.contentService = contentService;
		this.contentGenreService = contentGenreService;
		this.contentCreditService = contentCreditService;
		this.contentImageService = contentImageService;
	}

	/** 영화 상세 화면 */
	@GetMapping("/movies/{contentId}")
	public String detail(@PathVariable int contentId, Model model) {

		try {
			ContentVO content = contentService.get(contentId);
			List<ContentGenreVO> genres = contentGenreService.retrieveAll(contentId);
			List<ContentCreditVO> castAndCrew = contentCreditService.retrieveAll(contentId);
			// 썸네일용·확대용 URL이 모두 채워진 상태로 넘긴다.
			List<ContentImageVO> galleryImages = contentImageService.retrieveAll(contentId);

			model.addAttribute("content", content);
			model.addAttribute("genres", genres);
			model.addAttribute("castAndCrew", castAndCrew);
			model.addAttribute("galleryImages", galleryImages);
			model.addAttribute("notFound", false);
		} catch (NoSuchElementException e) {
			model.addAttribute("notFound", true);
		}

		return "content/detail";
	}

}
