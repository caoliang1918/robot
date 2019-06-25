package com.zhongweixian.service;

import com.zhongweixian.domain.HttpMessage;
import com.zhongweixian.domain.request.RevokeRequst;

/**
 * Created by caoliang on 2019-06-25
 */
public interface WbService {

    /**
     * 微博登录
     *
     * @return
     */
    Boolean login();

    /**
     * 获取微博cookie
     *
     * @return
     */
    String getCookie();


    /**
     * 发送微博
     *
     * @param httpMessage
     */
    void sendWbBlog(HttpMessage httpMessage);

    /**
     * 删除微博
     *
     * @param revokeRequst
     */
    void deleteWeiBo(RevokeRequst revokeRequst);


    /**
     * 发送微博IM消息
     *
     * @param username
     * @param message
     */
    void sendWbMessage(String username, String message);

    /**
     * @return
     */
    String getUid();
}
