package com.zhongweixian.task;

import com.zhongweixian.domain.WxUserCache;
import com.zhongweixian.domain.shared.Contact;
import com.zhongweixian.cache.CacheService;
import com.zhongweixian.service.WxMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by caoliang on 2019-05-05
 */

@Component
public class WxTaskMessage {
    private Logger logger = LoggerFactory.getLogger(WxTaskMessage.class);


    @Value("${wx.uid}")
    private String uid;

    private String toUser = "朝颜";

    @Autowired
    private WxMessageHandler wxMessageHandler;

    @Autowired
    private CacheService cacheService;

    private Contact contact = null;
    private WxUserCache wxUserCache = null;



    @Scheduled(cron = "0 30 6 * * ?")
    public void baobaoWakeUp() {
        String[] array = new String[]{"小宝宝，起床时间到了哦",
                "小仙女，早上好啊!"};
        sendMessage(array);
    }


    @Scheduled(cron = "0 0 23 * * ?")
    public void _2300() {
        String[] array = new String[]{"小宝宝，现在是睡觉时间",
                "该休息啦！",
                "晚安，好梦！[月亮][月亮][月亮]"};
        sendMessage(array);

    }

    private void sendMessage(String[] array) {
        this.wxUserCache = cacheService.getUserCache(uid);
        if (wxUserCache == null || !wxUserCache.getAlive()) {
            return;
        }
        wxUserCache.getChatContants().values().forEach(contact -> {
            if (contact.getRemarkName().contains(toUser)) {
                this.contact = contact;
            }
        });

        if (contact == null) {
            return;
        }
        for (String str : array) {
            logger.info("send to {} , {}", contact.getRemarkName(), str);
            wxMessageHandler.sendText(wxUserCache, str, contact.getUserName());
        }
    }

}
