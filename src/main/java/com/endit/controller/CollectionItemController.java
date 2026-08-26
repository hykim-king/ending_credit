package com.endit.controller;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.endit.cmn.DTO;
import com.endit.cmn.MessageVO;
import com.endit.domain.CollectionItemVO;
import com.endit.service.CollectionItemService;

/**
 * <pre>
 * Class Name  : CollectionItemController
 * Description : 컬렉션 작품의 목록 조회, 추가 및 삭제 요청을 처리하는 REST Controller
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 8. 26. jinyoung    최초 생성
 * ------------------------------------------------------------
 * </pre>
 *
 * @author jinyoung
 * @since 2026. 8. 26.
 */
@RestController
@RequestMapping("/api/collections/{collectionId}/items")
public class CollectionItemController {

	private final CollectionItemService collectionItemService;

	/** CollectionItemService를 주입받아 Controller 생성 */
	public CollectionItemController(CollectionItemService collectionItemService) {
		this.collectionItemService = collectionItemService;
	}

	/** 컬렉션 작품 목록 조회 */
	@GetMapping
	public ResponseEntity<Map<String, Object>> retrieve(
			@PathVariable int collectionId,
			@RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "12") int pageSize) {

		DTO param = new DTO();
		
		param.setPageNo(pageNo);
		param.setPageSize(pageSize);

		List<CollectionItemVO> items =
				collectionItemService.retrieve(collectionId, param);

		Map<String, Object> response = new LinkedHashMap<>();
		
		response.put("items", items);
		response.put("page", param);

		return ResponseEntity.ok(response);
	}

	/** 컬렉션 작품 단건 조회 */
	@GetMapping("/{contentId}")
	public ResponseEntity<CollectionItemVO> get(
			@PathVariable int collectionId,
			@PathVariable int contentId) {

		return ResponseEntity.ok(
				collectionItemService.get(collectionId, contentId));
	}

	/** 컬렉션에 작품 추가 */
	@PostMapping
	public ResponseEntity<CollectionItemVO> create(
			@PathVariable int collectionId,
			@RequestBody CollectionItemVO param) {

		CollectionItemVO created =
				collectionItemService.create(collectionId, param);

		URI location = URI.create(
				"/api/collections/" + collectionId
				+ "/items/" + created.getContentId());

		return ResponseEntity.created(location).body(created);
	}

	/** 컬렉션에서 작품 삭제 */
	@DeleteMapping("/{contentId}")
	public ResponseEntity<Void> delete(
			@PathVariable int collectionId,
			@PathVariable int contentId) {

		collectionItemService.delete(collectionId, contentId);

		return ResponseEntity.noContent().build();
	}

	/** 잘못된 요청값 예외를 HTTP 400 응답으로 변환 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<MessageVO> handleBadRequest(
			IllegalArgumentException exception) {

		MessageVO message = new MessageVO(
				"400",
				exception.getMessage(),
				"컬렉션 작품 요청값을 확인해 주세요.");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
	}

	/** 외래 키 등 데이터 무결성 예외를 HTTP 400 응답으로 변환 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<MessageVO> handleDataIntegrityViolation(
			DataIntegrityViolationException exception) {

		MessageVO message = new MessageVO(
				"400",
				"존재하는 컬렉션과 콘텐츠 번호를 입력해 주세요.",
				"컬렉션 작품 데이터의 참조 관계를 확인해 주세요.");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
	}

	/** 존재하지 않는 컬렉션 작품 예외를 HTTP 404 응답으로 변환 */
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<MessageVO> handleNotFound(
			NoSuchElementException exception) {

		MessageVO message = new MessageVO(
				"404",
				exception.getMessage(),
				"요청한 컬렉션 작품을 찾을 수 없습니다.");

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
	}

	/** 중복 작품 등 현재 상태 충돌을 HTTP 409 응답으로 변환 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<MessageVO> handleConflict(
			IllegalStateException exception) {

		MessageVO message = new MessageVO(
				"409",
				exception.getMessage(),
				"컬렉션 작품의 현재 상태를 확인해 주세요.");

		return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
	}

}
