package com.zhongweixian.domain;

/**
 * Created by caoliang on 2019/1/24
 */
public class BaseUrl {
    private static final String WX_HOST = "https://wx.qq.com/";
    private static final String WX_COMMON_URI = "cgi-bin/mmwebwx-bin/";
    private static final String WX_LOGIN_HOST = "https://login.weixin.qq.com";

    private static final String WX_LOGIN = WX_LOGIN_HOST + WX_COMMON_URI + "login?loginicon=true&uuid=%s&tip=0&r=%s&_=%s";
    private static final String WX_UUID = "https://login.weixin.qq.com/jslogin?appid=wx782c26e4c19acffb&fun=new&lang=zh_CN&_=%s";
    private static final String WX_QRCODE = "https://login.weixin.qq.com/qrcode";
    private static final String WX_WEB_INIT = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxinit";
    private static final String WX_REPORT = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxstatreport?fun=new";
    private static final String WX_STATUS_NOTIFY = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxstatusnotify";
    private static final String WX_SYNC_CHECK = "https://wx.qq.com/cgi-bin/mmwebwx-bin/synccheck";


    /**
     * wechat.url.uuid=${wechat.url.login_base}/jslogin?appid=wx782c26e4c19acffb&fun=new&lang=zh_CN&_=%s
     * wechat.url.qrcode=${wechat.url.login_base}/qrcode
     * wechat.url.login=${wechat.url.login_base}${wechat.url.path_base}/login?loginicon=true&uuid=%s&tip=0&r=%s&_=%s
     */
}
