package com.heima.model.behavior.dtos;


import lombok.Data;

@Data
public class LikesBehaviorDto {
	private Long articleId;
	/**
	 * 0 点赞   1 取消点赞
	 */
	private Short operation;
	/**
	 * 0文章  1动态   2评论
	 */
	private Short type;
}
