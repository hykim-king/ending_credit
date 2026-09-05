-- =====================================================================
-- Ending Credit - jinyoung00~10 페르소나 테스트 데이터 안전 등록
--
-- 실행 환경
-- - Oracle SQL Developer에서 Run Script(F5)로 실행한다.
-- - 기준 데이터: enditpcwk_data_20260905.sql
--
-- 생성 데이터
-- - MEMBER 11건: ADMIN 1건 + USER 10건
-- - COLLECTION 42건: 작품 포함 36건 + 빈 컬렉션 6건
-- - MEMBER_CONTENT: 일반 회원별 평가 및 보고싶어요
-- - PERSON_LIKE / COLLECTION_LIKE: 일반 회원별 좋아요
-- - USER_COMMENT: 페르소나 활동을 보여 주는 콘텐츠/컬렉션 코멘트
--
-- 안전 정책
-- - 대상 이메일이나 닉네임이 하나라도 존재하면 등록하지 않는다.
-- - 필요한 CONTENT/PERSON 기준 데이터가 없으면 전체 ROLLBACK한다.
-- - 모든 검증을 통과한 경우에만 마지막 COMMIT을 실행한다.
-- - Oracle 시퀀스 값은 오류로 ROLLBACK되어도 되돌아가지 않아 번호가 건너뛸 수 있다.
--
-- 로그인 공통 비밀번호: 123123**
-- 저장 비밀번호: BCrypt strength 12 해시
-- =====================================================================

SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- ---------------------------------------------------------------------
-- 0. 사전 충돌 검사
-- ---------------------------------------------------------------------
DECLARE
    V_COLLISION_COUNT NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO V_COLLISION_COUNT
      FROM MEMBER
     WHERE REGEXP_LIKE(
               LOWER(EMAIL),
               '^jinyoung(0[0-9]|10)@pcwk\.com$'
           )
        OR NICKNAME IN (
               'Admin 진영',
               '마블캐처',
               '고전멜로수집가',
               '액션직진러',
               '컬렉션장인',
               '댓글요정',
               '심야스릴러',
               '애니프레임',
               '인디시네마',
               '세계관탐험가',
               '개봉작헌터'
           );

    IF V_COLLISION_COUNT > 0 THEN
        RAISE_APPLICATION_ERROR(
            -20001,
            'jinyoung00~10 이메일 또는 대상 닉네임이 이미 존재합니다.'
        );
    END IF;
END;
/

SAVEPOINT BEFORE_JINYOUNG_PERSONA_DATA;

-- ---------------------------------------------------------------------
-- 1. 데이터 등록
-- ---------------------------------------------------------------------
DECLARE
    C_PASSWORD CONSTANT VARCHAR2(255) :=
        '$2a$12$PjWrya4dCHdsTecC1tum8ewPHC/3aRrpo.ua/o3CL8ryHVhSJ6tOe';

    PROCEDURE REQUIRE_CONTENT(P_CONTENT_ID NUMBER) IS
        V_COUNT NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO V_COUNT
          FROM CONTENT
         WHERE CONTENT_ID = P_CONTENT_ID;

        IF V_COUNT <> 1 THEN
            RAISE_APPLICATION_ERROR(
                -20010,
                '필수 CONTENT_ID가 없습니다: ' || P_CONTENT_ID
            );
        END IF;
    END REQUIRE_CONTENT;

    PROCEDURE REQUIRE_PERSON(P_PERSON_ID NUMBER) IS
        V_COUNT NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO V_COUNT
          FROM PERSON
         WHERE PERSON_ID = P_PERSON_ID;

        IF V_COUNT <> 1 THEN
            RAISE_APPLICATION_ERROR(
                -20011,
                '필수 PERSON_ID가 없습니다: ' || P_PERSON_ID
            );
        END IF;
    END REQUIRE_PERSON;

    PROCEDURE ADD_MEMBER(
        P_EMAIL       VARCHAR2,
        P_NICKNAME    NVARCHAR2,
        P_INTRO       NVARCHAR2,
        P_ROLE        VARCHAR2,
        P_DAYS_AGO    NUMBER
    ) IS
    BEGIN
        INSERT INTO MEMBER (
            MEMBER_ID,
            EMAIL,
            PASSWORD,
            NICKNAME,
            INTRODUCTION,
            PROFILE_IMG_URL,
            ROLE,
            CREATED_DT,
            UPDATED_DT
        ) VALUES (
            SEQ_MEMBER.NEXTVAL,
            P_EMAIL,
            C_PASSWORD,
            P_NICKNAME,
            P_INTRO,
            NULL,
            P_ROLE,
            SYSDATE - P_DAYS_AGO,
            NULL
        );
    END ADD_MEMBER;

    PROCEDURE ADD_COLLECTION(
        P_EMAIL        VARCHAR2,
        P_TITLE        NVARCHAR2,
        P_DESCRIPTION  NVARCHAR2,
        P_DAYS_AGO     NUMBER,
        P_IS_PUBLIC    CHAR DEFAULT 'Y'
    ) IS
        V_MEMBER_ID MEMBER.MEMBER_ID%TYPE;
    BEGIN
        SELECT MEMBER_ID
          INTO V_MEMBER_ID
          FROM MEMBER
         WHERE EMAIL = P_EMAIL;

        INSERT INTO COLLECTION (
            COLLECTION_ID,
            MEMBER_ID,
            TITLE,
            DESCRIPTION,
            IS_PUBLIC,
            CREATED_DT,
            UPDATED_DT
        ) VALUES (
            SEQ_COLLECTION.NEXTVAL,
            V_MEMBER_ID,
            P_TITLE,
            P_DESCRIPTION,
            P_IS_PUBLIC,
            SYSDATE - P_DAYS_AGO,
            NULL
        );
    END ADD_COLLECTION;

    PROCEDURE ADD_ITEM(
        P_EMAIL       VARCHAR2,
        P_TITLE       NVARCHAR2,
        P_CONTENT_ID  NUMBER,
        P_DAYS_AGO    NUMBER
    ) IS
        V_COLLECTION_ID COLLECTION.COLLECTION_ID%TYPE;
    BEGIN
        REQUIRE_CONTENT(P_CONTENT_ID);

        SELECT C.COLLECTION_ID
          INTO V_COLLECTION_ID
          FROM COLLECTION C
          JOIN MEMBER M
            ON M.MEMBER_ID = C.MEMBER_ID
         WHERE M.EMAIL = P_EMAIL
           AND C.TITLE = P_TITLE;

        INSERT INTO COLLECTION_ITEM (
            COLLECTION_ID,
            CONTENT_ID,
            ADDED_DT
        ) VALUES (
            V_COLLECTION_ID,
            P_CONTENT_ID,
            SYSDATE - P_DAYS_AGO
        );
    END ADD_ITEM;

    PROCEDURE ADD_RECORD(
        P_EMAIL        VARCHAR2,
        P_CONTENT_ID   NUMBER,
        P_RATING       NUMBER,
        P_WATCHLIST    CHAR,
        P_DAYS_AGO     NUMBER
    ) IS
        V_MEMBER_ID MEMBER.MEMBER_ID%TYPE;
    BEGIN
        REQUIRE_CONTENT(P_CONTENT_ID);

        SELECT MEMBER_ID
          INTO V_MEMBER_ID
          FROM MEMBER
         WHERE EMAIL = P_EMAIL;

        INSERT INTO MEMBER_CONTENT (
            MEMBER_ID,
            CONTENT_ID,
            RATING_SCORE,
            WATCHLIST,
            RATED_DT,
            WATCHLIST_DT,
            UPDATED_DT
        ) VALUES (
            V_MEMBER_ID,
            P_CONTENT_ID,
            P_RATING,
            P_WATCHLIST,
            CASE WHEN P_RATING IS NOT NULL THEN SYSDATE - P_DAYS_AGO END,
            CASE WHEN P_WATCHLIST = 'Y' THEN SYSDATE - P_DAYS_AGO END,
            SYSDATE - P_DAYS_AGO
        );
    END ADD_RECORD;

    PROCEDURE ADD_PERSON_LIKE(
        P_EMAIL       VARCHAR2,
        P_PERSON_ID   NUMBER,
        P_DAYS_AGO    NUMBER
    ) IS
        V_MEMBER_ID MEMBER.MEMBER_ID%TYPE;
    BEGIN
        REQUIRE_PERSON(P_PERSON_ID);

        SELECT MEMBER_ID
          INTO V_MEMBER_ID
          FROM MEMBER
         WHERE EMAIL = P_EMAIL;

        INSERT INTO PERSON_LIKE (
            MEMBER_ID,
            PERSON_ID,
            CREATED_DT
        ) VALUES (
            V_MEMBER_ID,
            P_PERSON_ID,
            SYSDATE - P_DAYS_AGO
        );
    END ADD_PERSON_LIKE;

    PROCEDURE ADD_COLLECTION_LIKE(
        P_EMAIL        VARCHAR2,
        P_OWNER_EMAIL  VARCHAR2,
        P_TITLE        NVARCHAR2,
        P_DAYS_AGO     NUMBER
    ) IS
        V_MEMBER_ID     MEMBER.MEMBER_ID%TYPE;
        V_COLLECTION_ID COLLECTION.COLLECTION_ID%TYPE;
    BEGIN
        SELECT MEMBER_ID
          INTO V_MEMBER_ID
          FROM MEMBER
         WHERE EMAIL = P_EMAIL;

        SELECT C.COLLECTION_ID
          INTO V_COLLECTION_ID
          FROM COLLECTION C
          JOIN MEMBER M
            ON M.MEMBER_ID = C.MEMBER_ID
         WHERE M.EMAIL = P_OWNER_EMAIL
           AND C.TITLE = P_TITLE;

        INSERT INTO COLLECTION_LIKE (
            MEMBER_ID,
            COLLECTION_ID,
            CREATED_DT
        ) VALUES (
            V_MEMBER_ID,
            V_COLLECTION_ID,
            SYSDATE - P_DAYS_AGO
        );
    END ADD_COLLECTION_LIKE;

    PROCEDURE ADD_CONTENT_COMMENT(
        P_EMAIL        VARCHAR2,
        P_CONTENT_ID   NUMBER,
        P_DETAIL       NVARCHAR2,
        P_SPOILER      CHAR,
        P_DAYS_AGO     NUMBER
    ) IS
        V_MEMBER_ID MEMBER.MEMBER_ID%TYPE;
    BEGIN
        REQUIRE_CONTENT(P_CONTENT_ID);

        SELECT MEMBER_ID
          INTO V_MEMBER_ID
          FROM MEMBER
         WHERE EMAIL = P_EMAIL;

        INSERT INTO USER_COMMENT (
            COMMENT_ID,
            MEMBER_ID,
            CONTENT_ID,
            COLLECTION_ID,
            COMMENT_DETAIL,
            SPOILER,
            CREATED_DT,
            UPDATED_DT
        ) VALUES (
            SEQ_USER_COMMENT.NEXTVAL,
            V_MEMBER_ID,
            P_CONTENT_ID,
            NULL,
            P_DETAIL,
            P_SPOILER,
            SYSDATE - P_DAYS_AGO,
            NULL
        );
    END ADD_CONTENT_COMMENT;

    PROCEDURE ADD_COLLECTION_COMMENT(
        P_EMAIL        VARCHAR2,
        P_OWNER_EMAIL  VARCHAR2,
        P_TITLE        NVARCHAR2,
        P_DETAIL       NVARCHAR2,
        P_DAYS_AGO     NUMBER
    ) IS
        V_MEMBER_ID     MEMBER.MEMBER_ID%TYPE;
        V_COLLECTION_ID COLLECTION.COLLECTION_ID%TYPE;
    BEGIN
        SELECT MEMBER_ID
          INTO V_MEMBER_ID
          FROM MEMBER
         WHERE EMAIL = P_EMAIL;

        SELECT C.COLLECTION_ID
          INTO V_COLLECTION_ID
          FROM COLLECTION C
          JOIN MEMBER M
            ON M.MEMBER_ID = C.MEMBER_ID
         WHERE M.EMAIL = P_OWNER_EMAIL
           AND C.TITLE = P_TITLE;

        INSERT INTO USER_COMMENT (
            COMMENT_ID,
            MEMBER_ID,
            CONTENT_ID,
            COLLECTION_ID,
            COMMENT_DETAIL,
            SPOILER,
            CREATED_DT,
            UPDATED_DT
        ) VALUES (
            SEQ_USER_COMMENT.NEXTVAL,
            V_MEMBER_ID,
            NULL,
            V_COLLECTION_ID,
            P_DETAIL,
            'N',
            SYSDATE - P_DAYS_AGO,
            NULL
        );
    END ADD_COLLECTION_COMMENT;

