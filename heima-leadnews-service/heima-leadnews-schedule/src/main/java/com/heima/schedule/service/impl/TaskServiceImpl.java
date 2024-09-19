package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;


@Service
@Slf4j
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
	private final TaskinfoMapper taskinfoMapper;
	private final TaskinfoLogsMapper taskinfoLogsMapper;
	private final CacheService cacheService;


	/**
	 * 添加延时任务
	 *
	 * @param task
	 * @return
	 */
	@Override
	public long addTask(Task task) {
		//1、添加任务到数据库
		boolean success = addTaskToDb(task);

		if (success) {
			//2、添加任务到redis
			addTaskToCache(task);
		}
		return task.getTaskId();
	}

	/**
	 * 取消任务
	 *
	 * @param taskId
	 * @return
	 */
	@Override
	public boolean cancleTask(long taskId) {
		boolean flag = false;

		//删除任务，更新日志
		Task task = updateDb(taskId, ScheduleConstants.CANCELLED);
		if (task != null) {
			//删除redis缓存
			removeTaskFromCache(task);
			flag = true;
		}
		return flag;
	}

	/**
	 * 按照类型和优先级来拉取任务
	 *
	 * @param type
	 * @param priority
	 * @return
	 */
	@Override
	public Task poll(int type, int priority) {
		Task task = null;
		try {
			String key = type + "_" + priority;
			String taskJson = cacheService.lRightPop(ScheduleConstants.TOPIC + key);
			if (taskJson != null) {
				task = JSON.parseObject(taskJson, Task.class);
				updateDb(task.getTaskId(), ScheduleConstants.EXECUTED);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("poll task error");
		}
		return task;
	}

	/**
	 * 删除redis中的任务数据
	 * @param task
	 */
	private void removeTaskFromCache(Task task) {

		String key = task.getTaskType()+"_"+task.getPriority();

		if(task.getExecuteTime()<=System.currentTimeMillis()){
			cacheService.lRemove(ScheduleConstants.TOPIC+key,0,JSON.toJSONString(task));
		}else {
			cacheService.zRemove(ScheduleConstants.FUTURE+key, JSON.toJSONString(task));
		}
	}

	/**
	 * 删除任务，更新任务日志状态
	 * @param taskId
	 * @param status
	 * @return
	 */
	private Task updateDb(long taskId, int status) {
		Task task = null;
		try {
			//删除任务
			taskinfoMapper.deleteById(taskId);

			TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
			taskinfoLogs.setStatus(status);
			taskinfoLogsMapper.updateById(taskinfoLogs);

			task = new Task();
			BeanUtils.copyProperties(taskinfoLogs,task);
			task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
		}catch (Exception e){
			log.error("task cancel exception taskid={}",taskId);
		}

		return task;

	}

	/**
	 * 添加任务到redis
	 * @param task
	 */
	private void addTaskToCache(Task task) {
		String key = task.getTaskType() + "_" + task.getPriority();

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, 5);
		long nextSchedule = calendar.getTimeInMillis();
		//2.1 如果任务执行时间小于等于当前时间，加入list
		if(task.getExecuteTime() <= System.currentTimeMillis()) {
			cacheService.lLeftPush(ScheduleConstants.TOPIC+key, JSON.toJSONString(task));
		}else if(task.getExecuteTime() < nextSchedule) {
			//2.2如果任务执行时间在5分钟以内，加入zset中
			cacheService.zAdd(ScheduleConstants.FUTURE+key, JSON.toJSONString(task), task.getExecuteTime());
		}
	}

	/**
	 * 添加任务到数据库
	 * @param task
	 */
	private boolean addTaskToDb(Task task) {
		boolean flag = false;
		try {
			//1、添加到任务表
			Taskinfo taskinfo = new Taskinfo();
			BeanUtils.copyProperties(task, taskinfo);
			taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
			taskinfoMapper.insert(taskinfo);

			//设置任务id
			task.setTaskId(taskinfo.getTaskId());

			//2、添加到任务日志表
			TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
			BeanUtils.copyProperties(taskinfo, taskinfoLogs);
			taskinfoLogs.setVersion(1);
			taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
			taskinfoLogsMapper.insert(taskinfoLogs);

			flag = true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return flag;
	}

	/**
	 * 未来数据定时刷新
	 */
	@Scheduled(cron = "0 */1 * * * ?")
	public void refresh() {
		String token = cacheService.tryLock("FUTURE_TASK_SYNC", 1000 * 30);
		if(token != null) {
			System.out.println(System.currentTimeMillis() / 1000 + "执行了定时任务");

			// 获取所有未来数据集合的key值
			Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");// future_*
			for (String futureKey : futureKeys) { // future_250_250

				String topicKey = ScheduleConstants.TOPIC + futureKey.split(ScheduleConstants.FUTURE)[1];
				//获取该组key下当前需要消费的任务数据
				Set<String> tasks = cacheService.zRangeByScore(futureKey, 0, System.currentTimeMillis());
				if (!tasks.isEmpty()) {
					//将这些任务数据添加到消费者队列中
					cacheService.refreshWithPipeline(futureKey, topicKey, tasks);
					System.out.println("成功的将" + futureKey + "下的当前需要执行的任务数据刷新到" + topicKey + "下");
				}
			}
		}
	}

	/**
	 * 数据库同步到redis
	 */
	@PostConstruct
	@Scheduled(cron = "0 */5 * * * ?")
	public void reloadData(){
		//清理缓存中的数据
		clearRedis();
		log.info("数据库同步到redis");

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, 5);
		//查看小于未来5分钟的所有任务
		List<Taskinfo> allTasks = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().lt(Taskinfo::getExecuteTime,calendar.getTime()));
		if(allTasks != null && allTasks.size() > 0){
			for (Taskinfo taskinfo : allTasks) {
				Task task = new Task();
				BeanUtils.copyProperties(taskinfo,task);
				task.setExecuteTime(taskinfo.getExecuteTime().getTime());
				addTaskToCache(task);
			}
		}
	}

	/**
	 * 清空redis中的数据
	 */
	public void clearRedis(){
		Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
		cacheService.delete(futureKeys);
		Set<String> topicKeys = cacheService.scan(ScheduleConstants.TOPIC + "*");
		cacheService.delete(topicKeys);
	}
}
