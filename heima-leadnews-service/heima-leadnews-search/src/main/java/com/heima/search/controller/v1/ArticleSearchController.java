package com.heima.search.controller.v1;


import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.service.ApArticleSearchService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.rest.RestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping("/api/v1/article/search")
public class ArticleSearchController {

	@Autowired
	private ApArticleSearchService articleSearchService;

	@PostMapping("/search")
	public ResponseResult search(@RequestBody UserSearchDto dto) throws IOException {
		return articleSearchService.search(dto);
	}
}
