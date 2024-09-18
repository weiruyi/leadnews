package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {
	private final WmNewsMaterialMapper wmNewsMaterialMapper;
	private final WmMaterialMapper wmMaterialMapper;
	private final WmNewsAutoScanService wmNewsAutoScanService;

	/**
	 * 查询文章
	 *
	 * @param dto
	 * @return
	 */
	@Override
	public ResponseResult findAll(WmNewsPageReqDto dto) {
		if(dto==null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}
		//获取登录信息
		WmUser user = WmThreadLocalUtil.getUser();
		if(user==null){
			return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
		}

		//1、校验分页参数
		dto.checkParam();

		IPage page = new Page(dto.getPage(), dto.getSize());

		//构造分页查询条件
		LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();
		//用户
		queryWrapper.eq(WmNews::getUserId, user.getId());
		//状态
		if(dto.getStatus() != null){
			queryWrapper.eq(WmNews::getStatus, dto.getStatus());
		}
		//时间范围
		if(dto.getBeginPubDate() != null && dto.getEndPubDate() != null){
			queryWrapper.between(WmNews::getCreatedTime, dto.getBeginPubDate(), dto.getEndPubDate());
		}
		//所属频道id
		if(dto.getChannelId() != null){
			queryWrapper.eq(WmNews::getChannelId, dto.getChannelId());
		}
		//关键词模糊查询
		if(dto.getKeyword() != null){
			queryWrapper.like(WmNews::getTitle, dto.getKeyword());
		}

		//按时间倒序排序
		queryWrapper.orderByDesc(WmNews::getCreatedTime);

		//分页查询
		page = page(page, queryWrapper);
		//封装查询结果
		ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
		responseResult.setData(page.getRecords());
		return responseResult;
	}

	/**
	 * 发布文章或保存为草稿
	 *
	 * @param dto
	 * @return
	 */
	@Transactional
	@Override
	public ResponseResult submitNews(WmNewsDto dto) {
		if(dto==null || dto.getContent() == null){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}
		WmUser user = WmThreadLocalUtil.getUser();
		if(user==null){
			return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
		}
		WmNews wmNews = new WmNews();
		BeanUtils.copyProperties(dto, wmNews);
		wmNews.setPublishTime(dto.getPublishTime());
		wmNews.setUserId(user.getId());
		//image list->string
		if(dto.getImages() != null && dto.getImages().size() > 0){
			String imageStr = StringUtils.join(dto.getImages(), ",");
			wmNews.setImages(imageStr);
		}

		//封面类型为自动的时候，type设为null
		if(dto.getType() == -1){
			wmNews.setType(null);
		}

		//保存或者修改文章
		saveOrUpdateWmNews(wmNews);

		//判断是否为草稿，如果是草稿则结束
		if(dto.getStatus().equals(WmNews.Status.NORMAL.getCode())){
			return ResponseResult.errorResult(AppHttpCodeEnum.SUCCESS);
		}

		//不是草稿
		//保存文章内容图片与素材的关系
		List<String> materials = extractUrlInfo(dto.getContent());
		saveRelativeInfoForContent(materials,wmNews.getId());

		//保存文章封面图片与素材的关系，如果当前布局是自动，需要匹配封面图片
		saveRelativeInfoForCover(dto,wmNews,materials);

		//异步审核文章
		wmNewsAutoScanService.autoScanWmNews(wmNews.getId());

		return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

	}

	// 第一个功能：如果当前封面类型为自动，则设置封面类型的数据
	//     * 匹配规则：
	//     * 1，如果内容图片大于等于1，小于3  单图  type 1
	//     * 2，如果内容图片大于等于3  多图  type 3
	//     * 3，如果内容没有图片，无图  type 0
	//     *
	// 第二个功能：保存封面图片与素材的关系
	private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {
		List<String> images = dto.getImages();

		//如果当前封面类型为自动，则设置封面类型的数据
		if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
			//多图
			if(materials.size() >= 3){
				wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
				images = materials.stream().limit(3).collect(Collectors.toList());
			}else if(materials.size() >= 1 && materials.size() < 3){
				//单图
				wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
				images = materials.stream().limit(1).collect(Collectors.toList());
			}else {
				//无图
				wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
			}

			//修改文章
			if(images != null && images.size() > 0){
				wmNews.setImages(StringUtils.join(images,","));
			}
			updateById(wmNews);
		}
		if(images != null && images.size() > 0){
			saveRelativeInfo(images,wmNews.getId(),WemediaConstants.WM_COVER_REFERENCE);
		}
	}

	//保存文章内容图片与素材的关系
	private void saveRelativeInfoForContent(List<String> materials, Integer id) {
		saveRelativeInfo(materials, id, WemediaConstants.WM_CONTENT_REFERENCE);
	}

	/**
	 * 保存文章图片与素材的关系到数据库中
	 * @param materials
	 * @param newsId
	 * @param type
	 */
	private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
		if(materials!=null && !materials.isEmpty()){
			//通过图片的url查询素材的id
			List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials));

			//判断素材是否有效
			if(dbMaterials==null || dbMaterials.size() == 0){
				//手动抛出异常   第一个功能：能够提示调用者素材失效了，第二个功能，进行数据的回滚
				throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
			}

			if(materials.size() != dbMaterials.size()){
				throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
			}

			List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());

			//批量保存
			wmNewsMaterialMapper.saveRelations(idList,newsId,type);
		}

	}

	//从文章内容中提取图片url
	private List<String> extractUrlInfo(String content) {
		List<String> materials = new ArrayList<>();
		List<Map> list = JSON.parseArray(content, Map.class);
		for (Map map : list) {
			String type = map.get("type").toString();
			if(type.equals(WemediaConstants.WM_NEWS_TYPE_IMAGE)){
				materials.add(map.get("value").toString());
			}
		}
		return materials;
	}


	//保存或者修改文章信息
	private void saveOrUpdateWmNews(WmNews wmNews) {
		//默认上架
		wmNews.setEnable((short) 1);
		if(wmNews.getId() == null){
			//保存
			save(wmNews);
		}else{
			//修改
			//删除文章与素材的关系
			wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
			updateById(wmNews);
		}
	}
}
