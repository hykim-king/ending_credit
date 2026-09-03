package com.endit.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.MessageVO;
import com.endit.service.MemberService;

/**
 * Class Name : MemberApiController
 * Description : 회원가입 화면에서 호출하는 이메일 / 닉네임 중복확인 REST Controller.
 *
 *   - 화면(뷰) 반환은 MemberPageController 가 담당하고, 여기는 JSON 만 반환한다.
 *   - 응답은 {"used": true} 형태. true 면 이미 사용 중이라 가입할 수 없다.
 */
@RestController
@RequestMapping("/api/members")
public class MemberApiController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MemberService memberService;

	public MemberApiController(MemberService memberService) {
		this.memberService = memberService;
	}

	/**
	 * 이메일 중복확인.
	 *
	 * @param email 확인할 이메일
	 * @return {"used": 사용중이면 true}
	 */
	@GetMapping("/email-check")
	public ResponseEntity<Map<String, Boolean>> checkEmail(
			@RequestParam String email) {

		log.debug("checkEmail(email={})", email);

		if (email == null || email.trim().isEmpty()) {
			throw new IllegalArgumentException("이메일을 입력해 주세요.");
		}

		boolean used = memberService.isEmailUsed(email.trim());

		return ResponseEntity.ok(Map.of("used", used));
	}

	/**
	 * 닉네임 중복확인.
	 *
	 * @param nickname 확인할 닉네임
	 * @return {"used": 사용중이면 true}
	 */
	@GetMapping("/nickname-check")
	public ResponseEntity<Map<String, Boolean>> checkNickname(
			@RequestParam String nickname) {
		
		log.debug("checkNickname(nickname={})", nickname);
		
		if (nickname == null || nickname.trim().isEmpty()) {
			throw new IllegalArgumentException("닉네임을 입력해 주세요.");
		}

		boolean used = memberService.isNicknameUsed(nickname.trim());

		return ResponseEntity.ok(Map.of("used", used));		
	
	}

	/** 잘못된 요청값 예외를 HTTP 400 응답으로 변환 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(
			IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"회원 중복확인 요청값을 확인해 주세요.");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
	}

}
