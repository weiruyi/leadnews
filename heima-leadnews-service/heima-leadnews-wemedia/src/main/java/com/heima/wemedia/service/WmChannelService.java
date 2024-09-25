package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmChannelPageReqDto;
import com.heima.model.wemedia.pojos.WmChannel;

public interface WmChannelService extends IService<WmChannel> {

	/**
	 * 查询所有频道
	 * @return
	 */
	public ResponseResult findAllChannel();

	/**
	 * 频道分页查询
	 * @param dto
	 * @return
	 */
	public ResponseResult list(WmChannelPageReqDto dto);

	/**
	 * 删除频道
	 * @param id
	 * @return
	 */
	public ResponseResult del(Integer id);

}