BEGIN
    -- ================================================================
    -- 1-1. MEMBER 11건
    -- ================================================================
    ADD_MEMBER(
        'jinyoung00@pcwk.com', 'Admin 진영',
        'Ending Credit 테스트 데이터를 관리하는 관리자입니다.',
        'ADMIN', 240
    );
    ADD_MEMBER(
        'jinyoung01@pcwk.com', '마블캐처',
        '마블과 히어로 블록버스터를 빠짐없이 챙겨 보는 20대 관객입니다.',
        'USER', 180
    );
    ADD_MEMBER(
        'jinyoung02@pcwk.com', '고전멜로수집가',
        '오래된 명작과 마음에 오래 남는 멜로 영화를 좋아합니다.',
        'USER', 174
    );
    ADD_MEMBER(
        'jinyoung03@pcwk.com', '액션직진러',
        '복잡한 고민 없이 시원하게 달리는 액션과 첩보 영화를 즐깁니다.',
        'USER', 165
    );
    ADD_MEMBER(
        'jinyoung04@pcwk.com', '컬렉션장인',
        '장르를 가리지 않고 보고 주제별 컬렉션으로 정리하는 헤비 유저입니다.',
        'USER', 210
    );
    ADD_MEMBER(
        'jinyoung05@pcwk.com', '댓글요정',
        '좋은 영화와 컬렉션을 발견하면 짧게라도 꼭 감상을 남깁니다.',
        'USER', 152
    );
    ADD_MEMBER(
        'jinyoung06@pcwk.com', '심야스릴러',
        '불을 끄고 공포와 스릴러를 보는 시간이 가장 즐겁습니다.',
        'USER', 143
    );
    ADD_MEMBER(
        'jinyoung07@pcwk.com', '애니프레임',
        '애니메이션의 섬세한 프레임과 따뜻한 이야기를 좋아합니다.',
        'USER', 136
    );
    ADD_MEMBER(
        'jinyoung08@pcwk.com', '인디시네마',
        '독립영화와 예술영화 속 작지만 선명한 이야기를 찾아봅니다.',
        'USER', 128
    );
    ADD_MEMBER(
        'jinyoung09@pcwk.com', '세계관탐험가',
        'SF와 판타지의 설정을 파고들며 시리즈를 정주행합니다.',
        'USER', 121
    );
    ADD_MEMBER(
        'jinyoung10@pcwk.com', '개봉작헌터',
        '새로 공개되는 화제작을 빠르게 확인하고 기록합니다.',
        'USER', 112
    );

    -- ================================================================
    -- 1-2. COLLECTION 42건
    -- 작품 포함 36건 + 빈 컬렉션 6건
    -- ================================================================

    -- jinyoung01: 4 + 빈 컬렉션 1
    ADD_COLLECTION('jinyoung01@pcwk.com', 'MCU 입문자를 위한 핵심 6편',
        '아이언맨부터 엔드게임까지 MCU의 큰 흐름을 빠르게 잡는 입문 코스입니다.', 82);
    ADD_COLLECTION('jinyoung01@pcwk.com', '어벤져스 결전의 흐름',
        '팀의 결성과 충돌, 인피니티 사가의 결말을 한 번에 이어 봅니다.', 68);
    ADD_COLLECTION('jinyoung01@pcwk.com', '토르의 유쾌한 우주 여행',
        '천둥의 신과 가디언즈가 만드는 유쾌한 우주 모험 모음입니다.', 49);
    ADD_COLLECTION('jinyoung01@pcwk.com', '스파이더맨 세대 교차',
        '서로 다른 시대의 스파이더맨을 비교하며 보는 컬렉션입니다.', 31);
    ADD_COLLECTION('jinyoung01@pcwk.com', '곧 채울 마블 기대작',
        '공개 예정작을 본 뒤 차근차근 채워 갈 마블 컬렉션입니다.', 5);

    -- jinyoung02: 4 + 빈 컬렉션 1
    ADD_COLLECTION('jinyoung02@pcwk.com', '시간을 건너온 멜로',
        '시대가 달라도 사랑의 감정은 그대로인 멜로 명작을 모았습니다.', 91);
    ADD_COLLECTION('jinyoung02@pcwk.com', '음악과 사랑이 남는 밤',
        '노래와 사랑의 기억이 함께 남는 영화를 늦은 밤에 감상합니다.', 74);
    ADD_COLLECTION('jinyoung02@pcwk.com', '눈물 버튼 로맨스',
        '알면서도 다시 보고 울게 되는 로맨스 영화를 담았습니다.', 53);
    ADD_COLLECTION('jinyoung02@pcwk.com', '오래 기억할 사랑의 장면',
        '한 장면만으로도 영화 전체가 떠오르는 사랑 이야기입니다.', 28);
    ADD_COLLECTION('jinyoung02@pcwk.com', '흑백 멜로 탐색 노트',
        '앞으로 발견할 흑백 멜로 고전을 기록하기 위한 빈 노트입니다.', 8);

    -- jinyoung03: 4
    ADD_COLLECTION('jinyoung03@pcwk.com', '질주 본능 액션',
        '엔진 소리와 속도감이 화면을 가득 채우는 액션 영화입니다.', 79);
    ADD_COLLECTION('jinyoung03@pcwk.com', '첩보 액션 정주행',
        '잠입과 추격, 불가능한 임무가 이어지는 첩보 액션 모음입니다.', 61);
    ADD_COLLECTION('jinyoung03@pcwk.com', '인간 병기와 추격전',
        '한순간도 쉬지 않는 추격과 생존을 중심으로 골랐습니다.', 42);
    ADD_COLLECTION('jinyoung03@pcwk.com', '전장의 영웅들',
        '압도적인 전투와 영웅의 선택이 돋보이는 작품들입니다.', 19);

    -- jinyoung04: 6 + 빈 컬렉션 1
    ADD_COLLECTION('jinyoung04@pcwk.com', '세 시간이 아깝지 않은 대작',
        '긴 러닝타임을 잊게 만드는 거대한 이야기와 화면을 모았습니다.', 101);
    ADD_COLLECTION('jinyoung04@pcwk.com', '감독의 세계가 선명한 영화',
        '몇 장면만 봐도 연출자의 색이 느껴지는 작품들입니다.', 88);
    ADD_COLLECTION('jinyoung04@pcwk.com', '장르별 인생 영화 선반',
        '액션부터 애니메이션까지 장르마다 한 편씩 꺼내 보는 선반입니다.', 72);
    ADD_COLLECTION('jinyoung04@pcwk.com', '다시 보면 더 좋은 영화',
        '두 번째 감상에서 복선과 감정이 더 또렷해지는 영화입니다.', 54);
    ADD_COLLECTION('jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈',
        '주말을 통째로 비우고 시리즈 순서대로 달리기 위한 목록입니다.', 37);
    ADD_COLLECTION('jinyoung04@pcwk.com', '포스터만 봐도 설레는 영화',
        '포스터의 분위기만으로도 다시 재생하고 싶은 작품입니다.', 21);
    ADD_COLLECTION('jinyoung04@pcwk.com', '다음 달 큐레이션 준비실',
        '다음 달 공개할 새 주제의 후보를 정리하는 준비 공간입니다.', 4);

    -- jinyoung05: 2 + 빈 컬렉션 1
    ADD_COLLECTION('jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화',
        '감상 뒤 누군가와 바로 이야기를 나누고 싶은 작품입니다.', 63);
    ADD_COLLECTION('jinyoung05@pcwk.com', '결말 토론이 필요한 작품',
        '한 가지 해석으로 끝나지 않는 결말을 가진 영화를 모았습니다.', 34);
    ADD_COLLECTION('jinyoung05@pcwk.com', '댓글 추천으로 채울 컬렉션',
        '다른 회원의 추천 댓글을 받아 한 편씩 채워 갈 컬렉션입니다.', 3);

    -- jinyoung06: 3
    ADD_COLLECTION('jinyoung06@pcwk.com', '불 끄고 보면 안 되는 영화',
        '혼자 심야에 보면 작은 소리에도 뒤를 돌아보게 되는 공포 영화입니다.', 69);
    ADD_COLLECTION('jinyoung06@pcwk.com', '소리 없이 조여오는 공포',
        '큰 소리보다 침묵과 긴장으로 압박하는 작품을 모았습니다.', 46);
    ADD_COLLECTION('jinyoung06@pcwk.com', '한국형 공포와 미스터리',
        '익숙한 풍경을 낯설고 섬뜩하게 만드는 한국 영화 중심 목록입니다.', 17);

    -- jinyoung07: 3 + 빈 컬렉션 1
    ADD_COLLECTION('jinyoung07@pcwk.com', '어른도 울리는 애니메이션',
        '아이와 어른 모두에게 오래 남는 감정을 전하는 애니메이션입니다.', 76);
    ADD_COLLECTION('jinyoung07@pcwk.com', '픽사와 함께 자란 시간',
        '어린 시절부터 지금까지 함께해 온 픽사 작품을 모았습니다.', 51);
    ADD_COLLECTION('jinyoung07@pcwk.com', '온 가족 판타지 극장',
        '주말 저녁 가족과 편안하게 볼 수 있는 판타지 애니메이션입니다.', 24);
    ADD_COLLECTION('jinyoung07@pcwk.com', '다음에 볼 단편 애니',
        '아직 발견하지 못한 단편 애니메이션을 담아 둘 목록입니다.', 6);

    -- jinyoung08: 4
    ADD_COLLECTION('jinyoung08@pcwk.com', '작은 영화 큰 여운',
        '조용하지만 엔딩 이후 오랫동안 마음을 떠나지 않는 영화입니다.', 84);
    ADD_COLLECTION('jinyoung08@pcwk.com', '한국 영화의 날카로운 시선',
        '한국 사회와 인간을 예리하게 바라보는 작품을 골랐습니다.', 58);
    ADD_COLLECTION('jinyoung08@pcwk.com', '영화제에서 만난 보석',
        '낯선 형식과 새로운 얼굴을 만날 수 있는 영화제 화제작입니다.', 39);
    ADD_COLLECTION('jinyoung08@pcwk.com', '음악과 리듬으로 읽는 영화',
        '대사만큼 음악과 리듬이 중요한 영화를 모았습니다.', 16);

    -- jinyoung09: 4 + 빈 컬렉션 1
    ADD_COLLECTION('jinyoung09@pcwk.com', '우주와 시간의 경계',
        '우주를 배경으로 시간과 인간의 선택을 질문하는 SF입니다.', 86);
    ADD_COLLECTION('jinyoung09@pcwk.com', '가상현실과 인간성',
        '기술이 인간의 정체성과 현실 감각을 흔드는 작품입니다.', 64);
    ADD_COLLECTION('jinyoung09@pcwk.com', '중간계와 마법학교',
        '중간계의 여정과 마법학교의 추억을 함께 정주행합니다.', 43);
    ADD_COLLECTION('jinyoung09@pcwk.com', '거대한 세계관 정주행',
        '여러 편에 걸쳐 확장되는 세계관을 순서대로 탐험합니다.', 22);
    ADD_COLLECTION('jinyoung09@pcwk.com', '아직 열지 않은 평행우주',
        '앞으로 발견할 멀티버스와 평행우주 작품을 위한 자리입니다.', 2);

    -- jinyoung10: 2
    ADD_COLLECTION('jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화',
        '공개 소식만으로도 가장 먼저 확인하고 싶은 신작 목록입니다.', 33);
    ADD_COLLECTION('jinyoung10@pcwk.com', '2025 화제작 체크리스트',
        '2025년 화제가 된 작품을 놓치지 않기 위한 체크리스트입니다.', 11);

    -- ================================================================
    -- 1-3. COLLECTION_ITEM: 작품 포함 컬렉션 36건
    -- ================================================================

    -- jinyoung01 / 마블캐처
    ADD_ITEM('jinyoung01@pcwk.com', 'MCU 입문자를 위한 핵심 6편', 80, 81);
    ADD_ITEM('jinyoung01@pcwk.com', 'MCU 입문자를 위한 핵심 6편', 21, 80);
    ADD_ITEM('jinyoung01@pcwk.com', 'MCU 입문자를 위한 핵심 6편', 448, 79);
    ADD_ITEM('jinyoung01@pcwk.com', 'MCU 입문자를 위한 핵심 6편', 213, 78);
    ADD_ITEM('jinyoung01@pcwk.com', 'MCU 입문자를 위한 핵심 6편', 19, 77);
    ADD_ITEM('jinyoung01@pcwk.com', 'MCU 입문자를 위한 핵심 6편', 40, 76);
    ADD_ITEM('jinyoung01@pcwk.com', '어벤져스 결전의 흐름', 21, 67);
    ADD_ITEM('jinyoung01@pcwk.com', '어벤져스 결전의 흐름', 93, 66);
    ADD_ITEM('jinyoung01@pcwk.com', '어벤져스 결전의 흐름', 19, 65);
    ADD_ITEM('jinyoung01@pcwk.com', '어벤져스 결전의 흐름', 40, 64);
    ADD_ITEM('jinyoung01@pcwk.com', '토르의 유쾌한 우주 여행', 289, 48);
    ADD_ITEM('jinyoung01@pcwk.com', '토르의 유쾌한 우주 여행', 175, 47);
    ADD_ITEM('jinyoung01@pcwk.com', '토르의 유쾌한 우주 여행', 282, 46);
    ADD_ITEM('jinyoung01@pcwk.com', '토르의 유쾌한 우주 여행', 448, 45);
    ADD_ITEM('jinyoung01@pcwk.com', '토르의 유쾌한 우주 여행', 202, 44);
    ADD_ITEM('jinyoung01@pcwk.com', '토르의 유쾌한 우주 여행', 76, 43);
    ADD_ITEM('jinyoung01@pcwk.com', '스파이더맨 세대 교차', 371, 30);
    ADD_ITEM('jinyoung01@pcwk.com', '스파이더맨 세대 교차', 12, 29);
    ADD_ITEM('jinyoung01@pcwk.com', '스파이더맨 세대 교차', 1, 28);

    -- jinyoung02 / 고전멜로수집가
    ADD_ITEM('jinyoung02@pcwk.com', '시간을 건너온 멜로', 88, 90);
    ADD_ITEM('jinyoung02@pcwk.com', '시간을 건너온 멜로', 423, 89);
    ADD_ITEM('jinyoung02@pcwk.com', '시간을 건너온 멜로', 302, 88);
    ADD_ITEM('jinyoung02@pcwk.com', '시간을 건너온 멜로', 212, 87);
    ADD_ITEM('jinyoung02@pcwk.com', '시간을 건너온 멜로', 560, 86);
    ADD_ITEM('jinyoung02@pcwk.com', '음악과 사랑이 남는 밤', 231, 73);
    ADD_ITEM('jinyoung02@pcwk.com', '음악과 사랑이 남는 밤', 1186, 72);
    ADD_ITEM('jinyoung02@pcwk.com', '음악과 사랑이 남는 밤', 560, 71);
    ADD_ITEM('jinyoung02@pcwk.com', '음악과 사랑이 남는 밤', 590, 70);
    ADD_ITEM('jinyoung02@pcwk.com', '음악과 사랑이 남는 밤', 212, 69);
    ADD_ITEM('jinyoung02@pcwk.com', '눈물 버튼 로맨스', 88, 52);
    ADD_ITEM('jinyoung02@pcwk.com', '눈물 버튼 로맨스', 212, 51);
    ADD_ITEM('jinyoung02@pcwk.com', '눈물 버튼 로맨스', 590, 50);
    ADD_ITEM('jinyoung02@pcwk.com', '눈물 버튼 로맨스', 807, 49);
    ADD_ITEM('jinyoung02@pcwk.com', '눈물 버튼 로맨스', 1318, 48);
    ADD_ITEM('jinyoung02@pcwk.com', '오래 기억할 사랑의 장면', 423, 27);
    ADD_ITEM('jinyoung02@pcwk.com', '오래 기억할 사랑의 장면', 302, 26);
    ADD_ITEM('jinyoung02@pcwk.com', '오래 기억할 사랑의 장면', 231, 25);
    ADD_ITEM('jinyoung02@pcwk.com', '오래 기억할 사랑의 장면', 560, 24);
    ADD_ITEM('jinyoung02@pcwk.com', '오래 기억할 사랑의 장면', 807, 23);

    -- jinyoung03 / 액션직진러
    ADD_ITEM('jinyoung03@pcwk.com', '질주 본능 액션', 97, 78);
    ADD_ITEM('jinyoung03@pcwk.com', '질주 본능 액션', 236, 77);
    ADD_ITEM('jinyoung03@pcwk.com', '질주 본능 액션', 515, 76);
    ADD_ITEM('jinyoung03@pcwk.com', '질주 본능 액션', 655, 75);
    ADD_ITEM('jinyoung03@pcwk.com', '질주 본능 액션', 148, 74);
    ADD_ITEM('jinyoung03@pcwk.com', '첩보 액션 정주행', 1017, 60);
    ADD_ITEM('jinyoung03@pcwk.com', '첩보 액션 정주행', 406, 59);
    ADD_ITEM('jinyoung03@pcwk.com', '첩보 액션 정주행', 430, 58);
    ADD_ITEM('jinyoung03@pcwk.com', '첩보 액션 정주행', 242, 57);
    ADD_ITEM('jinyoung03@pcwk.com', '첩보 액션 정주행', 571, 56);
    ADD_ITEM('jinyoung03@pcwk.com', '인간 병기와 추격전', 242, 41);
    ADD_ITEM('jinyoung03@pcwk.com', '인간 병기와 추격전', 346, 40);
    ADD_ITEM('jinyoung03@pcwk.com', '인간 병기와 추격전', 521, 39);
    ADD_ITEM('jinyoung03@pcwk.com', '인간 병기와 추격전', 603, 38);
    ADD_ITEM('jinyoung03@pcwk.com', '인간 병기와 추격전', 515, 37);
    ADD_ITEM('jinyoung03@pcwk.com', '전장의 영웅들', 519, 18);
    ADD_ITEM('jinyoung03@pcwk.com', '전장의 영웅들', 515, 17);
    ADD_ITEM('jinyoung03@pcwk.com', '전장의 영웅들', 97, 16);
    ADD_ITEM('jinyoung03@pcwk.com', '전장의 영웅들', 1013, 15);
    ADD_ITEM('jinyoung03@pcwk.com', '전장의 영웅들', 93, 14);

    -- jinyoung04 / 컬렉션장인
    ADD_ITEM('jinyoung04@pcwk.com', '세 시간이 아깝지 않은 대작', 35, 100);
    ADD_ITEM('jinyoung04@pcwk.com', '세 시간이 아깝지 않은 대작', 510, 99);
    ADD_ITEM('jinyoung04@pcwk.com', '세 시간이 아깝지 않은 대작', 64, 98);
    ADD_ITEM('jinyoung04@pcwk.com', '세 시간이 아깝지 않은 대작', 65, 97);
    ADD_ITEM('jinyoung04@pcwk.com', '세 시간이 아깝지 않은 대작', 117, 96);
    ADD_ITEM('jinyoung04@pcwk.com', '감독의 세계가 선명한 영화', 520, 87);
    ADD_ITEM('jinyoung04@pcwk.com', '감독의 세계가 선명한 영화', 333, 86);
    ADD_ITEM('jinyoung04@pcwk.com', '감독의 세계가 선명한 영화', 35, 85);
    ADD_ITEM('jinyoung04@pcwk.com', '감독의 세계가 선명한 영화', 95, 84);
    ADD_ITEM('jinyoung04@pcwk.com', '감독의 세계가 선명한 영화', 771, 83);
    ADD_ITEM('jinyoung04@pcwk.com', '감독의 세계가 선명한 영화', 532, 82);
    ADD_ITEM('jinyoung04@pcwk.com', '장르별 인생 영화 선반', 40, 71);
    ADD_ITEM('jinyoung04@pcwk.com', '장르별 인생 영화 선반', 231, 70);
    ADD_ITEM('jinyoung04@pcwk.com', '장르별 인생 영화 선반', 514, 69);
    ADD_ITEM('jinyoung04@pcwk.com', '장르별 인생 영화 선반', 520, 68);
    ADD_ITEM('jinyoung04@pcwk.com', '장르별 인생 영화 선반', 82, 67);
    ADD_ITEM('jinyoung04@pcwk.com', '장르별 인생 영화 선반', 512, 66);
    ADD_ITEM('jinyoung04@pcwk.com', '다시 보면 더 좋은 영화', 560, 53);
    ADD_ITEM('jinyoung04@pcwk.com', '다시 보면 더 좋은 영화', 423, 52);
    ADD_ITEM('jinyoung04@pcwk.com', '다시 보면 더 좋은 영화', 35, 51);
    ADD_ITEM('jinyoung04@pcwk.com', '다시 보면 더 좋은 영화', 520, 50);
    ADD_ITEM('jinyoung04@pcwk.com', '다시 보면 더 좋은 영화', 512, 49);
    ADD_ITEM('jinyoung04@pcwk.com', '다시 보면 더 좋은 영화', 333, 48);
    ADD_ITEM('jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈', 65, 36);
    ADD_ITEM('jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈', 117, 35);
    ADD_ITEM('jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈', 64, 34);
    ADD_ITEM('jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈', 77, 33);
    ADD_ITEM('jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈', 95, 32);
    ADD_ITEM('jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈', 512, 31);
    ADD_ITEM('jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈', 598, 30);
    ADD_ITEM('jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈', 581, 29);
    ADD_ITEM('jinyoung04@pcwk.com', '포스터만 봐도 설레는 영화', 231, 20);
    ADD_ITEM('jinyoung04@pcwk.com', '포스터만 봐도 설레는 영화', 190, 19);
    ADD_ITEM('jinyoung04@pcwk.com', '포스터만 봐도 설레는 영화', 95, 18);
    ADD_ITEM('jinyoung04@pcwk.com', '포스터만 봐도 설레는 영화', 132, 17);
    ADD_ITEM('jinyoung04@pcwk.com', '포스터만 봐도 설레는 영화', 148, 16);
    ADD_ITEM('jinyoung04@pcwk.com', '포스터만 봐도 설레는 영화', 87, 15);

    -- jinyoung05 / 댓글요정
    ADD_ITEM('jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화', 520, 62);
    ADD_ITEM('jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화', 540, 61);
    ADD_ITEM('jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화', 190, 60);
    ADD_ITEM('jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화', 510, 59);
    ADD_ITEM('jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화', 573, 58);
    ADD_ITEM('jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화', 231, 57);
    ADD_ITEM('jinyoung05@pcwk.com', '결말 토론이 필요한 작품', 35, 33);
    ADD_ITEM('jinyoung05@pcwk.com', '결말 토론이 필요한 작품', 512, 32);
    ADD_ITEM('jinyoung05@pcwk.com', '결말 토론이 필요한 작품', 560, 31);
    ADD_ITEM('jinyoung05@pcwk.com', '결말 토론이 필요한 작품', 771, 30);
    ADD_ITEM('jinyoung05@pcwk.com', '결말 토론이 필요한 작품', 548, 29);
    ADD_ITEM('jinyoung05@pcwk.com', '결말 토론이 필요한 작품', 520, 28);

    -- jinyoung06 / 심야스릴러
    ADD_ITEM('jinyoung06@pcwk.com', '불 끄고 보면 안 되는 영화', 522, 68);
    ADD_ITEM('jinyoung06@pcwk.com', '불 끄고 보면 안 되는 영화', 554, 67);
    ADD_ITEM('jinyoung06@pcwk.com', '불 끄고 보면 안 되는 영화', 538, 66);
    ADD_ITEM('jinyoung06@pcwk.com', '불 끄고 보면 안 되는 영화', 548, 65);
    ADD_ITEM('jinyoung06@pcwk.com', '불 끄고 보면 안 되는 영화', 584, 64);
    ADD_ITEM('jinyoung06@pcwk.com', '불 끄고 보면 안 되는 영화', 573, 63);
    ADD_ITEM('jinyoung06@pcwk.com', '소리 없이 조여오는 공포', 290, 45);
    ADD_ITEM('jinyoung06@pcwk.com', '소리 없이 조여오는 공포', 391, 44);
    ADD_ITEM('jinyoung06@pcwk.com', '소리 없이 조여오는 공포', 938, 43);
    ADD_ITEM('jinyoung06@pcwk.com', '소리 없이 조여오는 공포', 529, 42);
    ADD_ITEM('jinyoung06@pcwk.com', '소리 없이 조여오는 공포', 568, 41);
    ADD_ITEM('jinyoung06@pcwk.com', '소리 없이 조여오는 공포', 576, 40);
    ADD_ITEM('jinyoung06@pcwk.com', '한국형 공포와 미스터리', 771, 16);
    ADD_ITEM('jinyoung06@pcwk.com', '한국형 공포와 미스터리', 333, 15);
    ADD_ITEM('jinyoung06@pcwk.com', '한국형 공포와 미스터리', 520, 14);
    ADD_ITEM('jinyoung06@pcwk.com', '한국형 공포와 미스터리', 573, 13);
    ADD_ITEM('jinyoung06@pcwk.com', '한국형 공포와 미스터리', 548, 12);

    -- jinyoung07 / 애니프레임
    ADD_ITEM('jinyoung07@pcwk.com', '어른도 울리는 애니메이션', 92, 75);
    ADD_ITEM('jinyoung07@pcwk.com', '어른도 울리는 애니메이션', 147, 74);
    ADD_ITEM('jinyoung07@pcwk.com', '어른도 울리는 애니메이션', 211, 73);
    ADD_ITEM('jinyoung07@pcwk.com', '어른도 울리는 애니메이션', 153, 72);
    ADD_ITEM('jinyoung07@pcwk.com', '어른도 울리는 애니메이션', 82, 71);
    ADD_ITEM('jinyoung07@pcwk.com', '어른도 울리는 애니메이션', 126, 70);
    ADD_ITEM('jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 94, 50);
    ADD_ITEM('jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 180, 49);
    ADD_ITEM('jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 6, 48);
    ADD_ITEM('jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 147, 47);
    ADD_ITEM('jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 132, 46);
    ADD_ITEM('jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 166, 45);
    ADD_ITEM('jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 153, 44);
    ADD_ITEM('jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 126, 43);
    ADD_ITEM('jinyoung07@pcwk.com', '온 가족 판타지 극장', 177, 23);
    ADD_ITEM('jinyoung07@pcwk.com', '온 가족 판타지 극장', 118, 22);
    ADD_ITEM('jinyoung07@pcwk.com', '온 가족 판타지 극장', 98, 21);
    ADD_ITEM('jinyoung07@pcwk.com', '온 가족 판타지 극장', 29, 20);
    ADD_ITEM('jinyoung07@pcwk.com', '온 가족 판타지 극장', 115, 19);
    ADD_ITEM('jinyoung07@pcwk.com', '온 가족 판타지 극장', 445, 18);
    ADD_ITEM('jinyoung07@pcwk.com', '온 가족 판타지 극장', 82, 17);

    -- jinyoung08 / 인디시네마
    ADD_ITEM('jinyoung08@pcwk.com', '작은 영화 큰 여운', 984, 83);
    ADD_ITEM('jinyoung08@pcwk.com', '작은 영화 큰 여운', 1301, 82);
    ADD_ITEM('jinyoung08@pcwk.com', '작은 영화 큰 여운', 1318, 81);
    ADD_ITEM('jinyoung08@pcwk.com', '작은 영화 큰 여운', 807, 80);
    ADD_ITEM('jinyoung08@pcwk.com', '작은 영화 큰 여운', 1025, 79);
    ADD_ITEM('jinyoung08@pcwk.com', '작은 영화 큰 여운', 1088, 78);
    ADD_ITEM('jinyoung08@pcwk.com', '한국 영화의 날카로운 시선', 520, 57);
    ADD_ITEM('jinyoung08@pcwk.com', '한국 영화의 날카로운 시선', 333, 56);
    ADD_ITEM('jinyoung08@pcwk.com', '한국 영화의 날카로운 시선', 771, 55);
    ADD_ITEM('jinyoung08@pcwk.com', '한국 영화의 날카로운 시선', 578, 54);
    ADD_ITEM('jinyoung08@pcwk.com', '한국 영화의 날카로운 시선', 540, 53);
    ADD_ITEM('jinyoung08@pcwk.com', '영화제에서 만난 보석', 1088, 38);
    ADD_ITEM('jinyoung08@pcwk.com', '영화제에서 만난 보석', 807, 37);
    ADD_ITEM('jinyoung08@pcwk.com', '영화제에서 만난 보석', 1318, 36);
    ADD_ITEM('jinyoung08@pcwk.com', '영화제에서 만난 보석', 984, 35);
    ADD_ITEM('jinyoung08@pcwk.com', '영화제에서 만난 보석', 540, 34);
    ADD_ITEM('jinyoung08@pcwk.com', '영화제에서 만난 보석', 1025, 33);
    ADD_ITEM('jinyoung08@pcwk.com', '음악과 리듬으로 읽는 영화', 514, 15);
    ADD_ITEM('jinyoung08@pcwk.com', '음악과 리듬으로 읽는 영화', 231, 14);
    ADD_ITEM('jinyoung08@pcwk.com', '음악과 리듬으로 읽는 영화', 1186, 13);
    ADD_ITEM('jinyoung08@pcwk.com', '음악과 리듬으로 읽는 영화', 211, 12);
    ADD_ITEM('jinyoung08@pcwk.com', '음악과 리듬으로 읽는 영화', 540, 11);

    -- jinyoung09 / 세계관탐험가
    ADD_ITEM('jinyoung09@pcwk.com', '우주와 시간의 경계', 35, 85);
    ADD_ITEM('jinyoung09@pcwk.com', '우주와 시간의 경계', 77, 84);
    ADD_ITEM('jinyoung09@pcwk.com', '우주와 시간의 경계', 95, 83);
    ADD_ITEM('jinyoung09@pcwk.com', '우주와 시간의 경계', 748, 82);
    ADD_ITEM('jinyoung09@pcwk.com', '우주와 시간의 경계', 532, 81);
    ADD_ITEM('jinyoung09@pcwk.com', '우주와 시간의 경계', 545, 80);
    ADD_ITEM('jinyoung09@pcwk.com', '가상현실과 인간성', 512, 63);
    ADD_ITEM('jinyoung09@pcwk.com', '가상현실과 인간성', 598, 62);
    ADD_ITEM('jinyoung09@pcwk.com', '가상현실과 인간성', 581, 61);
    ADD_ITEM('jinyoung09@pcwk.com', '가상현실과 인간성', 540, 60);
    ADD_ITEM('jinyoung09@pcwk.com', '가상현실과 인간성', 1086, 59);
    ADD_ITEM('jinyoung09@pcwk.com', '중간계와 마법학교', 65, 42);
    ADD_ITEM('jinyoung09@pcwk.com', '중간계와 마법학교', 117, 41);
    ADD_ITEM('jinyoung09@pcwk.com', '중간계와 마법학교', 64, 40);
    ADD_ITEM('jinyoung09@pcwk.com', '중간계와 마법학교', 105, 39);
    ADD_ITEM('jinyoung09@pcwk.com', '중간계와 마법학교', 1002, 38);
    ADD_ITEM('jinyoung09@pcwk.com', '거대한 세계관 정주행', 77, 21);
    ADD_ITEM('jinyoung09@pcwk.com', '거대한 세계관 정주행', 95, 20);
    ADD_ITEM('jinyoung09@pcwk.com', '거대한 세계관 정주행', 748, 19);
    ADD_ITEM('jinyoung09@pcwk.com', '거대한 세계관 정주행', 65, 18);
    ADD_ITEM('jinyoung09@pcwk.com', '거대한 세계관 정주행', 117, 17);
    ADD_ITEM('jinyoung09@pcwk.com', '거대한 세계관 정주행', 64, 16);
    ADD_ITEM('jinyoung09@pcwk.com', '거대한 세계관 정주행', 512, 15);
    ADD_ITEM('jinyoung09@pcwk.com', '거대한 세계관 정주행', 598, 14);
    ADD_ITEM('jinyoung09@pcwk.com', '거대한 세계관 정주행', 581, 13);

    -- jinyoung10 / 개봉작헌터
    ADD_ITEM('jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화', 1, 32);
    ADD_ITEM('jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화', 6, 31);
    ADD_ITEM('jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화', 748, 30);
    ADD_ITEM('jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화', 87, 29);
    ADD_ITEM('jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화', 59, 28);
    ADD_ITEM('jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화', 29, 27);
    ADD_ITEM('jinyoung10@pcwk.com', '2025 화제작 체크리스트', 49, 10);
    ADD_ITEM('jinyoung10@pcwk.com', '2025 화제작 체크리스트', 538, 9);
    ADD_ITEM('jinyoung10@pcwk.com', '2025 화제작 체크리스트', 578, 8);
    ADD_ITEM('jinyoung10@pcwk.com', '2025 화제작 체크리스트', 87, 7);
    ADD_ITEM('jinyoung10@pcwk.com', '2025 화제작 체크리스트', 59, 6);
    ADD_ITEM('jinyoung10@pcwk.com', '2025 화제작 체크리스트', 148, 5);

    -- ================================================================
    -- 1-4. MEMBER_CONTENT: 회원별 평가 84건 + 보고싶어요 42건
    -- ================================================================

    -- jinyoung01 / 마블캐처: 평가 8 + 보고싶어요 4
    ADD_RECORD('jinyoung01@pcwk.com', 80, 5, 'N', 76);
    ADD_RECORD('jinyoung01@pcwk.com', 21, 4, 'N', 70);
    ADD_RECORD('jinyoung01@pcwk.com', 448, 5, 'N', 61);
    ADD_RECORD('jinyoung01@pcwk.com', 213, 4, 'N', 53);
    ADD_RECORD('jinyoung01@pcwk.com', 19, 5, 'N', 42);
    ADD_RECORD('jinyoung01@pcwk.com', 40, 5, 'N', 31);
    ADD_RECORD('jinyoung01@pcwk.com', 175, 4, 'N', 19);
    ADD_RECORD('jinyoung01@pcwk.com', 12, 4, 'N', 8);
    ADD_RECORD('jinyoung01@pcwk.com', 1, NULL, 'Y', 7);
    ADD_RECORD('jinyoung01@pcwk.com', 49, NULL, 'Y', 6);
    ADD_RECORD('jinyoung01@pcwk.com', 59, NULL, 'Y', 4);
    ADD_RECORD('jinyoung01@pcwk.com', 87, NULL, 'Y', 2);

    -- jinyoung02 / 고전멜로수집가
    ADD_RECORD('jinyoung02@pcwk.com', 88, 5, 'N', 83);
    ADD_RECORD('jinyoung02@pcwk.com', 423, 5, 'N', 75);
    ADD_RECORD('jinyoung02@pcwk.com', 302, 4, 'N', 66);
    ADD_RECORD('jinyoung02@pcwk.com', 212, 4, 'N', 58);
    ADD_RECORD('jinyoung02@pcwk.com', 560, 5, 'N', 44);
    ADD_RECORD('jinyoung02@pcwk.com', 231, 4, 'N', 33);
    ADD_RECORD('jinyoung02@pcwk.com', 590, 4, 'N', 21);
    ADD_RECORD('jinyoung02@pcwk.com', 807, 5, 'N', 9);
    ADD_RECORD('jinyoung02@pcwk.com', 1186, NULL, 'Y', 8);
    ADD_RECORD('jinyoung02@pcwk.com', 1318, NULL, 'Y', 6);
    ADD_RECORD('jinyoung02@pcwk.com', 1025, NULL, 'Y', 4);
    ADD_RECORD('jinyoung02@pcwk.com', 540, NULL, 'Y', 1);

    -- jinyoung03 / 액션직진러
    ADD_RECORD('jinyoung03@pcwk.com', 97, 5, 'N', 74);
    ADD_RECORD('jinyoung03@pcwk.com', 236, 4, 'N', 68);
    ADD_RECORD('jinyoung03@pcwk.com', 515, 5, 'N', 60);
    ADD_RECORD('jinyoung03@pcwk.com', 655, 4, 'N', 51);
    ADD_RECORD('jinyoung03@pcwk.com', 1017, 5, 'N', 39);
    ADD_RECORD('jinyoung03@pcwk.com', 406, 4, 'N', 27);
    ADD_RECORD('jinyoung03@pcwk.com', 242, 4, 'N', 16);
    ADD_RECORD('jinyoung03@pcwk.com', 521, 5, 'N', 7);
    ADD_RECORD('jinyoung03@pcwk.com', 148, NULL, 'Y', 6);
    ADD_RECORD('jinyoung03@pcwk.com', 571, NULL, 'Y', 5);
    ADD_RECORD('jinyoung03@pcwk.com', 346, NULL, 'Y', 3);
    ADD_RECORD('jinyoung03@pcwk.com', 519, NULL, 'Y', 1);

    -- jinyoung04 / 컬렉션장인: 평가 12 + 보고싶어요 6
    ADD_RECORD('jinyoung04@pcwk.com', 35, 5, 'N', 97);
    ADD_RECORD('jinyoung04@pcwk.com', 510, 5, 'N', 90);
    ADD_RECORD('jinyoung04@pcwk.com', 64, 5, 'N', 84);
    ADD_RECORD('jinyoung04@pcwk.com', 65, 5, 'N', 78);
    ADD_RECORD('jinyoung04@pcwk.com', 117, 5, 'N', 71);
    ADD_RECORD('jinyoung04@pcwk.com', 520, 5, 'N', 63);
    ADD_RECORD('jinyoung04@pcwk.com', 333, 5, 'N', 52);
    ADD_RECORD('jinyoung04@pcwk.com', 95, 4, 'N', 43);
    ADD_RECORD('jinyoung04@pcwk.com', 771, 4, 'N', 34);
    ADD_RECORD('jinyoung04@pcwk.com', 532, 5, 'N', 25);
    ADD_RECORD('jinyoung04@pcwk.com', 514, 5, 'N', 14);
    ADD_RECORD('jinyoung04@pcwk.com', 82, 5, 'N', 5);
    ADD_RECORD('jinyoung04@pcwk.com', 1, NULL, 'Y', 9);
    ADD_RECORD('jinyoung04@pcwk.com', 6, NULL, 'Y', 8);
    ADD_RECORD('jinyoung04@pcwk.com', 748, NULL, 'Y', 7);
    ADD_RECORD('jinyoung04@pcwk.com', 29, NULL, 'Y', 4);
    ADD_RECORD('jinyoung04@pcwk.com', 87, NULL, 'Y', 3);
    ADD_RECORD('jinyoung04@pcwk.com', 59, NULL, 'Y', 1);

    -- jinyoung05 / 댓글요정
    ADD_RECORD('jinyoung05@pcwk.com', 520, 5, 'N', 69);
    ADD_RECORD('jinyoung05@pcwk.com', 540, 4, 'N', 62);
    ADD_RECORD('jinyoung05@pcwk.com', 190, 4, 'N', 54);
    ADD_RECORD('jinyoung05@pcwk.com', 510, 5, 'N', 46);
    ADD_RECORD('jinyoung05@pcwk.com', 573, 4, 'N', 37);
    ADD_RECORD('jinyoung05@pcwk.com', 231, 4, 'N', 28);
    ADD_RECORD('jinyoung05@pcwk.com', 35, 5, 'N', 17);
    ADD_RECORD('jinyoung05@pcwk.com', 512, 5, 'N', 8);
    ADD_RECORD('jinyoung05@pcwk.com', 548, NULL, 'Y', 7);
    ADD_RECORD('jinyoung05@pcwk.com', 771, NULL, 'Y', 5);
    ADD_RECORD('jinyoung05@pcwk.com', 560, NULL, 'Y', 3);
    ADD_RECORD('jinyoung05@pcwk.com', 1318, NULL, 'Y', 1);

    -- jinyoung06 / 심야스릴러
    ADD_RECORD('jinyoung06@pcwk.com', 522, 5, 'N', 67);
    ADD_RECORD('jinyoung06@pcwk.com', 554, 4, 'N', 59);
    ADD_RECORD('jinyoung06@pcwk.com', 548, 5, 'N', 51);
    ADD_RECORD('jinyoung06@pcwk.com', 584, 4, 'N', 43);
    ADD_RECORD('jinyoung06@pcwk.com', 573, 5, 'N', 35);
    ADD_RECORD('jinyoung06@pcwk.com', 290, 4, 'N', 26);
    ADD_RECORD('jinyoung06@pcwk.com', 391, 4, 'N', 17);
    ADD_RECORD('jinyoung06@pcwk.com', 529, 5, 'N', 8);
    ADD_RECORD('jinyoung06@pcwk.com', 538, NULL, 'Y', 7);
    ADD_RECORD('jinyoung06@pcwk.com', 938, NULL, 'Y', 5);
    ADD_RECORD('jinyoung06@pcwk.com', 568, NULL, 'Y', 3);
    ADD_RECORD('jinyoung06@pcwk.com', 576, NULL, 'Y', 1);

    -- jinyoung07 / 애니프레임
    ADD_RECORD('jinyoung07@pcwk.com', 92, 5, 'N', 73);
    ADD_RECORD('jinyoung07@pcwk.com', 147, 5, 'N', 65);
    ADD_RECORD('jinyoung07@pcwk.com', 211, 5, 'N', 57);
    ADD_RECORD('jinyoung07@pcwk.com', 153, 5, 'N', 49);
    ADD_RECORD('jinyoung07@pcwk.com', 82, 5, 'N', 40);
    ADD_RECORD('jinyoung07@pcwk.com', 126, 4, 'N', 31);
    ADD_RECORD('jinyoung07@pcwk.com', 94, 5, 'N', 20);
    ADD_RECORD('jinyoung07@pcwk.com', 166, 5, 'N', 8);
    ADD_RECORD('jinyoung07@pcwk.com', 6, NULL, 'Y', 7);
    ADD_RECORD('jinyoung07@pcwk.com', 132, NULL, 'Y', 5);
    ADD_RECORD('jinyoung07@pcwk.com', 29, NULL, 'Y', 3);
    ADD_RECORD('jinyoung07@pcwk.com', 118, NULL, 'Y', 1);

    -- jinyoung08 / 인디시네마
    ADD_RECORD('jinyoung08@pcwk.com', 984, 5, 'N', 79);
    ADD_RECORD('jinyoung08@pcwk.com', 1301, 5, 'N', 70);
    ADD_RECORD('jinyoung08@pcwk.com', 1318, 5, 'N', 62);
    ADD_RECORD('jinyoung08@pcwk.com', 807, 5, 'N', 53);
    ADD_RECORD('jinyoung08@pcwk.com', 1025, 4, 'N', 44);
    ADD_RECORD('jinyoung08@pcwk.com', 1088, 5, 'N', 34);
    ADD_RECORD('jinyoung08@pcwk.com', 520, 5, 'N', 22);
    ADD_RECORD('jinyoung08@pcwk.com', 333, 5, 'N', 9);
    ADD_RECORD('jinyoung08@pcwk.com', 540, NULL, 'Y', 8);
    ADD_RECORD('jinyoung08@pcwk.com', 578, NULL, 'Y', 6);
    ADD_RECORD('jinyoung08@pcwk.com', 190, NULL, 'Y', 3);
    ADD_RECORD('jinyoung08@pcwk.com', 1186, NULL, 'Y', 1);

    -- jinyoung09 / 세계관탐험가
    ADD_RECORD('jinyoung09@pcwk.com', 35, 5, 'N', 81);
    ADD_RECORD('jinyoung09@pcwk.com', 77, 5, 'N', 72);
    ADD_RECORD('jinyoung09@pcwk.com', 95, 5, 'N', 63);
    ADD_RECORD('jinyoung09@pcwk.com', 532, 5, 'N', 54);
    ADD_RECORD('jinyoung09@pcwk.com', 545, 4, 'N', 45);
    ADD_RECORD('jinyoung09@pcwk.com', 512, 5, 'N', 34);
    ADD_RECORD('jinyoung09@pcwk.com', 598, 4, 'N', 22);
    ADD_RECORD('jinyoung09@pcwk.com', 65, 5, 'N', 9);
    ADD_RECORD('jinyoung09@pcwk.com', 748, NULL, 'Y', 8);
    ADD_RECORD('jinyoung09@pcwk.com', 581, NULL, 'Y', 6);
    ADD_RECORD('jinyoung09@pcwk.com', 1002, NULL, 'Y', 3);
    ADD_RECORD('jinyoung09@pcwk.com', 105, NULL, 'Y', 1);

    -- jinyoung10 / 개봉작헌터
    ADD_RECORD('jinyoung10@pcwk.com', 49, 3, 'N', 65);
    ADD_RECORD('jinyoung10@pcwk.com', 538, 4, 'N', 57);
    ADD_RECORD('jinyoung10@pcwk.com', 578, 4, 'N', 48);
    ADD_RECORD('jinyoung10@pcwk.com', 87, 4, 'N', 39);
    ADD_RECORD('jinyoung10@pcwk.com', 59, 4, 'N', 31);
    ADD_RECORD('jinyoung10@pcwk.com', 148, 5, 'N', 22);
    ADD_RECORD('jinyoung10@pcwk.com', 190, 4, 'N', 13);
    ADD_RECORD('jinyoung10@pcwk.com', 510, 5, 'N', 5);
    ADD_RECORD('jinyoung10@pcwk.com', 1, NULL, 'Y', 4);
    ADD_RECORD('jinyoung10@pcwk.com', 6, NULL, 'Y', 3);
    ADD_RECORD('jinyoung10@pcwk.com', 748, NULL, 'Y', 2);
    ADD_RECORD('jinyoung10@pcwk.com', 29, NULL, 'Y', 1);

    -- ================================================================
    -- 1-5. PERSON_LIKE: 일반 회원별 인물 좋아요
    -- ================================================================
    ADD_PERSON_LIKE('jinyoung01@pcwk.com', 190, 46);   -- 로버트 다우니 주니어
    ADD_PERSON_LIKE('jinyoung01@pcwk.com', 191, 39);   -- 크리스 에반스
    ADD_PERSON_LIKE('jinyoung01@pcwk.com', 1, 31);     -- 톰 홀랜드
    ADD_PERSON_LIKE('jinyoung01@pcwk.com', 194, 18);   -- 스칼렛 요한슨
    ADD_PERSON_LIKE('jinyoung01@pcwk.com', 761, 6);    -- 엠마 스톤

    ADD_PERSON_LIKE('jinyoung02@pcwk.com', 134, 52);   -- 라이언 고슬링
    ADD_PERSON_LIKE('jinyoung02@pcwk.com', 761, 43);   -- 엠마 스톤
    ADD_PERSON_LIKE('jinyoung02@pcwk.com', 245, 31);   -- 제임스 카메론
    ADD_PERSON_LIKE('jinyoung02@pcwk.com', 1559, 17);  -- 그레타 거윅
    ADD_PERSON_LIKE('jinyoung02@pcwk.com', 2524, 5);   -- 송강호

    ADD_PERSON_LIKE('jinyoung03@pcwk.com', 851, 49);   -- 톰 크루즈
    ADD_PERSON_LIKE('jinyoung03@pcwk.com', 1493, 38);  -- 키아누 리브스
    ADD_PERSON_LIKE('jinyoung03@pcwk.com', 1612, 27);  -- 맷 데이먼
    ADD_PERSON_LIKE('jinyoung03@pcwk.com', 3714, 15);  -- 조지 밀러
    ADD_PERSON_LIKE('jinyoung03@pcwk.com', 191, 4);    -- 크리스 에반스

    ADD_PERSON_LIKE('jinyoung04@pcwk.com', 358, 70);   -- 크리스토퍼 놀란
    ADD_PERSON_LIKE('jinyoung04@pcwk.com', 709, 60);   -- 드니 빌뇌브
    ADD_PERSON_LIKE('jinyoung04@pcwk.com', 2534, 50);  -- 봉준호
    ADD_PERSON_LIKE('jinyoung04@pcwk.com', 5631, 40);  -- 박찬욱
    ADD_PERSON_LIKE('jinyoung04@pcwk.com', 4552, 30);  -- 웨스 앤더슨
    ADD_PERSON_LIKE('jinyoung04@pcwk.com', 752, 20);   -- 미야자키 하야오
    ADD_PERSON_LIKE('jinyoung04@pcwk.com', 1492, 7);   -- 조던 필

    ADD_PERSON_LIKE('jinyoung05@pcwk.com', 761, 44);
    ADD_PERSON_LIKE('jinyoung05@pcwk.com', 134, 35);
    ADD_PERSON_LIKE('jinyoung05@pcwk.com', 703, 26);   -- 티모시 샬라메
    ADD_PERSON_LIKE('jinyoung05@pcwk.com', 2524, 15);
    ADD_PERSON_LIKE('jinyoung05@pcwk.com', 1492, 3);

    ADD_PERSON_LIKE('jinyoung06@pcwk.com', 1492, 42);
    ADD_PERSON_LIKE('jinyoung06@pcwk.com', 6593, 33);  -- 알프레드 히치콕
    ADD_PERSON_LIKE('jinyoung06@pcwk.com', 709, 24);
    ADD_PERSON_LIKE('jinyoung06@pcwk.com', 5631, 13);
    ADD_PERSON_LIKE('jinyoung06@pcwk.com', 2524, 2);

    ADD_PERSON_LIKE('jinyoung07@pcwk.com', 752, 41);
    ADD_PERSON_LIKE('jinyoung07@pcwk.com', 1559, 32);
    ADD_PERSON_LIKE('jinyoung07@pcwk.com', 761, 23);
    ADD_PERSON_LIKE('jinyoung07@pcwk.com', 134, 12);
    ADD_PERSON_LIKE('jinyoung07@pcwk.com', 703, 3);

    ADD_PERSON_LIKE('jinyoung08@pcwk.com', 2534, 43);
    ADD_PERSON_LIKE('jinyoung08@pcwk.com', 5631, 34);
    ADD_PERSON_LIKE('jinyoung08@pcwk.com', 4552, 25);
    ADD_PERSON_LIKE('jinyoung08@pcwk.com', 1559, 14);
    ADD_PERSON_LIKE('jinyoung08@pcwk.com', 2524, 2);

    ADD_PERSON_LIKE('jinyoung09@pcwk.com', 358, 40);
    ADD_PERSON_LIKE('jinyoung09@pcwk.com', 709, 31);
    ADD_PERSON_LIKE('jinyoung09@pcwk.com', 703, 22);
    ADD_PERSON_LIKE('jinyoung09@pcwk.com', 1493, 11);
    ADD_PERSON_LIKE('jinyoung09@pcwk.com', 752, 1);

    ADD_PERSON_LIKE('jinyoung10@pcwk.com', 703, 35);
    ADD_PERSON_LIKE('jinyoung10@pcwk.com', 761, 27);
    ADD_PERSON_LIKE('jinyoung10@pcwk.com', 358, 18);
    ADD_PERSON_LIKE('jinyoung10@pcwk.com', 2534, 9);
    ADD_PERSON_LIKE('jinyoung10@pcwk.com', 851, 1);

    -- ================================================================
    -- 1-6. COLLECTION_LIKE: 다른 회원 컬렉션 좋아요
    -- ================================================================
    ADD_COLLECTION_LIKE('jinyoung01@pcwk.com', 'jinyoung04@pcwk.com', '세 시간이 아깝지 않은 대작', 40);
    ADD_COLLECTION_LIKE('jinyoung01@pcwk.com', 'jinyoung09@pcwk.com', '우주와 시간의 경계', 32);
    ADD_COLLECTION_LIKE('jinyoung01@pcwk.com', 'jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화', 25);
    ADD_COLLECTION_LIKE('jinyoung01@pcwk.com', 'jinyoung03@pcwk.com', '전장의 영웅들', 17);
    ADD_COLLECTION_LIKE('jinyoung01@pcwk.com', 'jinyoung07@pcwk.com', '온 가족 판타지 극장', 9);
    ADD_COLLECTION_LIKE('jinyoung01@pcwk.com', 'jinyoung08@pcwk.com', '영화제에서 만난 보석', 2);

    ADD_COLLECTION_LIKE('jinyoung02@pcwk.com', 'jinyoung08@pcwk.com', '작은 영화 큰 여운', 43);
    ADD_COLLECTION_LIKE('jinyoung02@pcwk.com', 'jinyoung04@pcwk.com', '다시 보면 더 좋은 영화', 35);
    ADD_COLLECTION_LIKE('jinyoung02@pcwk.com', 'jinyoung07@pcwk.com', '어른도 울리는 애니메이션', 27);
    ADD_COLLECTION_LIKE('jinyoung02@pcwk.com', 'jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화', 18);
    ADD_COLLECTION_LIKE('jinyoung02@pcwk.com', 'jinyoung09@pcwk.com', '중간계와 마법학교', 8);
    ADD_COLLECTION_LIKE('jinyoung02@pcwk.com', 'jinyoung06@pcwk.com', '한국형 공포와 미스터리', 1);

    ADD_COLLECTION_LIKE('jinyoung03@pcwk.com', 'jinyoung01@pcwk.com', '어벤져스 결전의 흐름', 42);
    ADD_COLLECTION_LIKE('jinyoung03@pcwk.com', 'jinyoung10@pcwk.com', '2025 화제작 체크리스트', 10);
    ADD_COLLECTION_LIKE('jinyoung03@pcwk.com', 'jinyoung04@pcwk.com', '주말 몰아보기 프랜차이즈', 26);
    ADD_COLLECTION_LIKE('jinyoung03@pcwk.com', 'jinyoung06@pcwk.com', '소리 없이 조여오는 공포', 16);
    ADD_COLLECTION_LIKE('jinyoung03@pcwk.com', 'jinyoung09@pcwk.com', '거대한 세계관 정주행', 7);
    ADD_COLLECTION_LIKE('jinyoung03@pcwk.com', 'jinyoung07@pcwk.com', '온 가족 판타지 극장', 2);

    ADD_COLLECTION_LIKE('jinyoung04@pcwk.com', 'jinyoung01@pcwk.com', 'MCU 입문자를 위한 핵심 6편', 51);
    ADD_COLLECTION_LIKE('jinyoung04@pcwk.com', 'jinyoung02@pcwk.com', '시간을 건너온 멜로', 44);
    ADD_COLLECTION_LIKE('jinyoung04@pcwk.com', 'jinyoung03@pcwk.com', '첩보 액션 정주행', 37);
    ADD_COLLECTION_LIKE('jinyoung04@pcwk.com', 'jinyoung05@pcwk.com', '결말 토론이 필요한 작품', 29);
    ADD_COLLECTION_LIKE('jinyoung04@pcwk.com', 'jinyoung06@pcwk.com', '불 끄고 보면 안 되는 영화', 21);
    ADD_COLLECTION_LIKE('jinyoung04@pcwk.com', 'jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 13);
    ADD_COLLECTION_LIKE('jinyoung04@pcwk.com', 'jinyoung08@pcwk.com', '음악과 리듬으로 읽는 영화', 5);
    ADD_COLLECTION_LIKE('jinyoung04@pcwk.com', 'jinyoung10@pcwk.com', '2025 화제작 체크리스트', 1);

    ADD_COLLECTION_LIKE('jinyoung05@pcwk.com', 'jinyoung01@pcwk.com', '스파이더맨 세대 교차', 30);
    ADD_COLLECTION_LIKE('jinyoung05@pcwk.com', 'jinyoung02@pcwk.com', '오래 기억할 사랑의 장면', 27);
    ADD_COLLECTION_LIKE('jinyoung05@pcwk.com', 'jinyoung03@pcwk.com', '질주 본능 액션', 31);
    ADD_COLLECTION_LIKE('jinyoung05@pcwk.com', 'jinyoung04@pcwk.com', '감독의 세계가 선명한 영화', 23);
    ADD_COLLECTION_LIKE('jinyoung05@pcwk.com', 'jinyoung06@pcwk.com', '한국형 공포와 미스터리', 15);
    ADD_COLLECTION_LIKE('jinyoung05@pcwk.com', 'jinyoung07@pcwk.com', '어른도 울리는 애니메이션', 8);
    ADD_COLLECTION_LIKE('jinyoung05@pcwk.com', 'jinyoung08@pcwk.com', '작은 영화 큰 여운', 4);
    ADD_COLLECTION_LIKE('jinyoung05@pcwk.com', 'jinyoung09@pcwk.com', '가상현실과 인간성', 1);

    ADD_COLLECTION_LIKE('jinyoung06@pcwk.com', 'jinyoung05@pcwk.com', '결말 토론이 필요한 작품', 33);
    ADD_COLLECTION_LIKE('jinyoung06@pcwk.com', 'jinyoung08@pcwk.com', '한국 영화의 날카로운 시선', 31);
    ADD_COLLECTION_LIKE('jinyoung06@pcwk.com', 'jinyoung09@pcwk.com', '가상현실과 인간성', 23);
    ADD_COLLECTION_LIKE('jinyoung06@pcwk.com', 'jinyoung03@pcwk.com', '인간 병기와 추격전', 15);
    ADD_COLLECTION_LIKE('jinyoung06@pcwk.com', 'jinyoung04@pcwk.com', '다시 보면 더 좋은 영화', 7);
    ADD_COLLECTION_LIKE('jinyoung06@pcwk.com', 'jinyoung10@pcwk.com', '2025 화제작 체크리스트', 1);

    ADD_COLLECTION_LIKE('jinyoung07@pcwk.com', 'jinyoung02@pcwk.com', '눈물 버튼 로맨스', 38);
    ADD_COLLECTION_LIKE('jinyoung07@pcwk.com', 'jinyoung04@pcwk.com', '장르별 인생 영화 선반', 30);
    ADD_COLLECTION_LIKE('jinyoung07@pcwk.com', 'jinyoung08@pcwk.com', '작은 영화 큰 여운', 22);
    ADD_COLLECTION_LIKE('jinyoung07@pcwk.com', 'jinyoung09@pcwk.com', '중간계와 마법학교', 14);
    ADD_COLLECTION_LIKE('jinyoung07@pcwk.com', 'jinyoung01@pcwk.com', '토르의 유쾌한 우주 여행', 6);
    ADD_COLLECTION_LIKE('jinyoung07@pcwk.com', 'jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화', 1);

    ADD_COLLECTION_LIKE('jinyoung08@pcwk.com', 'jinyoung02@pcwk.com', '시간을 건너온 멜로', 41);
    ADD_COLLECTION_LIKE('jinyoung08@pcwk.com', 'jinyoung04@pcwk.com', '감독의 세계가 선명한 영화', 33);
    ADD_COLLECTION_LIKE('jinyoung08@pcwk.com', 'jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화', 25);
    ADD_COLLECTION_LIKE('jinyoung08@pcwk.com', 'jinyoung06@pcwk.com', '한국형 공포와 미스터리', 17);
    ADD_COLLECTION_LIKE('jinyoung08@pcwk.com', 'jinyoung09@pcwk.com', '우주와 시간의 경계', 9);
    ADD_COLLECTION_LIKE('jinyoung08@pcwk.com', 'jinyoung07@pcwk.com', '어른도 울리는 애니메이션', 1);

    ADD_COLLECTION_LIKE('jinyoung09@pcwk.com', 'jinyoung01@pcwk.com', 'MCU 입문자를 위한 핵심 6편', 36);
    ADD_COLLECTION_LIKE('jinyoung09@pcwk.com', 'jinyoung03@pcwk.com', '첩보 액션 정주행', 29);
    ADD_COLLECTION_LIKE('jinyoung09@pcwk.com', 'jinyoung04@pcwk.com', '세 시간이 아깝지 않은 대작', 21);
    ADD_COLLECTION_LIKE('jinyoung09@pcwk.com', 'jinyoung06@pcwk.com', '소리 없이 조여오는 공포', 13);
    ADD_COLLECTION_LIKE('jinyoung09@pcwk.com', 'jinyoung07@pcwk.com', '픽사와 함께 자란 시간', 6);
    ADD_COLLECTION_LIKE('jinyoung09@pcwk.com', 'jinyoung10@pcwk.com', '지금 극장에서 궁금한 영화', 1);

    ADD_COLLECTION_LIKE('jinyoung10@pcwk.com', 'jinyoung01@pcwk.com', '스파이더맨 세대 교차', 30);
    ADD_COLLECTION_LIKE('jinyoung10@pcwk.com', 'jinyoung03@pcwk.com', '질주 본능 액션', 25);
    ADD_COLLECTION_LIKE('jinyoung10@pcwk.com', 'jinyoung04@pcwk.com', '포스터만 봐도 설레는 영화', 18);
    ADD_COLLECTION_LIKE('jinyoung10@pcwk.com', 'jinyoung05@pcwk.com', '같이 이야기하고 싶은 영화', 11);
    ADD_COLLECTION_LIKE('jinyoung10@pcwk.com', 'jinyoung08@pcwk.com', '영화제에서 만난 보석', 5);
    ADD_COLLECTION_LIKE('jinyoung10@pcwk.com', 'jinyoung09@pcwk.com', '거대한 세계관 정주행', 1);

    -- ================================================================
    -- 1-7. USER_COMMENT: 콘텐츠 및 컬렉션 활동
    -- ================================================================
    ADD_CONTENT_COMMENT('jinyoung01@pcwk.com', 40,
        '긴 여정을 마무리하는 감정선과 전투의 균형이 좋았습니다.', 'N', 20);
    ADD_CONTENT_COMMENT('jinyoung01@pcwk.com', 175,
        '토르 영화 중 유머와 액션의 조합이 가장 마음에 듭니다.', 'N', 9);
    ADD_COLLECTION_COMMENT('jinyoung01@pcwk.com', 'jinyoung09@pcwk.com',
        '거대한 세계관 정주행', '시리즈 순서가 잘 보여서 주말 정주행에 참고하겠습니다.', 3);

    ADD_CONTENT_COMMENT('jinyoung02@pcwk.com', 423,
        '영화를 사랑하는 마음 자체를 아름답게 기억하게 하는 작품입니다.', 'N', 24);
    ADD_CONTENT_COMMENT('jinyoung02@pcwk.com', 560,
        '사랑과 기억을 다루는 방식이 볼 때마다 새롭게 다가옵니다.', 'N', 11);
    ADD_COLLECTION_COMMENT('jinyoung02@pcwk.com', 'jinyoung08@pcwk.com',
        '작은 영화 큰 여운', '조용한 날 천천히 보고 싶은 작품이 많네요.', 4);

    ADD_CONTENT_COMMENT('jinyoung03@pcwk.com', 97,
        '비행 장면의 속도감은 큰 화면에서 봐야 제대로 느껴집니다.', 'N', 19);
    ADD_CONTENT_COMMENT('jinyoung03@pcwk.com', 1017,
        '후반부로 갈수록 커지는 액션 규모가 만족스러웠습니다.', 'N', 8);
    ADD_COLLECTION_COMMENT('jinyoung03@pcwk.com', 'jinyoung04@pcwk.com',
        '주말 몰아보기 프랜차이즈', '순서대로 달리기 좋은 구성이네요. 이번 주말에 도전합니다.', 2);

    ADD_CONTENT_COMMENT('jinyoung04@pcwk.com', 35,
        '큰 화면과 긴 호흡이 필요한 영화지만 다시 볼수록 디테일이 보입니다.', 'N', 27);
    ADD_CONTENT_COMMENT('jinyoung04@pcwk.com', 520,
        '장르의 재미와 사회적인 시선이 함께 살아 있는 작품입니다.', 'N', 13);
    ADD_COLLECTION_COMMENT('jinyoung04@pcwk.com', 'jinyoung06@pcwk.com',
        '소리 없이 조여오는 공포', '공포를 소리와 침묵으로 나눈 구성이 인상적입니다.', 5);

    -- 댓글요정: 여러 콘텐츠와 다른 회원 컬렉션에 고르게 활동
    ADD_CONTENT_COMMENT('jinyoung05@pcwk.com', 520,
        '웃다가도 불편해지는 순간이 계속 남아서 이야깃거리가 많아요.', 'N', 38);
    ADD_CONTENT_COMMENT('jinyoung05@pcwk.com', 540,
        '정신없는 상상력 속에서도 가족 이야기가 중심을 잘 잡아 줍니다.', 'N', 33);
    ADD_CONTENT_COMMENT('jinyoung05@pcwk.com', 190,
        '밝고 유쾌한 화면 뒤에 담긴 메시지도 함께 이야기해 보고 싶어요.', 'N', 28);
    ADD_CONTENT_COMMENT('jinyoung05@pcwk.com', 510,
        '배우들의 연기와 음향이 긴 러닝타임을 팽팽하게 끌고 갑니다.', 'N', 23);
    ADD_CONTENT_COMMENT('jinyoung05@pcwk.com', 573,
        '익숙한 상황이 조금씩 뒤틀리는 과정이 정말 영리합니다.', 'N', 18);
    ADD_CONTENT_COMMENT('jinyoung05@pcwk.com', 231,
        '마지막 장면을 보고 나면 앞의 선택들이 전부 다르게 느껴집니다.', 'Y', 13);
    ADD_CONTENT_COMMENT('jinyoung05@pcwk.com', 35,
        '시간에 대한 설정보다 가족의 감정이 더 오래 남았습니다.', 'N', 8);
    ADD_CONTENT_COMMENT('jinyoung05@pcwk.com', 512,
        '지금 다시 봐도 액션과 질문이 모두 낡지 않았네요.', 'N', 3);
    ADD_COLLECTION_COMMENT('jinyoung05@pcwk.com', 'jinyoung01@pcwk.com',
        'MCU 입문자를 위한 핵심 6편', '처음 보는 친구에게 그대로 추천하기 좋은 순서네요.', 16);
    ADD_COLLECTION_COMMENT('jinyoung05@pcwk.com', 'jinyoung02@pcwk.com',
        '음악과 사랑이 남는 밤', '플레이리스트까지 같이 듣고 싶어지는 컬렉션입니다.', 14);
    ADD_COLLECTION_COMMENT('jinyoung05@pcwk.com', 'jinyoung03@pcwk.com',
        '질주 본능 액션', '속도감 있는 영화만 모여 있어서 스트레스 풀기 좋겠어요.', 12);
    ADD_COLLECTION_COMMENT('jinyoung05@pcwk.com', 'jinyoung04@pcwk.com',
        '감독의 세계가 선명한 영화', '감독별로 한 편씩 비교해 보는 재미가 있겠습니다.', 10);
    ADD_COLLECTION_COMMENT('jinyoung05@pcwk.com', 'jinyoung06@pcwk.com',
        '불 끄고 보면 안 되는 영화', '제목부터 무섭지만 한 편씩 도전해 볼게요.', 8);
    ADD_COLLECTION_COMMENT('jinyoung05@pcwk.com', 'jinyoung07@pcwk.com',
        '어른도 울리는 애니메이션', '어른에게 더 크게 다가오는 장면들이 많은 작품들이네요.', 6);
    ADD_COLLECTION_COMMENT('jinyoung05@pcwk.com', 'jinyoung08@pcwk.com',
        '영화제에서 만난 보석', '놓쳤던 작품을 발견했습니다. 좋은 추천 고마워요.', 4);
    ADD_COLLECTION_COMMENT('jinyoung05@pcwk.com', 'jinyoung09@pcwk.com',
        '우주와 시간의 경계', '설정 설명도 곁들여지면 더 재미있을 것 같아요.', 2);

    ADD_CONTENT_COMMENT('jinyoung06@pcwk.com', 548,
        '직접 보여 주는 장면보다 보이지 않는 불안이 훨씬 무서웠습니다.', 'N', 21);
    ADD_CONTENT_COMMENT('jinyoung06@pcwk.com', 771,
        '설명되지 않는 공포가 끝까지 긴장을 놓지 못하게 합니다.', 'N', 10);
    ADD_COLLECTION_COMMENT('jinyoung06@pcwk.com', 'jinyoung05@pcwk.com',
        '결말 토론이 필요한 작품', '보고 나서 해석을 찾아보게 되는 작품이 가득하네요.', 3);

    ADD_CONTENT_COMMENT('jinyoung07@pcwk.com', 92,
        '음악과 색감, 가족의 기억을 다루는 방식이 따뜻합니다.', 'N', 22);
    ADD_CONTENT_COMMENT('jinyoung07@pcwk.com', 82,
        '매 장면을 멈춰 보고 싶을 만큼 배경과 움직임이 아름답습니다.', 'N', 9);
    ADD_COLLECTION_COMMENT('jinyoung07@pcwk.com', 'jinyoung02@pcwk.com',
        '눈물 버튼 로맨스', '애니메이션과 함께 보기 좋은 감성 영화가 많네요.', 2);

    ADD_CONTENT_COMMENT('jinyoung08@pcwk.com', 1318,
        '말로 설명하지 않는 감정이 이미지 사이에 오래 머뭅니다.', 'N', 20);
    ADD_CONTENT_COMMENT('jinyoung08@pcwk.com', 1088,
        '인물의 시간과 영화의 시간이 나란히 흐르는 느낌이 좋았습니다.', 'N', 7);
    ADD_COLLECTION_COMMENT('jinyoung08@pcwk.com', 'jinyoung04@pcwk.com',
        '감독의 세계가 선명한 영화', '연출 스타일을 비교하며 보기 좋은 출발점입니다.', 2);

    ADD_CONTENT_COMMENT('jinyoung09@pcwk.com', 35,
        '과학적 상상력이 결국 인간의 선택과 사랑으로 이어지는 점이 좋습니다.', 'N', 23);
    ADD_CONTENT_COMMENT('jinyoung09@pcwk.com', 512,
        '현실을 의심하게 만드는 설정과 액션이 여전히 강렬합니다.', 'N', 10);
    ADD_COLLECTION_COMMENT('jinyoung09@pcwk.com', 'jinyoung04@pcwk.com',
        '주말 몰아보기 프랜차이즈', '세계관별 감상 순서를 정리할 때 참고하겠습니다.', 3);

    ADD_CONTENT_COMMENT('jinyoung10@pcwk.com', 148,
        '속도와 현장감이 좋아서 극장 관람의 장점이 확실했습니다.', 'N', 18);
    ADD_CONTENT_COMMENT('jinyoung10@pcwk.com', 578,
        '독특한 설정과 블랙코미디가 섞여서 예상보다 재미있었습니다.', 'N', 7);
    ADD_COLLECTION_COMMENT('jinyoung10@pcwk.com', 'jinyoung01@pcwk.com',
        '스파이더맨 세대 교차', '신작을 보기 전에 복습하기 좋은 목록입니다.', 2);

    -- ================================================================
    -- 2. 등록 결과 검증
    -- ================================================================
    DECLARE
        V_MEMBER_COUNT            NUMBER;
        V_ADMIN_COUNT             NUMBER;
        V_USER_COUNT              NUMBER;
        V_COLLECTION_COUNT        NUMBER;
        V_FILLED_COLLECTION_COUNT NUMBER;
        V_EMPTY_COLLECTION_COUNT  NUMBER;
        V_COLLECTION_ITEM_COUNT   NUMBER;
        V_RECORD_COUNT            NUMBER;
        V_PERSON_LIKE_COUNT       NUMBER;
        V_COLLECTION_LIKE_COUNT   NUMBER;
        V_COMMENT_COUNT           NUMBER;
        V_ACTIVE_USER_COUNT       NUMBER;
    BEGIN
        SELECT COUNT(*),
               SUM(CASE WHEN ROLE = 'ADMIN' THEN 1 ELSE 0 END),
               SUM(CASE WHEN ROLE = 'USER' THEN 1 ELSE 0 END)
          INTO V_MEMBER_COUNT, V_ADMIN_COUNT, V_USER_COUNT
          FROM MEMBER
         WHERE REGEXP_LIKE(
                   LOWER(EMAIL),
                   '^jinyoung(0[0-9]|10)@pcwk\.com$'
               );

        SELECT COUNT(*),
               SUM(CASE
                       WHEN EXISTS (
                           SELECT 1
                             FROM COLLECTION_ITEM CI
                            WHERE CI.COLLECTION_ID = C.COLLECTION_ID
                       ) THEN 1 ELSE 0
                   END),
               SUM(CASE
                       WHEN NOT EXISTS (
                           SELECT 1
                             FROM COLLECTION_ITEM CI
                            WHERE CI.COLLECTION_ID = C.COLLECTION_ID
                       ) THEN 1 ELSE 0
                   END)
          INTO V_COLLECTION_COUNT,
               V_FILLED_COLLECTION_COUNT,
               V_EMPTY_COLLECTION_COUNT
          FROM COLLECTION C
          JOIN MEMBER M
            ON M.MEMBER_ID = C.MEMBER_ID
         WHERE REGEXP_LIKE(
                   LOWER(M.EMAIL),
                   '^jinyoung(0[1-9]|10)@pcwk\.com$'
               );

        SELECT COUNT(*)
          INTO V_COLLECTION_ITEM_COUNT
          FROM COLLECTION_ITEM CI
          JOIN COLLECTION C
            ON C.COLLECTION_ID = CI.COLLECTION_ID
          JOIN MEMBER M
            ON M.MEMBER_ID = C.MEMBER_ID
         WHERE REGEXP_LIKE(
                   LOWER(M.EMAIL),
                   '^jinyoung(0[1-9]|10)@pcwk\.com$'
               );

        SELECT COUNT(*)
          INTO V_RECORD_COUNT
          FROM MEMBER_CONTENT MC
          JOIN MEMBER M
            ON M.MEMBER_ID = MC.MEMBER_ID
         WHERE REGEXP_LIKE(
                   LOWER(M.EMAIL),
                   '^jinyoung(0[1-9]|10)@pcwk\.com$'
               );

        SELECT COUNT(*)
          INTO V_PERSON_LIKE_COUNT
          FROM PERSON_LIKE PL
          JOIN MEMBER M
            ON M.MEMBER_ID = PL.MEMBER_ID
         WHERE REGEXP_LIKE(
                   LOWER(M.EMAIL),
                   '^jinyoung(0[1-9]|10)@pcwk\.com$'
               );

        SELECT COUNT(*)
          INTO V_COLLECTION_LIKE_COUNT
          FROM COLLECTION_LIKE CL
          JOIN MEMBER M
            ON M.MEMBER_ID = CL.MEMBER_ID
         WHERE REGEXP_LIKE(
                   LOWER(M.EMAIL),
                   '^jinyoung(0[1-9]|10)@pcwk\.com$'
               );

        SELECT COUNT(*)
          INTO V_COMMENT_COUNT
          FROM USER_COMMENT UC
          JOIN MEMBER M
            ON M.MEMBER_ID = UC.MEMBER_ID
         WHERE REGEXP_LIKE(
                   LOWER(M.EMAIL),
                   '^jinyoung(0[1-9]|10)@pcwk\.com$'
               );

        SELECT COUNT(*)
          INTO V_ACTIVE_USER_COUNT
          FROM MEMBER M
         WHERE REGEXP_LIKE(
                   LOWER(M.EMAIL),
                   '^jinyoung(0[1-9]|10)@pcwk\.com$'
               )
           AND EXISTS (
                   SELECT 1
                     FROM COLLECTION C
                    WHERE C.MEMBER_ID = M.MEMBER_ID
               )
           AND EXISTS (
                   SELECT 1
                     FROM MEMBER_CONTENT MC
                    WHERE MC.MEMBER_ID = M.MEMBER_ID
                      AND MC.RATING_SCORE IS NOT NULL
               )
           AND EXISTS (
                   SELECT 1
                     FROM MEMBER_CONTENT MC
                    WHERE MC.MEMBER_ID = M.MEMBER_ID
                      AND MC.WATCHLIST = 'Y'
               )
           AND EXISTS (
                   SELECT 1
                     FROM PERSON_LIKE PL
                    WHERE PL.MEMBER_ID = M.MEMBER_ID
               )
           AND EXISTS (
                   SELECT 1
                     FROM COLLECTION_LIKE CL
                    WHERE CL.MEMBER_ID = M.MEMBER_ID
               )
           AND EXISTS (
                   SELECT 1
                     FROM USER_COMMENT UC
                    WHERE UC.MEMBER_ID = M.MEMBER_ID
               );

        IF V_MEMBER_COUNT <> 11
           OR V_ADMIN_COUNT <> 1
           OR V_USER_COUNT <> 10 THEN
            RAISE_APPLICATION_ERROR(-20020, '회원 등록 건수 검증 실패');
        END IF;

        IF V_COLLECTION_COUNT <> 42
           OR V_FILLED_COLLECTION_COUNT <> 36
           OR V_EMPTY_COLLECTION_COUNT <> 6
           OR V_COLLECTION_ITEM_COUNT <> 205 THEN
            RAISE_APPLICATION_ERROR(-20021, '컬렉션 42/36/6 건수 검증 실패');
        END IF;

        IF V_RECORD_COUNT <> 126 THEN
            RAISE_APPLICATION_ERROR(-20022, '회원 기록 126건 검증 실패');
        END IF;

        IF V_PERSON_LIKE_COUNT <> 52 THEN
            RAISE_APPLICATION_ERROR(-20023, '인물 좋아요 52건 검증 실패');
        END IF;

        IF V_COLLECTION_LIKE_COUNT <> 64 THEN
            RAISE_APPLICATION_ERROR(-20024, '컬렉션 좋아요 64건 검증 실패');
        END IF;

        IF V_COMMENT_COUNT <> 43 THEN
            RAISE_APPLICATION_ERROR(-20025, '코멘트 43건 검증 실패');
        END IF;

        IF V_ACTIVE_USER_COUNT <> 10 THEN
            RAISE_APPLICATION_ERROR(-20026, '일반 회원 활동 데이터 검증 실패');
        END IF;

        DBMS_OUTPUT.PUT_LINE('회원: ' || V_MEMBER_COUNT || '건 (ADMIN 1 / USER 10)');
        DBMS_OUTPUT.PUT_LINE(
            '컬렉션: ' || V_COLLECTION_COUNT ||
            '건 (작품 포함 ' || V_FILLED_COLLECTION_COUNT ||
            ' / 빈 컬렉션 ' || V_EMPTY_COLLECTION_COUNT || ')'
        );
        DBMS_OUTPUT.PUT_LINE('컬렉션 작품: ' || V_COLLECTION_ITEM_COUNT || '건');
        DBMS_OUTPUT.PUT_LINE('회원 기록: ' || V_RECORD_COUNT || '건');
        DBMS_OUTPUT.PUT_LINE('인물 좋아요: ' || V_PERSON_LIKE_COUNT || '건');
        DBMS_OUTPUT.PUT_LINE('컬렉션 좋아요: ' || V_COLLECTION_LIKE_COUNT || '건');
        DBMS_OUTPUT.PUT_LINE('코멘트: ' || V_COMMENT_COUNT || '건');
        DBMS_OUTPUT.PUT_LINE('활동 검증 일반 회원: ' || V_ACTIVE_USER_COUNT || '명');
    END;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO BEFORE_JINYOUNG_PERSONA_DATA;
        RAISE;
END;
/

COMMIT;

PROMPT ================================================================
PROMPT jinyoung00~10 페르소나 테스트 데이터 등록이 완료되었습니다.
PROMPT 공통 로그인 비밀번호: 123123**
PROMPT ================================================================

-- ---------------------------------------------------------------------
-- 3. 실행 후 확인용 조회
-- ---------------------------------------------------------------------
SELECT MEMBER_ID,
       EMAIL,
       NICKNAME,
       ROLE,
       TO_CHAR(CREATED_DT, 'YYYY-MM-DD') AS CREATED_DT
  FROM MEMBER
 WHERE REGEXP_LIKE(
           LOWER(EMAIL),
           '^jinyoung(0[0-9]|10)@pcwk\.com$'
       )
 ORDER BY EMAIL;

SELECT M.EMAIL,
       M.NICKNAME,
       COUNT(DISTINCT C.COLLECTION_ID) AS COLLECTION_COUNT,
       COUNT(DISTINCT CASE
           WHEN CI.COLLECTION_ID IS NOT NULL THEN C.COLLECTION_ID
       END) AS FILLED_COLLECTION_COUNT,
       COUNT(DISTINCT CI.CONTENT_ID) AS DISTINCT_CONTENT_COUNT
  FROM MEMBER M
  LEFT JOIN COLLECTION C
    ON C.MEMBER_ID = M.MEMBER_ID
  LEFT JOIN COLLECTION_ITEM CI
    ON CI.COLLECTION_ID = C.COLLECTION_ID
 WHERE REGEXP_LIKE(
           LOWER(M.EMAIL),
           '^jinyoung(0[1-9]|10)@pcwk\.com$'
       )
 GROUP BY M.EMAIL, M.NICKNAME
 ORDER BY M.EMAIL;

SELECT M.EMAIL,
       NVL(MC.RATING_COUNT, 0) AS RATING_COUNT,
       NVL(MC.WATCHLIST_COUNT, 0) AS WATCHLIST_COUNT,
       NVL(PL.PERSON_LIKE_COUNT, 0) AS PERSON_LIKE_COUNT,
       NVL(CL.COLLECTION_LIKE_COUNT, 0) AS COLLECTION_LIKE_COUNT,
       NVL(UC.COMMENT_COUNT, 0) AS COMMENT_COUNT
  FROM MEMBER M
  LEFT JOIN (
        SELECT MEMBER_ID,
               SUM(CASE WHEN RATING_SCORE IS NOT NULL THEN 1 ELSE 0 END)
                   AS RATING_COUNT,
               SUM(CASE WHEN WATCHLIST = 'Y' THEN 1 ELSE 0 END)
                   AS WATCHLIST_COUNT
          FROM MEMBER_CONTENT
         GROUP BY MEMBER_ID
  ) MC
    ON MC.MEMBER_ID = M.MEMBER_ID
  LEFT JOIN (
        SELECT MEMBER_ID, COUNT(*) AS PERSON_LIKE_COUNT
          FROM PERSON_LIKE
         GROUP BY MEMBER_ID
  ) PL
    ON PL.MEMBER_ID = M.MEMBER_ID
  LEFT JOIN (
        SELECT MEMBER_ID, COUNT(*) AS COLLECTION_LIKE_COUNT
          FROM COLLECTION_LIKE
         GROUP BY MEMBER_ID
  ) CL
    ON CL.MEMBER_ID = M.MEMBER_ID
  LEFT JOIN (
        SELECT MEMBER_ID, COUNT(*) AS COMMENT_COUNT
          FROM USER_COMMENT
         GROUP BY MEMBER_ID
  ) UC
    ON UC.MEMBER_ID = M.MEMBER_ID
 WHERE REGEXP_LIKE(
           LOWER(M.EMAIL),
           '^jinyoung(0[1-9]|10)@pcwk\.com$'
       )
 ORDER BY M.EMAIL;
