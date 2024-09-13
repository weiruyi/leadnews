package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.utils.common.AppJwtUtil;
import com.heima.utils.common.MD5Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService {


	/**
	 * app端登录
	 *
	 * @param loginDto
	 * @return
	 */
	@Override
	public ResponseResult login(LoginDto loginDto) {
		String phone = loginDto.getPhone();
		String password = loginDto.getPassword();
		if(StringUtils.isNotBlank(phone) && StringUtils.isNotBlank(password)){
			//1、正常登录（手机号+密码）
			ApUser dbUser = getOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, phone));
			if(dbUser == null){
				return ResponseResult.errorResult(AppHttpCodeEnum.AP_USER_DATA_NOT_EXIST, "用户不存在");
			}

			String salt = dbUser.getSalt();
			String dbPsaaword = dbUser.getPassword();
			password = DigestUtils.md5DigestAsHex((password + salt).getBytes());
			if(!password.equals(dbPsaaword)){
				return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
			}

			Map<String, Object> map = new HashMap<>();
			map.put("token", AppJwtUtil.getToken(dbUser.getId().longValue()));
			dbUser.setSalt("");
			dbUser.setPassword("");
			map.put("user", dbUser);
			return ResponseResult.okResult(map);

		}else{
			//2、游客登录，返回jwt用户id=0
			Map<String, Object> map = new HashMap<>();
			map.put("token", AppJwtUtil.getToken(0L));
			return ResponseResult.okResult(map);

		}
	}


}
