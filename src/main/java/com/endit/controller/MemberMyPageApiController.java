package com.endit.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.MessageVO;
import com.endit.domain.MemberVO;
import com.endit.security.LoginMemberHelper;
import com.endit.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Class Name  : MemberMyPageApiController
 * Description : 마이페이지 화면이 호출하는 회원 계정/프로필 REST Controller
 *
 *   - 화면(뷰) 반환은 MemberMyPageController가 담당하고, 여기는 JSON만 반환한다.
 *
 *   [보안 원칙 3가지]
 *   1) "내" 정보(/me/**)는 URL이나 요청 본문으로 회원 번호를 받지 않는다.
 *      항상 LoginMemberHelper.getMemberId()로 세션에서 꺼낸다.
 *      → 남의 회원 번호를 끼워 넣어 남의 정보를 조회/수정/삭제할 수 없다.
 *   2) 응답에 비밀번호(BCrypt 해시)를 절대 담지 않는다.
 *      MemberService.getMember()는 내부 검증용으로 password가 채워진 MemberVO를
 *      그대로 돌려주므로, 응답으로 내보내기 직전에 필요한 필드만 골라 담는다.
 *      (서비스/VO는 건드리지 않고 이 컨트롤러에서만 걸러 낸다)
 *   3) 다른 유저 프로필은 공개 필드(닉네임/프로필이미지/소개)만 내보낸다.
 *      이메일·권한·비밀번호는 포함하지 않는다.
 */
@RestController
@RequestMapping("/api/members")
public class MemberMyPageApiController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** LoginMemberHelper가 비로그인 상태에서 던지는 메시지. 이 값으로 401/409를 가른다. */
	private static final String LOGIN_REQUIRED = "로그인이 필요합니다.";

	private final MemberService memberService;

	/**
	 * MemberService를 주입받아 Controller 생성
	 *
	 * @param memberService 회원 Service
	 */
	public MemberMyPageApiController(MemberService memberService) {
		this.memberService = memberService;
	}

	// ===================== 내 계정 =====================

	/**
	 * 내 계정 정보 조회.
	 *
	 * 회원 번호는 세션에서 꺼내므로 요청 파라미터가 없다.
	 *
	 * @return 비밀번호를 제외한 내 계정 정보
	 */
	@GetMapping("/me/account")
	public ResponseEntity<Map<String, Object>> getMyAccount() {

		Long memberId = LoginMemberHelper.getMemberId();

		log.debug("getMyAccount(memberId={})", memberId);

		MemberVO member = memberService.getMember(memberId);

		if (member == null) {
			// 세션은 살아 있는데 회원이 지워진 경우(탈퇴 후 세션 잔존 등)
			throw new NoSuchElementException("회원을 찾을 수 없습니다.");
		}

		return ResponseEntity.ok(toAccountResponse(member));
	}

	/**
	 * 내 프로필 수정 (닉네임 / 소개 / 프로필 이미지).
	 *
	 * 요청 본문에 memberId를 넣어 보내도 무시하고 세션 회원 번호로 덮어쓴다.
	 * 또한 서비스에 넘길 VO를 새로 만들어 수정 허용 필드만 옮겨 담는다.
	 * (요청 본문이 password나 role을 실어 보내도 서비스까지 전달되지 않는다)
	 *
	 * @param param 수정할 닉네임 / 소개 / 프로필 이미지
	 * @return 수정 후 내 계정 정보 (비밀번호 제외)
	 */
	@PatchMapping("/me/profile")
	public ResponseEntity<Map<String, Object>> updateMyProfile(
			@RequestBody MemberVO param) {

		Long memberId = LoginMemberHelper.getMemberId();

		log.debug("updateMyProfile(memberId={}, nickname={})", memberId, param.getNickname());

		// 수정 허용 필드만 옮겨 담는 화이트리스트. memberId는 반드시 세션 값.
		MemberVO target = new MemberVO();
		target.setMemberId(memberId);
		target.setNickname(param.getNickname());
		target.setIntroduction(param.getIntroduction());
		target.setProfileImgUrl(param.getProfileImgUrl());

		memberService.updateProfile(target);

		// 수정 결과를 다시 읽어 최신 상태(updatedDt 포함)를 돌려준다.
		MemberVO updated = memberService.getMember(memberId);

		return ResponseEntity.ok(toAccountResponse(updated));
	}

	/**
	 * 내 비밀번호 변경.
	 *
	 * 요청 본문 예) {"currentPassword": "현재비번", "newPassword": "새비번"}
	 * 현재 비밀번호 검증은 서비스가 담당한다(PASSWORD_MISMATCH).
	 * 소셜 전용 회원은 서비스가 SOCIAL_ONLY_NO_PASSWORD를 던진다.
	 *
	 * @param param currentPassword / newPassword
	 * @return 본문이 없는 응답
	 */
	@PatchMapping("/me/password")
	public ResponseEntity<Void> changeMyPassword(
			@RequestBody(required = false) Map<String, String> param) {

		Long memberId = LoginMemberHelper.getMemberId();

		log.debug("changeMyPassword(memberId={})", memberId);

		if (param == null) {
			throw new IllegalArgumentException("비밀번호 정보를 입력해 주세요.");
		}

		String currentPassword = param.get("currentPassword");
		String newPassword     = param.get("newPassword");

		if (currentPassword == null || currentPassword.trim().isEmpty()) {
			throw new IllegalArgumentException("현재 비밀번호를 입력해 주세요.");
		}
		if (newPassword == null || newPassword.trim().isEmpty()) {
			throw new IllegalArgumentException("새 비밀번호를 입력해 주세요.");
		}

		memberService.changePassword(memberId, currentPassword, newPassword);

		return ResponseEntity.noContent().build();
	}

	/**
	 * 회원 탈퇴 (하드 삭제).
	 *
	 * 요청 본문 예) {"confirmNickname": "내 닉네임"}
	 * 확인용 닉네임 검증은 서비스가 담당한다(NICKNAME_CONFIRM_MISMATCH).
	 *
	 * 삭제 후에도 로그인 세션은 그대로 남아 '없는 회원으로 로그인된 상태'가 되므로
	 * 여기서 세션을 무효화한다. 세션이 없으면 만들 이유가 없어 getSession(false)를 쓴다.
	 *
	 * @param param   confirmNickname
	 * @param request 세션 무효화를 위해 주입받는 요청 객체
	 * @return 본문이 없는 응답
	 */
	@DeleteMapping("/me")
	public ResponseEntity<Void> withdrawMe(
			@RequestBody(required = false) Map<String, String> param,
			HttpServletRequest request) {

		Long memberId = LoginMemberHelper.getMemberId();

		log.debug("withdrawMe(memberId={})", memberId);

		if (param == null) {
			throw new IllegalArgumentException("확인용 닉네임을 입력해 주세요.");
		}

		String confirmNickname = param.get("confirmNickname");

		if (confirmNickname == null || confirmNickname.trim().isEmpty()) {
			throw new IllegalArgumentException("확인용 닉네임을 입력해 주세요.");
		}

		memberService.withdraw(memberId, confirmNickname);

		// 탈퇴가 성공한 뒤에만 세션을 끊는다.
		// (서비스가 예외를 던지면 이 줄까지 오지 않으므로 로그인 상태가 유지된다)
		HttpSession session = request.getSession(false);

		if (session != null) {
			session.invalidate();
		}

		log.debug("회원 탈퇴 완료 memberId={}", memberId);

		return ResponseEntity.noContent().build();
	}

	// ===================== 다른 유저 공개 프로필 =====================

	/**
	 * 다른 유저의 공개 프로필 조회.
	 *
	 * 공개 필드만 담는다. 이메일 / 비밀번호 / 권한은 응답에 포함하지 않는다.
	 * 경로에 숫자 제약을 두어 /api/members/me/**, /api/members/email-check 등
	 * 다른 매핑과 섞이지 않게 한다.
	 *
	 * @param memberId 조회할 회원 번호
	 * @return 공개 프로필 정보
	 */
	@GetMapping("/{memberId:[0-9]+}")
	public ResponseEntity<Map<String, Object>> getPublicProfile(
			@PathVariable long memberId) {

		log.debug("getPublicProfile(memberId={})", memberId);

		MemberVO member = memberService.getMember(memberId);

		if (member == null) {
			throw new NoSuchElementException("회원을 찾을 수 없습니다.");
		}

		return ResponseEntity.ok(toPublicProfileResponse(member));
	}

	// ===================== 응답 조립 (비밀번호 제거 지점) =====================

	/**
	 * 내 계정 정보 응답을 만든다.
	 *
	 * MemberVO를 그대로 반환하지 않고 필드를 골라 담는 이유는,
	 * MemberVO에 BCrypt 해시 password가 들어 있기 때문이다.
	 * 여기서 password는 '값' 대신 socialOnly(비밀번호 없음 = 소셜 전용) 여부로만 바뀐다.
	 * (화면에서 비밀번호 변경 UI를 띄울지 판단하는 데 쓴다)
	 *
	 * @param member 서비스가 조회한 회원 정보
	 * @return 비밀번호를 제외한 계정 정보
	 */
	private Map<String, Object> toAccountResponse(MemberVO member) {

		// LinkedHashMap은 응답 키 순서를 일정하게 유지하기 위해 사용한다.
		// (Map.of는 null 값을 담을 수 없어 소개/프로필이미지가 비면 예외가 난다)
		Map<String, Object> response = new LinkedHashMap<>();

		response.put("memberId",      member.getMemberId());
		response.put("email",         member.getEmail());
		response.put("nickname",      member.getNickname());
		response.put("introduction",  member.getIntroduction());
		response.put("profileImgUrl", member.getProfileImgUrl());
		response.put("role",          member.getRole());
		response.put("createdDt",     member.getCreatedDt());
		response.put("updatedDt",     member.getUpdatedDt());
		response.put("socialOnly",    member.getPassword() == null);
		response.put("stats",         toEmptyStats());

		return response;
	}

	/**
	 * 다른 유저에게 보여줄 공개 프로필 응답을 만든다.
	 *
	 * 허용 목록 방식이라 MemberVO에 필드가 추가되어도 자동으로 새어 나가지 않는다.
	 *
	 * @param member 서비스가 조회한 회원 정보
	 * @return 공개 필드만 담긴 프로필 정보
	 */
	private Map<String, Object> toPublicProfileResponse(MemberVO member) {

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("memberId",      member.getMemberId());
		response.put("nickname",      member.getNickname());
		response.put("profileImgUrl", member.getProfileImgUrl());
		response.put("introduction",  member.getIntroduction());
		response.put("createdDt",     member.getCreatedDt());
		response.put("stats",         toEmptyStats());

		return response;
	}

	/**
	 * 프로필 화면에 표시할 활동 집계.
	 *
	 * 현재 각 도메인에 '회원별 집계'를 제공하는 Service 메서드가 없어 0으로 채운다.
	 * (Mapper에 일부 건수 조회가 있으나 Controller가 다른 도메인의 Mapper를 직접
	 *  주입하면 Controller → Service → Mapper 계층 규칙이 깨지므로 연결하지 않았다)
	 * 각 도메인에 Service 메서드가 생기면 그때 주입받아 값을 채운다.
	 *
	 * @return 0으로 채워진 집계 정보
	 */
	private Map<String, Object> toEmptyStats() {

		Map<String, Object> stats = new LinkedHashMap<>();

		// TODO: 평가 도메인 집계 서비스 연결 필요 (회원별 평가 수)
		stats.put("ratingCnt", 0);

		// TODO: 코멘트 도메인 집계 서비스 연결 필요 (회원별 코멘트 수)
		stats.put("commentCnt", 0);

		// TODO: 컬렉션 도메인 집계 서비스 연결 필요 (회원별 컬렉션 수)
		stats.put("collectionCnt", 0);

		// TODO: 좋아요 도메인 집계 서비스 연결 필요 (회원별 좋아요 수)
		stats.put("likeCnt", 0);

		return stats;
	}

	// ===================== 예외 처리 =====================

	/**
	 * 로그인 상태 / 충돌 예외를 HTTP 응답으로 변환.
	 *
	 * IllegalStateException 하나를 두 곳에서 쓰고 있어 메시지로 구분한다.
	 *   - LoginMemberHelper.getMemberId() : 비로그인 → "로그인이 필요합니다." → 401
	 *   - MemberService                   : 중복/충돌 코드 문자열          → 409
	 * (LoginMemberHelper와 MemberService는 수정하지 않고 여기서만 갈라 낸다)
	 *
	 * @param exception 로그인 필요 또는 충돌 예외
	 * @return 오류 메시지
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<MessageVO> handleLoginOrConflict(
			IllegalStateException exception) {

		String reason = exception.getMessage();

		if (LOGIN_REQUIRED.equals(reason)) {
			MessageVO message = new MessageVO(
					"401",
					LOGIN_REQUIRED,
					"로그인 후 다시 시도해 주세요.");

			return ResponseEntity
					.status(HttpStatus.UNAUTHORIZED)
					.body(message);
		}

		MessageVO message = new MessageVO(
				"409",
				toConflictMessage(reason),
				"요청을 처리할 수 없는 상태입니다.");

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(message);
	}

	/**
	 * 잘못된 요청값 예외를 HTTP 400 응답으로 변환
	 *
	 * @param exception 잘못된 요청값 예외
	 * @return 오류 메시지
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(
			IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"회원 요청값을 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(message);
	}

	/**
	 * 존재하지 않는 회원 예외를 HTTP 404 응답으로 변환
	 *
	 * @param exception 회원 미존재 예외
	 * @return 오류 메시지
	 */
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<MessageVO> handleNotFound(
			NoSuchElementException exception) {

		MessageVO message = new MessageVO(
				"404",
				exception.getMessage(),
				"요청한 회원을 찾을 수 없습니다.");

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(message);
	}

	/**
	 * 서비스가 던진 충돌 코드 문자열을 화면용 한국어 메시지로 변환한다.
	 *
	 * @param code 서비스가 던진 코드 문자열
	 * @return 화면에 보여줄 메시지
	 */
	private String toConflictMessage(String code) {

		if ("NICKNAME_DUPLICATED".equals(code)) {
			return "이미 사용 중인 닉네임입니다.";
		}
		if ("PASSWORD_MISMATCH".equals(code)) {
			return "현재 비밀번호가 올바르지 않습니다.";
		}
		if ("SOCIAL_ONLY_NO_PASSWORD".equals(code)) {
			return "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.";
		}
		if ("NICKNAME_CONFIRM_MISMATCH".equals(code)) {
			return "확인용 닉네임이 일치하지 않습니다.";
		}

		// 서비스가 이미 한국어 메시지를 던진 경우("이미 없는 회원입니다." 등)는 그대로 보여준다.
		return (code == null) ? "요청을 처리할 수 없습니다." : code;
	}
}
