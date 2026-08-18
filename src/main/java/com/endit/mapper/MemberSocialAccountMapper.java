package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.endit.domain.MemberSocialAccountVO;

@Mapper
public interface MemberSocialAccountMapper {

    int insertSocialAccount(MemberSocialAccountVO account);

    // 로그인 핵심: 제공자+제공자식별값으로 조회
    MemberSocialAccountVO selectByProvider(@Param("providerCode") String providerCode,
                                           @Param("providerUserId") String providerUserId);
}