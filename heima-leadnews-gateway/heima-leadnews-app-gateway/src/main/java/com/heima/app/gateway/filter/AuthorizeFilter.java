package com.heima.app.gateway.filter;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.heima.app.gateway.utils.AppJwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthorizeFilter implements Ordered, GlobalFilter {
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		//1、获取request和response对象
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		//2、判断是否是登录
		if(request.getURI().getPath().contains("/login")){
			//放行
			return chain.filter(exchange);
		}

		//3、获取token
		String token = request.getHeaders().getFirst("token");
		if(StringUtils.isBlank(token)){
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return response.setComplete();
		}
		try{
			//解析token
			Claims claimsBody = AppJwtUtil.getClaimsBody(token);
			//判断是否过期
			int result = AppJwtUtil.verifyToken(claimsBody);
			if(result == 1 || result==2){
				response.setStatusCode(HttpStatus.UNAUTHORIZED);
				return response.setComplete();
			}

			//获取用户id
			Object userId = claimsBody.get("id");
			//存入header中
			ServerHttpRequest serverHttpRequest = request.mutate().headers(httpHeaders -> {
				httpHeaders.add("userId", userId + "");
			}).build();
			//重置请求
			exchange.mutate().request(serverHttpRequest);

		}catch (Exception e){
			e.printStackTrace();
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return response.setComplete();
		}

		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
