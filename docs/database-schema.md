# ENDIT 데이터베이스 스키마 및 마이그레이션 가이드

## 1. 기준과 범위

이 문서는 다음 Oracle DDL을 기준으로 작성했다.

- `1.ENDIT_CREATE(테이블&시퀀스생성).txt`: 시퀀스 11개와 업무 테이블 17개
- `3.ENDIT_COMMIT_TABLE(공통코드_관련_테이블_생성).txt`: 공통코드 테이블 2개
- 기준 문서: `EndingCredit_테이블정의서_0814.xlsx`

2026-08-19에 추출한 `ENDITPCWK` 및 `ENDIT_TEST` Export와 위 생성 DDL을 비교한 기존 검토 결과, 테이블·컬럼·PK·FK·CHECK·논리 인덱스 구조가 일치했다. 이 문서는 추정된 Mapper 구조가 아니라 해당 DDL의 실제 자료형, 기본값 및 제약조건을 기록한다.

전체 객체 구성은 다음과 같다.

| 구분 | 수량 |
| --- | ---: |
| 업무 테이블 | 17 |
| 공통코드 테이블 | 2 |
| 전체 컬럼 | 111 |
| 시퀀스 | 11 |
| 기본 키 | 19 |
| 외래 키 | 26 |
| CHECK 제약조건 | 17 |
| UNIQUE 제약조건 | 5 |
| 별도 UNIQUE 인덱스 | 4 |
| 별도 일반 인덱스 | 25 |

## 2. 시퀀스

모든 시퀀스의 생성 옵션은 `START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE`이다.

| 시퀀스 | 테이블 | 대상 컬럼 |
| --- | --- | --- |
| `SEQ_MEMBER` | `MEMBER` | `MEMBER_ID` |
| `SEQ_MEMBER_SOCIAL_ACCOUNT` | `MEMBER_SOCIAL_ACCOUNT` | `MEMBER_SOCIAL_ACCOUNT_ID` |
| `SEQ_GENRE` | `GENRE` | `GENRE_ID` |
| `SEQ_CONTENT` | `CONTENT` | `CONTENT_ID` |
| `SEQ_CONTENT_IMAGE` | `CONTENT_IMAGE` | `IMAGE_ID` |
| `SEQ_PERSON` | `PERSON` | `PERSON_ID` |
| `SEQ_CONTENT_CREDIT` | `CONTENT_CREDIT` | `CREDIT_ID` |
| `SEQ_COLLECTION` | `COLLECTION` | `COLLECTION_ID` |
| `SEQ_USER_COMMENT` | `USER_COMMENT` | `COMMENT_ID` |
| `SEQ_REPORT` | `REPORT_COMMENT` | `REPORT_ID` |
| `SEQ_NOTICE` | `NOTICE` | `NOTICE_ID` |

## 3. 테이블과 컬럼

`PK` 컬럼은 DDL에 `NOT NULL`이 생략돼 있어도 Oracle 기본 키 제약조건에 의해 null이 허용되지 않는다.

### 3.1 `MEMBER`

이메일·소셜 회원의 계정, 공개 프로필과 권한을 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `MEMBER_ID` | `NUMBER` | 불가 | - | PK |
| `EMAIL` | `VARCHAR2(255 BYTE)` | 불가 | - | UNIQUE 인덱스 |
| `PASSWORD` | `VARCHAR2(255 BYTE)` | 허용 | - | 소셜 전용 회원은 null 가능 |
| `NICKNAME` | `NVARCHAR2(30)` | 불가 | - | UNIQUE 인덱스 |
| `INTRODUCTION` | `NVARCHAR2(300)` | 허용 | - | - |
| `PROFILE_IMG_URL` | `VARCHAR2(1000 BYTE)` | 허용 | - | - |
| `ROLE` | `VARCHAR2(20 BYTE)` | 불가 | `'USER'` | `USER`, `ADMIN` |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |
| `UPDATED_DT` | `DATE` | 허용 | - | - |

### 3.2 `GENRE`

콘텐츠 장르 기준 정보를 관리한다.

| 컬럼 | 자료형 | Null | 키/비고 |
| --- | --- | --- | --- |
| `GENRE_ID` | `NUMBER` | 불가 | PK |
| `EXTERNAL_GENRE_ID` | `VARCHAR2(50 BYTE)` | 불가 | UNIQUE |
| `NAME` | `NVARCHAR2(50)` | 불가 | UNIQUE |

