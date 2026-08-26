package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.endit.cmn.DTO;
import com.endit.domain.MemberVO;
import com.endit.mapper.MemberMapper;

@SpringBootTest
@Transactional
class MemberDaoTest {
	
	@Autowired
	private MemberMapper memberMapper;

	@BeforeEach
	void setUp() throws Exception {
		
		
	}
	
    /** 테스트용 회원 객체를 만드는 헬퍼 (중복 방지를 위해 값 일부를 파라미터로 받음) */
    private MemberVO newMember(String email, String nickname) {
        MemberVO member = new MemberVO();
        member.setEmail(email);
        member.setPassword("encodedPw123");
        member.setNickname(nickname);
        member.setIntroduction("안녕하세요");
        member.setProfileImgUrl(null);
        member.setRole("USER");
        return member;
    }

    @Test
    @DisplayName("회원 등록 후 회원번호로 조회")
    void insertAndSelectById() {
        
        MemberVO member = newMember("test1@endit.com", "테스터1");

        // 회원 등록
        int flag = memberMapper.insertMember(member);

        
        assertEquals(1, flag);
        assertNotNull(member.getMemberId());

        // 회원번호로 조회
        MemberVO outVO = memberMapper.selectMemberById(member.getMemberId());

        // 넣은 값과 일치
        assertNotNull(outVO);
        assertEquals("test1@endit.com", outVO.getEmail());
        assertEquals("테스터1", outVO.getNickname());
        assertEquals("USER", outVO.getRole());
        assertNotNull(outVO.getCreatedDt());   // SYSDATE로 채워짐
    }

