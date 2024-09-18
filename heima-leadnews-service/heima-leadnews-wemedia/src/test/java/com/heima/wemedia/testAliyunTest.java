package com.heima.wemedia;

import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.file.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
@Slf4j
public class testAliyunTest {

	@Autowired
	private GreenTextScan greenTextScan;
	@Autowired
	private GreenImageScan greenImageScan;
	@Autowired
	private FileStorageService fileStorageService;

	/**
	 * 测试文本内容审核
	 */
	@Test
	public void testScanText() throws Exception {
		Map map = greenTextScan.greeTextScan("我是一个好人,冰毒");
		log.info("结果");
		System.out.println(map);
	}

	/**
	 * 测试图片内容审核
	 */
	@Test
	public void testScanImage(){

	}
}
