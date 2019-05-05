package com.zhongweixian.controller;

import com.zhongweixian.domain.BaseUserCache;
import com.zhongweixian.domain.HttpMessage;
import com.zhongweixian.domain.request.RevokeRequst;
import com.zhongweixian.service.WeiBoService;
import com.zhongweixian.utils.Levenshtein;
import com.zhongweixian.domain.response.SendMsgResponse;
import com.zhongweixian.service.CacheService;
import com.zhongweixian.service.WechatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by caoliang on 2019/1/11
 */
@RestController
@RequestMapping("index")
public class MessageController {
    private Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private WechatMessageService wechatMessageService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private WeiBoService weiBoService;

    private Map<Long, List<RevokeRequst>> messageMap = new HashMap<>();

    private String[] array = new String[]{"美股新闻机器人群"};
    private String[] option = new String[]{"SPY末日期权"};
    private String[] position = new String[]{"曹亮"};
    private Set<String> toUsers = new HashSet<>();
    private Set<String> optionUser = new HashSet<>();
    private Set<String> positions = new HashSet<>();

    @Value("${wx.uid}")
    private String uid = "5275953";


    /**
     * 给微信、微博推送美股实时资讯
     *
     * @param httpMessage
     * @return
     */
    @PostMapping("sendMessage")
    public String send(@RequestBody HttpMessage httpMessage) {
        try {
            weiBoService.sendWeiBoMessage(httpMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        BaseUserCache userCache = cacheService.getUserCache(uid);
        if (userCache == null || !userCache.getAlive()) {
            toUsers.clear();
            optionUser.clear();
            cacheService.deleteCacheUser(uid);
            return "user not login";
        }
        if (CollectionUtils.isEmpty(toUsers)) {
            userCache.getChatRooms().values().forEach(room -> {
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
                        wechatMessageService.revoke(userCache, revokeRequst.getClientMsgId(), revokeRequst.getToUserName());
                    }
                    messageMap.remove(httpMessage.getId());
                }
                if ("delete".equals(httpMessage.getOption())) {
                    return "delete ok";
                }
            }
            revokeRequsts = new ArrayList<>();
            checkMessage(userCache, httpMessage.getContent());
            for (String user : toUsers) {
                response = wechatMessageService.sendText(userCache, user, httpMessage.getContent());
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


    /**
     * 给指定的微信群推送期权数据
     *
     * @param httpMessage
     * @return
     */
    @PostMapping("sendOption")
    public String sendOption(@RequestBody HttpMessage httpMessage) {
        System.out.println("\n");
        BaseUserCache userCache = cacheService.getUserCache(uid);
        if (userCache == null || !userCache.getAlive()) {
            return "user  not login";
        }
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
                response = wechatMessageService.sendText(userCache, user, httpMessage.getContent());
                logger.info("send message : {} ,  {} , {} , {}", response.getMsgID(), httpMessage.getId(), httpMessage.getOption(), httpMessage.getContent());
            }
        } catch (IOException e) {
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
        BaseUserCache userCache = cacheService.getUserCache(uid);
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

        try {
            SendMsgResponse response = null;
            httpMessage.setSendTime(new Date());

            for (String user : positions) {
                response = wechatMessageService.sendText(userCache, user, httpMessage.getContent());
                logger.info("send message : {} ,  {} , {} , {}", response.getMsgID(), httpMessage.getId(), httpMessage.getOption(), httpMessage.getContent());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "send success";
    }


    private void checkMessage(BaseUserCache userCache, String content) throws IOException {
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
                    wechatMessageService.revoke(userCache, revoke.getClientMsgId(), revoke.getToUserName());
                }
            }
            if (check) {
                iterable.remove();
            }
        }
    }
}
