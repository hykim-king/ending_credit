/**
 * 공통코드 조회 Service 인터페이스 (조회 전용 — WorkDiv 미상속)
 */
package com.endit.service;

import java.util.List;
import java.util.Map;

import com.endit.domain.CodeVO;

public interface CodeService {

	/**
	 *
	 * <pre>
	 * Method Name : doRetrieve
	 * Description : 코드 그룹 여러 개를 한 번에 조회
	 *
	 * </pre>
	 *
	 * @param code (key "code" = 코드 그룹 배열)
	 * @return List<CodeVO>
	 */
	List<CodeVO> doRetrieve(Map<String, Object> code);

}
