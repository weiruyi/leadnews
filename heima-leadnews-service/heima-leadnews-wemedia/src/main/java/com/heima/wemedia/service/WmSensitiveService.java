package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmSensitiveDto;
import com.heima.model.wemedia.dtos.WmSensitivePageReqDto;
import com.heima.model.wemedia.pojos.WmSensitive;

public interface WmSensitiveService extends IService<WmSensitive> {
	/**
	 * 敏感词分页查询
	 * @param dto
	 * @return
	 */
	public ResponseResult list(WmSensitivePageReqDto dto);

	/**
	 * 删除敏感词
	 * @param id
	 * @return
	 */
	public ResponseResult del(Integer id);

	/**
	 * 添加敏感词
	 * @param dto
	 * @return
	 */
	public ResponseResult saveOrUpdate(WmSensitiveDto dto);
}
