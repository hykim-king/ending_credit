/**
 * <pre>
 * Class Name : GlobalRestExceptionHandler
 * Description : Fetch(AJAX) 예외 처리
 *               컨트롤러에서 던져진 예외를 한 곳에서 받아 MessageVO(JSON)로 응답한다.
 *               id="0"(실패), message=사용자 안내, detailMessage=스택트레이스.
 *               ※ 학원 원본(sb13)은 NotFound를 NO_CONTENT(204)로 응답하나 주석 의도(404)대로 교정했다
 *                 (204는 fetch에서 응답 본문이 무시될 수 있음).
 *               ※ @Order(1): 화면용 advice와 둘 다 전역이라 스프링이 순서로 하나만 고른다 —
 *                 순서를 명시해 동작을 결정적으로 고정(현 단계는 AJAX 우선. 화면 단계에서 재설계 예정).
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 18.  홍선기   최초 생성
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 18.
 */
package com.endit.cmn.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.endit.cmn.MessageVO;

@Order(1)
@RestControllerAdvice
public class GlobalRestExceptionHandler {

	final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 신고 없음 — 404
	 *
	 * @param e
	 * @return ResponseEntity<MessageVO>
	 */
	@ExceptionHandler(ReportNotFoundException.class)
	public ResponseEntity<MessageVO> handlerReportNotFoundException(ReportNotFoundException e) {
		log.debug("=============================");
		log.debug("handlerReportNotFoundException: {}", e.getMessage());
		log.debug("=============================");

		MessageVO messageVO = new MessageVO();
		messageVO.setId("0");
		messageVO.setMessage(e.getMessage());
		messageVO.setDetailMessage(ExceptionUtils.getStackTrace(e));

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(messageVO);
	}

	/**
	 * 입력값 오류(검색어 숫자 검증 등) — 400
	 *
	 * @param e
	 * @return ResponseEntity<MessageVO>
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handlerIllegalArgumentException(IllegalArgumentException e) {
		log.debug("=============================");
		log.debug("handlerIllegalArgumentException: {}", e.getMessage());
		log.debug("=============================");

		MessageVO messageVO = new MessageVO();
		messageVO.setId("0");
		messageVO.setMessage(e.getMessage());
		messageVO.setDetailMessage(ExceptionUtils.getStackTrace(e));

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(messageVO);
	}

	/**
	 * 그 외 모든 예외 — 500 (사용자에게는 고정 메시지, 상세는 detailMessage에만)
	 *
	 * @param e
	 * @return ResponseEntity<MessageVO>
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<MessageVO> handlerException(Exception e) {
		log.debug("=============================");
		log.debug("handlerException: {}", e.getMessage());
		log.debug("=============================");

		MessageVO messageVO = new MessageVO();
		messageVO.setId("0");
		messageVO.setMessage("서비스 처리중 오류가 발생했습니다.");
		messageVO.setDetailMessage(ExceptionUtils.getStackTrace(e));

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(messageVO);
	}

}
