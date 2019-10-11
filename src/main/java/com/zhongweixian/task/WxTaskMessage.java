package com.zhongweixian.task;

import com.zhongweixian.domain.WxUserCache;
import com.zhongweixian.domain.shared.Contact;
import com.zhongweixian.cache.CacheService;
import com.zhongweixian.service.WxMessageHandler;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by caoliang on 2019-05-05
 */

@Component
public class WxTaskMessage {
    private Logger logger = LoggerFactory.getLogger(WxTaskMessage.class);


    @Value("${wx.uid}")
    private String uid;

    String[] zhao_up = new String[]{"小宝宝，起床时间到了哦", "小仙女，早上好啊!"};
    String[] zhao_night = new String[]{"小宝宝，现在是睡觉时间", "该休息啦！", "晚安，好梦！[月亮][月亮][月亮]"};


    String[] la_up = new String[]{"蜡笔小珠，起床时间到了哦", "小仙女，早上好啊!"};
    String[] la_night = new String[]{"蜡笔小珠，现在是睡觉时间", "该休息啦！", "晚安，好梦！[月亮][月亮][月亮]"};

    @Autowired
    private WxMessageHandler wxMessageHandler;

    @Autowired
    private CacheService cacheService;

    private WxUserCache wxUserCache = null;


    @Scheduled(cron = "0 30 6 * * ?")
    public void _0630() {
        sendMessage("朝颜", zhao_up);
    }

    @Scheduled(cron = "0 30 7 * * ?")
    public void _0730() {
        sendMessage("蜡笔小猪", la_up);
    }


    @Scheduled(cron = "0 0 23 * * ?")
    public void _2300() {
        sendMessage("朝颜", zhao_night);
        sendMessage("蜡笔小珠", la_night);

    }

    @Scheduled(cron = "0 0 23 * * ?")
    public void time() {
        String content = "现在是北京时间: " + DateFormatUtils.format(new Date(), "HH:mm:ss");
        this.wxUserCache = cacheService.getUserCache(uid);
        if (wxUserCache == null) {
            return;
        }
        wxUserCache.getChatRoomMembers().values().forEach(room -> {
            if (room.getNickName().equals("沧海遗珠")) {
                wxMessageHandler.sendText(wxUserCache, content, room.getUserName());
            }
        });
        logger.info("send to: 沧海遗珠 , content:{}", content);
    }

    private void sendMessage(String toUser, String[] array) {
        this.wxUserCache = cacheService.getUserCache(uid);
        if (wxUserCache == null || !wxUserCache.getAlive()) {
            logger.warn("user:{} not login", uid);
            return;
        }
        List<Contact> contacts = new ArrayList<>();
        wxUserCache.getChatContants().values().forEach(c -> {
            if (toUser.equals(c.getRemarkName()) || toUser.equals(c.getNickName())) {
                contacts.add(c);
            }
        });

        if (contacts.size() == 0) {
            logger.warn("contact:{} not found", toUser);
            return;
        }
        for (String str : array) {
            for (Contact contact : contacts) {
                logger.info("send to {} , {}", contact.getNickName(), str);
                wxMessageHandler.sendText(wxUserCache, str, contact.getUserName());
            }
        }
    }

}
