package com.zhongweixian.wechat.controller;

import com.zhongweixian.wechat.domain.HttpMessage;
import com.zhongweixian.wechat.domain.request.RevokeRequst;
import com.zhongweixian.wechat.domain.response.SendMsgResponse;
import com.zhongweixian.wechat.service.CacheService;
import com.zhongweixian.wechat.service.WechatHttpService;
import com.zhongweixian.wechat.utils.Levenshtein;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caoliang on 2019/1/11
 */
@RestController
@RequestMapping("index")
public class MessageController {
    private Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private WechatHttpService wechatHttpService;

    @Autowired
    private CacheService cacheService;

    private Map<Long, List<RevokeRequst>> messageMap = new HashMap<>();

    private String[] array = new String[]{"美股新闻机器人群"};
    private String[] option = new String[]{"SPY末日期权"};
    private String[] position = new String[]{"filehelper"};
    private Set<String> toUsers = new HashSet<>();
    private Set<String> optionUser = new HashSet<>();
    private String uid = "5275953";


    @PostMapping("sendMessage")
    public String send(@RequestBody HttpMessage httpMessage) {
        if (cacheService.getUserCache(uid) == null || !cacheService.getUserCache(uid).getAlive()) {
            toUsers.clear();
            optionUser.clear();
            cacheService.deleteCacheUser(uid);
            return "user not login";
        }
        if (CollectionUtils.isEmpty(toUsers)) {
            cacheService.getUserCache(uid).getChatRooms().values().forEach(room -> {
                for (String s : array) {
                    if (room.getUserName().equals(s)) {
                        toUsers.add(room.getChatRoomId());
                    }
                }
            });
        }
        System.out.println("\n");
        try {
            SendMsgResponse response = null;
            httpMessage.setSendTime(new Date());
            List<RevokeRequst> revokeRequsts = null;
            if ("delete".equals(httpMessage.getOption()) || "update".equals(httpMessage.getOption())) {
                revokeRequsts = messageMap.get(httpMessage.getId());
                if (!CollectionUtils.isEmpty(revokeRequsts)) {
                    for (RevokeRequst revokeRequst : revokeRequsts) {
                        wechatHttpService.revoke(revokeRequst.getClientMsgId(), revokeRequst.getToUserName());
                    }
                    messageMap.remove(httpMessage.getId());
                }
                if ("delete".equals(httpMessage.getOption())) {
                    return "delete ok";
                }
            }
            revokeRequsts = new ArrayList<>();
            checkMessage(httpMessage.getContent());
            for (String user : toUsers) {
                response = wechatHttpService.sendText(user, httpMessage.getContent());
                if (response == null || response.getMsgID() == null) {
                    if (!cacheService.getUserCache(uid).getAlive()) {
                        toUsers.clear();
                        optionUser.clear();
                        break;
                    }
                }
                //保存消息
                revokeRequsts.add(new RevokeRequst(user, response.getMsgID(), httpMessage.getContent()));
                logger.info("send message : {} ,  {} , {} , {}", response.getMsgID(), httpMessage.getId(), httpMessage.getOption(), httpMessage.getContent());
            }
            messageMap.put(httpMessage.getId(), revokeRequsts);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "send success";
    }


    @PostMapping("sendOption")
    public String sendOption(@RequestBody HttpMessage httpMessage) {
        System.out.println("\n");
        try {
            if (CollectionUtils.isEmpty(optionUser)) {
                cacheService.getUserCache(uid).getChatRooms().values().forEach(room -> {
                    for (String s : option) {
                        if (room.getUserName().equals(s)) {
                            optionUser.add(room.getChatRoomId());
                        }
                    }
                });
            }
            SendMsgResponse response = null;
            httpMessage.setSendTime(new Date());

            for (String user : optionUser) {
                response = wechatHttpService.sendText(user, httpMessage.getContent());
                logger.info("send message : {} ,  {} , {} , {}", response.getMsgID(), httpMessage.getId(), httpMessage.getOption(), httpMessage.getContent());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "send success";
    }

    @PostMapping("positionChange")
    public String positionChange(@RequestBody HttpMessage httpMessage) {
        try {
            SendMsgResponse response = null;
            httpMessage.setSendTime(new Date());

            for (String user : position) {
                response = wechatHttpService.sendText(user, httpMessage.getContent());
                logger.info("send message : {} ,  {} , {} , {}", response.getMsgID(), httpMessage.getId(), httpMessage.getOption(), httpMessage.getContent());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "send success";
    }


    private void checkMessage(String content) throws IOException {
        /**
         * 判断相似度
         */
        Levenshtein levenshtein = new Levenshtein();
        Date now = new Date();
        Iterator<Long> iterable = messageMap.keySet().iterator();
        while (iterable.hasNext()) {
            List<RevokeRequst> revokeRequsts = messageMap.get(iterable.next());
            if (CollectionUtils.isEmpty(revokeRequsts)) {
                continue;
            }
            RevokeRequst revokeRequst = revokeRequsts.get(0);
            /**
             * 已经超时
             */
            if (now.getTime() - revokeRequst.getDate().getTime() > 100 * 1000L) {
                iterable.remove();
                continue;
            }

            /**
             * 文本相似度
             */
            Boolean check = false;
            if (levenshtein.getSimilarityRatio(revokeRequst.getContent(), content) > 0.6F) {
                check = true;
                for (RevokeRequst revoke : revokeRequsts) {
                    wechatHttpService.revoke(revoke.getClientMsgId(), revoke.getToUserName());
                }
            }
            if (check) {
                iterable.remove();
            }
        }
    }
}
