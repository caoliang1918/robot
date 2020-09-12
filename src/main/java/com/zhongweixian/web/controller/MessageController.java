package com.zhongweixian.web.controller;

import com.zhongweixian.cache.CacheService;
import com.zhongweixian.wechat.domain.WxUserCache;
import com.zhongweixian.wechat.domain.HttpMessage;
import com.zhongweixian.wechat.domain.request.RevokeRequst;
import com.zhongweixian.wechat.domain.response.SendMsgResponse;
import com.zhongweixian.wechat.service.WxMessageHandler;
import com.zhongweixian.utils.Levenshtein;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

/**
 * Created by caoliang on 2019/1/11
 */
@RestController
@RequestMapping("index")
public class MessageController {
    private Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private CacheService cacheService;

    @Autowired
    private WxMessageHandler wxMessageHandler;

    /**
     * 记录给每个人或者群组发的消息
     */
    private Map<String, List<RevokeRequst>> messageMap = new HashMap<>();

    private String[] option = new String[]{"SPY末日期权"};
    private String[] position = new String[]{"曹亮"};
    private Set<String> optionUser = new HashSet<>();
    private Set<String> positions = new HashSet<>();

    @Value("${wx.uid}")
    private String uid = "5275953";

    /**
     * 给微信推送美股实时资讯
     *
     * @param httpMessage
     * @return
     */
    @PostMapping("sendMessage")
    public String send(@RequestBody com.zhongweixian.wechat.domain.HttpMessage httpMessage) {
        if (httpMessage == null || httpMessage.getChannel() == null) {
            return "message is null";
        }
        WxUserCache userCache = cacheService.getUserCache(uid);
        if (userCache == null || !userCache.getAlive()) {
            optionUser.clear();
            cacheService.deleteCacheUser(uid);
            return "user not login";
        }
        Set<String> toUsers = new HashSet<>();
        userCache.getChatRoomMembers().values().forEach(room -> {
            if (httpMessage.getChannel().contains("金十") && room.getNickName().contains("金十")) {
                toUsers.add(room.getUserName());
            }
            if (httpMessage.getChannel().contains("见闻") && room.getNickName().contains("美股行情")) {
                toUsers.add(room.getUserName());
            }
        });
        System.out.println("\n");
        try {
            SendMsgResponse response = null;
            httpMessage.setSendTime(new Date());
            List<RevokeRequst> revokeRequsts = null;
            if ("delete".equals(httpMessage.getOption()) || "update".equals(httpMessage.getOption())) {
                revokeRequsts = messageMap.get(httpMessage.getId());
                if (!CollectionUtils.isEmpty(revokeRequsts)) {
                    for (RevokeRequst revokeRequst : revokeRequsts) {
                        wxMessageHandler.revoke(userCache, revokeRequst.getClientMsgId(), revokeRequst.getToUserName());
                    }
                    messageMap.remove(httpMessage.getId());
                }
                if ("delete".equals(httpMessage.getOption())) {
                    return "delete ok";
                }
            }
            for (String user : toUsers) {
                revokeRequsts = messageMap.getOrDefault(user, new ArrayList<>());
                checkMessage(userCache, user, httpMessage.getContent());
                response = wxMessageHandler.sendText(userCache, httpMessage.getContent(), user);
                //保存消息
                revokeRequsts.add(new RevokeRequst(user, response.getMsgID(), httpMessage.getContent(), httpMessage.getId()));
                logger.info("send message msgId:{}, channle:{} , content:{}", response.getMsgID(), httpMessage.getChannel(), httpMessage.getContent());
                messageMap.put(user, revokeRequsts);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "send success";
    }


    /**
     * 给指定的微信群推送期权数据
     *
     * @param httpMessage
     * @return
     */
    @PostMapping("sendOption")
    public String sendOption(@RequestBody HttpMessage httpMessage) {
        System.out.println("\n");
        WxUserCache userCache = cacheService.getUserCache(uid);
        if (userCache == null || !userCache.getAlive()) {
            return "user  not login";
        }
        try {
            if (CollectionUtils.isEmpty(optionUser)) {
                cacheService.getUserCache(uid).getChatRoomMembers().values().forEach(room -> {
                    for (String s : option) {
                        if (room.getNickName().equals(s)) {
                            optionUser.add(room.getUserName());
                        }
                    }
                });
            }
            SendMsgResponse response = null;
            httpMessage.setSendTime(new Date());
            for (String user : optionUser) {
                response = wxMessageHandler.sendText(userCache, httpMessage.getContent(), user);
                logger.info("sendOption message : {} ,  {} , {} , {}", response.getMsgID(), httpMessage.getId(), httpMessage.getOption(), httpMessage.getContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "send success";
    }

    /**
     * 持仓位置变更，给指定的微信好友发送消息
     *
     * @param httpMessage
     * @return
     */
    @PostMapping("positionChange")
    public String positionChange(@RequestBody HttpMessage httpMessage) {
        WxUserCache userCache = cacheService.getUserCache(uid);
        if (userCache == null || !userCache.getAlive()) {
            return "user  not login";
        }

        if (CollectionUtils.isEmpty(positions)) {
            userCache.getChatContants().values().forEach(contact -> {
                for (String s : position) {
                    if (contact.getNickName().equals(s)) {
                        positions.add(contact.getUserName());
                    }
                }
            });
        }

        SendMsgResponse response = null;
        httpMessage.setSendTime(new Date());
        for (String user : positions) {
            response = wxMessageHandler.sendText(userCache, httpMessage.getContent(), user);
            logger.info("send message : {} ,  {} , {} , {}", response.getMsgID(), httpMessage.getId(), httpMessage.getOption(), httpMessage.getContent());
        }

        return "send success";
    }


    /**
     * @param userCache
     * @param toUserName
     * @param content
     * @throws IOException
     */
    private void checkMessage(WxUserCache userCache, String toUserName, String content) throws IOException {
        /**
         * 判断相似度
         */
        Levenshtein levenshtein = new Levenshtein();
        Date now = new Date();

        List<RevokeRequst> list = messageMap.get(toUserName);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        Iterator<RevokeRequst> iterator = list.iterator();
        while (iterator.hasNext()) {
            List<RevokeRequst> revokeRequsts = messageMap.get(iterator.next());
            if (CollectionUtils.isEmpty(revokeRequsts)) {
                continue;
            }
            RevokeRequst revokeRequst = revokeRequsts.get(0);
            /**
             * 已经超时
             */
            if (now.getTime() - revokeRequst.getDate().getTime() > 100 * 1000L && iterator.next().equals(toUserName)) {
                iterator.remove();
                continue;
            }
            /**
             * 文本相似度
             */
            Boolean check = false;
            if (levenshtein.getSimilarityRatio(revokeRequst.getContent(), content) > 0.6F) {
                check = true;
                for (RevokeRequst revoke : revokeRequsts) {
                    wxMessageHandler.revoke(userCache, revoke.getClientMsgId(), revoke.getToUserName());
                }
            }
            if (check) {
                iterator.remove();
            }
        }
    }

    RestTemplate restTemplate = new RestTemplate();

    @PostMapping("filehelper")
    public String sendFilehelper(@RequestBody HttpMessage httpMessage) {
        WxUserCache userCache = cacheService.getUserCache("5275953");
        if (userCache != null) {
            SendMsgResponse response = wxMessageHandler.sendText(userCache, httpMessage.getContent(), "filehelper");
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json;charset=UTF-8");
        httpHeaders.add("Host", "wx2.qq.com");
        httpHeaders.add("Origin", "https://wx2.qq.com");
        httpHeaders.add("Referer", "https://wx2.qq.com/");
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36");
        httpHeaders.add("Cookie", "webwxuvid=4bcb051b6d8811919a2f3cc6385b10d03b33cfdbcc4bfa1d4d26a06f6544b077ebf97cb2b4deb10842828e058cf8e34e; pgv_pvid=5686192450; pgv_pvi=3331826688; RK=mvKwIBzca8; ptcz=f12d4fd1e4283a56f3cd4af477a1234bb4caa300311da731c07ee7df5421a5fb; sd_userid=45821558332466871; sd_cookie_crttime=1558332466871; pac_uid=0_5d3a990e00957; wxuin=5275953; wxsid=lYKqV77B63JPTcQE; mm_lang=zh_CN; webwx_data_ticket=gSdPFAcsoYLJOYT1I6cwZ3K2; webwx_auth_ticket=CIsBEMKG474PGoABew76Yqf7InsKPwWqyzvodWbXgBfucypOKDxi5+AK7IlHWSHgRx3Iwc3IzV7g6B992dJYtHK+3mzQopu6SYQytobZVF+3qwoW1C6oG+Mg8UpZ4swkHZsUrwFh5y6AT2xlJ2SuXvHhOq7S54fVOmXbaduGx58pnY5jwQ6B1e883TI=; MM_WX_NOTIFY_STATE=1; MM_WX_SOUND_STATE=1; wxloadtime=1567328345_expired; wxpluginkey=1567322642");

        Long time = System.currentTimeMillis() * 10000;
        String DeviceID = "e15119935153" + RandomStringUtils.randomNumeric(4);
        String payload = "{\"BaseRequest\":{\"Uin\":5275953,\"Sid\":\"lYKqV77B63JPTcQE\",\"Skey\":\"@crypt_cc2ae297_6d150840b50ef9ec84c1a71f825ca870\",\"DeviceID\":\"e811742848076557\"},\"Msg\":{\"Type\":1,\"Content\":\"" + System.currentTimeMillis() + "\",\"FromUserName\":\"@0519796d43f6b546e6f52c4b6545af9b3be48e682d97a2371e931444e07026d5\",\"ToUserName\":\"filehelper\",\"LocalID\":\"" + time + "\",\"ClientMsgId\":\"" + time + "\"},\"Scene\":0}";

        ResponseEntity<String> responseEntity = restTemplate.postForEntity("https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxsendmsg", new HttpEntity<>(payload, httpHeaders), String.class);

        logger.info("responseEntity:{}", responseEntity.getBody());
        return responseEntity.getBody();
    }

    @PostMapping("sendAll")
    public String sendAll(String uid) {
        StringBuilder sbf = new StringBuilder("鹤发垂肩尺许长，离家三十五端阳。\n");
        sbf.append("儿童见说深惊讶，却问何方是故乡。\n");
        WxUserCache userCache = cacheService.getUserCache(uid);
        if (userCache == null) {
            logger.error("user:{} is not login", uid);
            return "userCache is null";
        }
        userCache.getChatContants().values().forEach(contact -> {
            SendMsgResponse response = wxMessageHandler.sendText(userCache, sbf.toString(), contact.getUserName());
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("nickname:{} , response:{}", contact.getNickName(), response);
            wxMessageHandler.revoke(userCache, response.getMsgID(), contact.getUserName());
        });
        return "send All test";
    }
}