    @Test
    @DisplayName("이메일로 회원 조회")
    void selectByEmail() {
        
    	// 회원 등록
        MemberVO member = newMember("test2@endit.com", "테스터2");
        memberMapper.insertMember(member);

        // 이메일로 조회
        MemberVO outVO = memberMapper.selectMemberByEmail("test2@endit.com");

        
        assertNotNull(outVO);
        assertEquals(member.getMemberId(), outVO.getMemberId());
        assertEquals("테스터2", outVO.getNickname());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 null이 반환")
    void selectByEmail_notFound() {
        MemberVO outVO = memberMapper.selectMemberByEmail("nobody@endit.com");
        assertNull(outVO);
    }

    @Test
    @DisplayName("닉네임 중복 체크")
    void countByNickname() {
        // 닉네임 '중복닉' 회원 등록
        memberMapper.insertMember(newMember("test3@endit.com", "중복닉"));

        // 있는 닉네임은 1, 없는 닉네임은 0
        assertEquals(1, memberMapper.countByNickname("중복닉"));
        assertEquals(0, memberMapper.countByNickname("없는닉네임"));
    }

    @Test
    @DisplayName("같은 이메일로 두 번 등록하면 예외 발생 (UNIQUE 제약)")
    void insert_duplicateEmail_throws() {
        // 첫 등록은 성공
        memberMapper.insertMember(newMember("dup@endit.com", "닉네임A"));

        // 같은 이메일, 다른 닉네임으로 재등록 시 예외
        assertThrows(DataIntegrityViolationException.class, () -> {
            memberMapper.insertMember(newMember("dup@endit.com", "닉네임B"));
        });
    }

    @Test
    @DisplayName("같은 닉네임으로 두 번 등록하면 예외 발생 (UNIQUE 제약)")
    void insert_duplicateNickname_throws() {
    	
        // 첫 등록은 성공
        memberMapper.insertMember(newMember("emailA@endit.com", "같은닉"));

        // 다른 이메일, 같은 닉네임 → 예외
        assertThrows(DataIntegrityViolationException.class, () -> {
            memberMapper.insertMember(newMember("emailB@endit.com", "같은닉"));
        });
    }

    @Test
    @DisplayName("role에 허용되지 않은 값을 넣으면 예외 발생 (CHECK 제약)")
    void insert_invalidRole_throws() {
        // role을 'GUEST'로 (USER/ADMIN만 허용)
        MemberVO member = newMember("role@endit.com", "권한테스트");
        member.setRole("GUEST");

        assertThrows(DataIntegrityViolationException.class, () -> {
            memberMapper.insertMember(member);
        });
    }

    @Test
    @DisplayName("프로필 수정 후 조회하면 변경된 값이 반영")
    void updateProfile() {
        
        MemberVO member = newMember("update@endit.com", "수정전닉");
        memberMapper.insertMember(member);

        // 닉네임/소개/이미지 변경
        member.setNickname("수정후닉");
        member.setIntroduction("소개 변경됨");
        member.setProfileImgUrl("http://img/new.png");
        int flag = memberMapper.updateProfile(member);

        
        assertEquals(1, flag);
        MemberVO outVO = memberMapper.selectMemberById(member.getMemberId());
        assertEquals("수정후닉", outVO.getNickname());
        assertEquals("소개 변경됨", outVO.getIntroduction());
        assertEquals("http://img/new.png", outVO.getProfileImgUrl());
        assertNotNull(outVO.getUpdatedDt());   // 수정일시가 채워짐
    }

    @Test
    @DisplayName("비밀번호 변경 후 조회하면 변경된 비밀번호가 반영")
    void updatePassword() {
        
        MemberVO member = newMember("pw@endit.com", "비번테스터");
        memberMapper.insertMember(member);

        
        int updated = memberMapper.updatePassword(member.getMemberId(), "newEncodedPw");

        
        assertEquals(1, updated);
        MemberVO outVO = memberMapper.selectMemberById(member.getMemberId());
        assertEquals("newEncodedPw", outVO.getPassword());
    }

    @Test
    @DisplayName("회원 삭제 후 조회하면 null 반환")
    void deleteMember() {
        
        MemberVO member = newMember("delete@endit.com", "삭제테스터");
        memberMapper.insertMember(member);
        Long id = member.getMemberId();

        
        int deleted = memberMapper.deleteMember(id);

        
        assertEquals(1, deleted);
        assertNull(memberMapper.selectMemberById(id));
    }
    
    @Test
    @DisplayName("회원 목록 페이징 조회 - 1페이지 10개")
    void selectMemberList() {
        DTO dto = new DTO();
        dto.setPageNo(1);
        dto.setPageSize(10);

        List<MemberVO> list = memberMapper.selectMemberList(dto);

        assertNotNull(list);
        assertTrue(list.size() <= 10);   // 한 페이지 최대 10개
    }

    @Test
    @DisplayName("회원 전체 개수 조회")
    void selectMemberCount() {
        DTO dto = new DTO();

        int count = memberMapper.selectMemberCount(dto);

        assertTrue(count >= 0);
    }

    @Test
    @DisplayName("이메일로 검색하면 해당 회원만 조회된다")
    void selectMemberList_searchByEmail() {
        DTO dto = new DTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        dto.setSearchDiv("email");
        dto.setSearchWord("admin1");   // admin1@endit.com

        List<MemberVO> list = memberMapper.selectMemberList(dto);

        assertNotNull(list);
        assertTrue(list.size() >= 1);
        assertTrue(list.get(0).getEmail().contains("admin1"));
    }

    @Test
    @DisplayName("닉네임으로 검색하면 해당 회원만 조회된다")
    void selectMemberList_searchByNickname() {
        DTO dto = new DTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        dto.setSearchDiv("nickname");
        dto.setSearchWord("관리자");   // ENDIT수석관리자 등

        List<MemberVO> list = memberMapper.selectMemberList(dto);

        assertNotNull(list);
        assertTrue(list.size() >= 1);
    }

    @Test
    @DisplayName("검색어와 개수가 일치한다 - 목록과 카운트 조건 동일")
    void listCountMatch() {
        DTO dto = new DTO();
        dto.setPageNo(1);
        dto.setPageSize(100);        // 넉넉히 잡아 전체를 한 페이지에
        dto.setSearchDiv("email");
        dto.setSearchWord("endit.com");   // 다 포함

        List<MemberVO> list = memberMapper.selectMemberList(dto);
        int count = memberMapper.selectMemberCount(dto);

        assertEquals(count, list.size());   // 목록 개수 == 카운트
    }

}
