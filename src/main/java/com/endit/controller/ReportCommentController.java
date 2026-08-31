/**
 * 코멘트 신고 Controller
 * 접수/처리(반려)/승인은 fetch(AJAX)용 MessageVO, 관리자 목록(AD-09)은 화면 반환.
 * 경로 규칙(2조 SecurityConfig): 관리자 기능은 /admin/** 아래여야 ADMIN 보호를 받는다
 * → 접수(doSave)만 회원 경로(/report), 목록·상세·처리·승인은 /admin/report/**.
 * ⚠️ 회원 인증(2조 시큐리티 설정)이 아직 준비되지 않아 신고자·처리자 ID는 폼 값으로 받는다.
 * 시큐리티 도입 후 LoginMemberHelper 기반으로 교체할 것.
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.endit.cmn.CodeUtil;
import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.cmn.exception.ReportNotFoundException;
import com.endit.domain.CodeVO;
import com.endit.domain.ReportCommentVO;
import com.endit.service.CodeService;
import com.endit.service.ReportCommentService;

@Controller
public class ReportCommentController {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final ReportCommentService reportCommentService;
	private final CodeService codeService; // 검색 필터 select 렌더링용 (학원 28장 두 서비스 주입 패턴)

	public ReportCommentController(ReportCommentService reportCommentService, CodeService codeService) {
		super();
		this.reportCommentService = reportCommentService;
		this.codeService = codeService;
		log.debug("reportCommentService: {}", reportCommentService);
		log.debug("codeService: {}", codeService);
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
	@GetMapping("/admin/report/doRetrieve")
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

		// 2. 검색 필터 select용 공통코드 조회 (사유·처리상태 — 학원 28장 패턴)
		String[] codeStr = { "REPORT_REASON", "REPORT_STATUS" };
		Map<String, Object> codeMap = new HashMap<>();
		codeMap.put("code", codeStr);
		List<CodeVO> codeList = codeService.doRetrieve(codeMap);
		model.addAttribute("reasonList", CodeUtil.getCodeList(codeList, "REPORT_REASON"));
		model.addAttribute("statusList", CodeUtil.getCodeList(codeList, "REPORT_STATUS"));

		// 3. 목록 조회
		List<ReportCommentVO> list = reportCommentService.doRetrieve(dto);

		// 4. 총건수 — 각 행에 실려 오는 totalCnt 사용
		int totalCnt = list.size() > 0 ? list.get(0).getTotalCnt() : 0;

		// 5. 페이지 블록 계산
		DTO pageDTO = new DTO(dto.getPageNo(), dto.getPageSize(), totalCnt);

		model.addAttribute("list", list);
		model.addAttribute("pageDTO", pageDTO);
		model.addAttribute("dto", dto);

		return viewName;
	}

	/**
	 *
	 * <pre>
	 * Method Name : doSelectOne
	 * Description : 신고 상세·처리 화면(AD-10)
	 *
	 * </pre>
	 *
	 * @param reportId
	 * @param model
	 * @return report/report_mng
	 */
	@GetMapping("/admin/report/doSelectOne")
	public String doSelectOne(@RequestParam(name = "reportId") long reportId, Model model) {
		String viewName = "report/report_mng";
		log.debug("=============================");
		log.debug("{}()", "doSelectOne");
		log.debug("reportId: {}", reportId);
		log.debug("=============================");

		ReportCommentVO param = new ReportCommentVO();
		param.setReportId(reportId);

		ReportCommentVO outVO = reportCommentService.doSelectOne(param);
		if (null == outVO) {
			throw new ReportNotFoundException("신고가 존재하지 않습니다. reportId=" + reportId);
		}
		log.debug("outVO: {}", outVO);

		model.addAttribute("outVO", outVO);

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
	@PostMapping("/report/doSave")
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
	@PostMapping("/admin/report/doUpdate")
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
	 * Description : 신고 승인(AD-10) — 신고 상태만 ACCEPTED로 저장(팀 결정: 삭제 없음).
	 *               해당 코멘트는 목록에서 사유별 안내 문구로 가려진다.
	 *
	 * </pre>
	 *
	 * @param param (reportId, processedByMemberId, processNote)
	 * @return MessageVO(id: 1성공/0실패)
	 */
	@PostMapping("/admin/report/upApproveReport")
	@ResponseBody
	public MessageVO upApproveReport(ReportCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "upApproveReport");
		log.debug("param: {}", param);
		log.debug("=============================");

		int flag = reportCommentService.upApproveReport(param);
		String message = 1 == flag ? "신고를 승인했습니다. 해당 댓글은 목록에서 안내 문구로 가려집니다." : "신고 승인에 실패 했습니다.";

		return new MessageVO(flag + "", message);
	}

}
