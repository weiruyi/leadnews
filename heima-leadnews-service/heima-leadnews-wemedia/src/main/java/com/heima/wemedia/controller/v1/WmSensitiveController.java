package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmSensitiveDto;
import com.heima.model.wemedia.dtos.WmSensitivePageReqDto;
import com.heima.wemedia.service.WmSensitiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/v1/sensitive")
public class WmSensitiveController {

	@Autowired
	WmSensitiveService wmSensitiveService;

	@PostMapping("/list")
	public ResponseResult list(@RequestBody WmSensitivePageReqDto dto){

		return wmSensitiveService.list(dto);
	}

	@DeleteMapping("/del/{id}")
	public ResponseResult del(@PathVariable Integer id){
		return wmSensitiveService.del(id);
	}

	@PostMapping("/save")
	public ResponseResult save(@RequestBody WmSensitiveDto dto){
		return wmSensitiveService.saveOrUpdate(dto);
	}

	@PostMapping("/update")
	public ResponseResult update(@RequestBody WmSensitiveDto dto){
		return wmSensitiveService.saveOrUpdate(dto);
	}
}
