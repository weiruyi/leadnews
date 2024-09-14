package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.wemedia.service.WmMaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequestMapping("/api/v1/material")
@RequiredArgsConstructor
public class WmMaterialController {

	private final WmMaterialService wmMaterialService;

	@PostMapping("/upload_picture")
	public ResponseResult uploadPicture(MultipartFile multipartFile) {
		log.info("上传传图片");
		return wmMaterialService.uploadPicture(multipartFile);
	}

	@PostMapping("/list")
	public ResponseResult list(@RequestBody WmMaterialDto wmMaterialDto){
		log.info("查询素材列表");
		return wmMaterialService.getList(wmMaterialDto);
	}

}
