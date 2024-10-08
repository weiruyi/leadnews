package com.heima.wemedia.feign;

import com.heima.apis.wemedia.IWemediaClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.wemedia.service.WmChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
public class WemediaClient implements IWemediaClient {

	@Autowired
	private WmChannelService wmChannelService;

	@Override
	@GetMapping("/api/v1/channel/list")
	public ResponseResult getChannels() {
		return wmChannelService.findAllChannel();
	}
}
