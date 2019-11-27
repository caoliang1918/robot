package com.zhongweixian.service;

import com.zhongweixian.domain.HttpMessage;
import com.zhongweixian.domain.request.RevokeRequst;
import com.zhongweixian.domain.weibo.WeiBoUser;

/**
 * Created by caoliang on 2019-06-25
 */
public interface WbService {

    /**
     * @param username
     * @param pwd
     * @return
     */
    WeiBoUser login(String username, String pwd);

    /**
     * 获取微博cookie
     *
     * @return
     */
    String getCookie();


    /**
     * 发送微博
     *
     * @param weiBoUser
     * @param httpMessage
     */
    void sendWbBlog(WeiBoUser weiBoUser, HttpMessage httpMessage);

    /**
     * 删除微博
     *
     * @param weiBoUser
     * @param revokeRequst
     */
    void deleteWeiBo(WeiBoUser weiBoUser, RevokeRequst revokeRequst);


    /**
     * 发送微博IM消息
     *
     * @param username
     * @param message
     */
    void sendWbMessage(String username, String message);

}
