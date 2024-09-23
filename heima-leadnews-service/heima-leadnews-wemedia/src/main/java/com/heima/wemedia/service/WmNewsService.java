package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import org.springframework.web.bind.annotation.RequestBody;

public interface WmNewsService extends IService<WmNews> {

	/**
	 * 查询文章
	 * @param dto
	 * @return
	 */
	public ResponseResult findAll(WmNewsPageReqDto dto);

	/**
	 * 发布文章或保存为草稿
	 * @param dto
	 * @return
	 */
	public ResponseResult submitNews(WmNewsDto dto);

	/**
	 * 文章上下架
	 * @param wmNewsDto
	 * @return
	 */
	public ResponseResult downOrUp(WmNewsDto wmNewsDto);
}
