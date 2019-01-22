package com.zhongweixian.wechat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WechatBootApplication {

    private static final Logger logger = LoggerFactory.getLogger(WechatBootApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(WechatBootApplication.class, args);
    }
}
