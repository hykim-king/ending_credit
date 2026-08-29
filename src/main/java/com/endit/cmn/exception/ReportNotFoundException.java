/**
 * <pre>
 * Class Name : ReportNotFoundException
 * Description : 신고가 존재하지 않을 때 던지는 업무 예외
 *               (학원 표준: cmn.exception 아래 RuntimeException 상속, 메시지 생성자 하나)
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

public class ReportNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ReportNotFoundException(String message) {
		super(message);
	}

}
