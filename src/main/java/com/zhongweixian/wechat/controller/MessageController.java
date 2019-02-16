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

    private String[] array = new String[]{"美股新闻机器人群", "11班"};
    private Set<String> toUsers = new HashSet<>();


    @PostMapping("sendMessage")
    public String send(@RequestBody HttpMessage httpMessage) {
        if (CollectionUtils.isEmpty(toUsers)) {
            cacheService.getUserCache("2334107403").getChatRooms().values().forEach(room -> {
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
            for (String user : toUsers) {
                response = wechatHttpService.sendText(user, httpMessage.getContent());
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

    @GetMapping("addToUser")
    public String addGroup(String groupId) {
        toUsers.add(groupId);
        return "is ok";
    }

    @DeleteMapping("deleteGroup")
    public String deleteGroup(String groupId) {
        toUsers.remove(groupId);
        return "is ok";
    }

    private void putMessage(Long httpMessageId, RevokeRequst revokeRequst) throws IOException {
        /**
         * 判断相似度
         */
        Levenshtein levenshtein = new Levenshtein();
        Date now = new Date();
        Iterator<Long> iterable = messageMap.keySet().iterator();
        while (iterable.hasNext()) {
            RevokeRequst exist = messageMap.get(iterable.next()).get(0);
            if (exist != null) {
                if (now.getTime() - exist.getDate().getTime() > 100 * 1000L) {
                    iterable.remove();
                    continue;
                }
                if (levenshtein.getSimilarityRatio(exist.getContent(), revokeRequst.getContent()) > 0.6F) {
                    wechatHttpService.revoke(exist.getClientMsgId(), exist.getToUserName());
                    iterable.remove();
                    continue;
                }
            }
        }
        List<RevokeRequst> revokeRequsts = messageMap.get(httpMessageId);
        if (revokeRequsts == null) {
            revokeRequsts = new ArrayList<>();
        }
        revokeRequsts.add(revokeRequst);
        messageMap.put(httpMessageId, revokeRequsts);
    }


}
