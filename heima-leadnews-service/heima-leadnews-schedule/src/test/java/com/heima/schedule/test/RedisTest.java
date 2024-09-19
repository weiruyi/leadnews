package com.heima.schedule.test;

import com.heima.common.redis.CacheService;
import com.heima.schedule.ScheduleApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class RedisTest {

	@Autowired
	private CacheService cacheService;

	@Test
	public void testList(){
//		cacheService.lLeftPush("list_001", "hello-list");
		String list001 = cacheService.lRightPop("list_001");
		System.out.println(list001);
	}

//	@Test
//	public void testZset(){
//
//	}

}
