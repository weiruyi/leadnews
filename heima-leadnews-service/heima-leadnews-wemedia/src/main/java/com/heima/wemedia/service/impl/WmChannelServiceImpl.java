package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmChannelPageReqDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.service.WmChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {
	/**
	 * 频道分页查询
	 *
	 * @param dto
	 * @return
	 */
	@Override
	public ResponseResult list(WmChannelPageReqDto dto) {
		//1、检查参数
		if(dto == null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}
		//检查分页参数
		dto.checkParam();

		IPage page = new Page(dto.getPage(), dto.getSize());
		//构造查询条件
		LambdaQueryWrapper<WmChannel> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.like(WmChannel::getName, dto.getName());
		page = page(page, queryWrapper);

		//封装查询结果
		ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
		responseResult.setData(page.getRecords());
		return responseResult;
	}

	/**
	 * 删除频道
	 *
	 * @param id
	 * @return
	 */
	@Override
	public ResponseResult del(Integer id) {
		if(id == null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}
		removeById(id);
		return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
	}

	/**
	 * 查询所有频道
	 *
	 * @return
	 */
	@Override
	public ResponseResult findAllChannel() {
		return ResponseResult.okResult(list());
	}
}
