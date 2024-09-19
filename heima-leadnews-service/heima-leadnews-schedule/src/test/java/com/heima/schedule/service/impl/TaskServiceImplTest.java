package com.heima.schedule.service.impl;

import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.service.TaskService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TaskServiceImplTest {

	@Autowired
	private TaskService taskService;

	@Test
	void addTask() {
		Task task = new Task();
		task.setTaskType(100);
		task.setPriority(50);
		task.setParameters("task_test".getBytes());
		task.setExecuteTime(new Date().getTime());

		long taskId = taskService.addTask(task);
		System.out.println(taskId);

	}

	@Test
	void cancleTask() {
		boolean b = taskService.cancleTask(1836672253454610433L);
		System.out.println(b);
	}

	@Test
	void poll(){
		Task poll = taskService.poll(100, 50);
		System.out.println(poll);
	}


}