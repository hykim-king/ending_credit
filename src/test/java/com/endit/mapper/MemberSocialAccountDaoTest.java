package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.MemberSocialAccountVO;
import com.endit.domain.MemberVO;

@SpringBootTest
@Transactional
class MemberSocialAccountDaoTest {

	@Autowired
	private MemberSocialAccountMapper socialMapper;
	
	@Autowired
	private MemberMapper memberMapper;
	
	@BeforeEach
	void setUp() throws Exception {
		
	}

    private Long createMember(String email, String nickname) {
        MemberVO m = new MemberVO();
        m.setEmail(email);
        m.setNickname(nickname);
        m.setRole("USER");
        memberMapper.insertMember(m);
        return m.getMemberId();
    }

    private MemberSocialAccountVO newSocial(Long memberId, String email, String providerUserId) {
        MemberSocialAccountVO a = new MemberSocialAccountVO();
        a.setMemberId(memberId);
        a.setProviderCode("GOOGLE");
        a.setProviderUserId(providerUserId);
        a.setProviderEmail(email);
        return a;
    }

    @Test
    @DisplayName("소셜 계정 등록 후 제공자+식별값으로 조회된다")
    void insertAndSelectByProvider() {
    	String email = "social@endit.com";
        Long memberId = createMember(email, "소셜1");

        MemberSocialAccountVO account = newSocial(memberId, email, "google-uid-001");
        int flag = socialMapper.insertSocialAccount(account);

        assertEquals(1, flag);
        assertNotNull(account.getMemberSocialAccountId());

        MemberSocialAccountVO outVO = socialMapper.selectByProvider("GOOGLE", "google-uid-001");
        assertNotNull(outVO);
        assertEquals(memberId, outVO.getMemberId());
        assertEquals("GOOGLE", outVO.getProviderCode());
        assertEquals(email, outVO.getProviderEmail());
    }

    @Test
    @DisplayName("존재하지 않는 제공자 식별값으로 조회하면 null")
    void selectByProvider_notFound() {
        assertNull(socialMapper.selectByProvider("GOOGLE", "no-such-uid"));
    }

    @Test
    @DisplayName("provider_code에 허용되지 않은 값을 넣으면 예외 (CHECK 제약)")
    void insert_invalidProvider_throws() {
        String email = "s3@endit.com";
        Long memberId = createMember(email, "소셜3");
        MemberSocialAccountVO account = newSocial(memberId, email, "google-uid-003");
        account.setProviderCode("FACEBOOK");

        assertThrows(Exception.class, () -> {
            socialMapper.insertSocialAccount(account);
        });
    }
	

}
