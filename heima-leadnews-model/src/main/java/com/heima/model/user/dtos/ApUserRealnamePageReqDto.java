package com.heima.model.user.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import lombok.Data;

@Data
public class ApUserRealnamePageReqDto extends PageRequestDto {
	private Integer id;
	private String msg;
	private Integer status;
}
