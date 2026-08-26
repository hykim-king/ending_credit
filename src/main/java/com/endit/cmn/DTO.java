package com.endit.cmn;
import java.util.HashMap;
import java.util.Map;

public class DTO {
	private int pageNo; 	// 페이지번호
	private int pageSize;	// 페이지사이즈(10>20>30>50>100)
	private int no; 		// 글번호
	private int totalCnt;	// 총글수

	private String searchDiv; 	// 검색구분
	private String searchWord;	// 검색어

	private Map<String, String> searchMap = new HashMap<String, String>();

	public DTO() {
		super();
	}

	/** Paging */
	private int startNo;	// 시작 페이지 번호(1,11,21,...)
	private int endNo;		// 종료 페이지 번호(10,20,30,...)

	private boolean pre; 	// 이전 페이지 존재 유무
	private boolean next;	// 이후 페이지 존재 유무

	public DTO(int pageNo, int pageSize, int totalCnt) {
		super();

		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.totalCnt = totalCnt;

		recalculatePagination();
	}

	private void recalculatePagination() {
		if (pageNo <= 0 || pageSize <= 0 || totalCnt <= 0) {
			startNo = 0;
			endNo = 0;
			pre = false;
			next = false;
			return;
		}

		int blockSize = 10; // 페이지 묶음

		// Math.ceil(4.2) -> 5
		// Math.ceil(1/10.0) -> 1 *blockSize
		endNo = (int) Math.ceil(pageNo / (double) blockSize) * blockSize;
		startNo = endNo - blockSize + 1;

		// 마지막 페이지 번호
		int realEnd = (int) Math.ceil(totalCnt / (double) pageSize);

		if (endNo > realEnd) {
			endNo = realEnd;
		}

		// 이전 페이지 존재 유무
		pre = startNo > 1;

		// 이후 페이지 존재 유무
		next = endNo < realEnd;
	}

	public int getStartNo() {
		return startNo;
	}

	public void setStartNo(int startNo) {
		this.startNo = startNo;
	}

	public int getEndNo() {
		return endNo;
	}

	public void setEndNo(int endNo) {
		this.endNo = endNo;
	}

	public boolean isPre() {
		return pre;
	}

	public void setPre(boolean pre) {
		this.pre = pre;
	}

	public boolean isNext() {
		return next;
	}

	public void setNext(boolean next) {
		this.next = next;
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
		recalculatePagination();
	}

	public String getSearchDiv() {
		return searchDiv;
	}

	public void setSearchDiv(String searchDiv) {
		this.searchDiv = searchDiv;
	}

	public String getSearchWord() {
		return searchWord;
	}

	public void setSearchWord(String searchWord) {
		this.searchWord = searchWord;
	}

	/**
	 * 숫자 컬럼 검색에 사용할 값을 반환한다.
	 * 숫자가 아닌 검색어는 null을 반환해 Oracle 암묵적 형변환 오류를 방지한다.
	 *
	 * @return 숫자로 변환된 검색어 또는 null
	 */
	public Long getSearchNumber() {
		if (searchWord == null || searchWord.isBlank()) {
			return null;
		}

		try {
			return Long.valueOf(searchWord.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public Map<String, String> getSearchMap() {
		return searchMap;
	}

	public void setSearchMap(Map<String, String> searchMap) {
		this.searchMap = searchMap;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
		recalculatePagination();
	}

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public int getTotalCnt() {
		return totalCnt;
	}

	public void setTotalCnt(int totalCnt) {
		this.totalCnt = totalCnt;
		recalculatePagination();
	}

	@Override
	public String toString() {
		return "DTO [pageNo=" + pageNo + ", pageSize=" + pageSize + ", no=" + no + ", totalCnt=" + totalCnt
				+ ", searchDiv=" + searchDiv + ", searchWord=" + searchWord + ", searchMap=" + searchMap
				+ ", startNo=" + startNo + ", endNo=" + endNo + ", pre=" + pre + ", next=" + next + "]";
	}

}
