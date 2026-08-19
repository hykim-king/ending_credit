/**
 * <pre>
 * Class Name : CommentController
 * Description : 코멘트 Controller
 *               목록은 화면(뷰) 반환, 등록/수정/삭제는 fetch(AJAX)용 MessageVO 반환.
 *               ⚠️ 회원 인증(2조 시큐리티 설정)이 아직 준비되지 않아 작성자(memberId)는 폼 값으로 받는다.
 *                  시큐리티 도입 후 Authentication.getName() 기반으로 교체할 것(학원 23장 방식).
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.UserCommentVO;
import com.endit.service.UserCommentService;

@Controller
@RequestMapping("/comment")
public class CommentController {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final UserCommentService userCommentService;

	public CommentController(UserCommentService userCommentService) {
		super();
		this.userCommentService = userCommentService;
		log.debug("userCommentService: {}", userCommentService);
	}

	/**
	 *
	 * <pre>
	 * Method Name : doRetrieve
	 * Description : 코멘트 목록 화면 (검색 10=회원ID/20=영화ID/30=컬렉션ID + 페이징)
	 *               정렬(sort): latest(기본)/likes/rating_desc/rating_asc — 화면설계서 C-04
	 *
	 * </pre>
	 *
	 * @param searchDiv
	 * @param searchWord
	 * @param pageSize
	 * @param pageNo
	 * @param sort
	 * @param model
	 * @return comment/comment_list
	 */
	@GetMapping("/doRetrieve")
	public String doRetrieve(
			@RequestParam(required = false, name = "searchDiv", defaultValue = "") String searchDiv,
			@RequestParam(required = false, name = "searchWord", defaultValue = "") String searchWord,
			@RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
			@RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
			@RequestParam(name = "sort", defaultValue = "latest") String sort,
			Model model) {
		String viewName = "comment/comment_list";
		log.debug("=============================");
		log.debug("{}()", "doRetrieve");
		log.debug("searchDiv: {}, searchWord: {}, pageSize: {}, pageNo: {}, sort: {}", searchDiv, searchWord,
				pageSize, pageNo, sort);
		log.debug("=============================");

		// 1. 검색·페이징·정렬 조건 (정렬은 학원 24장 searchMap 방식 — 허용값 외에는 매퍼가 latest로 처리)
		DTO dto = new DTO();
		dto.setSearchDiv(searchDiv);
		dto.setSearchWord(searchWord);
		dto.setPageSize(pageSize);
		dto.setPageNo(pageNo);
		dto.getSearchMap().put("sort", sort);

		// 2. 목록 조회
		List<UserCommentVO> list = userCommentService.doRetrieve(dto);

		// 3. 총건수 — 각 행에 실려 오는 totalCnt 사용 (학원 user 계열 방식)
		int totalCnt = list.size() > 0 ? list.get(0).getTotalCnt() : 0;

		// 4. 페이지 블록 계산 (blockSize=10: startNo/endNo/pre/next)
		DTO pageDTO = new DTO(dto.getPageNo(), dto.getPageSize(), totalCnt);

		model.addAttribute("list", list);
		model.addAttribute("pageDTO", pageDTO);
		model.addAttribute("dto", dto);

		return viewName;
	}

	/**
	 *
	 * <pre>
	 * Method Name : doSave
	 * Description : 코멘트 등록 (fetch: requestPostForm → @ModelAttribute 바인딩)
	 *
	 * </pre>
	 *
	 * @param param
	 * @return MessageVO(id: 1성공/0실패)
	 */
	@PostMapping("/doSave")
	@ResponseBody
	public MessageVO doSave(UserCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doSave");
		log.debug("param: {}", param);
		log.debug("=============================");

		int flag = userCommentService.doSave(param);
		String message = 1 == flag ? "코멘트가 등록 되었습니다." : "코멘트 등록에 실패 했습니다.";

		return new MessageVO(flag + "", message);
	}

	/**
	 *
	 * <pre>
	 * Method Name : doUpdate
	 * Description : 코멘트 수정 — 내용·스포일러 여부만 바꿀 수 있다(대상은 불변)
	 *
	 * </pre>
	 *
	 * @param param (commentId, commentDetail, spoiler)
	 * @return MessageVO(id: 1성공/0실패)
	 */
	@PostMapping("/doUpdate")
	@ResponseBody
	public MessageVO doUpdate(UserCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doUpdate");
		log.debug("param: {}", param);
		log.debug("=============================");

		int flag = userCommentService.doUpdate(param);
		String message = 1 == flag ? "코멘트가 수정 되었습니다." : "코멘트 수정에 실패 했습니다.";

		return new MessageVO(flag + "", message);
	}

	/**
	 *
	 * <pre>
	 * Method Name : doDelete
	 * Description : 코멘트 삭제 — 좋아요·신고도 FK CASCADE로 함께 삭제된다
	 *
	 * </pre>
	 *
	 * @param commentId
	 * @return MessageVO(id: 1성공/0실패)
	 */
	@GetMapping("/doDelete")
	@ResponseBody
	public MessageVO doDelete(@RequestParam(name = "commentId") long commentId) {
		log.debug("=============================");
		log.debug("{}()", "doDelete");
		log.debug("commentId: {}", commentId);
		log.debug("=============================");

		UserCommentVO param = new UserCommentVO();
		param.setCommentId(commentId);

		int flag = userCommentService.doDelete(param);
		String message = 1 == flag ? "코멘트가 삭제 되었습니다." : "코멘트 삭제에 실패 했습니다.";

		return new MessageVO(flag + "", message);
	}

}
