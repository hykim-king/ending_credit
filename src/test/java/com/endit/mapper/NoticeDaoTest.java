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
    @DisplayName("공지 목록 페이징 - 페이지 크기만큼 조회된다")
    void selectNoticeList_paging() {
        // given: 공지 3개 등록
        for (int i = 0; i < 3; i++) {
            noticeMapper.insertNotice(newNotice());
        }

        // when: 1페이지, 2개씩 조회
        List<NoticeVO> list = noticeMapper.selectNoticeList(1, 2);

        // then: 최대 2개
        assertNotNull(list);
        assertTrue(list.size() <= 2);
    }

    @Test
    @DisplayName("공지 전체 개수를 조회할 수 있다")
    void countNoticeList() {
        // given: 등록 전 개수
        int before = noticeMapper.countNoticeList();

        // 공지 1개 등록
        noticeMapper.insertNotice(newNotice());

        // then: 1 늘어남
        int after = noticeMapper.countNoticeList();
        assertEquals(before + 1, after);
    }

    @Test
    @DisplayName("2페이지 조회 시 1페이지와 다른 데이터가 나온다")
    void selectNoticeList_secondPage() {
        // given: 공지 4개 등록
        for (int i = 0; i < 4; i++) {
            noticeMapper.insertNotice(newNotice());
        }

        // when: 1페이지 2개, 2페이지 2개
        List<NoticeVO> page1 = noticeMapper.selectNoticeList(1, 2);
        List<NoticeVO> page2 = noticeMapper.selectNoticeList(2, 2);

        // then: 두 페이지의 첫 공지 번호가 다름
        assertNotNull(page1);
        assertNotNull(page2);
        if (!page1.isEmpty() && !page2.isEmpty()) {
            assertNotEquals(page1.get(0).getNoticeId(), page2.get(0).getNoticeId());
        }
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

        int page = 1;
        int size = 10;

        List<NoticeVO> list = noticeMapper.selectNoticeList(page, size);

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