### 3.3 `CONTENT`

영화 콘텐츠 기본 정보, 외부 데이터 식별값과 이미지 URL을 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `CONTENT_ID` | `NUMBER` | 불가 | - | PK |
| `EXTERNAL_ID` | `VARCHAR2(50 BYTE)` | 불가 | - | - |
| `TITLE_KO` | `NVARCHAR2(200)` | 허용 | - | - |
| `TITLE_ORG` | `NVARCHAR2(200)` | 불가 | - | - |
| `OVERVIEW` | `CLOB` | 허용 | - | - |
| `RELEASE_YEAR` | `DATE` | 허용 | - | - |
| `RUNTIME_MIN` | `NUMBER(4,0)` | 허용 | - | - |
| `COUNTRY` | `NVARCHAR2(100)` | 허용 | - | - |
| `POSTER_URL` | `VARCHAR2(1000 BYTE)` | 허용 | - | - |
| `BACKDROP_URL` | `VARCHAR2(1000 BYTE)` | 허용 | - | - |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | `UPDATED_DT` 없음 |

### 3.4 `PERSON`

배우·감독 등 콘텐츠 참여 인물의 기본 정보를 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `PERSON_ID` | `NUMBER` | 불가 | - | PK |
| `EXTERNAL_ID` | `VARCHAR2(50 BYTE)` | 불가 | - | UNIQUE |
| `NAME_KO` | `NVARCHAR2(100)` | 허용 | - | - |
| `NAME_ORG` | `NVARCHAR2(100)` | 불가 | - | - |
| `PROFILE_IMAGE_URL` | `VARCHAR2(1000 BYTE)` | 허용 | - | - |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |
| `UPDATED_DT` | `DATE` | 허용 | - | - |

### 3.5 `MEMBER_SOCIAL_ACCOUNT`

회원과 소셜 로그인 계정의 연결 정보를 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `MEMBER_SOCIAL_ACCOUNT_ID` | `NUMBER` | 불가 | - | PK |
| `MEMBER_ID` | `NUMBER` | 불가 | - | FK → `MEMBER` |
| `PROVIDER_CODE` | `VARCHAR2(20 BYTE)` | 불가 | - | `GOOGLE`, `KAKAO`, `NAVER` |
| `PROVIDER_USER_ID` | `VARCHAR2(255 BYTE)` | 불가 | - | 제공자 내 회원 식별값 |
| `PROVIDER_EMAIL` | `VARCHAR2(255 BYTE)` | 허용 | - | - |
| `CONNECTED_DT` | `DATE` | 불가 | `SYSDATE` | - |

UNIQUE 조합은 `(PROVIDER_CODE, PROVIDER_USER_ID)`와 `(MEMBER_ID, PROVIDER_CODE)`다.

### 3.6 `CONTENT_GENRE`

콘텐츠와 장르의 다대다 관계를 관리한다.

| 컬럼 | 자료형 | Null | 키/비고 |
| --- | --- | --- | --- |
| `CONTENT_ID` | `NUMBER` | 불가 | 복합 PK, FK → `CONTENT` |
| `GENRE_ID` | `NUMBER` | 불가 | 복합 PK, FK → `GENRE` |

### 3.7 `CONTENT_IMAGE`

영화별 다중 이미지 URL을 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `IMAGE_ID` | `NUMBER` | 불가 | - | PK |
| `CONTENT_ID` | `NUMBER` | 불가 | - | FK → `CONTENT` |
| `IMAGE_URL` | `VARCHAR2(1000 BYTE)` | 불가 | - | - |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |

### 3.8 `CONTENT_CREDIT`

콘텐츠 참여 인물과 역할 정보를 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `CREDIT_ID` | `NUMBER` | 불가 | - | PK |
| `CONTENT_ID` | `NUMBER` | 불가 | - | FK → `CONTENT` |
| `PERSON_ID` | `NUMBER` | 불가 | - | FK → `PERSON` |
| `ROLE` | `VARCHAR2(20 BYTE)` | 불가 | - | `ACTOR`, `DIRECTOR`, `WRITER`, `PRODUCER` |
| `CHARACTER` | `NVARCHAR2(200)` | 허용 | - | 배역명 |
| `DISPLAY_ORDER` | `NUMBER(5,0)` | 불가 | `0` | 0 이상 |

