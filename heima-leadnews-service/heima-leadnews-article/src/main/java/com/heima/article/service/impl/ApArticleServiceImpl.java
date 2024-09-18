package com.heima.article.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {
	private final ApArticleMapper apArticleMapper;
	private final ApArticleConfigMapper apArticleConfigMapper;
	private final ApArticleContentMapper apArticleContentMapper;

	private final static short MAX_PAGE_SIZE = 50;

	/**
	 * 加载文章列表
	 *
	 * @param articleHomeDto
	 * @param type           1、加载更多 2、加载最新
	 * @return
	 */
	@Override
	public ResponseResult load(ArticleHomeDto articleHomeDto, Short type) {
		//1、参数校验
		//分页参数
		Integer size = articleHomeDto.getSize();
		if(size == null || size <= 0){
			size = 10;
		}
		size = Math.min(size, MAX_PAGE_SIZE);
		articleHomeDto.setSize(size);

		//类型参数
		if(type != ArticleConstants.LOADTYPE_LOAD_MORE && type != ArticleConstants.LOADTYPE_LOAD_NEW){
			type = ArticleConstants.LOADTYPE_LOAD_MORE;
		}

		//文章频道
		if(StringUtils.isBlank(articleHomeDto.getTag())){
			articleHomeDto.setTag(ArticleConstants.DEFAULT_TAG);
		}

		//时间
		if(articleHomeDto.getMaxBehotTime() == null) articleHomeDto.setMaxBehotTime(new Date());
		if(articleHomeDto.getMinBehotTime() == null) articleHomeDto.setMinBehotTime(new Date());

		//2、数据查询
		List<ApArticle> apArticles = apArticleMapper.loadArticleList(articleHomeDto, type);
		ResponseResult responseResult = ResponseResult.okResult(apArticles);
		return responseResult;
	}

	/**
	 * 保存文章信息
	 *
	 * @param dto
	 * @return
	 */
	@Override
	@Transactional
	public ResponseResult saveArticle(ArticleDto dto) {
		//1、检查参数
		if(dto == null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}

		//2、判断是否存在id
		ApArticle apArticle = new ApArticle();
		BeanUtils.copyProperties(dto, apArticle);

		//2.1 不存在  添加 文章，文章配置，文章内容
		if(dto.getId() == null){
			//添加文章
			save(apArticle);
			//保存配置
			ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
			apArticleConfigMapper.insert(apArticleConfig);
			//文章内容
			ApArticleContent apArticleContent = new ApArticleContent();
			apArticleContent.setArticleId(apArticle.getId());
			apArticleContent.setContent(dto.getContent());
			apArticleContentMapper.insert(apArticleContent);
		}else{
			//2.2 存在 修改 文章，文章内容
			//文章
			updateById(apArticle);
			//文章内容
			ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getId()));
			apArticleContent.setContent(dto.getContent());
			apArticleContentMapper.updateById(apArticleContent);
		}

		//结果返回，返回文章id
		return ResponseResult.okResult(apArticle.getId());
	}
}
