/**
 * <pre>
 * Class Name : AdminService
 * Description : ⚠️ 관리자 임시 조회 전용 Service (4조 데모용 — 담당 조 구현 시 대체)
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 25.  홍선기   최초 생성 (임시)
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 25.
 */
package com.endit.service;

import java.util.List;
import java.util.Map;

public interface AdminService {

	Map<String, Object> getSummary();

	List<Map<String, Object>> getContentList();

	List<Map<String, Object>> getPersonList();

	List<Map<String, Object>> getMemberList();

	List<Map<String, Object>> getNoticeList();

}
