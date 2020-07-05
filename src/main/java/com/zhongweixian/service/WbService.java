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


    void setCookie(String cookie);

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

}
