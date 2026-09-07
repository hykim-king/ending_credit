package com.endit.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.endit.domain.NoticeSearchVO;
import com.endit.domain.NoticeVO;
import com.endit.domain.PageResponse;
import com.endit.mapper.NoticeMapper;
import com.endit.service.NoticeService;

@Service
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private static final Set<String> ALLOWED_IMPORTANT =
            Set.of("Y", "N");

    private final NoticeMapper noticeMapper;

    public NoticeServiceImpl(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @Override
    public PageResponse<NoticeVO> getPublicNoticeList(
            NoticeSearchVO search
    ) {

        normalizePaging(search);

        List<NoticeVO> list =
                noticeMapper.selectPublicNoticeList(search);

        int totalCnt =
                noticeMapper.countPublicNoticeList();

        return new PageResponse<>(
                list,
                search.getPageNo(),
                search.getPageSize(),
                totalCnt
        );
    }

    @Override
    @Transactional
    public NoticeVO getPublicNotice(
            Long noticeId,
            boolean increaseViewCount
    ) {

        validateNoticeId(noticeId);

        if (increaseViewCount) {

            int flag =
                    noticeMapper.increasePublicViewCount(
                            noticeId
                    );

            if (flag != 1) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "게시된 공지사항을 찾을 수 없습니다."
                );
            }
        }

        NoticeVO notice =
                noticeMapper.selectPublicNoticeById(
                        noticeId
                );

        if (notice == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "게시된 공지사항을 찾을 수 없습니다."
            );
        }

        return notice;
    }

    @Override
    public PageResponse<NoticeVO> getAdminNoticeList(
            NoticeSearchVO search
    ) {

        normalizePaging(search);
        normalizeAdminSearch(search);

        List<NoticeVO> list =
                noticeMapper.selectAdminNoticeList(search);

        int totalCnt =
                noticeMapper.countAdminNoticeList(search);

        return new PageResponse<>(
                list,
                search.getPageNo(),
                search.getPageSize(),
                totalCnt
        );
    }

    @Override
    public NoticeVO getAdminNotice(Long noticeId) {

        validateNoticeId(noticeId);

        NoticeVO notice =
                noticeMapper.selectNoticeById(noticeId);

        if (notice == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "공지사항을 찾을 수 없습니다."
            );
        }

        return notice;
    }

    @Override
    @Transactional
    public Long createNotice(
            NoticeVO notice,
            Long adminId
    ) {

        validateAdminId(adminId);
        normalizeAndValidateNotice(notice);

        notice.setCreatedId(adminId);
        notice.setUpdatedId(adminId);

        int flag =
                noticeMapper.insertNotice(notice);

        if (flag != 1
                || notice.getNoticeId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "공지 등록에 실패했습니다."
            );
        }

        return notice.getNoticeId();
    }

    @Override
    @Transactional
    public void updateNotice(
            Long noticeId,
            NoticeVO notice,
            Long adminId
    ) {

        validateNoticeId(noticeId);
        validateAdminId(adminId);
        normalizeAndValidateNotice(notice);

        notice.setNoticeId(noticeId);
        notice.setUpdatedId(adminId);

        int flag =
                noticeMapper.updateNotice(notice);

        if (flag != 1) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "수정할 공지사항을 찾을 수 없습니다."
            );
        }
    }

    private void normalizePaging(
            NoticeSearchVO search
    ) {

        if (search == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "검색 조건이 없습니다."
            );
        }

        if (search.getPageNo() <= 0) {
            search.setPageNo(1);
        }

        if (search.getPageSize() <= 0) {
            search.setPageSize(10);

        } else if (search.getPageSize() > 100) {
            search.setPageSize(100);
        }
    }

    private void normalizeAdminSearch(
            NoticeSearchVO search
    ) {

        if (StringUtils.hasText(
                search.getSearchWord()
        )) {

            search.setSearchWord(
                    search.getSearchWord()
                            .trim()
            );

        } else {
            search.setSearchWord(null);
        }

        if (StringUtils.hasText(
                search.getImportant()
        )) {

            String important =
                    search.getImportant()
                            .trim()
                            .toUpperCase();

            if (!ALLOWED_IMPORTANT.contains(
                    important
            )) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "중요 여부는 Y 또는 N만 가능합니다."
                );
            }

            search.setImportant(important);

        } else {
            search.setImportant(null);
        }
    }
    private void normalizeAndValidateNotice(
            NoticeVO notice
    ) {

        if (notice == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "공지 데이터가 없습니다."
            );
        }

        String title =
                notice.getTitle() == null
                        ? ""
                        : notice.getTitle().trim();

        String content =
                notice.getContent() == null
                        ? ""
                        : notice.getContent().trim();

        if (!StringUtils.hasText(title)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "제목은 필수입니다."
            );
        }

        if (title.length() > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "제목은 200자 이하여야 합니다."
            );
        }

        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "본문은 필수입니다."
            );
        }

        String important =
                StringUtils.hasText(
                        notice.getImportant()
                )
                        ? notice.getImportant()
                                .trim()
                                .toUpperCase()
                        : "N";

        if (!ALLOWED_IMPORTANT.contains(
                important
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "중요 여부는 Y 또는 N만 가능합니다."
            );
        }

        notice.setTitle(title);
        notice.setContent(content);
        notice.setImportant(important);

        /*
         * 게시 상태 선택 기능을 사용하지 않는다.
         * 신규 등록과 수정 모두 항상 공개 상태로 저장한다.
         */
        notice.setStatus("PUBLISHED");
    }

    private void validateNoticeId(
            Long noticeId
    ) {

        if (noticeId == null
                || noticeId <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "공지 번호가 올바르지 않습니다."
            );
        }
    }

    private void validateAdminId(
            Long adminId
    ) {

        if (adminId == null
                || adminId <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "관리자 인증이 필요합니다."
            );
        }
    }

    @Override
    @Transactional
    public void deleteNotice(
            Long noticeId,
            Long adminId
    ) {

        validateNoticeId(noticeId);
        validateAdminId(adminId);

        int flag =
                noticeMapper.deleteNotice(noticeId);

        if (flag != 1) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "삭제할 공지사항을 찾을 수 없습니다."
            );
        }
    }
}