### 3.9 `MEMBER_CONTENT`

회원별 콘텐츠 별점과 보고싶어요 상태를 한 행으로 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `MEMBER_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `MEMBER` |
| `CONTENT_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `CONTENT` |
| `RATING_SCORE` | `NUMBER(1,0)` | 허용 | - | null 또는 1~5 |
| `WATCHLIST` | `CHAR(1 BYTE)` | 불가 | `'N'` | `Y`, `N` |
| `RATED_DT` | `DATE` | 허용 | - | - |
| `WATCHLIST_DT` | `DATE` | 허용 | - | - |
| `UPDATED_DT` | `DATE` | 불가 | `SYSDATE` | - |

`RATING_SCORE`가 null이면 `WATCHLIST`가 반드시 `Y`여야 하므로 빈 활동 행은 저장할 수 없다.

### 3.10 `PERSON_LIKE`

회원이 좋아요를 누른 인물 기록을 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `MEMBER_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `MEMBER` |
| `PERSON_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `PERSON` |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |

### 3.11 `COLLECTION`

회원이 만든 콘텐츠 컬렉션의 제목·설명·공개 상태를 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `COLLECTION_ID` | `NUMBER` | 불가 | - | PK |
| `MEMBER_ID` | `NUMBER` | 불가 | - | FK → `MEMBER` |
| `TITLE` | `NVARCHAR2(100)` | 불가 | - | - |
| `DESCRIPTION` | `NVARCHAR2(1000)` | 허용 | - | - |
| `IS_PUBLIC` | `CHAR(1 BYTE)` | 불가 | `'Y'` | `Y`, `N` |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |
| `UPDATED_DT` | `DATE` | 허용 | - | - |

### 3.12 `NOTICE`

관리자가 등록하는 공지사항의 게시 상태와 중요 표시를 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `NOTICE_ID` | `NUMBER` | 불가 | - | PK |
| `TITLE` | `NVARCHAR2(200)` | 불가 | - | - |
| `CONTENT` | `CLOB` | 불가 | - | - |
| `IMPORTANT` | `CHAR(1 BYTE)` | 불가 | `'N'` | `Y`, `N` |
| `STATUS` | `VARCHAR2(20 BYTE)` | 불가 | `'DRAFT'` | `DRAFT`, `PUBLISHED`, `HIDDEN` |
| `VIEW_COUNT` | `NUMBER(10,0)` | 불가 | `0` | 0 이상 |
| `CREATED_ID` | `NUMBER` | 불가 | - | FK → `MEMBER` |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |
| `UPDATED_ID` | `NUMBER` | 불가 | - | FK → `MEMBER` |
| `UPDATED_DT` | `DATE` | 허용 | - | - |

### 3.13 `COLLECTION_ITEM`

컬렉션에 포함된 콘텐츠와 추가 일시를 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `COLLECTION_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `COLLECTION` |
| `CONTENT_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `CONTENT` |
| `ADDED_DT` | `DATE` | 불가 | `SYSDATE` | - |

### 3.14 `COLLECTION_LIKE`

회원이 좋아요를 누른 컬렉션 기록을 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `MEMBER_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `MEMBER` |
| `COLLECTION_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `COLLECTION` |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |

### 3.15 `USER_COMMENT`

회원이 콘텐츠 또는 컬렉션에 작성한 코멘트와 스포일러 상태를 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `COMMENT_ID` | `NUMBER` | 불가 | - | PK |
| `MEMBER_ID` | `NUMBER` | 불가 | - | FK → `MEMBER` |
| `CONTENT_ID` | `NUMBER` | 허용 | - | FK → `CONTENT` |
| `COLLECTION_ID` | `NUMBER` | 허용 | - | FK → `COLLECTION` |
| `COMMENT_DETAIL` | `CLOB` | 불가 | - | - |
| `SPOILER` | `CHAR(1 BYTE)` | 불가 | `'N'` | `Y`, `N` |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |
| `UPDATED_DT` | `DATE` | 허용 | - | - |

