/**
 * <pre>
 * Class Name : CommentLikeServiceImpl
 * Description : 코멘트 좋아요 Service 구현체
 *               핵심은 upToggleLike — 눌렀는지 확인 후 등록/취소를 한 트랜잭션으로 조합.
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 18.  홍선기   최초 생성
 * </pre>
 *
 * @author 홍선기
 * @since 2026. 8. 18.
 */
package com.endit.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.endit.domain.CommentLikeVO;
import com.endit.mapper.CommentLikeMapper;
import com.endit.service.CommentLikeService;

@Service
public class CommentLikeServiceImpl implements CommentLikeService {

	final Logger log = LoggerFactory.getLogger(getClass());

	private final CommentLikeMapper commentLikeMapper;

	public CommentLikeServiceImpl(CommentLikeMapper commentLikeMapper) {
		super();
		this.commentLikeMapper = commentLikeMapper;
		log.debug("commentLikeMapper: {}", commentLikeMapper);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int doSave(CommentLikeVO param) {
		log.debug("=============================");
		log.debug("{}()", "doSave");
		log.debug("=============================");

		return commentLikeMapper.doSave(param);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int doDelete(CommentLikeVO param) {
		log.debug("=============================");
		log.debug("{}()", "doDelete");
		log.debug("=============================");

		return commentLikeMapper.doDelete(param);
	}

	@Override
	public int likeCheck(CommentLikeVO param) {
		log.debug("=============================");
		log.debug("{}()", "likeCheck");
		log.debug("=============================");

		return commentLikeMapper.likeCheck(param);
	}

	@Override
	public int getLikeCnt(long commentId) {
		log.debug("=============================");
		log.debug("{}()", "getLikeCnt");
		log.debug("=============================");

		return commentLikeMapper.getLikeCnt(commentId);
	}

	@Override
	public int totalCnt() {
		log.debug("=============================");
		log.debug("{}()", "totalCnt");
		log.debug("=============================");

		return commentLikeMapper.totalCnt();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int upToggleLike(CommentLikeVO param) {
		log.debug("=============================");
		log.debug("{}()", "upToggleLike");
		log.debug("=============================");

		// 1. 안 눌렀으면 등록
		if (0 == commentLikeMapper.likeCheck(param)) {
			int flag = commentLikeMapper.doSave(param);
			if (1 != flag) {
				throw new RuntimeException("좋아요 등록에 실패했습니다.");
			}
			return LIKE_ON;
		}

		// 2. 이미 눌렀으면 취소
		int flag = commentLikeMapper.doDelete(param);
		if (1 != flag) {
			throw new RuntimeException("좋아요 취소에 실패했습니다.");
		}
		return LIKE_OFF;
	}

}
