package com.ict.board.controller;

import java.io.File;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.ict.board.service.BoardService;
import com.ict.board.vo.BoardVO;
import com.ict.common.Paging;

@Controller
public class BoardController {

	@Autowired
	private BoardService boardService;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private Paging paging ;
	
	@GetMapping("/board_list")
	public ModelAndView boardList(HttpServletRequest request) {
		ModelAndView mv = new ModelAndView("board/list");
		
		// 페이징 기법
		
		// 전체 게시물의 수 (DB처리)		
		int count = boardService.getTotalCount();
		paging.setTotalRecord(count);
		
		// 전체 페이지의 수를 구한다.
		if(paging.getTotalRecord() <= paging.getNumPerPage()) {
			paging.setTotalPage(1);
		}else {
			paging.setTotalPage(paging.getTotalRecord() / paging.getNumPerPage());
			if(paging.getTotalRecord() % paging.getNumPerPage() != 0) {
				paging.setTotalPage(paging.getTotalPage() + 1 );
			}
		}
		
		// 파라미터에서 넘어오는 cPage(보고싶은 페이지번호)를 구하자 
		String cPage = request.getParameter("cPage");

		// 만약에 cPage가 null 이면 무조건 1page 이다.
		if(cPage == null) {
			paging.setNowPage(1);
		}else {
			paging.setNowPage(Integer.parseInt(cPage));
		}
		
		// cPage 기준으로 begin, end, beginBlock, endBlock
		// MySqL, Mariadb는 limit, offset 를 이용하자 (begin, end 는 필요없다.)
		// Oracle 에서는 begin, end 필요함
		// offset = limit * (현재페이지 - 1 ) 
		// limit = numPerPage 
		// select * from 테이블 order by pk desc limit 6 offset  0 | 6 | 12 | 18
		
		paging.setOffset(paging.getNumPerPage() * (paging.getNowPage() - 1)) ;
		
		// 시작 블록 , 끝 블록
		paging.setBeginBlock(
				(int)(((paging.getNowPage()-1) / paging.getPagePerBlock()) * paging.getPagePerBlock()+1));
		paging.setEndBlock(paging.getBeginBlock() + paging.getPagePerBlock() - 1);
		
		// 주의 사항 
		// endBlock(3,6,9...) 나온다. 그런데 실제 가지고 있는 총페이지는 20개 경우 4페이지까지 
		if(paging.getEndBlock() > paging.getTotalPage()) {
			paging.setEndBlock(paging.getTotalPage());
		}
		
		// DB 처리
		List<BoardVO> board_list = boardService.getBoardList(paging.getOffset(), paging.getNumPerPage());
		if(board_list != null) {
			mv.addObject("board_list", board_list);
			mv.addObject("paging", paging);
			return mv;
		}
		return null;
	}
	
	@GetMapping("/board_write")
	public ModelAndView boardWrite() {
		return new ModelAndView("board/write");
	}
	
	@PostMapping("/board_write_ok")
	public ModelAndView boardWriteOK(BoardVO bovo, HttpServletRequest request) {
		try {
			ModelAndView mv = new ModelAndView("redirect:/board_list");
			String path = request.getSession().getServletContext().getRealPath("/resources/upload");
			MultipartFile file = bovo.getFile_name();
			if(file.isEmpty()) {
				bovo.setF_name("");
			}else {
				UUID uuid = UUID.randomUUID();
				String f_name = uuid.toString() + "_" + file.getOriginalFilename();
				bovo.setF_name(f_name);
			
				// 업로드
				file.transferTo(new File(path, f_name));
			}
			// 암호화
			String pwd = passwordEncoder.encode(bovo.getPwd());
			bovo.setPwd(pwd);
			
			int result = boardService.getBoardInsert(bovo);
			if(result > 0) {
				return mv;
			}
			return null;
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	@GetMapping("/board_detail")
	public ModelAndView boardDetail(@ModelAttribute("cPage") String cPage,
			@RequestParam("idx") String idx) {
		ModelAndView mv = new ModelAndView("board/view");
		
		// hit 업데이트 
		int result = boardService.getBoardHit(idx);
		
		// 상세보기 
		BoardVO bovo = boardService.getBoardDetail(idx);
		
		if(bovo != null) {
			mv.addObject("bovo", bovo);
			return mv;
		}
		return null;
	}
}




