`CONTENT_ID`와 `COLLECTION_ID` 중 정확히 하나만 값이 있어야 한다. 함수 기반 UNIQUE 인덱스로 회원당 콘텐츠 코멘트 1개, 회원당 컬렉션 코멘트 1개를 보장한다.

### 3.16 `COMMENT_LIKE`

회원이 좋아요를 누른 코멘트 기록을 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `MEMBER_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `MEMBER` |
| `COMMENT_ID` | `NUMBER` | 불가 | - | 복합 PK, FK → `USER_COMMENT` |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |

### 3.17 `REPORT_COMMENT`

코멘트 신고 접수와 관리자 처리 결과를 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `REPORT_ID` | `NUMBER` | 불가 | - | PK |
| `REPORT_MEMBER_ID` | `NUMBER` | 불가 | - | FK → `MEMBER` |
| `COMMENT_ID` | `NUMBER` | 불가 | - | FK → `USER_COMMENT` |
| `REASON` | `VARCHAR2(30 BYTE)` | 불가 | - | `SPOILER`, `INAPPROPRIATE`, `SPAM`, `OTHER` |
| `DETAIL` | `NVARCHAR2(1000)` | 허용 | - | `OTHER` 선택 시 필수 |
| `STATUS` | `VARCHAR2(20 BYTE)` | 불가 | `'RECEIVED'` | `RECEIVED`, `PROCESSING`, `ACCEPTED`, `REJECTED` |
| `PROCESSED_BY_MEMBER_ID` | `NUMBER` | 허용 | - | FK → `MEMBER` |
| `PROCESS_NOTE` | `NVARCHAR2(1000)` | 허용 | - | - |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |
| `PROCESSED_DT` | `DATE` | 허용 | - | - |

`ACCEPTED` 또는 `REJECTED` 상태에서는 처리 관리자와 처리 일시가 모두 필요하다.

### 3.18 `COMMON_CODE_GROUP`

공통 코드 그룹을 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `GROUP_CODE` | `VARCHAR2(30)` | 불가 | - | PK |
| `GROUP_NAME` | `NVARCHAR2(100)` | 불가 | - | - |
| `DESCRIPTION` | `NVARCHAR2(500)` | 허용 | - | - |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |

### 3.19 `COMMON_CODE`

공통 코드 상세를 관리한다.

| 컬럼 | 자료형 | Null | 기본값 | 키/비고 |
| --- | --- | --- | --- | --- |
| `CODE` | `VARCHAR2(30)` | 불가 | - | 복합 PK |
| `GROUP_CODE` | `VARCHAR2(30)` | 불가 | - | 복합 PK, FK → `COMMON_CODE_GROUP` |
| `CODE_NAME` | `NVARCHAR2(100)` | 불가 | - | - |
| `SORT_ORDER` | `NUMBER(3)` | 불가 | `0` | - |
| `CREATED_DT` | `DATE` | 불가 | `SYSDATE` | - |

## 4. 외래 키와 삭제 규칙

전체 외래 키 26개 중 25개는 `ON DELETE CASCADE`, 신고 처리 관리자 관계 1개는 `ON DELETE SET NULL`이다.

| 제약조건 | 자식 컬럼 | 부모 컬럼 | 삭제 규칙 |
| --- | --- | --- | --- |
| `FK_MEMBER_SOCIAL_MEMBER` | `MEMBER_SOCIAL_ACCOUNT.MEMBER_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_CONTENT_GENRE_CONTENT` | `CONTENT_GENRE.CONTENT_ID` | `CONTENT.CONTENT_ID` | CASCADE |
| `FK_CONTENT_GENRE_GENRE` | `CONTENT_GENRE.GENRE_ID` | `GENRE.GENRE_ID` | CASCADE |
| `FK_CONTENT_IMAGE_CONTENT` | `CONTENT_IMAGE.CONTENT_ID` | `CONTENT.CONTENT_ID` | CASCADE |
| `FK_CONTENT_CREDIT_CONTENT` | `CONTENT_CREDIT.CONTENT_ID` | `CONTENT.CONTENT_ID` | CASCADE |
| `FK_CONTENT_CREDIT_PERSON` | `CONTENT_CREDIT.PERSON_ID` | `PERSON.PERSON_ID` | CASCADE |
| `FK_MEMBER_CONTENT_MEMBER` | `MEMBER_CONTENT.MEMBER_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_MEMBER_CONTENT_CONTENT` | `MEMBER_CONTENT.CONTENT_ID` | `CONTENT.CONTENT_ID` | CASCADE |
| `FK_PERSON_LIKE_MEMBER` | `PERSON_LIKE.MEMBER_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_PERSON_LIKE_PERSON` | `PERSON_LIKE.PERSON_ID` | `PERSON.PERSON_ID` | CASCADE |
| `FK_COLLECTION_MEMBER` | `COLLECTION.MEMBER_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_NOTICE_CREATED_ADMIN` | `NOTICE.CREATED_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_NOTICE_UPDATED_ADMIN` | `NOTICE.UPDATED_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_COLLECTION_ITEM_COLLECTION` | `COLLECTION_ITEM.COLLECTION_ID` | `COLLECTION.COLLECTION_ID` | CASCADE |
| `FK_COLLECTION_ITEM_CONTENT` | `COLLECTION_ITEM.CONTENT_ID` | `CONTENT.CONTENT_ID` | CASCADE |
| `FK_COLLECTION_LIKE_MEMBER` | `COLLECTION_LIKE.MEMBER_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_COLLECTION_LIKE_COLLECTION` | `COLLECTION_LIKE.COLLECTION_ID` | `COLLECTION.COLLECTION_ID` | CASCADE |
| `FK_USER_COMMENT_MEMBER` | `USER_COMMENT.MEMBER_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_USER_COMMENT_CONTENT` | `USER_COMMENT.CONTENT_ID` | `CONTENT.CONTENT_ID` | CASCADE |
| `FK_USER_COMMENT_COLLECTION` | `USER_COMMENT.COLLECTION_ID` | `COLLECTION.COLLECTION_ID` | CASCADE |
| `FK_COMMENT_LIKE_MEMBER` | `COMMENT_LIKE.MEMBER_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_COMMENT_LIKE_COMMENT` | `COMMENT_LIKE.COMMENT_ID` | `USER_COMMENT.COMMENT_ID` | CASCADE |
| `FK_REPORT_REPORTER` | `REPORT_COMMENT.REPORT_MEMBER_ID` | `MEMBER.MEMBER_ID` | CASCADE |
| `FK_REPORT_COMMENT` | `REPORT_COMMENT.COMMENT_ID` | `USER_COMMENT.COMMENT_ID` | CASCADE |
| `FK_REPORT_ADMIN` | `REPORT_COMMENT.PROCESSED_BY_MEMBER_ID` | `MEMBER.MEMBER_ID` | SET NULL |
| `FK_CODE_GROUP` | `COMMON_CODE.GROUP_CODE` | `COMMON_CODE_GROUP.GROUP_CODE` | CASCADE |

회원은 hard delete 구조다. 회원 삭제 시 직접 연결된 활동과, 컬렉션·코멘트를 거쳐 연결된 자식 데이터가 연쇄 삭제된다. 특히 `NOTICE.CREATED_ID`와 `NOTICE.UPDATED_ID`도 CASCADE이므로 회원 삭제가 공지 삭제로 이어질 수 있다는 점을 운영 정책과 함께 검토해야 한다.

## 5. CHECK와 UNIQUE 규칙

### CHECK 제약조건

| 제약조건 | 규칙 |
| --- | --- |
| `CK_MEMBER_ROLE` | `ROLE IN ('USER', 'ADMIN')` |
| `CK_MEMBER_SOCIAL_PROVIDER` | `PROVIDER_CODE IN ('GOOGLE', 'KAKAO', 'NAVER')` |
| `CK_CONTENT_CREDIT_ROLE` | `ROLE IN ('ACTOR', 'DIRECTOR', 'WRITER', 'PRODUCER')` |
| `CK_CONTENT_CREDIT_ORDER` | `DISPLAY_ORDER >= 0` |
| `CK_MEMBER_CONTENT_RATING` | 평점은 null 또는 1~5 |
| `CK_MEMBER_CONTENT_WATCHLIST` | `WATCHLIST IN ('Y', 'N')` |
| `CK_MEMBER_CONTENT_NOT_EMPTY` | 평점 또는 보고싶어요 중 하나는 존재 |
| `CK_COLLECTION_PUBLIC` | `IS_PUBLIC IN ('Y', 'N')` |
| `CK_NOTICE_IMPORTANT` | `IMPORTANT IN ('Y', 'N')` |
| `CK_NOTICE_STATUS` | `STATUS IN ('DRAFT', 'PUBLISHED', 'HIDDEN')` |
| `CK_NOTICE_VIEW_COUNT` | `VIEW_COUNT >= 0` |
| `CK_USER_COMMENT_TARGET_ONE` | 콘텐츠와 컬렉션 중 정확히 하나만 대상 |
| `CK_USER_COMMENT_SPOILER` | `SPOILER IN ('Y', 'N')` |
| `CK_REPORT_REASON` | 신고 사유 4종 제한 |
| `CK_REPORT_STATUS` | 신고 상태 4종 제한 |
| `CK_REPORT_OTHER_DETAIL` | 사유가 `OTHER`이면 상세 내용 필수 |
| `CK_REPORT_PROCESS_COMPLETE` | 완료 상태이면 처리자와 처리 일시 필수 |

### UNIQUE 규칙

- 별도 UNIQUE 인덱스: `UK_MEMBER_EMAIL`, `UK_MEMBER_NICKNAME`
- UNIQUE 제약조건: `UK_GENRE_NAME`, `UK_GENRE_EXTERNAL_ID`, `UK_PERSON_EXTERNAL`
- UNIQUE 제약조건: `UK_MEMBER_SOCIAL_PROVIDER_USER`, `UK_MEMBER_SOCIAL_MEMBER_PROVIDER`
- 함수 기반 UNIQUE 인덱스: `UK_USER_COMMENT_CONTENT`, `UK_USER_COMMENT_COLLECTION`

함수 기반 인덱스는 대상 컬럼이 null이 아닐 때만 `(MEMBER_ID, 대상 ID)`를 인덱싱해 회원별 대상 코멘트 1개를 보장한다.

## 6. 조회용 인덱스

PK·UNIQUE용 인덱스를 제외하고 DDL에 명시된 일반 인덱스는 다음 25개다.

| 테이블 | 인덱스 |
| --- | --- |
| `MEMBER` | `IDX_MEMBER_CREATED (CREATED_DT DESC)` |
| `CONTENT` | `IDX_CONTENT_TITLE (TITLE_KO)`, `IDX_CONTENT_COUNTRY (COUNTRY)` |
| `PERSON` | `IDX_PERSON_NAME (NAME_KO)` |
| `MEMBER_SOCIAL_ACCOUNT` | `IDX_MEMBER_SOCIAL_MEMBER (MEMBER_ID)` |
| `CONTENT_GENRE` | `IDX_CONTENT_GENRE_GENRE (GENRE_ID, CONTENT_ID)` |
| `CONTENT_IMAGE` | `IDX_CONTENT_IMAGE_LIST (CONTENT_ID)` |
| `CONTENT_CREDIT` | `IDX_CONTENT_CREDIT_ROLE_ORDER (CONTENT_ID, ROLE, DISPLAY_ORDER)`, `IDX_CONTENT_CREDIT_PERSON (PERSON_ID, CONTENT_ID)` |
| `MEMBER_CONTENT` | `IDX_MEMBER_CONTENT_RATING (MEMBER_ID, RATING_SCORE)`, `IDX_MEMBER_CONTENT_WATCHLIST (MEMBER_ID, WATCHLIST, WATCHLIST_DT)`, `IDX_MEMBER_CONTENT_CONTENT_RATING (CONTENT_ID, RATING_SCORE)` |
| `PERSON_LIKE` | `IDX_PERSON_LIKE_PERSON (PERSON_ID, CREATED_DT)` |
| `COLLECTION` | `IDX_COLLECTION_MEMBER_CREATED (MEMBER_ID, CREATED_DT DESC)`, `IDX_COLLECTION_TITLE (TITLE)` |
| `NOTICE` | `IDX_NOTICE_STATUS_IMPORTANT (STATUS, IMPORTANT, CREATED_DT DESC)` |
| `COLLECTION_ITEM` | `IDX_COLLECTION_ITEM_CONTENT (CONTENT_ID, COLLECTION_ID)` |
| `COLLECTION_LIKE` | `IDX_COLLECTION_LIKE_COLLECTION (COLLECTION_ID, CREATED_DT)` |
| `USER_COMMENT` | `IDX_USER_COMMENT_CONTENT_CREATED (CONTENT_ID, CREATED_DT DESC)`, `IDX_USER_COMMENT_COLLECTION_CREATED (COLLECTION_ID, CREATED_DT DESC)`, `IDX_USER_COMMENT_MEMBER (MEMBER_ID, CREATED_DT DESC)` |
| `COMMENT_LIKE` | `IDX_COMMENT_LIKE_COMMENT (COMMENT_ID, CREATED_DT)` |
| `REPORT_COMMENT` | `IDX_REPORT_STATUS_CREATED (STATUS, CREATED_DT)`, `IDX_REPORT_REPORTER (REPORT_MEMBER_ID, CREATED_DT)`, `IDX_REPORT_COMMENT_TARGET (COMMENT_ID)` |

## 7. 마이그레이션 운영 지침

현재 저장소에는 Flyway나 Liquibase 의존성 및 버전 관리 SQL이 없다. 기존 팀 스크립트의 역할은 다음과 같다.

| 파일 | 역할 | 마이그레이션 사용 시 주의점 |
| --- | --- | --- |
| `0.ENDIT_DB초기화(테이블&시퀀스삭제).txt` | 전체 초기화 | 파괴적 작업이므로 일반 버전 마이그레이션에 포함하지 않음 |
| `1.ENDIT_CREATE(테이블&시퀀스생성).txt` | 시퀀스 11개와 업무 테이블 17개 생성 | 신규 DB용 기준 DDL로 사용 가능 |
| `2.ENDIT_DUMMY_DATA(가짜데이터생성).txt` | 개발용 더미데이터 생성 | 운영 마이그레이션에서 분리 |
| `3.ENDIT_COMMIT_TABLE(공통코드_관련_테이블_생성).txt` | 공통코드 테이블 삭제 후 재생성 | `DROP TABLE`을 제거한 신규 생성 DDL만 기준화 |
| `4.ENDIT_COMMIT_DATA(공통코드_관련_데이터_삽입).txt` | 공통코드 데이터 입력 | 중복 실행 정책을 정한 뒤 버전 데이터로 관리 |

Flyway를 도입한다면 다음과 같이 분리할 수 있다.

```text
src/main/resources/db/migration/
  V1__create_core_schema.sql
  V2__create_common_code_schema.sql
  V3__insert_common_codes.sql
