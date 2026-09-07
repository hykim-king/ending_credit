package com.endit.controller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

// P-01 인물 상세 - 프로필·역할 요약·필모그래피·좋아요를 한 화면에 그린다
@Controller
public class PersonViewController {

	private static final Logger log = LoggerFactory.getLogger(PersonViewController.class);

	// 필모그래피 첫 페이지 건수 - 더보기가 같은 크기로 이어받도록 화면에 내려 준다
	private static final int FILMOGRAPHY_PAGE_SIZE = 12;
	private static final int FIRST_PAGE_NO = 1;

	// POL-033 역할 4종 표기 - person/detail.js가 같은 표를 들고 있다
	// 라벨은 messages*.properties의 role.* 에 있다. ContentViewController와 같은 표라 문구가 갈리면 안 된다(F-01)
	private static final List<String> ROLE_CODES = List.of("DIRECTOR", "ACTOR", "WRITER", "PRODUCER");
	private static final String MSG_PREFIX_ROLE = "role.";

	private static final String PERSON_DETAIL_VIEW = "person/detail";

	private final PersonService personService;
	private final ContentCreditService contentCreditService;
	private final PersonLikeService personLikeService;
	private final CurrentMemberProvider currentMemberProvider;
	private final MessageSource messageSource;

	public PersonViewController(
			PersonService personService,
			ContentCreditService contentCreditService,
			PersonLikeService personLikeService,
			CurrentMemberProvider currentMemberProvider,
			MessageSource messageSource) {
		this.personService = personService;
		this.contentCreditService = contentCreditService;
		this.personLikeService = personLikeService;
		this.currentMemberProvider = currentMemberProvider;
		this.messageSource = messageSource;
	}

	// 화면이 코드→라벨로 찾아 쓰므로 맵으로 넘긴다. ContentViewController와 같은 표라 순서까지 맞춘다
	private Map<String, String> toRoleLabels() {
		Map<String, String> labels = new LinkedHashMap<>();

		for (String code : ROLE_CODES) {
			labels.put(code, messageSource.getMessage(MSG_PREFIX_ROLE + code, null,
					MSG_PREFIX_ROLE + code, LocaleContextHolder.getLocale()));
		}

		return Collections.unmodifiableMap(labels);
	}

	// P-01 인물 상세 화면
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
		model.addAttribute("roleLabels", toRoleLabels());
		model.addAttribute("pageSize", FILMOGRAPHY_PAGE_SIZE);

		addFilmography(personId, model);
		addLike(personId, model);

		return PERSON_DETAIL_VIEW;
	}

	// P-01 필모그래피 첫 페이지 - 더보기가 남은 건수를 재야 해서 totalCnt를 주는 경로로 읽는다
	private void addFilmography(int personId, Model model) {
		DTO param = new DTO();
		param.setPageNo(FIRST_PAGE_NO);
		param.setPageSize(FILMOGRAPHY_PAGE_SIZE);

		List<ContentCreditVO> filmography;

		try {
			filmography = contentCreditService.retrieveByPerson(personId, param);
		} catch (RuntimeException e) {
			// 목록 자리만 비우고 화면은 살린다
			log.warn("참여 작품 조회에 실패했습니다. personId={}", personId, e);
			filmography = Collections.emptyList();
			model.addAttribute("filmographyFailed", true);
		}

		model.addAttribute("filmography", filmography);
		model.addAttribute("filmographyTotalCnt", param.getTotalCnt());
		model.addAttribute("hasMoreFilmography", param.getTotalCnt() > filmography.size());
		model.addAttribute("roleSummary", toRoleSummary(filmography));
	}

	// P-01 히어로 역할 요약("감독, 배우") - 첫 페이지 크레딧에서만 모으므로 그 밖의 역할은 빠진다
	private String toRoleSummary(List<ContentCreditVO> filmography) {
		Set<String> labels = new LinkedHashSet<>();
		// 라벨은 로케일마다 다르므로 한 번 만들어 돌려 쓴다
		Map<String, String> roleLabels = toRoleLabels();

		for (ContentCreditVO credit : filmography) {
			String label = roleLabels.get(credit.getRole());

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

	// P-01 좋아요용 로그인 회원 번호 - PersonLikeController가 X-Member-Id 헤더를 받는 동안만 필요하다
	private Integer toCurrentMemberId() {
		OptionalLong memberId = currentMemberProvider.findCurrentMemberId();

		return memberId.isPresent() ? (int) memberId.getAsLong() : null;
	}

}
