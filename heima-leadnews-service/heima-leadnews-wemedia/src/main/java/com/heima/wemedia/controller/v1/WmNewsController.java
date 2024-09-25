package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController {

	@Autowired
	private WmNewsService wmNewsService;

	@PostMapping("/list")
	public ResponseResult findAll(@RequestBody WmNewsPageReqDto dto){
		log.info("查询文章列表");
		return  wmNewsService.findAll(dto);
	}

	@PostMapping("/submit")
	public ResponseResult submitNews(@RequestBody WmNewsDto dto){
		log.info("新增文章");
		return wmNewsService.submitNews(dto);
	}

	@PostMapping("/down_or_up")
	public ResponseResult downOrUp(@RequestBody WmNewsDto dto){
		return wmNewsService.downOrUp(dto);
	}

	@PostMapping("/list_vo")
	public ResponseResult listVo(@RequestBody WmNewsPageReqDto dto){
		return wmNewsService.listVo(dto);
	}

	@GetMapping("/one_vo/{id}")
	public ResponseResult oneVo(@PathVariable Integer id){
		return wmNewsService.oneVo(id);
	}

	@PostMapping("/auth_fail")
	public ResponseResult authFail(@RequestBody WmNewsPageReqDto dto){
		return wmNewsService.auth(dto, (short)2);
	}

	@PostMapping("/auth_pass")
	public ResponseResult authPass(@RequestBody WmNewsPageReqDto dto){
		return wmNewsService.auth(dto, (short)4);
	}

}