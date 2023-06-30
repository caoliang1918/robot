package com.zhongweixian.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Created by caoliang on 2023/6/29
 */

@Component
public class KuaiLanSiTask {
    private Logger logger = LoggerFactory.getLogger(KuaiLanSiTask.class);


    @Autowired
    private RestTemplate restTemplate;

    /**
     *
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void task() {
        ResponseEntity<String> response = restTemplate.getForEntity("http://m.fbecn.com/24h/news_fbe0406.json?newsid=0", String.class);
        if (response == null || response.getStatusCode() != HttpStatus.OK) {
            return;
        }
        JSONObject json = JSON.parseObject(response.getBody());
        JSONArray jsonArray = JSON.parseArray(json.getString("list"));

        for (Object obj : jsonArray) {
            logger.info("快兰斯  :{}", obj);
        }
    }
}
