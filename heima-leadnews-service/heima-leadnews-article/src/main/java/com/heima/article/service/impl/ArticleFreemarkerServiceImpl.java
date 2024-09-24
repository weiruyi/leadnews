package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.search.vos.SearchArticleVo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {
	@Autowired
	private FileStorageService fileStorageService;
	@Autowired
	@Lazy
	private ApArticleService apArticleService;
	@Autowired
	private Configuration configuration;
	@Autowired
	private KafkaTemplate<String,String> kafkaTemplate;


	/**
	 * 生成静态文件上传minio中
	 *
	 * @param article
	 * @param content
	 */
	@Override
	@Async
	public void buildArticleToMinio(ApArticle article, String content) {
		if(StringUtils.isNotBlank(content)){
			//2.文章内容通过freemarker生成html文件
			StringWriter out = new StringWriter();
			try {
				Template template = configuration.getTemplate("article.ftl");
				Map<String, Object> params = new HashMap<>();
				params.put("content", JSONArray.parseArray(content));
				template.process(params, out);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			//3.把html文件上传到minio中
			InputStream is = new ByteArrayInputStream(out.toString().getBytes());
			String path = fileStorageService.uploadHtmlFile("", article.getId() + ".html", is);

			//4.修改ap_article表，保存static_url字段
			apArticleService.update(Wrappers.<ApArticle>lambdaUpdate().eq(ApArticle::getId,article.getId())
					.set(ApArticle::getStaticUrl,path));

			//发送消息，创建索引
			createArticleESIndex(article,content,path);

		}
	}

	//发送消息，创建索引
	private void createArticleESIndex(ApArticle article, String content, String path) {
		SearchArticleVo vo = new SearchArticleVo();
		BeanUtils.copyProperties(article,vo);
		vo.setContent(content);
		vo.setStaticUrl(path);

		kafkaTemplate.send(ArticleConstants.ARTICLE_ES_SYNC_TOPIC, JSON.toJSONString(vo));
	}
}
