package com.zhongweixian.task;

import com.zhongweixian.web.service.SendMessageService;
import com.zhongweixian.wechat.domain.HttpMessage;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Create by caoliang on 2020/10/12
 */
@Component
public class CaiZhiTask {
    private Logger logger = LoggerFactory.getLogger(CaiZhiTask.class);


    private String url = "https://www.zhitongcaijing.com/immediately.html?type=usstock";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SendMessageService sendMessageService;


    @Scheduled(cron = "0/10 * * * * ?")
    public void task() throws Exception {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        if (responseEntity == null || StringUtils.isBlank(responseEntity.getBody())) {
            return;
        }
        Document document = Jsoup.parse(responseEntity.getBody());
        Element element = document.getElementsByClass("allday-box").first();
        Element dl = element.child(1);
        String content = dl.getElementsByClass("allday-item-content").get(0).child(0).text();
        logger.info("智通财经 :{} ", content);

        HttpMessage httpMessage = new HttpMessage();
        httpMessage.setContent(content);
        httpMessage.setChannel("智通");
        httpMessage.setId((long) content.hashCode());
        sendMessageService.sendMessage(httpMessage);
    }
}
