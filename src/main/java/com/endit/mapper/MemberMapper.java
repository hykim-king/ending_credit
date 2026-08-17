package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.domain.MemberVO;

@Mapper
public interface MemberMapper {
	
	/** 회원 등록 (SEQ_MEMBER 증가 후 memberId 세팅) */
	int insertMember(MemberVO member);
	
    /** 회원번호로 조회 */
    MemberVO selectMemberById(Long memberId);

    /** 이메일로 조회 (자체 로그인 / 중복가입 체크) */
    MemberVO selectMemberByEmail(String email);

    /** 닉네임 중복 검사 (Count로 중복시 1반환) */
    int countByNickname(String nickname);

    /** 프로필 수정 (닉네임/소개/프로필이미지) */
    int updateProfile(MemberVO member);

    /** 비밀번호 변경 */
    int updatePassword(@Param("memberId") Long memberId,
                       @Param("password") String password);

    /** 회원 삭제 (하드 삭제) */
    int deleteMember(Long memberId);	
}
