package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.ApUserRealnamePageReqDto;
import com.heima.model.user.pojos.ApUserRealname;

public interface ApUserRealnameService extends IService<ApUserRealname> {
	/**
	 * 用户信息分页查询
	 * @param dto
	 * @return
	 */
	public ResponseResult list(ApUserRealnamePageReqDto dto);

	/**
	 * 审核
	 * @param dto
	 * @param status
	 * @return
	 */
	public ResponseResult auth(ApUserRealnamePageReqDto dto, Short status);
}
