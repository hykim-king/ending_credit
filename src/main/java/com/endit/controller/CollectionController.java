package com.endit.controller;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.CollectionVO;
import com.endit.service.CollectionService;

/**
 * <pre>
 * Class Name  : CollectionController
 * Description : 컬렉션 목록, 단건, 등록, 수정 및 삭제 요청을 처리하는 REST Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 21. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 21.
 */
@RestController
@RequestMapping("/api/collections")
public class CollectionController {

	private final CollectionService collectionService;

	/**
	 * CollectionService를 주입받아 Controller 생성
	 *
	 * @param collectionService 컬렉션 Service
	 */
	public CollectionController(CollectionService collectionService) {
		this.collectionService = collectionService;
	}

	/**
	 * 검색 및 페이징 조건을 반영한 컬렉션 목록 조회
	 *
	 * @param pageNo 페이지 번호
	 * @param pageSize 페이지당 건수
	 * @param searchDiv 검색 구분
	 * @param searchWord 검색어
	 * @return 컬렉션 목록과 페이징 정보
	 */
	@GetMapping
	public ResponseEntity<Map<String, Object>> retrieve(
			@RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize,
			@RequestParam(defaultValue = "") String searchDiv,
			@RequestParam(defaultValue = "") String searchWord) {

		DTO param = new DTO();
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);
		param.setSearchDiv(searchDiv);
		param.setSearchWord(searchWord);

		List<CollectionVO> items = collectionService.retrieve(param);

		// 목록(items)과 페이징 정보(page)를 한 JSON 응답으로 전달한다.
		// LinkedHashMap은 디버깅할 때 응답 키 순서를 일정하게 유지하기 위해 사용한다.
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("items", items);
		response.put("page", param);

		return ResponseEntity.ok(response);
	}

	/**
	 * 컬렉션 번호를 이용한 단건 조회
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 컬렉션 정보
	 */
	@GetMapping("/{collectionId}")
	public ResponseEntity<CollectionVO> get(
			@PathVariable int collectionId) {

		return ResponseEntity.ok(collectionService.get(collectionId));
	}

	/**
	 * 컬렉션 등록
	 *
	 * @param param 등록할 컬렉션 정보
	 * @return 등록된 컬렉션 정보와 접근 URI
	 */
	@PostMapping
	public ResponseEntity<CollectionVO> create(
			@RequestBody CollectionVO param) {

		// 로그인 기능이 병합되기 전에는 화면에서 입력한 임시 회원 번호를 사용한다.
		// 최종 통합 시 이 값은 로그인 Principal의 MEMBER_ID로 교체해야 한다.
		CollectionVO created = collectionService.create(param);

		URI location = URI.create("/api/collections/" + created.getCollectionId());

		return ResponseEntity.created(location).body(created);
	}

	/**
	 * 컬렉션 제목, 설명 및 공개 여부 수정
	 *
	 * @param collectionId 컬렉션 번호
	 * @param param 수정할 컬렉션 정보
	 * @return 수정된 컬렉션 정보
	 */
	@PutMapping("/{collectionId}")
	public ResponseEntity<CollectionVO> update(
			@PathVariable int collectionId,
			@RequestBody CollectionVO param) {

		return ResponseEntity.ok(collectionService.update(
				collectionId,
				param));
	}

	/**
	 * 컬렉션 삭제
	 *
	 * @param collectionId 컬렉션 번호
	 * @return 본문이 없는 응답
	 */
	@DeleteMapping("/{collectionId}")
	public ResponseEntity<Void> delete(
			@PathVariable int collectionId) {

		collectionService.delete(collectionId);

		return ResponseEntity.noContent().build();
	}

	/**
	 * 잘못된 요청값 예외를 HTTP 400 응답으로 변환
	 *
	 * @param exception 잘못된 요청값 예외
	 * @return 오류 메시지
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(
			IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"컬렉션 요청값을 확인해 주세요.");

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(message);
	}

	/**
	 * 존재하지 않는 컬렉션 예외를 HTTP 404 응답으로 변환
	 *
	 * @param exception 컬렉션 미존재 예외
	 * @return 오류 메시지
	 */
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<MessageVO> handleNotFound(
			NoSuchElementException exception) {

		MessageVO message = new MessageVO(
				"404",
				exception.getMessage(),
				"요청한 컬렉션을 찾을 수 없습니다.");

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(message);
	}

}
