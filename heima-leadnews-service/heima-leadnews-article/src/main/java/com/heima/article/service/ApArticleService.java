package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;

public interface ApArticleService extends IService<ApArticle> {
	/**
	 * 加载文章列表
	 * @param articleHomeDto
	 * @param type 1、加载更多 2、加载最新
	 * @return
	 */
	public ResponseResult load(ArticleHomeDto articleHomeDto, Short type);

	/**
	 * 保存文章信息
	 * @param dto
	 * @return
	 */
	public ResponseResult saveArticle(ArticleDto dto);

	/**
	 * 加载文章详情 数据回显
	 * @param dto
	 * @return
	 */
	public ResponseResult loadArticleBehavior(ArticleInfoDto dto);

	/**
	 * 加载文章列表
	 * @param dto
	 * @param type  1 加载更多   2 加载最新
	 * @param firstPage  true  是首页  flase 非首页
	 * @return
	 */
	public ResponseResult load2(ArticleHomeDto dto,Short type,boolean firstPage);
}
