/**
 * <pre>
 * Class Name : CommentLikeController
 * Description : 코멘트 좋아요 Controller (전부 fetch/AJAX)
 *               ⚠️ 회원 인증(2조 시큐리티 설정)이 아직 준비되지 않아 memberId는 폼 값으로 받는다.
 *                  시큐리티 도입 후 Authentication 기반으로 교체할 것.
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
package com.endit.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.endit.cmn.MessageVO;
import com.endit.domain.CommentLikeVO;
import com.endit.service.CommentLikeService;

@Controller
@RequestMapping("/commentLike")
public class CommentLikeController {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final CommentLikeService commentLikeService;

	public CommentLikeController(CommentLikeService commentLikeService) {
		super();
		this.commentLikeService = commentLikeService;
		log.debug("commentLikeService: {}", commentLikeService);
	}

	/**
	 *
	 * <pre>
	 * Method Name : upToggleLike
	 * Description : 좋아요 토글 — 안 눌렀으면 등록, 눌렀으면 취소.
	 *               detailMessage에 토글 후의 좋아요 수를 담아 화면 갱신에 쓴다.
	 *
	 * </pre>
	 *
	 * @param param (memberId, commentId)
	 * @return MessageVO(id: 1성공, detailMessage: 좋아요 수)
	 */
	@PostMapping("/upToggleLike")
	@ResponseBody
	public MessageVO upToggleLike(CommentLikeVO param) {
		log.debug("=============================");
		log.debug("{}()", "upToggleLike");
		log.debug("param: {}", param);
		log.debug("=============================");

		int state = commentLikeService.upToggleLike(param);
		String message = CommentLikeService.LIKE_ON == state ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";
		int likeCnt = commentLikeService.getLikeCnt(param.getCommentId());

		return new MessageVO("1", message, String.valueOf(likeCnt));
	}

	/**
	 *
	 * <pre>
	 * Method Name : getLikeCnt
	 * Description : 코멘트 하나의 좋아요 수 조회
	 *
	 * </pre>
	 *
	 * @param commentId
	 * @return MessageVO(id: 1, message: 좋아요 수)
	 */
	@GetMapping("/getLikeCnt")
	@ResponseBody
	public MessageVO getLikeCnt(@RequestParam(name = "commentId") long commentId) {
		log.debug("=============================");
		log.debug("{}()", "getLikeCnt");
		log.debug("commentId: {}", commentId);
		log.debug("=============================");

		int likeCnt = commentLikeService.getLikeCnt(commentId);

		return new MessageVO("1", String.valueOf(likeCnt));
	}

}
