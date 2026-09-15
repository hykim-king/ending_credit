/**
 * 여러 그룹을 한 번에 조회한 코드 목록에서 그룹별 목록을 분리 (학원 sb15 원형)
 */
package com.endit.cmn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.endit.domain.CodeVO;

public class CodeUtil {

	/**
	 *
	 * <pre>
	 * Method Name : getCodeList
	 * Description : 코드 목록에서 특정 그룹의 코드만 추출
	 *
	 * </pre>
	 *
	 * @param codeList
	 * @param groupCode
	 * @return List<CodeVO>
	 */
	public static List<CodeVO> getCodeList(List<CodeVO> codeList, String groupCode) {

		if (null == codeList || null == groupCode) {
			return Collections.emptyList();
		}

		List<CodeVO> result = new ArrayList<>();

		for (CodeVO vo : codeList) {
			if (groupCode.equals(vo.getGroupCode())) {
				result.add(vo);
			}
		}

		return result;
	}

}
