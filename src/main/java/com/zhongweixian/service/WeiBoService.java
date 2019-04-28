package com.zhongweixian.service;

import com.zhongweixian.domain.request.WeiBoRequest;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * Created by caoliang on 2019-04-28
 * <p>
 * 微博比较简单，不做IM
 */
@Service
public class WeiBoService {
    Logger logger = LoggerFactory.getLogger(WeiBoService.class);

    @Value("weibo.username")
    private String username;

    @Value("weibo.password")
    private String password;


    @Autowired
    private RestTemplate restTemplate;

    private static final String SEND_URL = "https://www.weibo.com/aj/mblog/add?ajwvr=6&__rnd=";

    private HttpHeaders postHeader;
    private HttpHeaders getHeader;
    private String BROWSER_DEFAULT_ACCEPT_LANGUAGE = "en,zh-CN;q=0.8,zh;q=0.6,ja;q=0.4,zh-TW;q=0.2";
    private String BROWSER_DEFAULT_ACCEPT_ENCODING = "gzip, deflate, br";
    private String BROWSER_DEFAULT_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36";
    private String originValue = "https://www.weibo.com";

    @Value("weibo.Referer")
    private String referer;
    private CookieStore cookieStore;
    private HttpHeaders httpHeaders;


    public void login() {
        httpHeaders.add("Cookie", "SINAGLOBAL=4089284063058.8535.1551777110991; _s_tentry=passport.weibo.com; UOR=,,www.sogou.com; Apache=8015187046780.545.1555897323989; ULV=1555897324054:7:5:1:8015187046780.545.1555897323989:1555732338268; login_sid_t=b97f97c1daa327c9e5446df7b63261b7; cross_origin_proto=SSL; Ugrow-G0=169004153682ef91866609488943c77f; _ga=GA1.2.1039165707.1556434809; _gid=GA1.2.1929343943.1556434809; TC-V5-G0=666db167df2946fecd1ccee47498a93b; wb_view_log=1680*10502; appkey=; WB_register_version=84a7c082648185f6; ALF=1587971633; SSOLoginState=1556435638; SCF=AnsmkOO5RCBYyJVd-VGsD9PQjzJ8AiK2uRgk3sMYArlRntv6zfGbLWdAGk9_p2vwbsc2Sj7Q0gd3VW5kyeR5gW4.; SUB=_2A25xwSLnDeRhGeFP61EU8i3JyDyIHXVStxMvrDV8PUNbmtBeLVDwkW9NQS4245YA2aDU2Eee-aHtWwTnMy7N-MIM; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WF8puQJrYeKg6Y-pvo8b5.r5JpX5KzhUgL.FoMpehefeoefe052dJLoIp7LxKML1KBLBKnLxKqL1hnLBoMcShefSozRe0.R; SUHB=0pHhmhr-dBWmE9; wvr=6; wb_view_log_7103523530=1680*10502; WBtopGlobal_register_version=84a7c082648185f6; TC-Page-G0=1e758cd0025b6b0d876f76c087f85f2c|1556438583|1556438377; webim_unReadCount=%7B%22time%22%3A1556438657812%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A0%2C%22allcountNum%22%3A1%2C%22msgbox%22%3A0%7D; wb_timefeed_7103523530=1");

        cookieStore = new BasicCookieStore();
        Cookie cookie = new BasicClientCookie("", "");
        cookieStore.addCookie(cookie);
    }


    public void sendWeiBoMessage(String message) throws UnsupportedEncodingException {
        HttpHeaders httpHeaders = getHeaders();
        WeiBoRequest request = new WeiBoRequest(message);
        String formData = "location=v6_content_home&text=" + URLEncoder.encode(message) + "&appkey=&style_type=1&pic_id=&tid=&pdetail=&mid=&isReEdit=false&rank=0&rankid=&module=stissue&pub_source=main_&pub_type=dialog&isPri=0&_t=0\n";

        ResponseEntity<String> responseEntity = restTemplate.exchange(SEND_URL + System.currentTimeMillis(), HttpMethod.POST,
                new HttpEntity<>(formData, httpHeaders), String.class);

        logger.info("responseEntity:{}", responseEntity.getBody());
    }

    private HttpHeaders getHeaders() {
        if (this.httpHeaders == null) {
            httpHeaders = new HttpHeaders();
            httpHeaders.add("origin",this.originValue);
            httpHeaders.add("Referer", "https://www.weibo.com/u/7103523530/home?topnav=1&wvr=6");
            httpHeaders.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:34.0) Gecko/20100101 Firefox/34.0");
            httpHeaders.add("X-Requested-With", "XMLHttpRequest");
            httpHeaders.add("Content-Type", "application/x-www-form-urlencoded");
            httpHeaders.add("Cookie", "SINAGLOBAL=4089284063058.8535.1551777110991; _s_tentry=passport.weibo.com; UOR=,,www.sogou.com; Apache=8015187046780.545.1555897323989; ULV=1555897324054:7:5:1:8015187046780.545.1555897323989:1555732338268; login_sid_t=b97f97c1daa327c9e5446df7b63261b7; cross_origin_proto=SSL; Ugrow-G0=169004153682ef91866609488943c77f; _ga=GA1.2.1039165707.1556434809; _gid=GA1.2.1929343943.1556434809; TC-V5-G0=666db167df2946fecd1ccee47498a93b; wb_view_log=1680*10502; appkey=; WB_register_version=84a7c082648185f6; ALF=1587971633; SSOLoginState=1556435638; SCF=AnsmkOO5RCBYyJVd-VGsD9PQjzJ8AiK2uRgk3sMYArlRntv6zfGbLWdAGk9_p2vwbsc2Sj7Q0gd3VW5kyeR5gW4.; SUB=_2A25xwSLnDeRhGeFP61EU8i3JyDyIHXVStxMvrDV8PUNbmtBeLVDwkW9NQS4245YA2aDU2Eee-aHtWwTnMy7N-MIM; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WF8puQJrYeKg6Y-pvo8b5.r5JpX5KzhUgL.FoMpehefeoefe052dJLoIp7LxKML1KBLBKnLxKqL1hnLBoMcShefSozRe0.R; SUHB=0pHhmhr-dBWmE9; wvr=6; wb_view_log_7103523530=1680*10502; WBtopGlobal_register_version=84a7c082648185f6; TC-Page-G0=1e758cd0025b6b0d876f76c087f85f2c|1556438583|1556438377; webim_unReadCount=%7B%22time%22%3A1556438657812%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A0%2C%22allcountNum%22%3A1%2C%22msgbox%22%3A0%7D; wb_timefeed_7103523530=1");

        }



        return httpHeaders;
    }

}
