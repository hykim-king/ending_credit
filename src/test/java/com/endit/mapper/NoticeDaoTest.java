package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.NoticeVO;

@SpringBootTest
@Transactional
class NoticeDaoTest {

    /** 작성자/수정자로 사용할 관리자 회원 ID (admin1) */
    private static final Long WRITER_ID = 9L;

    @Autowired
    private NoticeMapper noticeMapper;

    /** 테스트용 공지 생성 (등록 시 게시 상태) */
    private NoticeVO newNotice() {
        NoticeVO n = new NoticeVO();
        n.setTitle("공지 제목");
        n.setContent("공지 본문 내용");
        n.setImportant("N");
        n.setStatus("PUBLISHED");
        n.setCreatedId(WRITER_ID);
        n.setUpdatedId(WRITER_ID);
        return n;
    }

    @Test
    @DisplayName("공지 등록 후 조회")
    void insertAndSelectById() {
        NoticeVO notice = newNotice();
        int flag = noticeMapper.insertNotice(notice);

        assertEquals(1, flag);
        assertNotNull(notice.getNoticeId());

        NoticeVO outVO = noticeMapper.selectNoticeById(notice.getNoticeId());
        assertNotNull(outVO);
        assertEquals("공지 제목", outVO.getTitle());
        assertEquals("PUBLISHED", outVO.getStatus());
        assertEquals(0, outVO.getViewCount());
    }

    @Test
    @DisplayName("공지 목록 조회")
    void selectNoticeList() {
        noticeMapper.insertNotice(newNotice());

        List<NoticeVO> list = noticeMapper.selectNoticeList();
        assertNotNull(list);
        assertTrue(list.size() >= 1);
    }

    @Test
    @DisplayName("공지 수정 후 조회")
    void updateNotice() {
        NoticeVO notice = newNotice();
        noticeMapper.insertNotice(notice);

        notice.setTitle("수정된 제목");
        notice.setContent("수정된 본문");
        int flag = noticeMapper.updateNotice(notice);

        assertEquals(1, flag);
        NoticeVO outVO = noticeMapper.selectNoticeById(notice.getNoticeId());
        assertEquals("수정된 제목", outVO.getTitle());
        assertEquals("수정된 본문", outVO.getContent());
        assertNotNull(outVO.getUpdatedDt());
    }

    @Test
    @DisplayName("조회수 1 증가")
    void increaseViewCount() {
        NoticeVO notice = newNotice();
        noticeMapper.insertNotice(notice);

        noticeMapper.increaseViewCount(notice.getNoticeId());

        NoticeVO outVO = noticeMapper.selectNoticeById(notice.getNoticeId());
        assertEquals(1, outVO.getViewCount());
    }

    @Test
    @DisplayName("status에 허용되지 않은 값을 넣으면 예외 (CHECK 제약)")
    void insert_invalidStatus_throws() {
        NoticeVO notice = newNotice();
        notice.setStatus("UNKNOWN");

        assertThrows(Exception.class, () -> {
            noticeMapper.insertNotice(notice);
        });
    }

    @Test
    @DisplayName("공지 삭제 후 조회")
    void deleteNotice() {
        NoticeVO notice = newNotice();
        noticeMapper.insertNotice(notice);
        Long id = notice.getNoticeId();

        int flag = noticeMapper.deleteNotice(id);

        assertEquals(1, flag);
        assertNull(noticeMapper.selectNoticeById(id));
    }
}