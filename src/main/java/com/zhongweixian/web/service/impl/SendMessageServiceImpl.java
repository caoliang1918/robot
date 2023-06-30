package com.zhongweixian.web.service.impl;

import com.zhongweixian.cache.CacheService;
import com.zhongweixian.utils.Levenshtein;
import com.zhongweixian.web.mapper.BaseMapper;
import com.zhongweixian.web.service.SendMessageService;
import com.zhongweixian.wechat.domain.HttpMessage;
import com.zhongweixian.wechat.domain.WxUserCache;
import com.zhongweixian.wechat.domain.request.RevokeRequst;
import com.zhongweixian.wechat.domain.response.SendMsgResponse;
import com.zhongweixian.wechat.domain.shared.Contact;
import com.zhongweixian.wechat.service.WxMessageHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

/**
 * Created by caoliang on 2023/6/30
 */
@Service
public class SendMessageServiceImpl extends BaseServiceImpl<HttpMessage> implements SendMessageService {
    private Logger logger = LoggerFactory.getLogger(SendMessageServiceImpl.class);
    @Autowired
    private CacheService cacheService;

    @Autowired
    private WxMessageHandler wxMessageHandler;

    @Value("${wx.uid}")
    private List<String> uidList;

    @Value("${wx.group}")
    private List<String> userGroupList;

    @Value("${wx.allGroup:}")
    private String allGroup;

    /**
     * 记录给每个人或者群组发的消息
     */
    private Map<String, List<RevokeRequst>> messageMap = new HashMap<>();

    @Override
    public void sendMessage(HttpMessage httpMessage) {
        if (CollectionUtils.isEmpty(uidList)) {
            return;
        }
        for (String uid : uidList) {
            WxUserCache userCache = cacheService.getUserCache(uid);
            sendWxUserMessage(userCache, httpMessage);
        }
    }

    @Override
    public String sendWxUserMessage(WxUserCache wxUserCache, HttpMessage httpMessage) {
        if (httpMessage == null || httpMessage.getChannel() == null) {
            return "message is null";
        }
        if (wxUserCache == null || !wxUserCache.getAlive()) {
            cacheService.deleteCacheUser(wxUserCache.getUin());
            return "user not login";
        }

        try {
            SendMsgResponse response = null;
            httpMessage.setSendTime(new Date());
            List<RevokeRequst> revokeRequsts = null;
            if ("delete".equals(httpMessage.getOption()) || "update".equals(httpMessage.getOption())) {
                revokeRequsts = messageMap.get(httpMessage.getId());
                if (!CollectionUtils.isEmpty(revokeRequsts)) {
                    for (RevokeRequst revokeRequst : revokeRequsts) {
                        wxMessageHandler.revoke(wxUserCache, revokeRequst.getClientMsgId(), revokeRequst.getToUserName());
                    }
                    messageMap.remove(httpMessage.getId());
                }
                if ("delete".equals(httpMessage.getOption())) {
                    return "delete ok";
                }
            }

            for (Contact contact : wxUserCache.getChatRoomMembers().values()) {
                if (StringUtils.isNotBlank(allGroup) && contact.getNickName().contains(allGroup)) {
                    wxMessageHandler.sendText(wxUserCache, httpMessage.getContent(), contact.getUserName());
                }
                for (String group : userGroupList) {
                    if (contact.getNickName().contains(group) && contact.getNickName().contains(httpMessage.getChannel())) {
                        revokeRequsts = messageMap.getOrDefault(contact.getUserName(), new ArrayList<>());
                        checkMessage(wxUserCache, contact.getUserName(), httpMessage.getContent());
                        response = wxMessageHandler.sendText(wxUserCache, httpMessage.getContent(), contact.getUserName());
                        //保存消息
                        revokeRequsts.add(new RevokeRequst(contact.getUserName(), response.getMsgID(), httpMessage.getContent(), httpMessage.getId()));
                        logger.info("send message msgId:{}, channle:{} , content:{}", response.getMsgID(), httpMessage.getChannel(), httpMessage.getContent());
                        messageMap.put(contact.getUserName(), revokeRequsts);
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return "send success";
    }

    @Override
    BaseMapper baseMapper() {
        return null;
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
}
