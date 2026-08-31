/**
 * ⚠️ 관리자 임시 조회 전용 Service (4조 데모용 — 담당 조 구현 시 대체)
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
