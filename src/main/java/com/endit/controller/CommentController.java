/**
 * 코멘트 Controller
 * 목록은 화면(뷰) 반환, 등록/수정/삭제는 fetch(AJAX)용 MessageVO 반환.
 * ⚠️ 회원 인증(2조 시큐리티 설정)이 아직 준비되지 않아 작성자(memberId)는 폼 값으로 받는다.
 * 시큐리티 도입 후 Authentication.getName() 기반으로 교체할 것(학원 23장 방식).
 */
package com.endit.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.endit.cmn.CodeUtil;
import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.CodeVO;
import com.endit.domain.UserCommentVO;
import com.endit.service.CodeService;
import com.endit.service.UserCommentService;

@Controller
@RequestMapping("/comment")
public class CommentController {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final UserCommentService userCommentService;
	private final CodeService codeService; // 신고 모달 사유 select 렌더링용 (학원 28장 두 서비스 주입 패턴)

	public CommentController(UserCommentService userCommentService, CodeService codeService) {
		super();
		this.userCommentService = userCommentService;
		this.codeService = codeService;
		log.debug("userCommentService: {}", userCommentService);
		log.debug("codeService: {}", codeService);
	}

	/**
	 *
	 * <pre>
	 * Method Name : doRetrieve
	 * Description : 코멘트 목록 화면 — 대상 고정 진입 (화면설계서 C-04·D-07)
	 *               contentId가 오면 그 영화의 코멘트(C-04, 정렬 4종·작성 버튼 없음),
	 *               collectionId가 오면 그 컬렉션의 코멘트(D-07, 정렬 2종·작성 버튼 있음).
	 *               대상 없이 직접 들어오면 진입 안내만 보여준다 —
	 *               정식 진입은 영화 상세(1조)·컬렉션 상세(3조)에서 연결된다.
	 *
	 * </pre>
	 *
	 * @param contentId
	 * @param collectionId
	 * @param sort
	 * @param pageSize
	 * @param pageNo
	 * @param model
	 * @return comment/comment_list
	 */
	@GetMapping("/doRetrieve")
	public String doRetrieve(
			@RequestParam(required = false, name = "contentId") Long contentId,
			@RequestParam(required = false, name = "collectionId") Long collectionId,
			@RequestParam(name = "sort", defaultValue = "latest") String sort,
			@RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
			@RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
			Model model) {
		String viewName = "comment/comment_list";
		log.debug("=============================");
		log.debug("{}()", "doRetrieve");
		log.debug("contentId: {}, collectionId: {}, sort: {}, pageSize: {}, pageNo: {}", contentId, collectionId,
				sort, pageSize, pageNo);
		log.debug("=============================");

		// 1. 진입 대상 판별 — 영화(C-04) / 컬렉션(D-07) / 없음(안내)
		String mode = "none";
		String targetTitle = null;

		DTO dto = new DTO();
		dto.setPageSize(pageSize);
		dto.setPageNo(pageNo);
		dto.getSearchMap().put("sort", sort);

		if (null != contentId) {
			mode = "content";
			dto.setSearchDiv("20"); // 영화ID 검색 (매퍼 검색조각 재사용)
			dto.setSearchWord(String.valueOf(contentId));
			targetTitle = userCommentService.getContentTitle(contentId);
		} else if (null != collectionId) {
			mode = "collection";
			dto.setSearchDiv("30"); // 컬렉션ID 검색
			dto.setSearchWord(String.valueOf(collectionId));
			targetTitle = userCommentService.getCollectionTitle(collectionId);
		}

		// 2. 신고 모달의 사유 select용 공통코드 조회 (학원 28장 패턴)
		String[] codeStr = { "REPORT_REASON" };
		Map<String, Object> codeMap = new HashMap<>();
		codeMap.put("code", codeStr);
		List<CodeVO> codeList = codeService.doRetrieve(codeMap);
		model.addAttribute("reasonList", CodeUtil.getCodeList(codeList, "REPORT_REASON"));

		// 3. 목록 조회 (진입 대상이 있을 때만)
		List<UserCommentVO> list = "none".equals(mode) ? List.of() : userCommentService.doRetrieve(dto);

		// 4. 총건수 — 각 행에 실려 오는 totalCnt 사용 (학원 user 계열 방식)
		int totalCnt = list.size() > 0 ? list.get(0).getTotalCnt() : 0;

		// 5. 페이지 블록 계산 (blockSize=10: startNo/endNo/pre/next)
		DTO pageDTO = new DTO(dto.getPageNo(), dto.getPageSize(), totalCnt);

		model.addAttribute("mode", mode);
		model.addAttribute("contentId", contentId);
		model.addAttribute("collectionId", collectionId);
		model.addAttribute("targetTitle", targetTitle);
		model.addAttribute("totalCnt", totalCnt);
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
