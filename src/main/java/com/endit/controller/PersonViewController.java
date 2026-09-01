package com.endit.controller;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.endit.auth.CurrentMemberProvider;
import com.endit.cmn.DTO;
import com.endit.domain.ContentCreditVO;
import com.endit.domain.PersonVO;
import com.endit.service.ContentCreditService;
import com.endit.service.PersonLikeService;
import com.endit.service.PersonService;

/**
 * 인물 상세 화면(P-01)의 경로를 처리하는 Controller
 */
@Controller
public class PersonViewController {

	private static final Logger log = LoggerFactory.getLogger(PersonViewController.class);

	// 필모그래피 첫 페이지 건수. 더보기가 같은 크기로 이어 받도록 화면에 함께 내려 준다
	private static final int FILMOGRAPHY_PAGE_SIZE = 12;
	private static final int FIRST_PAGE_NO = 1;

	// POL-033이 정한 크레딧 역할 4종의 표기. 더보기로 붙는 행은 person/detail.js가 같은 표를 들고 있다
	private static final Map<String, String> ROLE_LABELS = Map.of(
			"DIRECTOR", "감독",
			"ACTOR", "배우",
			"WRITER", "각본",
			"PRODUCER", "제작");

	private static final String PERSON_DETAIL_VIEW = "person/detail";

	private final PersonService personService;
	private final ContentCreditService contentCreditService;
	private final PersonLikeService personLikeService;
	private final CurrentMemberProvider currentMemberProvider;

	public PersonViewController(
			PersonService personService,
			ContentCreditService contentCreditService,
			PersonLikeService personLikeService,
			CurrentMemberProvider currentMemberProvider) {
		this.personService = personService;
		this.contentCreditService = contentCreditService;
		this.personLikeService = personLikeService;
		this.currentMemberProvider = currentMemberProvider;
	}

	/** 인물 상세 화면 */
	@GetMapping("/people/{personId}")
	public String detail(@PathVariable int personId, Model model) {

		PersonVO person = personService.get(personId);

		// PersonService.get은 없는 인물에 null을 돌려준다(컨트롤러가 404로 바꾸는 계약)
		if (person == null) {
			model.addAttribute("notFound", true);
			return PERSON_DETAIL_VIEW;
		}

		model.addAttribute("notFound", false);
		model.addAttribute("person", person);
		model.addAttribute("roleLabels", ROLE_LABELS);
		model.addAttribute("pageSize", FILMOGRAPHY_PAGE_SIZE);

		addFilmography(personId, model);
		addLike(personId, model);

		return PERSON_DETAIL_VIEW;
	}

	// 필모그래피 첫 페이지. PersonService.getFilmography는 pageSize 50 고정에 DTO를 버려
	// 총건수가 화면까지 오지 않아 더보기를 만들 수 없다. 우리 서비스를 직접 부른다
	private void addFilmography(int personId, Model model) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(FILMOGRAPHY_PAGE_SIZE);

		List<ContentCreditVO> filmography;

		try {
			filmography = contentCreditService.retrieveByPerson(personId, param);
		} catch (RuntimeException e) {
			// 03의 오류 상태는 "목록 재시도"다. 화면 전체가 죽는 것보다 목록 자리만 비우는 편이 낫다
			log.warn("참여 작품 조회에 실패했습니다. personId={}", personId, e);
			filmography = Collections.emptyList();
			model.addAttribute("filmographyFailed", true);
		}

		model.addAttribute("filmography", filmography);
		model.addAttribute("filmographyTotalCnt", param.getTotalCnt());
		model.addAttribute("hasMoreFilmography", param.getTotalCnt() > filmography.size());
		model.addAttribute("roleSummary", toRoleSummary(filmography));
	}

	// 히어로의 역할 요약("감독, 배우"). 첫 페이지 크레딧의 역할을 중복 없이 모은다.
	// 추가 쿼리가 없는 대신 13번째 작품에만 있는 역할은 잡히지 않는다
	private String toRoleSummary(List<ContentCreditVO> filmography) {
		Set<String> labels = new LinkedHashSet<>();

		for (ContentCreditVO credit : filmography) {
			String label = ROLE_LABELS.get(credit.getRole());

			// POL-033 밖의 값이 들어와 있으면 라벨을 만들지 않는다
			if (label != null) {
				labels.add(label);
			}
		}

		return String.join(", ", labels);
	}

	// 좋아요(ACT-P-001). PERSON_LIKE는 담당 밖이라 이미 있는 서비스를 읽기만 한다
	private void addLike(int personId, Model model) {
		Integer memberId = toCurrentMemberId();
		model.addAttribute("loginMemberId", memberId);

		try {
			// 건수는 비회원에게도 보이는 공개 정보다
			model.addAttribute("likeCount", personLikeService.countLikes(personId));
			model.addAttribute("liked", memberId != null && personLikeService.isLiked(memberId, personId));
		} catch (RuntimeException e) {
			// 좋아요는 부가 정보다. 못 읽어도 인물 정보와 참여작은 그려야 한다
			log.warn("좋아요 조회에 실패했습니다. personId={}", personId, e);
			model.addAttribute("likeCount", 0);
			model.addAttribute("liked", false);
		}
	}

	/*
	 * 로그인 회원 번호. 인증 경계는 CurrentMemberProvider가 맡고 있으므로 세션을 직접 보지 않는다 -
	 * 실제 인증이 병합되면 그 구현체만 바뀌고 이 화면은 그대로다.
	 *
	 * 화면이 번호를 알아야 하는 이유는 PersonLikeController가 아직 X-Member-Id 임시 헤더를 받기 때문이다.
	 * 그 컨트롤러가 CurrentMemberProvider로 옮겨 가면 여기서 번호를 내려 줄 필요도 없어진다.
	 */
	private Integer toCurrentMemberId() {
		OptionalLong memberId = currentMemberProvider.findCurrentMemberId();

		return memberId.isPresent() ? (int) memberId.getAsLong() : null;
	}

}
