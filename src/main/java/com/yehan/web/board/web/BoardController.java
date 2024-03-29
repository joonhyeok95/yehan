package com.yehan.web.board.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.yehan.web.board.model.BoardDTO;
import com.yehan.web.board.model.BoardRequestDTO;
import com.yehan.web.board.service.BoardService;
import com.yehan.web.util.Thumbnail;
import com.yehan.web.util.UUIDgenerate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BoardController {

	private final BoardService boardService;
	private final Thumbnail thumbnail = new Thumbnail();

	@Value("${file.download.directory}") 
	private String fileDownloadDirectory;
	
    @RequestMapping(value = "/boards",method = RequestMethod.GET)
    public String api(Model model, String year){
		List<?> salaryList = boardService.getBoard(year);
    	
    	return salaryList.toString();
    }

    // 인덱스 화면
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView mainPage(Model model, String year){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("index");
        // 파일리스트
		List<BoardDTO> boardList = (List<BoardDTO>) boardService.getBoard(year);
		// download url 세팅
		for(int i=0; i<boardList.size(); i++) {
			log.debug(""+boardList.get(i).getNo());
			String fileName = boardList.get(i).getFileName();
			String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
			if(!"".equals(boardList.get(0).getFileId())) {
				boardList.get(i).setUrl("/download-file/"+ boardList.get(i).getFileDate() + "/" + boardList.get(i).getFileId() + "." + ext);
				boardList.get(i).setUrlThumb("/download-file/"+ boardList.get(i).getFileDate() + "/THUMB_" + boardList.get(i).getFileId() + "." + ext);
			}
			log.debug(boardList.get(i).getUrl());
		}
        model.addAttribute("dataList", boardList);
        model.addAttribute("year", year);
        return modelAndView;
    }

    // 사진업로드 화면
    @RequestMapping(value = "/write",method = RequestMethod.GET)
    public ModelAndView upload(Model model){
    	log.info("write!");
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("write");
        return modelAndView;
    }
    
    // 사진업로드
	@RequestMapping(value = "/write", method = RequestMethod.POST)
	public RedirectView upload(BoardRequestDTO boardRequestDTO) throws Exception {
		log.info(boardRequestDTO.toString());
		log.info("### upload");
		
		BoardDTO boardDTO = new BoardDTO();
		boardDTO.setUserId("empty");
		boardDTO.setTitle(boardRequestDTO.getTitle());
		boardDTO.setContent(boardRequestDTO.getContent());
		boardDTO.setFileDate(boardRequestDTO.getFileDate());
		
		UUIDgenerate uuid = new UUIDgenerate();
		try {
			for(MultipartFile a : boardRequestDTO.getMultiFiles()) {
				// 고유값 생성
				boardDTO.setNo(uuid.getUUID());
				boardDTO.setFileId(uuid.getUUID());
				boardDTO.setFileName(a.getOriginalFilename());
				// 확장자 구하기
				String file = a.getOriginalFilename();
					log.info("원본이미지명 : " + file);
				String ext = file.substring(a.getOriginalFilename().lastIndexOf(".") + 1);
					log.info("확장자명 : " + ext);

				file = fileDownloadDirectory + boardDTO.getFileDate()+ "/" + boardDTO.getFileId() + "." + ext;
				
				// 파일 저장
				File targetFile = new File(file);
				InputStream fileStream = a.getInputStream();
				FileUtils.copyInputStreamToFile(fileStream, targetFile);
				
				fileStream.close();
                // 썸네일 생성
                thumbnail.makeThumbnail(fileDownloadDirectory + boardDTO.getFileDate()+ "/", boardDTO.getFileId() + "." + ext , ext);
				
                ////////////////////////////////////////////////////
                // db insert
                boardService.insertBoard(boardDTO);
                
			}
		} catch (IOException e) {
//			FileUtils.deleteQuietly(targetFile);
			e.printStackTrace();
		}

		return new RedirectView("/");
	}
    
    
}
