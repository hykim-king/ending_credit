package com.endit.domain;
 
import com.endit.cmn.DTO;
 
/**
 * Class Name  : RatingDistributionVO
 * Description : 회원 별점 분포 집계 결과 VO (점수 + 해당 점수 개수)
 */
public class RatingDistributionVO extends DTO {
 
    private int ratingScore;   // 별점 (1~5)
    private int scoreCnt;      // 그 별점을 준 개수
 
    public RatingDistributionVO() {
        super();
    }
 
    public int getRatingScore() {
        return ratingScore;
    }
    public void setRatingScore(int ratingScore) {
        this.ratingScore = ratingScore;
    }
    public int getScoreCnt() {
        return scoreCnt;
    }
    public void setScoreCnt(int scoreCnt) {
        this.scoreCnt = scoreCnt;
    }
 
    @Override
    public String toString() {
        return "RatingDistributionVO [ratingScore=" + ratingScore + ", scoreCnt=" + scoreCnt + "]";
    }
}