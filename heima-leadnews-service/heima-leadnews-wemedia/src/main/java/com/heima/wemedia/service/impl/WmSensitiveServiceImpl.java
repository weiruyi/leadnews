package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmSensitiveDto;
import com.heima.model.wemedia.dtos.WmSensitivePageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmSensitiveService;
import org.springframework.stereotype.Service;

@Service
public class WmSensitiveServiceImpl extends ServiceImpl<WmSensitiveMapper, WmSensitive> implements WmSensitiveService {
	/**
	 * 敏感词分页查询
	 *
	 * @param dto
	 * @return
	 */
	@Override
	public ResponseResult list(WmSensitivePageReqDto dto) {
		if(dto==null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}

		//1、校验分页参数
		dto.checkParam();

		IPage page = new Page(dto.getPage(), dto.getSize());

		//构造分页查询条件
		LambdaQueryWrapper<WmSensitive> queryWrapper = new LambdaQueryWrapper<>();

		queryWrapper.like(WmSensitive::getSensitives, dto.getName());

		//分页查询
		page = page(page, queryWrapper);
		//封装查询结果
		ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
		responseResult.setData(page.getRecords());
		return responseResult;
	}

	/**
	 * 删除敏感词
	 *
	 * @param id
	 * @return
	 */
	@Override
	public ResponseResult del(Integer id) {
		if(id==null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}
		remove(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getId, id));
		return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
	}

	/**
	 * 添加敏感词
	 *
	 * @param dto
	 * @return
	 */
	@Override
	public ResponseResult saveOrUpdate(WmSensitiveDto dto) {
		if(dto==null || StringUtils.isBlank(dto.getSensitives())){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}

		WmSensitive wmSensitive = new WmSensitive();
		wmSensitive.setSensitives(dto.getSensitives());
		if(dto.getCreatedTime() != null){
			wmSensitive.setCreatedTime(dto.getCreatedTime());
		}
		if(dto.getId() != null){
			wmSensitive.setId(dto.getId());
			updateById(wmSensitive);
		}else{
			save(wmSensitive);
		}
		return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
	}
}
