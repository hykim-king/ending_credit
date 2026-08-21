package com.endit.cmn;
import java.util.List;

public interface WorkDiv<T> {

	/**
	 * 목록조회
	 * @param param
	 * @return List<T>
	 */
	List<T> doRetrieve(DTO param);
	
	/**
	 * 수정
	 * 
	 * @param param
	 * @return 1(성공)/0(실패)
	 */
	int doUpdate(T param);

	/**
	 * 단건 삭제
	 * 
	 * @param param
	 * @return 1(성공)/0(실패)
	 */
	int doDelete(T param);

	/**
	 * 등록
	 * 
	 * @param param
	 * @return 1(성공)/0(실패)
	 */
	int doSave(T param);

	/**
	 * 단건조회
	 * 
	 * @param param
	 * @return T(성공)/null(실패)
	 */
	T doSelectOne(T param) ;

}
