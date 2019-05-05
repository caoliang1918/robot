package com.zhongweixian.task;

import com.zhongweixian.domain.BaseUserCache;
import com.zhongweixian.domain.shared.Contact;
import com.zhongweixian.service.CacheService;
import com.zhongweixian.service.WechatMessageService;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

/**
 * Created by caoliang on 2019-05-05
 */

@Component
public class BaoMessage {
    private Logger logger = LoggerFactory.getLogger(BaoMessage.class);


    @Value("${wx.uid2}")
    private String uid;

    private String baobao = "阿宝宝";

    @Autowired
    private WechatMessageService wechatMessageService;

    @Autowired
    private CacheService cacheService;

    private Contact contact = null;
    private BaseUserCache baseUserCache = null;


    @Scheduled(cron = "0 5 7 * * ?")
    public void baobaoWakeUp() {
        String[] array = new String[]{"宝宝，起床时间到了哦",
                "宝宝，快点起床了好不好啊！不然我就要把你抱起来了喔~",
                "起床了小可爱~，宝宝起床了。。。。",
                "我的可爱的宝宝起床了,早安，起床了吗？",
                "小仙女，早上好呀，在被窝里呢还是起床了？",
                "早安啊[太阳]，宝宝"};
        try {
            sendMessage(array);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 30 22 * * ?")
    public void _2230() {
        String[] array = new String[]{"宝宝，现在是北京时间" + DateUtils.formatDate(new Date(), "HH:mm:ss"),
                "宝宝，你该休息啦！",
                "晚安，宝宝，好梦！"};
        try {
            sendMessage(array);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 0 23 * * ?")
    public void _1100() {
        String[] array = new String[]{"宝宝，现在是北京时间" + DateUtils.formatDate(new Date(), "HH:mm:ss"),
                "宝宝，我猜你还没睡，肯定是在玩手机，对不对？",
                "我不怪你，你要早点睡觉哦！这会真的晚安了，好梦！[抱抱][抱抱][抱抱]"};
        try {
            sendMessage(array);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String[] array) throws IOException, InterruptedException {
        this.baseUserCache = cacheService.getUserCache(uid);
        if (baseUserCache == null || !baseUserCache.getAlive()) {
            return;
        }
        baseUserCache.getChatContants().values().forEach(contact -> {
            if (contact.getNickName().equals(baobao)) {
                this.contact = contact;
            }
        });

        if (contact == null) {
            return;
        }
        for (String str : array) {
            logger.info("send to {} , {}", contact.getNickName(), str);
            wechatMessageService.sendText(baseUserCache, contact.getUserName(), str);
            Thread.sleep(3500L);
        }
    }

}