```

이 경로와 파일명은 도입 예시이며 현재 저장소에는 실행 SQL이나 Flyway 의존성을 추가하지 않았다.

### 적용 원칙

1. 기존 `ENDITPCWK`와 `ENDIT_TEST`에는 생성 DDL을 다시 실행하지 않고 현재 스키마 버전을 baseline으로 등록한다.
2. 새 데이터베이스에는 부모 테이블, 자식 테이블, 인덱스와 주석 순으로 적용한다.
3. 이미 적용된 버전 SQL은 수정하지 않고 변경마다 새 버전을 추가한다.
4. 개발 더미데이터는 운영 스키마 마이그레이션과 분리한다.
5. 테이블·컬럼 삭제 전에는 데이터 보존, 역마이그레이션과 서비스 호환 계획을 작성한다.
6. 스키마 변경 시 Domain VO, Mapper 인터페이스, Mapper XML과 DAO 테스트를 함께 검토한다.

### 현재 스크립트의 주의사항

- 생성 DDL의 시퀀스는 모두 1부터 시작하지만 더미데이터의 주요 PK는 1~10을 사용한다. 더미데이터 적용 후 시퀀스를 조정하지 않으면 다음 INSERT에서 PK 충돌이 발생할 수 있다.
- SQL Developer Export본은 CLOB 본문 누락과 인덱스 DDL 중복이 확인돼 완전 복원용 baseline으로 사용하기 어렵다.
- 실제 baseline SQL을 만들 때는 팀 생성 DDL과 Oracle의 `DBMS_METADATA.GET_DDL` 결과를 함께 대조하고, 시퀀스 현재값과 CLOB 데이터는 별도로 검증한다.
- 공통코드 테이블 스크립트의 선행 `DROP TABLE`은 재생성 편의를 위한 구문이므로 버전 마이그레이션에 그대로 포함하지 않는다.