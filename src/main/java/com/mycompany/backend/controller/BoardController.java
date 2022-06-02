package com.mycompany.backend.controller;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycompany.backend.dto.Board;
import com.mycompany.backend.dto.Pager;
import com.mycompany.backend.service.BoardService;

import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequestMapping("/board")
public class BoardController {
	
	@Resource
	private BoardService boardService;
	
	//게시물 목록 : GET -> http://localhost/board/list?pageNo=1
	@GetMapping("/list")
	public Map<String, Object> list(@RequestParam(defaultValue="1") int pageNo) {
		log.info("실행");
		
		int totalRows = boardService.getTotalBoardNum();
		
		Pager pager = new Pager(5,5,totalRows, pageNo);
		List<Board> list = boardService.getBoards(pager);
		
		Map<String, Object> map = new HashMap<>();
		map.put("boards", list);
		map.put("pager", pager);
		
		return map;
	}
	
	//create : 게시물 생성
	@PostMapping("/")
	public Board create(Board board) {
		log.info("실행");
		
		//첨부 파일이 있다면
		if(board.getBattach() != null && !board.getBattach().isEmpty()) {
			MultipartFile mf = board.getBattach();
			board.setBattachoname(mf.getOriginalFilename());
			board.setBattachsname(new Date().getTime()+"-" + mf.getOriginalFilename());
			board.setBattachtype(mf.getContentType()); //파일의 종류
			log.info(board);
			try {
				File file = new File("C:/Temp/uploadfiles/"+board.getBattachsname());
				mf.transferTo(file);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		
		boardService.writeBoard(board);
		
		//성공적으로 저장되었는지 확인하기 위해 db에서 가져와보는 것
		Board dbBoard = boardService.getBoard(board.getBno(), false);
		
		return dbBoard;
	}
	
	//update : 게시물 수정
	@PutMapping("/")
	public Board update(Board board) {
		log.info("실행");
		
		//첨부 파일이 있다면
		if(board.getBattach() != null && !board.getBattach().isEmpty()) {
			MultipartFile mf = board.getBattach();
			board.setBattachoname(mf.getOriginalFilename());
			board.setBattachsname(new Date().getTime()+"-" + mf.getOriginalFilename());
			board.setBattachtype(mf.getContentType()); //파일의 종류
			try {
				File file = new File("C:/Temp/uploadfiles/"+board.getBattachsname());
				mf.transferTo(file);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		
		boardService.updateBoard(board);
		
		//성공적으로 수정되었는지 확인하기 위해 db에서 가져와서 응답으로 제공해줌
		Board dbBoard = boardService.getBoard(board.getBno(), false);
		
		return dbBoard;
	}
	
	//http://localhost/board/3 : 게시물 정보 읽기
	@GetMapping("/{bno}")
	public Board read(@PathVariable int bno, @RequestParam(defaultValue="false") boolean hit) {
		log.info("run");
		
		Board dbBoard = boardService.getBoard(bno, hit);
		return dbBoard;
	}
	
	//delete
	@DeleteMapping("/{bno}")
	public Map<String, String> delete(@PathVariable int bno) {
		boardService.removeBoard(bno);
		Map<String, String> map = new HashMap<>();
		map.put("result", "success");
		return map;
	}
	
	//download : 게시물 첨부 파일 다운로드
	@GetMapping("/battach/{bno}")
	public ResponseEntity<InputStreamResource> download(@PathVariable int bno) throws Exception{
		Board board = boardService.getBoard(bno, false);
		String battachoname = board.getBattachoname();
		
		//첨부 파일이 없을 경우
		if(battachoname==null) return null;
		
		//파일 이름이 한글일 경우를 위한 설정
		battachoname = new String(battachoname.getBytes("UTF-8"),"ISO-8859-1");
		/*		//-> 다른 브라우저일 경우
				//다운로드할 파일명을 헤더에 추가 : 브라우저별로 한글을 처리하는 방식이 다름
				if(userAgent.contains("Trident") || userAgent.contains("MSIE")) {
					//IE 브라우저일 경우
					originalFilename = URLEncoder.encode(originalFilename, "UTF-8");
				}else {
					//크롬, 엣지, 사파리일 경우
					originalFilename = new String(originalFilename.getBytes("UTF-8"), "ISO-8859-1");
				}
		*/
		
		//파일 입력 스트링 생성
		FileInputStream fis = new FileInputStream("C:/Temp/uploadfiles/" + board.getBattachsname());
		InputStreamResource resource = new InputStreamResource(fis);
		
		//응답 생성
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+ battachoname + "\";")
				.header(HttpHeaders.CONTENT_TYPE, board.getBattachtype())
				.body(resource);
		
		//Content-Disposition : HTTP Response Body에 오는 컨텐츠의 기질/성향을 알려주는 속성
		//attachment + filename을 써주면 Body에 오는 값을 다운로드 받으라는 뜻이다
	}
}
