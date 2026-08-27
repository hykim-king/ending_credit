# 브랜치·담당 파트·커밋 메시지 규칙

## 문서 목적

팀 개발 과정에서 브랜치 역할, Pull Request 방향, 조별 담당 테이블 및 커밋 메시지 형식을 통일하기 위한 협업 규칙이다.

## 브랜치 구조

브랜치 이름은 현재 실제로 생성된 구조를 그대로 사용한다. 1조는 `group1/...`, 2~4조는 `feature/groupN/...` 형식이다.

```text
main
├─ group1/dev
│  ├─ group1/sinwoo
│  └─ group1/gijun
├─ feature/group2/dev
│  ├─ feature/group2/heetae
│  └─ feature/group2/jaeyoung
├─ feature/group3/dev
│  ├─ feature/group3/jinyoung
│  └─ feature/group3/gunwoo
└─ feature/group4/dev
   ├─ feature/group4/sunki
   └─ feature/group4/eunhoo
```

> 3조 장소은 팀원의 개인 브랜치 이름은 현재 공유된 목록에 없으므로, 확정 후 위 구조에 추가한다.

## 브랜치 역할과 Pull Request 방향

### `main`

전체 통합 개발 브랜치다. 각 조의 작업물이 최종적으로 합쳐지는 중심 브랜치다.

- 각 조장이 개인 작업을 조별 통합 브랜치에 취합한다.
- 조별 기능과 충돌 여부를 검증한 후 조장이 `main`을 대상으로 Pull Request를 생성한다.
- 최종 Pull Request 검토 및 병합은 전지용 담당자에게 요청한다.
- 개인 브랜치에서 `main`으로 직접 Pull Request를 생성하지 않는다.

### 조별 통합 브랜치

각 조의 통합 브랜치다.

- 1조: `group1/dev`
- 2~4조: `feature/groupN/dev`

- 해당 조 팀원들의 개인 작업물을 취합한다.
- 조 내부 충돌과 통합 동작을 먼저 확인한다.
- 조장이 검증을 마친 후 `main`으로 Pull Request를 생성한다.

### 개인 개발 브랜치

팀원별 개인 개발 브랜치다.

- 1조: `group1/이름`
- 2~4조: `feature/groupN/이름`

- 각 팀원은 담당 기능을 자신의 개인 브랜치에서 개발한다.
- 작업 완료 후 같은 조의 조별 통합 브랜치를 대상으로 Pull Request를 생성한다.
- 다른 팀원의 파일이나 담당 범위를 수정해야 한다면 먼저 해당 팀원 또는 조장과 협의한다.

## 기본 작업 흐름

```text
개인 브랜치
    ↓ Pull Request
조별 통합 브랜치
    ↓ 조장 취합·검증 후 Pull Request
main
    ↓ 최종 검토·병합
전체 통합
```

1. 작업 시작 전 개인 브랜치가 어느 조의 `dev`에서 분기되었는지 확인한다.
2. 담당 기능을 구현하고 관련 테스트를 수행한다.
3. 변경 목적에 맞는 커밋 메시지로 커밋한다.
4. 자신의 조 통합 브랜치(`group1/dev` 또는 `feature/groupN/dev`)를 대상으로 Pull Request를 생성한다.
5. 조장은 조별 브랜치에서 충돌, 컴파일, Mapper 계약 및 주요 기능을 검증한다.
6. 조별 검증이 끝나면 조장이 `main`을 대상으로 Pull Request를 생성한다.

팀원 코드를 취합할 때는 [팀원 코드 통합 체크리스트](team-code-integration-checklist.md)도 함께 확인한다.

## 조별 담당 파트

| 조 | 조원 | 조 전체 담당 DB 테이블 |
| --- | --- | --- |
| 1조 | 김신우(조장), 이기준 | `CONTENT`, `CONTENTS_IMAGE`, `GENRE`, `CONTENT_GENRE`, `PERSON`, `CONTENT_CREDIT` |
| 2조 | 김희태(조장), 장재영 | `MEMBER`, `MEMBER_SOCIAL_ACCOUNT`, `NOTICE` |
| 3조 | 이진영(조장), 김건우, 장소은 | `COLLECTION`, `COLLECTION_ITEM`, `COLLECTION_LIKE`, `MEMBER_CONTENT`, `PERSON_LIKE` |
| 4조 | 홍선기(조장), 강은후 | `USER_COMMENT`, `COMMENT_LIKE`, `REPORT` |

### 3조 개인 담당

| 담당자 | 담당 DB 테이블 |
| --- | --- |
| 이진영 | `COLLECTION`, `COLLECTION_ITEM`, `MEMBER_CONTENT`, `PERSON_LIKE` |
| 김건우 | `COLLECTION_LIKE` |
| 장소은 | 현재 공유된 개인 담당 테이블 없음 |

개인 담당이 명시되지 않은 조는 위 표를 조 전체 범위로만 사용한다. 개인별 세부 배정은 조에서 확정한 뒤 문서에 추가한다.

## 커밋 메시지 규칙

커밋 제목은 다음 형식을 사용한다.

```text
종류: 변경 내용을 설명하는 한글 제목
```

| 종류 | 사용 목적 | 예시 |
| --- | --- | --- |
| `feat` | 새로운 기능 추가 | `feat: 로그인 비밀번호 암호화 기능 추가` |
| `fix` | 버그 수정 | `fix: 영화 목록 페이징 처리 오류 수정` |
| `docs` | 문서 추가 또는 수정 | `docs: README.md 수정 및 ERD 반영` |
| `style` | UI, CSS 및 화면 스타일 작업 | `style: 로그인 페이지 CSS 레이아웃 조정` |
| `refactor` | 기능 동작을 유지하는 코드 구조 개선 | `refactor: 회원 검증 로직 중복 제거` |

### 작성 원칙

- 종류는 영문 소문자로 작성한다.
- 종류 뒤에 콜론과 공백 한 칸을 넣는다.
- 변경한 파일 이름만 나열하지 말고 무엇을 바꿨는지 작성한다.
- 하나의 커밋에는 가능한 한 하나의 목적만 담는다.
- 기능 추가와 무관한 대규모 정리를 같은 커밋에 섞지 않는다.
- 실제 변경 내용과 일치하는 종류를 선택한다.

예시:

```text
feat: 컬렉션 항목 등록 기능 추가
fix: 인물 좋아요 중복 등록 오류 수정
docs: 팀 브랜치 운영 규칙 추가
style: 컬렉션 상세 화면 간격 조정
refactor: 회원 콘텐츠 검색 조건 공통화
```

## Pull Request 확인 항목

- 대상 브랜치가 개인 브랜치에서 조별 `dev` 방향인지 확인한다.
- 조장의 통합 Pull Request는 조별 `dev`에서 `main` 방향인지 확인한다.
- 담당 범위 밖의 변경이 포함되었다면 이유를 설명한다.
- 관련 파일, Mapper XML, VO 및 테스트가 함께 반영되었는지 확인한다.
- 충돌 해결 과정에서 다른 팀원의 구현이 사라지지 않았는지 확인한다.
- 실행한 테스트와 실행하지 못한 테스트를 Pull Request에 기록한다.
- DB 스키마 또는 설정 변경이 있다면 별도로 명시한다.