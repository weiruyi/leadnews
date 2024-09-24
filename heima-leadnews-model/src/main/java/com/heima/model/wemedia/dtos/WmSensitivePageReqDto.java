package com.heima.model.wemedia.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import lombok.Data;

@Data
public class WmSensitivePageReqDto extends PageRequestDto {
	private String name;
}
