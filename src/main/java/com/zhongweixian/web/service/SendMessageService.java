package com.zhongweixian.web.service;

import com.zhongweixian.wechat.domain.HttpMessage;
import com.zhongweixian.wechat.domain.WxUserCache;

/**
 * Created by caoliang on 2023/6/30
 */
public interface SendMessageService extends BaseService<HttpMessage> {


    /**
     *
     * @param payload
     */
    void sendMessage(HttpMessage httpMessage);

    /**
     * 发送微信消息
     *
     * @param wxUserCache
     * @param httpMessage
     * @return
     */
    String sendWxUserMessage(WxUserCache wxUserCache, HttpMessage httpMessage);

}
