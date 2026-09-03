/**
 * 회원 관리 Service JUnit (AD-07)
 * 조회·페이징·검색과 강퇴 가드(관리자 보호, 없는 회원)를 검증한다.
 * 팀 테스트 규칙: 공용 더미 위에서 @Transactional 롤백.
 */
package com.endit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.MemberVO;
import com.endit.mapper.MemberMapper;

@SpringBootTest
@Transactional
@DisplayName("AdminMemberService 테스트")
class AdminMemberServiceJUnit {

	final Logger log = LoggerFactory.getLogger(getClass());

	private static final long ADMIN_MEMBER = 9L;          // admin1@endit.com — ROLE=ADMIN
	private static final long MISSING_MEMBER = 999_999_999L;

	@Autowired
	AdminMemberService service;

	@Autowired
	MemberMapper memberMapper; // 강퇴 대상 회원을 트랜잭션 안에서 만들기 위해

	private DTO dto;

	@BeforeEach
	void setUp() throws Exception {
		log.debug("*****************************");
		log.debug("*@BeforeEach*");
		log.debug("*****************************");
		dto = new DTO();
		dto.setPageNo(1);
		dto.setPageSize(10);
	}

	@Test
	public void doRetrieveAndPaging() {
		log.debug("---------------------------");
		log.debug("*doRetrieveAndPaging()*");
		log.debug("---------------------------");
		// 1. 조건 없이 조회 — 더미가 있으므로 1건 이상
		// 2. 페이지 크기만큼만 나온다
		// 3. 총건수는 목록 크기 이상

		List<MemberVO> list = service.doRetrieve(dto);
		assertNotNull(list);
		assertTrue(list.size() > 0);
		assertTrue(list.size() <= 10);

		int totalCnt = service.totalCnt(dto);
		log.debug("totalCnt: {}", totalCnt);
		assertTrue(totalCnt >= list.size());
	}

	@Test
	public void doRetrieveBySearch() {
		log.debug("---------------------------");
		log.debug("*doRetrieveBySearch()*");
		log.debug("---------------------------");
		// 이메일 검색 — admin 이 들어간 계정만 잡힌다
		dto.setSearchDiv("email");
		dto.setSearchWord("admin");

		List<MemberVO> list = service.doRetrieve(dto);
		assertNotNull(list);
		for (MemberVO vo : list) {
			assertTrue(vo.getEmail().contains("admin"), "검색어가 안 걸린 행: " + vo.getEmail());
		}
		assertEquals(list.size() > 0, service.totalCnt(dto) > 0);
	}

	@Test
	public void doSelectOne() {
		log.debug("---------------------------");
		log.debug("*doSelectOne()*");
		log.debug("---------------------------");
		MemberVO outVO = service.doSelectOne(ADMIN_MEMBER);
		assertNotNull(outVO);
		assertEquals(ADMIN_MEMBER, outVO.getMemberId().longValue());

		assertNull(service.doSelectOne(MISSING_MEMBER));
	}

	@Test
	public void upWithdrawMemberGuards() {
		log.debug("---------------------------");
		log.debug("*upWithdrawMemberGuards()*");
		log.debug("---------------------------");
		// 1. 없는 회원 → 거부
		// 2. 관리자 계정 → 거부 (마지막 관리자를 지우면 관리 화면에 못 들어간다)

		assertThrows(IllegalArgumentException.class, () -> service.upWithdrawMember(MISSING_MEMBER));
		assertThrows(IllegalArgumentException.class, () -> service.upWithdrawMember(ADMIN_MEMBER));
	}

	@Test
	public void upWithdrawMember() {
		log.debug("---------------------------");
		log.debug("*upWithdrawMember()*");
		log.debug("---------------------------");
		// 트랜잭션 안에서 일반 회원을 만들고 강퇴한다 (롤백되므로 공용 DB에 남지 않는다)
		MemberVO target = new MemberVO();
		target.setEmail("ban-target@endit.com");
		target.setPassword("{noop}x");
		target.setNickname("강퇴대상테스트");
		target.setRole("USER");
		memberMapper.insertMember(target);

		Long newId = target.getMemberId();
		assertNotNull(newId);
		assertNotNull(service.doSelectOne(newId));

		assertEquals(1, service.upWithdrawMember(newId));
		assertNull(service.doSelectOne(newId));
	}

	@Test
	void beans() {
		log.debug("---------------------------");
		log.debug("*beans()*");
		log.debug("---------------------------");
		assertNotNull(service);
		assertNotNull(memberMapper);
		log.debug("service: {}", service);
	}
}
