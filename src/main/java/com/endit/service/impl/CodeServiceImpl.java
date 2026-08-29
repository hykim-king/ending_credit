/**
 * <pre>
 * Class Name : CodeServiceImpl
 * Description : 공통코드 조회 Service 구현체 — 매퍼 위임(조회 전용, 트랜잭션 불요)
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 25.  홍선기   최초 생성
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

import com.endit.domain.CodeVO;
import com.endit.mapper.CodeMapper;
import com.endit.service.CodeService;

@Service
public class CodeServiceImpl implements CodeService {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final CodeMapper codeMapper;

	public CodeServiceImpl(CodeMapper codeMapper) {
		super();
		this.codeMapper = codeMapper;
		log.debug("codeMapper: {}", codeMapper);
	}

	@Override
	public List<CodeVO> doRetrieve(Map<String, Object> code) {
		log.debug("=============================");
		log.debug("{}()", "doRetrieve");
		log.debug("=============================");

		return codeMapper.doRetrieve(code);
	}

}
