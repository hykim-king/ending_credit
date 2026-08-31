/**
 * 공통코드 조회 Mapper (학원 27장 패턴 — 조회 전용이라 WorkDiv 미상속)
 */
package com.endit.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.endit.domain.CodeVO;

@Mapper // MyBatis 매퍼 인터페이스임을 선언(구현체는 MyBatis가 자동 생성)
public interface CodeMapper {

	/**
	 *
	 * <pre>
	 * Method Name : doRetrieve
	 * Description : 코드 그룹 여러 개를 IN절로 한 번에 조회
	 *
	 * </pre>
	 *
	 * @param code (key "code" = 코드 그룹 배열)
	 * @return List<CodeVO>
	 */
	List<CodeVO> doRetrieve(Map<String, Object> code);

}
