package com.endit.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.endit.domain.MemberVO;
import com.endit.mapper.MemberMapper;

/**
 *   - 사용자가 이메일+비밀번호로 로그인 버튼을 누르면
 *   - 시큐리티가 loadUserByUsername(입력한 이메일) 을 자동으로 호출한다.
 *   - 우리는 그 이메일로 DB에서 회원을 찾아 CustomUserDetails로 감싸 돌려준다.
 *   - 그 다음 비밀번호 비교/로그인 성공 처리는 시큐리티가 알아서 한다.
 *
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MemberMapper memberMapper;

	public CustomUserDetailsService(MemberMapper memberMapper) {
		this.memberMapper = memberMapper;
	}

	/**
	 * 시큐리티가 로그인 검증을 시작할 때 호출한다.
	 * @param email 사용자가 입력한 이메일 (시큐리티 규격상 파라미터명은 username)
	 * @return 찾은 회원을 감싼 CustomUserDetails
	 * @throws UsernameNotFoundException 해당 이메일 회원이 없거나, 로그인 불가한 계정일 때
	 */
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		log.debug("loadUserByUsername(email={})", email);

		// 1) 이메일로 회원 조회
		MemberVO member = memberMapper.selectMemberByEmail(email);

		// 2) 회원이 없으면 로그인 실패
		if (member == null) {
			// 보안상 "이메일이 없다/비번이 틀렸다"를 구분하지 않음.
			// 그래서 메시지는 일반적으로 두고, 실제 구분은 사용자에게 노출하지 않는다.
			throw new UsernameNotFoundException("로그인 정보가 올바르지 않습니다.");
		}

		// 3) 소셜 전용 회원(비밀번호 null)이 이메일 로그인 시도하는 경우 차단
		//    구글로만 가입한 회원은 PASSWORD가 null이라 이메일/비번 로그인을 할 수 없다.
		if (member.getPassword() == null || member.getPassword().isBlank()) {
			throw new UsernameNotFoundException("로그인 정보가 올바르지 않습니다.");
		}

		// 4) 찾은 회원을 시큐리티 규격(CustomUserDetails)으로 감싸서 반환
		//    이후 비밀번호 비교/성공 처리는 시큐리티가 담당한다.
		return new CustomUserDetails(member);
	}
}