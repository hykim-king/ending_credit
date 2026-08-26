package com.endit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.endit.domain.MemberVO;

import jakarta.servlet.http.HttpSession;

/**
 * 인증 파트가 아직 프로젝트에 들어오지 않은 상태라 Notice에서 사용할 최소 세션 어댑터.
 *
 * 다음 형태를 순서대로 지원한다.
 * 1) session["loginMember"] = MemberVO
 * 2) session["member"]      = MemberVO
 * 3) session["memberId"], session["role"]
 *
 * 실제 인증 팀의 세션 키가 확정되면 이 클래스만 수정하면 된다.
 */
public final class NoticeSessionSupport {

    private NoticeSessionSupport() {
    }

    public static Long requireAdminId(HttpSession session) {
        MemberVO member = getMemberVO(session, "loginMember");
        if (member == null) {
            member = getMemberVO(session, "member");
        }

        if (member != null) {
            if (!"ADMIN".equalsIgnoreCase(member.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
            }
            return member.getMemberId();
        }

        Object role = session.getAttribute("role");
        Object memberId = session.getAttribute("memberId");

        if (!"ADMIN".equalsIgnoreCase(String.valueOf(role)) || memberId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 로그인이 필요합니다.");
        }

        if (memberId instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(memberId));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 세션 정보가 올바르지 않습니다.");
        }
    }

    private static MemberVO getMemberVO(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        if (value instanceof MemberVO member) {
            return member;
        }
        return null;
    }
}
