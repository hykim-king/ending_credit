/**
 * <pre>
 * Class Name  : FooterDataController
 * Description : 모든 뷰(HTML)에서 공통으로 사용할 수 있는 데이터(푸터 댓글 총 개수) 지금까지★숫자 개의 댓글이 쌓였어요.
 *
 */
package com.endit.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.endit.service.UserCommentService;

@ControllerAdvice
public class FooterDataController {
	/** 코멘트 데이터 조회를 위한 Service */
	private final UserCommentService userCommentService;

	public FooterDataController(UserCommentService userCommentService) {
		this.userCommentService = userCommentService;
	}
	
	/**
	 * 푸터 하단에 노출할 USER_COMMENT 테이블의 총 코멘트 개수를 전역으로 공급
	 */
	@ModelAttribute("totalCommentCount")
	public int getTotalCommentCount() {
		try {
			return userCommentService.totalCnt();
		} catch (Exception e) {
			return 0;
		}
	}
}