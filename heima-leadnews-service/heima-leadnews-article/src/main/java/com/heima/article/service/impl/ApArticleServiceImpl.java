package com.heima.article.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {
	private final ApArticleMapper apArticleMapper;
	private final ApArticleConfigMapper apArticleConfigMapper;
	private final ApArticleContentMapper apArticleContentMapper;
	private final ArticleFreemarkerService articleFreemarkerService;
	@Autowired
	private CacheService cacheService;

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
	 * 加载文章列表
	 * @param dto
	 * @param type      1 加载更多   2 加载最新
	 * @param firstPage true  是首页  flase 非首页
	 * @return
	 */
	@Override
	public ResponseResult load2(ArticleHomeDto dto, Short type, boolean firstPage) {
		if(firstPage){
			String jsonStr = cacheService.get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
			if(StringUtils.isNotBlank(jsonStr)){
				List<HotArticleVo> hotArticleVoList = JSON.parseArray(jsonStr, HotArticleVo.class);
				ResponseResult responseResult = ResponseResult.okResult(hotArticleVoList);
				return responseResult;
			}
		}
		return load(dto,type);
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

		//异步调用，生成静态文件并上传minio
		articleFreemarkerService.buildArticleToMinio(apArticle, dto.getContent());

		//结果返回，返回文章id
		return ResponseResult.okResult(apArticle.getId());
	}

	/**
	 * 加载文章详情 数据回显
	 *
	 * @param dto
	 * @return
	 */
	@Override
	public ResponseResult loadArticleBehavior(ArticleInfoDto dto) {

		//0.检查参数
		if (dto == null || dto.getArticleId() == null || dto.getAuthorId() == null) {
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}

		//{ "isfollow": true, "islike": true,"isunlike": false,"iscollection": true }
		boolean isfollow = false, islike = false, isunlike = false, iscollection = false;

		ApUser user = AppThreadLocalUtil.getUser();
		if(user != null){
			//喜欢行为
			String likeBehaviorJson = (String) cacheService.hGet(BehaviorConstants.LIKE_BEHAVIOR + dto.getArticleId().toString(), user.getId().toString());
			if(StringUtils.isNotBlank(likeBehaviorJson)){
				islike = true;
			}
			//不喜欢的行为
			String unLikeBehaviorJson = (String) cacheService.hGet(BehaviorConstants.UN_LIKE_BEHAVIOR + dto.getArticleId().toString(), user.getId().toString());
			if(StringUtils.isNotBlank(unLikeBehaviorJson)){
				isunlike = true;
			}
			//是否收藏
			String collctionJson = (String) cacheService.hGet(BehaviorConstants.COLLECTION_BEHAVIOR+user.getId(),dto.getArticleId().toString());
			if(StringUtils.isNotBlank(collctionJson)){
				iscollection = true;
			}

			//是否关注
			Double score = cacheService.zScore(BehaviorConstants.APUSER_FOLLOW_RELATION + user.getId(), dto.getAuthorId().toString());
			System.out.println(score);
			if(score != null){
				isfollow = true;
			}

		}

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("isfollow", isfollow);
		resultMap.put("islike", islike);
		resultMap.put("isunlike", isunlike);
		resultMap.put("iscollection", iscollection);

		return ResponseResult.okResult(resultMap);
	}


}
