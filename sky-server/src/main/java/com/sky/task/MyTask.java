package com.sky.task;

import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自定义定时任务类
 */
@Component
@Slf4j
public class MyTask {

    /**
     * 定时任务类,处理定时任务业务逻辑
     */
    //@Scheduled(cron = "0/5 * * * * ?")
    public void excuteTask(){
        log.info("每隔五秒触发一次：{}",new Date());
    }


}
