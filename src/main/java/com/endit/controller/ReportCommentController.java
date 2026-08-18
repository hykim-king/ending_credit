/**
 * <pre>
 * Class Name : ReportCommentController
 * Description : 코멘트 신고 Controller
 *               접수/처리(반려)/승인은 fetch(AJAX)용 MessageVO, 관리자 목록(AD-09)은 화면 반환.
 *               ⚠️ 회원 인증(2조 시큐리티 설정)이 아직 준비되지 않아 신고자·처리자 ID는 폼 값으로 받는다.
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
import com.endit.domain.ReportCommentVO;
import com.endit.service.ReportCommentService;

@Controller
@RequestMapping("/report")
public class ReportCommentController {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final ReportCommentService reportCommentService;

	public ReportCommentController(ReportCommentService reportCommentService) {
		super();
		this.reportCommentService = reportCommentService;
		log.debug("reportCommentService: {}", reportCommentService);
	}

	/**
	 *
	 * <pre>
	 * Method Name : doRetrieve
	 * Description : 신고 관리 목록 화면(AD-09) — 검색 10=처리상태/20=사유/30=코멘트ID
	 *
	 * </pre>
	 *
	 * @param searchDiv
	 * @param searchWord
	 * @param pageSize
	 * @param pageNo
	 * @param model
	 * @return report/report_list
	 */
	@GetMapping("/doRetrieve")
	public String doRetrieve(
			@RequestParam(required = false, name = "searchDiv", defaultValue = "") String searchDiv,
			@RequestParam(required = false, name = "searchWord", defaultValue = "") String searchWord,
			@RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
			@RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
			Model model) {
		String viewName = "report/report_list";
		log.debug("=============================");
		log.debug("{}()", "doRetrieve");
		log.debug("searchDiv: {}, searchWord: {}, pageSize: {}, pageNo: {}", searchDiv, searchWord, pageSize, pageNo);
		log.debug("=============================");

		// 1. 검색·페이징 조건
		DTO dto = new DTO();
		dto.setSearchDiv(searchDiv);
		dto.setSearchWord(searchWord);
		dto.setPageSize(pageSize);
		dto.setPageNo(pageNo);

		// 2. 목록 조회
		List<ReportCommentVO> list = reportCommentService.doRetrieve(dto);

		// 3. 총건수 — 각 행에 실려 오는 totalCnt 사용
		int totalCnt = list.size() > 0 ? list.get(0).getTotalCnt() : 0;

		// 4. 페이지 블록 계산
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
	 * Description : 신고 접수(MOD-03/04) — 처리상태는 DB DEFAULT 'RECEIVED'
	 *
	 * </pre>
	 *
	 * @param param (reportMemberId, commentId, reason, detail)
	 * @return MessageVO(id: 1성공/0실패)
	 */
	@PostMapping("/doSave")
	@ResponseBody
	public MessageVO doSave(ReportCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doSave");
		log.debug("param: {}", param);
		log.debug("=============================");

		int flag = reportCommentService.doSave(param);
		String message = 1 == flag ? "신고가 접수 되었습니다." : "신고 접수에 실패 했습니다.";

		return new MessageVO(flag + "", message);
	}

	/**
	 *
	 * <pre>
	 * Method Name : doUpdate
	 * Description : 신고 처리(반려 등 상태 변경, AD-10)
	 *               완료 상태(REJECTED 등)는 처리자·메모를 함께 보내야 한다(CK_REPORT_PROCESS_COMPLETE)
	 *
	 * </pre>
	 *
	 * @param param (reportId, status, processedByMemberId, processNote)
	 * @return MessageVO(id: 1성공/0실패)
	 */
	@PostMapping("/doUpdate")
	@ResponseBody
	public MessageVO doUpdate(ReportCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doUpdate");
		log.debug("param: {}", param);
		log.debug("=============================");

		int flag = reportCommentService.doUpdate(param);
		String message = 1 == flag ? "신고가 처리 되었습니다." : "신고 처리에 실패 했습니다.";

		return new MessageVO(flag + "", message);
	}

	/**
	 *
	 * <pre>
	 * Method Name : upApproveReport
	 * Description : 신고 승인(AD-10) — 승인 처리 후 대상 코멘트 삭제까지 한 트랜잭션.
	 *               코멘트가 지워지면 이 신고 이력도 CASCADE로 함께 사라진다(팀 잠정 ⓑ안).
	 *
	 * </pre>
	 *
	 * @param param (reportId, processedByMemberId, processNote)
	 * @return MessageVO(id: 1성공/0실패)
	 */
	@PostMapping("/upApproveReport")
	@ResponseBody
	public MessageVO upApproveReport(ReportCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "upApproveReport");
		log.debug("param: {}", param);
		log.debug("=============================");

		int flag = reportCommentService.upApproveReport(param);
		String message = 1 == flag ? "신고를 승인하고 해당 코멘트를 삭제했습니다." : "신고 승인에 실패 했습니다.";

		return new MessageVO(flag + "", message);
	}

}
