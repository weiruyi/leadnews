package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.article.IArticleClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {
	private final WmNewsMapper wmNewsMapper;
	private final FileStorageService fileStorageService;
	private final IArticleClient articleClient;
	private final WmChannelMapper wmChannelMapper;
	private final WmUserMapper wmUserMapper;

	/**
	 * 自媒体文章审核
	 *
	 * @param id
	 */
	@Override
	public void autoScanWmNews(Integer id) {
		//1、查询文章内容
		WmNews wmNews = wmNewsMapper.selectById(id);
		if(wmNews == null){
			throw new RuntimeException("WmNewsAutoScanServiceImpl-文章不存在");
		}
		if(wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())){
			//提取图片和文字信息
			Map<String, Object> textAndImages = extractTextAndImages(wmNews);

			//2、审核文章内容，阿里云
			boolean isTextScan = handleTextScan(textAndImages.get("text").toString(), wmNews);
			if(!isTextScan) return;

			//3、审核图片内容
			boolean isImageScan = handleImageScan((List<String>) textAndImages.get("images"), wmNews);
			if(!isImageScan) return;

			//4、审核成功，保存app端文章数据
			ResponseResult responseResult = saveAppArticle(wmNews);
			if(!responseResult.getCode().equals(200)){
				throw new RuntimeException("WmNewsAutoScanServiceImpl-文章审核，app保存相关内容失败");
			}

			//回写articleId
			wmNews.setArticleId((Long) responseResult.getData());
			updateWmNews(wmNews,(short) 9,"审核成功");
		}




	}

	/**
	 * 保存app端文章信息
	 * @param wmNews
	 * @return
	 */
	private ResponseResult saveAppArticle(WmNews wmNews) {
		ArticleDto dto = new ArticleDto();
		//属性的拷贝
		BeanUtils.copyProperties(wmNews,dto);
		//文章的布局
		dto.setLayout(wmNews.getType());
		//频道
		WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
		if(wmChannel != null){
			dto.setChannelName(wmChannel.getName());
		}

		//作者
		dto.setAuthorId(wmNews.getUserId().longValue());
		WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
		if(wmUser != null){
			dto.setAuthorName(wmUser.getName());
		}

		//设置文章id
		if(wmNews.getArticleId() != null){
			dto.setId(wmNews.getArticleId());
		}
		dto.setCreatedTime(new Date());

		ResponseResult responseResult = articleClient.saveArticle(dto);
		return responseResult;

	}

	/**
	 * 调用阿里云审核图片信息
	 * @param images
	 * @param wmNews
	 * @return
	 */
	private boolean handleImageScan(List<String> images, WmNews wmNews) {
		boolean flag = true;

		if(images == null || images.size() == 0){
			return flag;
		}

		//下载图片 minIO
		//图片去重
		images = images.stream().distinct().collect(Collectors.toList());

		List<byte[]> imageList = new ArrayList<>();

		for (String image : images) {
			byte[] bytes = fileStorageService.downLoadFile(image);
			imageList.add(bytes);
		}


		//审核图片
		try {
			//模拟阿里云审核成功
			Map<String, String>  map = new HashMap<>();
			map.put("suggestion", "success");

			if(map != null){
				//审核失败
				if(map.get("suggestion").equals("block")){
					flag = false;
					updateWmNews(wmNews, (short) 2, "当前文章中存在违规内容");
				}

				//不确定信息  需要人工审核
				if(map.get("suggestion").equals("review")){
					flag = false;
					updateWmNews(wmNews, (short) 3, "当前文章中存在不确定内容");
				}
			}

		} catch (Exception e) {
			flag = false;
			e.printStackTrace();
		}
		return flag;
	}

	/**
	 * 调用阿里云审核文本内容
	 * @param text
	 * @param wmNews
	 * @return
	 */
	private boolean handleTextScan(String text, WmNews wmNews) {
		boolean flag = true;
		text += wmNews.getTitle();
		if(text.length() == 0){
			return flag;
		}

		try{
			//模拟阿里云审核成功
			Map<String, String>  map = new HashMap<>();
			map.put("suggestion", "success");

			if(map != null){
				//审核失败
				if(map.get("suggestion").equals("block")){
					flag = false;
					updateWmNews(wmNews, (short) 2, "当前文章中存在违规内容");
				}

				//不确定信息  需要人工审核
				if(map.get("suggestion").equals("review")){
					flag = false;
					updateWmNews(wmNews, (short) 3, "当前文章中存在不确定内容");
				}
			}

		}catch (Exception e){
			flag = false;
			e.printStackTrace();
		}
		return flag;

	}

	/**
	 * 更新文章审核信息
	 * @param wmNews
	 * @param status
	 * @param reason
	 */
	private void updateWmNews(WmNews wmNews, short status, String reason) {
		wmNews.setStatus(status);
		wmNews.setReason(reason);
		wmNewsMapper.updateById(wmNews);
	}

	//从内容中提取纯文本和图片信息
	private Map<String, Object> extractTextAndImages(WmNews wmNews) {
		StringBuilder sb = new StringBuilder();

		List<String> images = new ArrayList<>();
		//提取内容中的图片
		String content = wmNews.getContent();
		if(StringUtils.isNotBlank(content)){
			List<Map> list = JSON.parseArray(content, Map.class);
			for(Map map : list){
				if(map.get("type").equals("text")){
					sb.append(map.get("value"));
				} else if (map.get("type").equals("image")) {
					images.add(map.get("value").toString());
				}
			}
		}
		//提取封面图片
		String faceImages = wmNews.getImages();
		if(StringUtils.isNotBlank(faceImages)){
			String[] split = faceImages.split(",");
			images.addAll(Arrays.asList(split));
		}

		Map<String, Object> textAndImages = new HashMap<>();
		textAndImages.put("text", sb.toString());
		textAndImages.put("images", images);
		return textAndImages;
	}
}
