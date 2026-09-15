/**
 * 파이썬 /search-intent 에 보내는 요청 (수업 05 ChatRequestVO 형태)
 */
package com.endit.domain;

import com.endit.cmn.DTO;

public class SearchIntentRequestVO extends DTO {

	private String query;

	public SearchIntentRequestVO() {
		super();
	}

	public SearchIntentRequestVO(String query) {
		super();
		this.query = query;
	}

	public String getQuery() { return query; }
	public void setQuery(String query) { this.query = query; }

	@Override
	public String toString() {
		return "SearchIntentRequestVO [query=" + query + "]";
	}
}
