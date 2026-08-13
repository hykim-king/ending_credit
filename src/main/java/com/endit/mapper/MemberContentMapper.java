package com.endit.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.endit.cmn.WorkDiv;
import com.endit.domain.MemberContentVO;

/**
 * <pre>
 * Class Name  : MemberContentMapper
 * Description : 회원별 콘텐츠 평가 및 보고 싶어요 정보의 등록, 조회, 수정 및 삭제를 처리하는 Mapper
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13.	jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 13.
 */
@Mapper
public interface MemberContentMapper extends WorkDiv<MemberContentVO> {

}
