/**
 * 신고가 존재하지 않을 때 던지는 업무 예외
 * (학원 표준: cmn.exception 아래 RuntimeException 상속, 메시지 생성자 하나)
 */
package com.endit.cmn.exception;

public class ReportNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ReportNotFoundException(String message) {
		super(message);
	}

}
