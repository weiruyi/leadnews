package com.heima.article.controller.v1;

import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.common.dtos.ResponseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/article")
@RequiredArgsConstructor
@Slf4j
public class ArticleHomeController {
	private final ApArticleService articleService;


	@PostMapping("/load")
	public ResponseResult load(@RequestBody ArticleHomeDto dto) {
		log.info("加载文章内容，dto={}", dto.toString());
//		ResponseResult responseResult = articleService.load(dto, ArticleConstants.LOADTYPE_LOAD_MORE);
//		return responseResult;
		return articleService.load2(dto, ArticleConstants.LOADTYPE_LOAD_MORE,true);
	}


	@PostMapping("/loadmore")
	public ResponseResult loadMore(@RequestBody ArticleHomeDto dto) {
		log.info("加载更多文章内容，dto={}", dto.toString());
		ResponseResult responseResult = articleService.load(dto, ArticleConstants.LOADTYPE_LOAD_MORE);
		return responseResult;
	}

	@PostMapping("/loadnew")
	public ResponseResult loadNew(@RequestBody ArticleHomeDto dto) {
		log.info("加载更新文章内容，dto={}", dto.toString());
		ResponseResult responseResult = articleService.load(dto, ArticleConstants.LOADTYPE_LOAD_NEW);
		return responseResult;
	}
}