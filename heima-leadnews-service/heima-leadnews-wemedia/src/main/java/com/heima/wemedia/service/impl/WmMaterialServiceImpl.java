package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {
	@Autowired
	private FileStorageService fileStorageService;

	/**
	 * 上传图片到minio
	 *
	 * @param multipartFile
	 * @return
	 */
	@Override
	@Transactional
	public ResponseResult uploadPicture(MultipartFile multipartFile) {
		//1、检查参数
		if(multipartFile == null || multipartFile.getSize() == 0){
			return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
		}

		//2、上传到minio
		String filename = UUID.randomUUID().toString().replaceAll("-", "");
		String originalFilename = multipartFile.getOriginalFilename();
		String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));

		String fileId = null;
		try {
			fileId = fileStorageService.uploadImgFile("", filename + postfix, multipartFile.getInputStream());
			log.info("上传图片到minio，fileId:{}", fileId);
		} catch (IOException e) {
			e.printStackTrace();
			log.info("wmMaterialService上传图片到minio失败");
			return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
		}

		//3、写入数据库
		WmMaterial wmMaterial = new WmMaterial();
		wmMaterial.setUserId(WmThreadLocalUtil.getUser().getId());
		wmMaterial.setUrl(fileId);
		wmMaterial.setIsCollection((short)0);
		wmMaterial.setCreatedTime(new Date());
		wmMaterial.setType((short)0);
		save(wmMaterial);

		//4、返回参数
		return ResponseResult.okResult(wmMaterial);
	}
}
