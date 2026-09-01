/**
 * 코멘트 Service 구현체
 * - CRUD는 매퍼 위임, 쓰기 메서드에만 @Transactional(rollbackFor)
 * - 목록 검색어는 전부 숫자 컬럼(회원/영화/컬렉션 ID)이라
 * 여기서 숫자 검증을 해 ORA-01722(수치 부적합)를 DB 전에 차단한다
 */
package com.endit.service.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.UserCommentVO;
import com.endit.mapper.UserCommentMapper;
import com.endit.service.UserCommentService;

@Service
public class UserCommentServiceImpl implements UserCommentService {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final UserCommentMapper userCommentMapper;

	public UserCommentServiceImpl(UserCommentMapper userCommentMapper) {
		super();
		this.userCommentMapper = userCommentMapper;
		log.debug("userCommentMapper: {}", userCommentMapper);
	}

	@Override
	public List<UserCommentVO> doRetrieve(DTO param) {
		log.debug("=============================");
		log.debug("{}()", "doRetrieve");
		log.debug("=============================");

		// 1. 검색어 숫자 검증 (searchDiv 10=회원ID/20=영화ID/30=컬렉션ID — 전부 숫자 컬럼)
		checkNumericSearchWord(param);

		// 2. 목록 조회 (총건수는 각 행의 totalCnt에 실려 온다 — CROSS JOIN 방식)
		return userCommentMapper.doRetrieve(param);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int doSave(UserCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doSave");
		log.debug("=============================");

		return userCommentMapper.doSave(param);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int doUpdate(UserCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doUpdate");
		log.debug("=============================");

		return userCommentMapper.doUpdate(param);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int doDelete(UserCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doDelete");
		log.debug("=============================");

		// 좋아요·신고는 FK ON DELETE CASCADE로 함께 삭제된다
		return userCommentMapper.doDelete(param);
	}

	@Override
	public UserCommentVO doSelectOne(UserCommentVO param) {
		log.debug("=============================");
		log.debug("{}()", "doSelectOne");
		log.debug("=============================");

		return userCommentMapper.doSelectOne(param);
	}

	@Override
	public int totalCnt() {
		log.debug("=============================");
		log.debug("{}()", "totalCnt");
		log.debug("=============================");

		return userCommentMapper.totalCnt();
	}

	@Override
	public int totalCntBySearch(DTO param) {
		log.debug("=============================");
		log.debug("{}()", "totalCntBySearch");
		log.debug("param: {}", param);
		log.debug("=============================");

		// 목록 조회와 같은 검증을 태운다 — 숫자 컬럼 검색에 문자가 오면 ORA-01722
		checkNumericSearchWord(param);

		return userCommentMapper.totalCntBySearch(param);
	}

	@Override
	public String getContentTitle(long contentId) {
		log.debug("=============================");
		log.debug("{}()", "getContentTitle");
		log.debug("=============================");

		return userCommentMapper.getContentTitle(contentId);
	}

	@Override
	public String getCollectionTitle(long collectionId) {
		log.debug("=============================");
		log.debug("{}()", "getCollectionTitle");
		log.debug("=============================");

		return userCommentMapper.getCollectionTitle(collectionId);
	}

	// 숫자 검색을 쓰는 검색구분 — 매퍼 XML의 <when> 값과 일치해야 한다 (10=회원ID/20=영화ID/30=컬렉션ID)
	private static final List<String> NUMERIC_SEARCH_DIVS = List.of("10", "20", "30");

	// ASCII 숫자 1~18자리만 허용. StringUtils.isNumeric은 전각(１２３)·아랍 숫자도 통과시켜
	// ORA-01722를 못 막고, 19자리 이상은 NUMBER 변환 오버플로(ORA-01426) 위험이 있다.
	private static final String NUMERIC_PATTERN = "\\d{1,18}";

	/**
	 * 숫자 검색(searchDiv 10/20/30)일 때 검색어가 ASCII 숫자인지 확인한다.
	 * 매퍼는 그 외의 searchDiv에서는 검색어를 SQL에 넣지 않으므로 검사하지 않는다.
	 *
	 * @param param
	 */
	private void checkNumericSearchWord(DTO param) {
		if (null == param || StringUtils.isEmpty(param.getSearchWord())) {
			return;
		}
		if (false == NUMERIC_SEARCH_DIVS.contains(param.getSearchDiv())) {
			return;
		}
		if (false == param.getSearchWord().matches(NUMERIC_PATTERN)) {
			throw new IllegalArgumentException("검색어는 숫자(번호)만 입력할 수 있습니다: " + param.getSearchWord());
		}
	}

}
