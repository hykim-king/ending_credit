package com.endit.mapper;

import static org.junit.jupiter.api.Assertions.*; 

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import com.endit.domain.NoticeSearchVO;

import com.endit.domain.NoticeVO;

@SpringBootTest
@Transactional
@Disabled("공용 DB 전체 DELETE를 피하도록 테스트 데이터 격리 후 다시 활성화")
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
    @DisplayName("일반 사용자 공지 목록은 PUBLISHED 상태만 조회")
    void selectPublicNoticeList_onlyPublished() {

        // Given: 게시/임시저장/숨김 공지를 각각 1건 등록
        NoticeVO published = newNotice();
        published.setTitle("게시 공지");
        published.setStatus("PUBLISHED");

        NoticeVO draft = newNotice();
        draft.setTitle("임시저장 공지");
        draft.setStatus("DRAFT");

        NoticeVO hidden = newNotice();
        hidden.setTitle("숨김 공지");
        hidden.setStatus("HIDDEN");

        assertEquals(1, noticeMapper.insertNotice(published));
        assertEquals(1, noticeMapper.insertNotice(draft));
        assertEquals(1, noticeMapper.insertNotice(hidden));

        // 검색 조건
        NoticeSearchVO search = new NoticeSearchVO();
        search.setPageNo(1);
        search.setPageSize(10);

        // When: 일반 사용자 공지 목록 조회
        List<NoticeVO> list =
                noticeMapper.selectPublicNoticeList(search);

        // Then: PUBLISHED 공지만 조회되어야 함
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("게시 공지", list.get(0).getTitle());
        assertEquals("PUBLISHED", list.get(0).getStatus());

        // 공개 공지 개수도 1건이어야 함
        assertEquals(1, noticeMapper.countPublicNoticeList());
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
    @DisplayName("status에 허용되지 않은 값을 넣으면 예외 발생 (CHECK 제약)")
    void insert_invalidStatus_throws() {

        // Given: 허용되지 않은 상태값 설정
        NoticeVO notice = newNotice();
        notice.setStatus("UNKNOWN");

        // When & Then: DB CHECK 제약조건에 의해 예외 발생
        assertThrows(DataIntegrityViolationException.class, () -> {
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
    
    @Test
    @DisplayName("일반 사용자 공지 목록은 중요 공지가 먼저 조회")
    void selectPublicNoticeList_importantFirst() {

        // Given: 중요 공지를 먼저 등록
        NoticeVO importantNotice = newNotice();
        importantNotice.setTitle("중요 공지");
        importantNotice.setImportant("Y");
        importantNotice.setStatus("PUBLISHED");

        assertEquals(1, noticeMapper.insertNotice(importantNotice));

        // 일반 공지를 나중에 등록
        NoticeVO normalNotice = newNotice();
        normalNotice.setTitle("일반 공지");
        normalNotice.setImportant("N");
        normalNotice.setStatus("PUBLISHED");

        assertEquals(1, noticeMapper.insertNotice(normalNotice));

        NoticeSearchVO search = new NoticeSearchVO();
        search.setPageNo(1);
        search.setPageSize(10);

        // When
        List<NoticeVO> list =
                noticeMapper.selectPublicNoticeList(search);

        // Then
        assertNotNull(list);
        assertEquals(2, list.size());

        assertEquals("중요 공지", list.get(0).getTitle());
        assertEquals("Y", list.get(0).getImportant());

        assertEquals("일반 공지", list.get(1).getTitle());
        assertEquals("N", list.get(1).getImportant());
    }
    @Test
    @DisplayName("일반 사용자 공지 목록 페이징")
    void selectPublicNoticeList_paging() {

        // Given: 공개 공지 3건 등록
        NoticeVO notice01 = newNotice();
        notice01.setTitle("공지 1");

        NoticeVO notice02 = newNotice();
        notice02.setTitle("공지 2");

        NoticeVO notice03 = newNotice();
        notice03.setTitle("공지 3");

        assertEquals(1, noticeMapper.insertNotice(notice01));
        assertEquals(1, noticeMapper.insertNotice(notice02));
        assertEquals(1, noticeMapper.insertNotice(notice03));

        // When: 한 페이지에 2건씩 조회
        NoticeSearchVO page1Search = new NoticeSearchVO();
        page1Search.setPageNo(1);
        page1Search.setPageSize(2);

        List<NoticeVO> page1 =
                noticeMapper.selectPublicNoticeList(page1Search);

        NoticeSearchVO page2Search = new NoticeSearchVO();
        page2Search.setPageNo(2);
        page2Search.setPageSize(2);

        List<NoticeVO> page2 =
                noticeMapper.selectPublicNoticeList(page2Search);

        // Then
        assertEquals(2, page1.size());
        assertEquals(1, page2.size());

        // 같은 중요도에서는 최신 공지가 먼저 조회
        assertEquals(notice03.getNoticeId(), page1.get(0).getNoticeId());
        assertEquals(notice02.getNoticeId(), page1.get(1).getNoticeId());
        assertEquals(notice01.getNoticeId(), page2.get(0).getNoticeId());

        // 공개 공지 전체 건수는 3건
        assertEquals(3, noticeMapper.countPublicNoticeList());
    }
    @Test
    @DisplayName("관리자 공지 목록 상태 필터")
    void selectAdminNoticeList_statusFilter() {

        // Given: 상태가 서로 다른 공지 3건 등록
        NoticeVO published = newNotice();
        published.setTitle("게시 공지");
        published.setStatus("PUBLISHED");

        NoticeVO draft = newNotice();
        draft.setTitle("임시저장 공지");
        draft.setStatus("DRAFT");

        NoticeVO hidden = newNotice();
        hidden.setTitle("숨김 공지");
        hidden.setStatus("HIDDEN");

        assertEquals(1, noticeMapper.insertNotice(published));
        assertEquals(1, noticeMapper.insertNotice(draft));
        assertEquals(1, noticeMapper.insertNotice(hidden));

        // When: HIDDEN 상태만 조회
        NoticeSearchVO search = new NoticeSearchVO();
        search.setPageNo(1);
        search.setPageSize(10);
        search.setStatus("HIDDEN");

        List<NoticeVO> list =
                noticeMapper.selectAdminNoticeList(search);

        // Then: HIDDEN 공지만 1건 조회
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("숨김 공지", list.get(0).getTitle());
        assertEquals("HIDDEN", list.get(0).getStatus());

        // 필터 조건에 맞는 전체 개수도 1건
        assertEquals(1, noticeMapper.countAdminNoticeList(search));
    }
    @Test
    @DisplayName("관리자 공지 목록 중요 공지 필터")
    void selectAdminNoticeList_importantFilter() {

        // Given: 중요 공지 2건, 일반 공지 1건 등록
        NoticeVO important01 = newNotice();
        important01.setTitle("중요 공지 1");
        important01.setImportant("Y");

        NoticeVO important02 = newNotice();
        important02.setTitle("중요 공지 2");
        important02.setImportant("Y");

        NoticeVO normal = newNotice();
        normal.setTitle("일반 공지");
        normal.setImportant("N");

        assertEquals(1, noticeMapper.insertNotice(important01));
        assertEquals(1, noticeMapper.insertNotice(important02));
        assertEquals(1, noticeMapper.insertNotice(normal));

        // When: 중요 공지만 조회
        NoticeSearchVO search = new NoticeSearchVO();
        search.setPageNo(1);
        search.setPageSize(10);
        search.setImportant("Y");

        List<NoticeVO> list =
                noticeMapper.selectAdminNoticeList(search);

        // Then: 중요 공지 2건만 조회
        assertNotNull(list);
        assertEquals(2, list.size());

        assertEquals("Y", list.get(0).getImportant());
        assertEquals("Y", list.get(1).getImportant());

        // 필터 조건에 맞는 전체 개수도 2건
        assertEquals(2, noticeMapper.countAdminNoticeList(search));
    }
    @Test
    @DisplayName("관리자 공지 목록 제목 검색")
    void selectAdminNoticeList_searchWord() {

        // Given: 제목이 서로 다른 공지 3건 등록
        NoticeVO notice01 = newNotice();
        notice01.setTitle("서버 점검 안내");

        NoticeVO notice02 = newNotice();
        notice02.setTitle("정기 점검 공지");

        NoticeVO notice03 = newNotice();
        notice03.setTitle("이벤트 안내");

        assertEquals(1, noticeMapper.insertNotice(notice01));
        assertEquals(1, noticeMapper.insertNotice(notice02));
        assertEquals(1, noticeMapper.insertNotice(notice03));

        // When: 제목에 '점검'이 포함된 공지만 조회
        NoticeSearchVO search = new NoticeSearchVO();
        search.setPageNo(1);
        search.setPageSize(10);
        search.setSearchWord("점검");

        List<NoticeVO> list =
                noticeMapper.selectAdminNoticeList(search);

        // Then: '점검'이 포함된 공지 2건만 조회
        assertNotNull(list);
        assertEquals(2, list.size());

        assertTrue(list.get(0).getTitle().contains("점검"));
        assertTrue(list.get(1).getTitle().contains("점검"));

        // 검색 조건에 맞는 전체 개수도 2건
        assertEquals(2, noticeMapper.countAdminNoticeList(search));
    }
    @Test
    @DisplayName("관리자 공지 목록 페이징")
    void selectAdminNoticeList_paging() {

        // Given: 공지 3건 등록
        NoticeVO notice01 = newNotice();
        notice01.setTitle("관리자 공지 1");

        NoticeVO notice02 = newNotice();
        notice02.setTitle("관리자 공지 2");

        NoticeVO notice03 = newNotice();
        notice03.setTitle("관리자 공지 3");

        assertEquals(1, noticeMapper.insertNotice(notice01));
        assertEquals(1, noticeMapper.insertNotice(notice02));
        assertEquals(1, noticeMapper.insertNotice(notice03));

        // When: 한 페이지당 2건씩 조회
        NoticeSearchVO page1Search = new NoticeSearchVO();
        page1Search.setPageNo(1);
        page1Search.setPageSize(2);

        List<NoticeVO> page1 =
                noticeMapper.selectAdminNoticeList(page1Search);

        NoticeSearchVO page2Search = new NoticeSearchVO();
        page2Search.setPageNo(2);
        page2Search.setPageSize(2);

        List<NoticeVO> page2 =
                noticeMapper.selectAdminNoticeList(page2Search);

        // Then: 1페이지는 2건, 2페이지는 1건
        assertEquals(2, page1.size());
        assertEquals(1, page2.size());

        // 같은 중요도에서는 최신 공지가 먼저 조회
        assertEquals(notice03.getNoticeId(), page1.get(0).getNoticeId());
        assertEquals(notice02.getNoticeId(), page1.get(1).getNoticeId());
        assertEquals(notice01.getNoticeId(), page2.get(0).getNoticeId());

        // 관리자 전체 공지 개수는 3건
        assertEquals(3, noticeMapper.countAdminNoticeList(page1Search));
    }
    
    @Test
    @DisplayName("공개 공지 조회수 1 증가")
    void increasePublicViewCount() {

        // Given: 공개 상태 공지 등록
        NoticeVO notice = newNotice();
        notice.setStatus("PUBLISHED");

        assertEquals(1, noticeMapper.insertNotice(notice));

        // When: 공개 공지 조회수 증가
        int result =
                noticeMapper.increasePublicViewCount(notice.getNoticeId());

        // Then
        assertEquals(1, result);

        NoticeVO outVO =
                noticeMapper.selectPublicNoticeById(notice.getNoticeId());

        assertNotNull(outVO);
        assertEquals(1, outVO.getViewCount());
    }
}
