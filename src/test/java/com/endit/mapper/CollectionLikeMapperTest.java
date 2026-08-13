/**
 * <pre>
 * Class Name  : CollectionLikeMapperTest
 * Description : CollectionLikeMapper의 CRUD 기능을 검증하는 JUnit 테스트
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 13.	gunwoo        최초생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author gunwoo
 * @since 2026. 8. 13.
 */
package com.endit.mapper;

// static import : Assertions.assertEquals(...) 를 assertEquals(...) 처럼
// 클래스명 없이 바로 쓰기 위한 문법. JUnit 검증 메서드들을 모아둔 것.
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName; // 테스트 이름을 한글 등으로 예쁘게 표시해주는 애노테이션
import org.junit.jupiter.api.Test;         // "이 메서드는 테스트다" 라고 표시하는 애노테이션
import org.springframework.beans.factory.annotation.Autowired; // 스프링이 관리하는 객체(빈)를 자동으로 주입받는 애노테이션
import org.springframework.boot.test.context.SpringBootTest;   // 스프링 컨텍스트(앱 전체)를 띄워서 테스트하겠다는 표시
import org.springframework.transaction.annotation.Transactional; // 테스트 후 DB 변경사항을 자동 롤백시켜주는 애노테이션

import com.endit.domain.CollectionLikeVO;

/**
 * ===== 클래스 레벨 애노테이션 설명 =====
 *
 * @SpringBootTest
 *   - 이 테스트를 실행할 때 스프링 부트 애플리케이션 전체를 띄운다.
 *   - 즉 application.yaml 설정, DB 연결(datasource), MyBatis 설정 등이
 *     전부 로딩된 상태에서 테스트가 진행된다.
 *   - 그래서 DB 연결 정보(datasource)가 세팅되어 있지 않으면
 *     이 애노테이션 단계에서부터 에러가 난다.
 *
 * @Transactional
 *   - 테스트 클래스(또는 메서드)에 이 애노테이션을 붙이면,
 *     테스트 안에서 실행한 insert/update/delete가 테스트가 끝나는 순간
 *     자동으로 ROLLBACK 된다.
 *   - 즉, 몇 번을 실행해도 endit_test DB에 실제로 데이터가 쌓이지 않는다.
 *   - "테스트니까 DB를 더럽히지 말자"는 안전장치.
 */
@SpringBootTest
@Transactional
class CollectionLikeMapperTest {

	/**
	 * @Autowired
	 *   - 스프링이 만들어둔 CollectionLikeMapper의 "실제 구현체"를
	 *     자동으로 주입(연결)해준다.
	 *   - 우리는 CollectionLikeMapper.java에 메서드 시그니처(설계도)만
	 *     작성했는데, 실제 SQL 실행 로직은 MyBatis가
	 *     CollectionLikeMapper.xml을 읽어서 자동으로 만들어준다.
	 *   - 그 자동 생성된 구현체가 여기 주입되는 것.
	 */
	@Autowired
	private CollectionLikeMapper collectionLikeMapper;

	// ===== 테스트용 데이터 =====
	// 주의 : COLLECTION_LIKE 테이블은 MEMBER, COLLECTION 테이블을
	// 외래키(FK)로 참조하고 있기 때문에, 여기 적은 번호가
	// endit_test DB의 MEMBER / COLLECTION 테이블에 실제로 존재해야 한다.
	// 존재하지 않는 번호를 넣으면 "FK 제약조건 위반" 에러가 난다.
	private final int TEST_MEMBER_ID = 1;     //: 실제 존재하는 회원번호로 변경
	private final int TEST_COLLECTION_ID = 1; //: 실제 존재하는 컬렉션번호로 변경

	/**
	 * [테스트 1] 좋아요 등록(insert)이 정상적으로 되는지 확인
	 *
	 * given : 회원번호 + 컬렉션번호를 담은 VO를 준비한다.
	 * when  : insertCollectionLike를 호출한다.
	 * then  : 영향받은 행(row) 개수가 1이면 성공.
	 *         (insert 문이 1건 실행되면 MyBatis가 1을 리턴해준다)
	 */
	@Test
	@DisplayName("좋아요 등록 테스트")
	void testInsertCollectionLike() {
		CollectionLikeVO vo = new CollectionLikeVO();
		vo.setMemberId(TEST_MEMBER_ID);
		vo.setCollectionId(TEST_COLLECTION_ID);

		int result = collectionLikeMapper.insertCollectionLike(vo);

		assertEquals(1, result, "insert된 행 수는 1이어야 합니다.");
		System.out.println("insert 결과 : " + result);
	}

	/**
	 * [테스트 2] 등록한 좋아요가 다시 조회되는지 확인
	 *
	 * given : 먼저 좋아요 하나를 등록해둔다 (조회할 대상을 만들기 위해).
	 * when  : selectCollectionLike로 방금 등록한 걸 조회한다.
	 * then  : null이 아니어야 하고, 넣은 값과 조회된 값이 같아야 한다.
	 */
	@Test
	@DisplayName("좋아요 단건 조회 테스트")
	void testSelectCollectionLike() {
		CollectionLikeVO insertVo = new CollectionLikeVO();
		insertVo.setMemberId(TEST_MEMBER_ID);
		insertVo.setCollectionId(TEST_COLLECTION_ID);
		collectionLikeMapper.insertCollectionLike(insertVo); // 먼저 데이터 하나 만들어둠

		CollectionLikeVO result = collectionLikeMapper.selectCollectionLike(insertVo);

		assertNotNull(result, "등록한 데이터가 조회되어야 합니다.");
		assertEquals(TEST_MEMBER_ID, result.getMemberId());
		assertEquals(TEST_COLLECTION_ID, result.getCollectionId());
		System.out.println("조회 결과 : " + result);
	}

