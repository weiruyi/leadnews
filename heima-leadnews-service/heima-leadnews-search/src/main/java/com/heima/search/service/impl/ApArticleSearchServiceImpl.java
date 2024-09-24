package com.heima.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.service.ApArticleSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApArticleSearchServiceImpl implements ApArticleSearchService {

	private final RestHighLevelClient restHighLevelClient;

	/**
	 * ES文章分页搜索
	 *
	 * @param dto
	 * @return
	 */
	@Override
	public ResponseResult search(UserSearchDto dto) {
		//1、检查参数
		if(dto == null || StringUtils.isBlank(dto.getSearchWords())){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}
		//2、设置查询条件
		SearchRequest searchRequest = new SearchRequest("app_info_article");
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		//布尔查询
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
		//关键字的分词之后查询
		QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(dto.getSearchWords()).field("title").field("content").defaultOperator(Operator.OR);
		boolQueryBuilder.must(queryStringQueryBuilder);
		//查询小于mindate的数据
//		RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("publishTime").lt(dto.getMinBehotTime().getTime());
//		boolQueryBuilder.filter(rangeQueryBuilder);

		//分页查询
		sourceBuilder.from(0);
		sourceBuilder.size(dto.getPageSize());

		//按照发布时间倒序查询
		sourceBuilder.sort("publishTime", SortOrder.DESC);

		//设置高亮  title
		HighlightBuilder highlightBuilder = new HighlightBuilder();
		highlightBuilder.field("title");
		highlightBuilder.preTags("<font style='color: red; font-size: inherit;'>");
		highlightBuilder.postTags("</font>");
		sourceBuilder.highlighter(highlightBuilder);

		sourceBuilder.query(boolQueryBuilder);
		searchRequest.source(sourceBuilder);
		SearchResponse searchResponse= null;
		try {
			searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		//3、封装结果
		List<Map> list = new ArrayList<>();

		SearchHit[] hits = searchResponse.getHits().getHits();
		for (SearchHit hit : hits) {
			String json = hit.getSourceAsString();
			Map map = JSON.parseObject(json, Map.class);
			//处理高亮
			if(hit.getHighlightFields() != null && hit.getHighlightFields().size() > 0){
				Text[] titles = hit.getHighlightFields().get("title").getFragments();
				String title = StringUtils.join(titles);
				//高亮标题
				map.put("h_title",title);
			}else {
				//原始标题
				map.put("h_title",map.get("title"));
			}
			list.add(map);
		}

		return ResponseResult.okResult(list);
	}
}
