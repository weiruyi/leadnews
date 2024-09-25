package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmChannelPageReqDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.service.WmChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/channel")
@Slf4j
@RequiredArgsConstructor
public class WmChannelController {
	private final WmChannelService wmChannelService;

	@GetMapping("/channels")
	public ResponseResult findAll(){
		log.info("查询所有频道");
		return wmChannelService.findAllChannel();
	}

	@PostMapping("/list")
	public ResponseResult list(@RequestBody WmChannelPageReqDto dto){
		return wmChannelService.list(dto);
	}

	@GetMapping("/del/{id}")
	public ResponseResult del(@PathVariable Integer id){
		return wmChannelService.del(id);
	}

	@PostMapping("/save")
	public ResponseResult save(@RequestBody WmChannel wmChannel){
		wmChannelService.saveOrUpdate(wmChannel);
		return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
	}

	@PostMapping("/update")
	public ResponseResult update(@RequestBody WmChannel wmChannel){
		wmChannelService.saveOrUpdate(wmChannel);
		return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
	}
}