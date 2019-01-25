package com.zhongweixian.wechat;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@EnableScheduling
@SpringBootApplication
public class WechatBootApplication {

    @Bean
    public ExecutorService executorService() {
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(10,
                new BasicThreadFactory.Builder().namingPattern("wx-login-schedule-pool-%d").daemon(true).build());
        return executorService;
    }

    public static void main(String[] args) {
        SpringApplication.run(WechatBootApplication.class, args);
    }
}
