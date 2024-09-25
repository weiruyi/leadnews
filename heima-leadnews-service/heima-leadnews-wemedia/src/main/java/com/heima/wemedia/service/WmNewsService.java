package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import org.springframework.web.bind.annotation.RequestBody;

public interface WmNewsService extends IService<WmNews> {
	/**
	 * 人工审核
	 * @param dto
	 * @param status
	 * @return
	 */
	public ResponseResult auth(WmNewsPageReqDto dto, Short status);

	/**
	 * 查询文章详细信息
	 * @param id
	 * @return
	 */
	public ResponseResult oneVo(Integer id);


	/**
	 * 人工审核分页查询
	 * @param dto
	 * @return
	 */
	public ResponseResult listVo(WmNewsPageReqDto dto);

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
