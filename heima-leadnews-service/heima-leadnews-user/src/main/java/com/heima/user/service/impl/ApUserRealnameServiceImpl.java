package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.ApUserRealnamePageReqDto;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {
	@Autowired
	private ApUserRealnameMapper apUserRealnameMapper;

	/**
	 * 用户信息分页查询
	 *
	 * @param dto
	 * @return
	 */
	@Override
	public ResponseResult list(ApUserRealnamePageReqDto dto) {
		if(dto == null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}
		dto.checkParam();
		IPage page = new Page(dto.getPage(), dto.getSize());
		LambdaQueryWrapper<ApUserRealname> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApUserRealname::getStatus, dto.getStatus());
		page = page(page, queryWrapper);

		ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
		responseResult.setData(page.getRecords());
		return responseResult;
	}

	/**
	 * 审核
	 * @param dto
	 * @param status
	 * @return
	 */
	@Override
	public ResponseResult auth(ApUserRealnamePageReqDto dto, Short status) {
		if(dto == null || dto.getId() == null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}

		ApUserRealname apUserRealname = apUserRealnameMapper.selectById(dto.getId());
		if(apUserRealname == null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}

		apUserRealname.setStatus(status);
		apUserRealname.setReason(dto.getMsg());
		apUserRealnameMapper.updateById(apUserRealname);
		return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
	}
}
