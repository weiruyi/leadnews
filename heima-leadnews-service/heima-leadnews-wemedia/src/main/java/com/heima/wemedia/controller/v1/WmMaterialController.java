package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.wemedia.service.WmMaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequestMapping("/api/v1/material")
@RequiredArgsConstructor
public class WmMaterialController {

	private final WmMaterialService wmMaterialService;

	@PostMapping("/upload_picture")
	public ResponseResult uploadPicture(MultipartFile multipartFile) {

		return wmMaterialService.uploadPicture(multipartFile);
	}

}
