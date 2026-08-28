/**
 * <pre>
 * Class Name : AdminServiceImpl
 * Description : ⚠️ 관리자 임시 조회 전용 Service 구현체 — 매퍼 위임(읽기만, 트랜잭션 불요)
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
package com.endit.service.impl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.endit.mapper.AdminMapper;
import com.endit.service.AdminService;

@Service
public class AdminServiceImpl implements AdminService {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final AdminMapper adminMapper;

	public AdminServiceImpl(AdminMapper adminMapper) {
		super();
		this.adminMapper = adminMapper;
		log.debug("adminMapper: {}", adminMapper);
	}

	@Override
	public Map<String, Object> getSummary() {
		log.debug("=============================");
		log.debug("{}()", "getSummary");
		log.debug("=============================");

		return adminMapper.getSummary();
	}

	@Override
	public List<Map<String, Object>> getContentList() {
		log.debug("=============================");
		log.debug("{}()", "getContentList");
		log.debug("=============================");

		return adminMapper.getContentList();
	}

	@Override
	public List<Map<String, Object>> getPersonList() {
		log.debug("=============================");
		log.debug("{}()", "getPersonList");
		log.debug("=============================");

		return adminMapper.getPersonList();
	}

	@Override
	public List<Map<String, Object>> getMemberList() {
		log.debug("=============================");
		log.debug("{}()", "getMemberList");
		log.debug("=============================");

		return adminMapper.getMemberList();
	}

	@Override
	public List<Map<String, Object>> getNoticeList() {
		log.debug("=============================");
		log.debug("{}()", "getNoticeList");
		log.debug("=============================");

		return adminMapper.getNoticeList();
	}

}
