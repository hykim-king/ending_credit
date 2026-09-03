/**
 * 회원 관리 Service 구현 (AD-07)
 * 회원 도메인은 2조 소유라 MemberMapper를 그대로 주입해 쓰고 새 매퍼를 만들지 않는다.
 */
package com.endit.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.MemberVO;
import com.endit.mapper.MemberMapper;
import com.endit.service.AdminMemberService;

@Service
public class AdminMemberServiceImpl implements AdminMemberService {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final MemberMapper memberMapper;

	public AdminMemberServiceImpl(MemberMapper memberMapper) {
		super();
		this.memberMapper = memberMapper;
		log.debug("memberMapper: {}", memberMapper);
	}

	@Override
	public List<MemberVO> doRetrieve(DTO param) {
		log.debug("=============================");
		log.debug("{}()", "doRetrieve");
		log.debug("param: {}", param);
		log.debug("=============================");

		return memberMapper.selectMemberList(param);
	}

	@Override
	public int totalCnt(DTO param) {
		log.debug("=============================");
		log.debug("{}()", "totalCnt");
		log.debug("=============================");

		return memberMapper.selectMemberCount(param);
	}

	@Override
	public MemberVO doSelectOne(long memberId) {
		log.debug("=============================");
		log.debug("{}()", "doSelectOne");
		log.debug("memberId: {}", memberId);
		log.debug("=============================");

		return memberMapper.selectMemberById(memberId);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int upWithdrawMember(long memberId) {
		log.debug("=============================");
		log.debug("{}()", "upWithdrawMember");
		log.debug("memberId: {}", memberId);
		log.debug("=============================");

		MemberVO outVO = memberMapper.selectMemberById(memberId);
		if (null == outVO) {
			throw new IllegalArgumentException("존재하지 않는 회원입니다. memberId=" + memberId);
		}

		// 관리자 계정은 강퇴 대상에서 제외 — 마지막 관리자를 지우면 관리 화면에 들어갈 수 없다
		if (ROLE_ADMIN.equals(outVO.getRole())) {
			throw new IllegalArgumentException("관리자 계정은 강퇴할 수 없습니다.");
		}

		int flag = memberMapper.deleteMember(memberId);
		if (1 != flag) {
			throw new RuntimeException("회원 강퇴에 실패했습니다.");
		}

		return flag;
	}

	private static final String ROLE_ADMIN = "ADMIN";
}
