package com.zhongweixian.wechat.controller;

import com.zhongweixian.wechat.domain.HttpMessage;
import com.zhongweixian.wechat.domain.request.RevokeRequst;
import com.zhongweixian.wechat.domain.response.SendMsgResponse;
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

    Map<Long, RevokeRequst> messageMap = new HashMap<>();


    private Set<String> toUsers = new HashSet<>();


    @PostMapping("sendMessage")
    public String send(@RequestBody HttpMessage httpMessage) {
        System.out.println("\n");
        try {
            SendMsgResponse response = null;
            httpMessage.setSendTime(new Date());

            if ("delete".equals(httpMessage.getOption()) || "update".equals(httpMessage.getOption())) {
                RevokeRequst revokeRequst = messageMap.get(httpMessage.getId());
                if (revokeRequst != null) {
                    messageMap.remove(httpMessage.getId());
                    wechatHttpService.revoke(revokeRequst.getClientMsgId(), revokeRequst.getToUserName());
                }
                if ("delete".equals(httpMessage.getOption())) {
                    return "delete ok";
                }
            }
            for (String user : toUsers) {
                response = wechatHttpService.sendText(user, httpMessage.getContent());
                //保存消息
                putMessage(httpMessage.getId(), new RevokeRequst(user, response.getMsgID(), httpMessage.getContent()));
                logger.info("send message : {} ,  {} , {} , {}", response.getMsgID(), httpMessage.getId(), httpMessage.getOption(), httpMessage.getContent());
            }
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
            RevokeRequst exist = messageMap.get(iterable.next());
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
        messageMap.put(httpMessageId, revokeRequst);
    }


}