	/**
	 * [테스트 3] 없는 데이터를 조회하면 null이 나오는지 확인
	 *
	 * - 999999라는, 사실상 존재하지 않을 값으로 조회를 시도한다.
	 * - 이런 "실패 케이스"도 같이 테스트해야
	 *   나중에 화면에서 "좋아요 안 누른 상태"를 제대로 처리할 수 있다.
	 */
	@Test
	@DisplayName("존재하지 않는 좋아요는 조회되지 않아야 한다")
	void testSelectCollectionLike_notExists() {
		CollectionLikeVO vo = new CollectionLikeVO();
		vo.setMemberId(999999);
		vo.setCollectionId(999999);

		CollectionLikeVO result = collectionLikeMapper.selectCollectionLike(vo);

		assertNull(result, "존재하지 않는 데이터는 null이어야 합니다.");
	}

	/**
	 * [테스트 4] 특정 회원이 좋아요 누른 컬렉션 목록이 잘 나오는지 확인
	 *
	 * - "마이페이지 > 내가 좋아요한 컬렉션" 같은 화면에서 쓰일 기능.
	 * - List 타입이므로, 결과가 null이 아니고 최소 1건 이상인지만 확인한다.
	 */
	@Test
	@DisplayName("특정 회원이 좋아요 누른 컬렉션 목록 조회 테스트")
	void testSelectCollectionLikeListByMember() {
		CollectionLikeVO insertVo = new CollectionLikeVO();
		insertVo.setMemberId(TEST_MEMBER_ID);
		insertVo.setCollectionId(TEST_COLLECTION_ID);
		collectionLikeMapper.insertCollectionLike(insertVo);

		List<CollectionLikeVO> list = collectionLikeMapper.selectCollectionLikeListByMember(TEST_MEMBER_ID);

		assertNotNull(list);
		assertTrue(list.size() > 0, "최소 1건 이상 조회되어야 합니다.");
		System.out.println("회원별 좋아요 목록 : " + list);
	}

	/**
	 * [테스트 5] 특정 컬렉션에 좋아요 누른 회원 목록이 잘 나오는지 확인
	 *
	 * - "이 컬렉션을 좋아요한 사람들" 같은 화면에서 쓰일 기능.
	 */
	@Test
	@DisplayName("특정 컬렉션에 좋아요 누른 회원 목록 조회 테스트")
	void testSelectCollectionLikeListByCollection() {
		CollectionLikeVO insertVo = new CollectionLikeVO();
		insertVo.setMemberId(TEST_MEMBER_ID);
		insertVo.setCollectionId(TEST_COLLECTION_ID);
		collectionLikeMapper.insertCollectionLike(insertVo);

		List<CollectionLikeVO> list = collectionLikeMapper.selectCollectionLikeListByCollection(TEST_COLLECTION_ID);

		assertNotNull(list);
		assertTrue(list.size() > 0, "최소 1건 이상 조회되어야 합니다.");
		System.out.println("컬렉션별 좋아요 목록 : " + list);
	}

	/**
	 * [테스트 6] 특정 컬렉션의 좋아요 총 개수가 잘 세어지는지 확인
	 *
	 * - 컬렉션 상세 화면에 "좋아요 123개" 처럼 숫자를 보여줄 때 쓰이는 기능.
	 * - COUNT(*) 결과이므로 리턴 타입이 List가 아니라 int(단일 값)이다.
	 */
	@Test
	@DisplayName("좋아요 개수 조회 테스트")
	void testSelectCollectionLikeCount() {
		CollectionLikeVO insertVo = new CollectionLikeVO();
		insertVo.setMemberId(TEST_MEMBER_ID);
		insertVo.setCollectionId(TEST_COLLECTION_ID);
		collectionLikeMapper.insertCollectionLike(insertVo);

		int count = collectionLikeMapper.selectCollectionLikeCount(TEST_COLLECTION_ID);

		assertTrue(count >= 1, "좋아요 개수는 1개 이상이어야 합니다.");
		System.out.println("좋아요 개수 : " + count);
	}

	/**
	 * [테스트 7] 좋아요 취소(delete)가 정상적으로 되는지 확인
	 *
	 * given : 먼저 좋아요를 하나 등록해둔다.
	 * when  : deleteCollectionLike로 삭제한다.
	 * then  : 삭제된 행 수가 1이어야 하고,
	 *         삭제 후 다시 조회하면 null이 나와야 한다 (진짜 지워졌는지 이중 확인).
	 */
	@Test
	@DisplayName("좋아요 취소(삭제) 테스트")
	void testDeleteCollectionLike() {
		CollectionLikeVO vo = new CollectionLikeVO();
		vo.setMemberId(TEST_MEMBER_ID);
		vo.setCollectionId(TEST_COLLECTION_ID);
		collectionLikeMapper.insertCollectionLike(vo);

		int result = collectionLikeMapper.deleteCollectionLike(vo);
		assertEquals(1, result, "delete된 행 수는 1이어야 합니다.");

		// 삭제 후 재조회 : 진짜로 없어졌는지 한 번 더 검증
		CollectionLikeVO afterDelete = collectionLikeMapper.selectCollectionLike(vo);
		assertNull(afterDelete, "삭제 후에는 조회되지 않아야 합니다.");
	}

}