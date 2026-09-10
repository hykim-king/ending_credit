/**
 * 사이트 사용법 안내 원문
 *
 * AI 는 어느 안내인지(faqKey)만 고르고, 본문은 여기 적힌 것을 그대로 보여준다.
 * AI 가 안내 문장을 지어내면 틀린 길을 알려줄 수 있어서다.
 * 내용은 실제 화면 기준으로 적는다 - 화면이 바뀌면 여기도 같이 고친다.
 */
package com.endit.ai;

import java.util.Map;

import com.endit.ai.dto.AiHelpAnswer;

public class FaqAnswers {

	/** 인스턴스로 만들 필요 없는 static 전용이라 생성자를 막는다 */
	private FaqAnswers() {
	}

	private static final Map<String, AiHelpAnswer> ANSWERS = Map.ofEntries(
			Map.entry("withdraw", new AiHelpAnswer(
					"회원 탈퇴",
					"마이페이지의 설정에서 \"회원 탈퇴\"를 누르고, 확인을 위해 닉네임을 입력하면 탈퇴됩니다. "
							+ "탈퇴하면 작성한 코멘트·별점·컬렉션이 함께 삭제되며 복구할 수 없습니다.",
					"/members/me", "마이페이지로 가기")),
			Map.entry("signup", new AiHelpAnswer(
					"회원가입",
					"오른쪽 위 \"회원가입\" 버튼에서 이메일로 가입할 수 있습니다. "
							+ "구글 계정으로도 가입됩니다.",
					"/signup", "회원가입 하러 가기")),
			Map.entry("login", new AiHelpAnswer(
					"로그인",
					"이메일과 비밀번호로 로그인하거나, 구글 계정으로 로그인할 수 있습니다.",
					"/login", "로그인 하러 가기")),
			Map.entry("rating", new AiHelpAnswer(
					"별점 남기기",
					"영화 상세 화면에서 별을 눌러 0.5점 단위로 평가할 수 있습니다. 로그인이 필요합니다.",
					"/", "영화 보러 가기")),
			Map.entry("comment", new AiHelpAnswer(
					"코멘트 작성",
					"영화 상세 화면의 \"코멘트 남기기\"에서 작성합니다. 영화당 한 편씩 쓸 수 있고, "
							+ "로그인이 필요합니다.",
					"/", "영화 보러 가기")),
			Map.entry("spoiler", new AiHelpAnswer(
					"스포일러 표시",
					"코멘트 작성창의 \"스포일러가 포함되어 있어요\"를 체크하면 다른 사람에게는 "
							+ "내용이 가려진 채 보이고, 원할 때만 펼쳐 볼 수 있습니다.",
					null, null)),
			Map.entry("report", new AiHelpAnswer(
					"코멘트 신고",
					"코멘트의 신고 버튼을 누르고 사유(스포일러·부적절·기타)를 골라 접수합니다. "
							+ "접수된 신고는 관리자가 확인한 뒤 처리합니다.",
					null, null)),
			Map.entry("collection", new AiHelpAnswer(
					"컬렉션 만들기",
					"컬렉션 화면의 \"새 컬렉션\"에서 제목과 설명을 적고 영화를 담아 만듭니다. "
							+ "로그인이 필요합니다.",
					"/collections", "컬렉션 보러 가기")));

	/**
	 * 키에 맞는 안내를 돌려준다.
	 *
	 * @param faqKey 파이썬이 고른 키
	 * @return 안내 원문. 모르는 키면 null
	 */
	public static AiHelpAnswer find(String faqKey) {
		if (null == faqKey) {
			return null;
		}
		return ANSWERS.get(faqKey);
	}
}
