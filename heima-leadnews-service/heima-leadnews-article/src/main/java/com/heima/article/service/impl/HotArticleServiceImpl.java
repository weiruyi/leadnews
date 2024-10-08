package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class HotArticleServiceImpl implements HotArticleService {

	private final ApArticleMapper apArticleMapper;
	@Autowired
	private IWemediaClient wemediaClient;
	@Autowired
	private CacheService cacheService;

	/**
	 * 计算热点文章
	 */
	@Override
	public void computeHotArticle() {
		//1、查询前5天的文章数据
		Date dateParam = DateTime.now().minusDays(15).toDate();
		List<ApArticle> apArticleList = apArticleMapper.findArticleListByLast5days(dateParam);

		//2、计算文章分值
		List<HotArticleVo> hotArticleVoList =  computeHotArticle(apArticleList);

		//3、为每个频道缓存30条分值较高的文章
		cacheTagToRedis(hotArticleVoList);

	}

	/**
	 * 为每个频道缓存30条分值较高的文章
	 * @param hotArticleVoList
	 */
	private void cacheTagToRedis(List<HotArticleVo> hotArticleVoList) {
		ResponseResult responseResult = wemediaClient.getChannels();
		if (responseResult.getCode() == 200) {
			String jsonString = JSON.toJSONString(responseResult.getData());
			List<WmChannel> wmChannels = JSON.parseArray(jsonString, WmChannel.class);
			//检索出每个频道的文章
			if(wmChannels != null && wmChannels.size() > 0){
				for (WmChannel wmChannel : wmChannels) {
					List<HotArticleVo> hotArticleVos = null;
					try {
						hotArticleVos = hotArticleVoList.stream().filter(x -> x.getChannelId().equals(wmChannel.getId())).collect(Collectors.toList());
					} catch (Exception e) {
						log.info("error, channelId:{}", wmChannel.getId());
					}
					//给文章进行排序，取30条分值较高的文章存入redis  key：频道id   value：30条分值较高的文章
					sortAndCache(hotArticleVos, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + wmChannel.getId());
				}
			}
		}
		//设置推荐数据
		//给文章进行排序，取30条分值较高的文章存入redis  key：频道id   value：30条分值较高的文章
		sortAndCache(hotArticleVoList, ArticleConstants.HOT_ARTICLE_FIRST_PAGE+ArticleConstants.DEFAULT_TAG);
	}

	/**
	 * 排序并且缓存数据
	 * @param hotArticleVos
	 * @param key
	 */
	private void sortAndCache(List<HotArticleVo> hotArticleVos, String key) {
		if(hotArticleVos != null && hotArticleVos.size() > 0){
			hotArticleVos = hotArticleVos.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
			if (hotArticleVos.size() > 30) {
				hotArticleVos = hotArticleVos.subList(0, 30);
			}
		}
		cacheService.set(key, JSON.toJSONString(hotArticleVos));
	}

	/**
	 * 计算文章分数
	 * @param apArticleList
	 * @return
	 */
	private List<HotArticleVo> computeHotArticle(List<ApArticle> apArticleList) {
		List<HotArticleVo> hotArticleVoList = new ArrayList<>();

		if(apArticleList != null && apArticleList.size() > 0) {
			for (ApArticle apArticle : apArticleList) {
				HotArticleVo hotArticleVo = new HotArticleVo();
				BeanUtils.copyProperties(apArticle, hotArticleVo);
				//计算分数
				Integer score = computeScore(apArticle);
				hotArticleVo.setScore(score);
				hotArticleVoList.add(hotArticleVo);
			}
		}
		return hotArticleVoList;
	}

	//计算文章的具体分数
	private Integer computeScore(ApArticle apArticle) {
		Integer scere = 0;
		if(apArticle.getLikes() != null){
			scere += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
		}
		if(apArticle.getViews() != null){
			scere += apArticle.getViews();
		}
		if(apArticle.getComment() != null){
			scere += apArticle.getComment() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
		}
		if(apArticle.getCollection() != null){
			scere += apArticle.getCollection() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
		}
		return scere;
	}
}
