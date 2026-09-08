package com.endit.domain;

import com.endit.cmn.DTO;

/**
 * Class Name  : GenrePreferenceVO
 * Description : 회원 선호 장르 분석 결과 VO.
 *              회원이 평가한 영화들의 장르별 집계(장르명 + 평가 개수)를 담는다.
 *              마이페이지 선호 장르 표시에 사용.
 */
public class GenrePreferenceVO extends DTO {

    private int    genreId;          // 장르 번호 (GENRE.GENRE_ID)
    private String genreName;        // 장르 이름 (GENRE.NAME, 예: "공포")
    private int    ratedCnt;         // 이 장르로 평가한 영화 개수

    public GenrePreferenceVO() {
        super();
    }

    public int getGenreId() {
        return genreId;
    }

    public void setGenreId(int genreId) {
        this.genreId = genreId;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public int getRatedCnt() {
        return ratedCnt;
    }

    public void setRatedCnt(int ratedCnt) {
        this.ratedCnt = ratedCnt;
    }

	@Override
	public String toString() {
		return "GenrePreferenceVO [genreId=" + genreId + ", genreName=" + genreName + ", ratedCnt=" + ratedCnt + "]";
	}

    
}