package com.heima.user.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.ApUserRealnamePageReqDto;
import com.heima.user.service.ApUserRealnameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/v1/auth/")
public class ApUserRealnameController {
	@Autowired
	private ApUserRealnameService apUserRealnameService;

	@PostMapping("/list")
	public ResponseResult list(@RequestBody ApUserRealnamePageReqDto dto){
		return apUserRealnameService.list(dto);
	}

	@PostMapping("/authFail")
	public ResponseResult authFail(@RequestBody ApUserRealnamePageReqDto dto){
		return apUserRealnameService.auth(dto, (short)2);
	}

	@PostMapping("/authPass")
	public ResponseResult authPass(@RequestBody ApUserRealnamePageReqDto dto){
		return apUserRealnameService.auth(dto, (short)9);
	}
}
