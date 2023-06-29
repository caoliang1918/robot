package com.zhongweixian.task;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 华尔街见闻
 */
@Component
public class VipJianShi {
    private Logger logger = LoggerFactory.getLogger(VipJianShi.class);

    private final static String BASE_URL = "https://api.wallstcn.com/apiv1/content/lives?channel=us-stock-channel&client=pc&limit=20&first_page=true&accept=live%2Cvip-live";

    @Autowired
    private RestTemplate restTemplate;

    private List<String> flagList = new ArrayList<>();

    {
        flagList.add("摩根");
        flagList.add("高盛");
        flagList.add("瑞银");
        flagList.add("评级");
        flagList.add("行情");
        flagList.add("开盘");
        flagList.add("收盘");
        flagList.add("纳斯达克");
        flagList.add("标普");
        flagList.add("指数");
        flagList.add("美股");
        flagList.add("期货");
        flagList.add("前值");
        flagList.add("IPO");
        flagList.add("上涨");
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void task() {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(BASE_URL, String.class);
        if (responseEntity == null || StringUtils.isBlank(responseEntity.getBody())) {
            return;
        }
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        if (jsonObject == null) {
            return;
        }
        JSONObject data = jsonObject.getJSONObject("data");

        String next_cursor = data.getString("next_cursor");
        JSONArray items = data.getJSONArray("items");
        if (items == null || items.size() == 0) {
            return;
        }
        JSONObject element = items.getJSONObject(0);

        logger.debug("element: \n{}", element);

        String content = element.getString("content_text");
        Long id = element.getLong("id");
        logger.info("=====:{} {}", id, content);
    }

}
