package com.zhongweixian.service;

import com.alibaba.fastjson.JSONObject;
import com.zhongweixian.domain.HttpMessage;
import com.zhongweixian.domain.request.RevokeRequst;
import com.zhongweixian.domain.request.WeiBoRequest;
import com.zhongweixian.utils.Levenshtein;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by caoliang on 2019-04-28
 * <p>
 * 微博比较简单，不做IM
 */
@Service
public class WeiBoService {
    Logger logger = LoggerFactory.getLogger(WeiBoService.class);

    @Value("${weibo.Referer}")
    private String referer;

    @Value("${User-Agent}")
    private String userAgent;

    @Value("${weibo.username}")
    private String username;

    @Value("${weibo.password}")
    private String password;

    private static final String HOME = "https://weibo.com/u/7103523530/home?topnav=1&wvr=6";
    private static final String SEND_URL = "https://www.weibo.com/aj/mblog/add?ajwvr=6&__rnd=";
    private static final String DELETE_URL = "https://www.weibo.com/aj/mblog/del?ajwvr=6";
    private static final String LOFIN_URL = "https://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.15)";

    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private String origin = "https://www.weibo.com";


    private HttpHeaders httpHeaders;

    @PostConstruct
    public void init() {
        httpHeaders = new HttpHeaders();
        httpHeaders.add("origin", origin);
        httpHeaders.add("Referer", referer);
        httpHeaders.add("User-Agent", userAgent);
        httpHeaders.add("X-Requested-With", "XMLHttpRequest");
        httpHeaders.add("Content-Type", CONTENT_TYPE);

        login();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ResponseEntity<String> responseEntity = new RestTemplate().exchange(HOME, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
                    logger.debug("home page :{}", responseEntity.getBody());

                    try {
                        Thread.sleep(1000 * 600);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    /**
     * 先用邮箱、密码登录，根据返回的URL再去拿cookie，这个URL是一次性的
     */
    public void login() {
        /*String formData = String.format(
                "entry=sso&gateway=1&from=null&savestate=30&useticket=0&pagerefer=&vsnf=1&su=%s&service=sso&sp=%s&sr=1280*800&encoding=UTF-8&cdult=3&domain=sina.com.cn&prelt=0&returntype=TEXT",
                URLEncoder.encode(Base64.encodeBase64String(username.replace("@", "%40").getBytes()), "UTF-8"), password);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Referer", "http://login.sina.com.cn/signup/signin.php?entry=sso");
        headers.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:34.0) Gecko/20100101 Firefox/34.0");
        headers.add("Content-Type", CONTENT_TYPE);
        ResponseEntity<String> responseEntity = restTemplate.exchange(LOFIN_URL, HttpMethod.POST, new HttpEntity<>(formData, httpHeaders), String.class);
        logger.info("login responseEntity :{}", responseEntity);
        String text = responseEntity.getBody();
        String token = null;
        try {
            token = text.substring(text.indexOf("https:"), text.indexOf(",\"https:") - 1).replace("\\", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (token == null) {
            return;
        }
        //https://passport.weibo.com/wbsso/login?ticket=ST-NzEwMzUyMzUzMA%3D%3D-1556522528-gz-C427A34DD45B0991800DA3F6DC59EB1F-1&ssosavestate=1588058528
        logger.info("token:{}", token);
        ResponseEntity<String> cookieResponse = restTemplate.getForEntity(new URI(token), String.class);
        HttpHeaders responseHeaders = cookieResponse.getHeaders();
        if (!responseHeaders.containsKey("Set-Cookie")) {
            logger.error("can not find Cookies");
            return;
        }
        StringBuilder cookies = new StringBuilder();
        List list = responseHeaders.get("Set-Cookie");
        responseHeaders.get("Set-Cookie").forEach(cookie -> {
            logger.info("{}", cookie);
            cookies.append(cookie);
            cookies.append(cookie.split(";")[0]).append(";");
        });
        System.out.println(cookies.toString());*/



        httpHeaders.add("Cookie", "Ugrow-G0=8751d9166f7676afdce9885c6d31cd61; login_sid_t=2da360175a6081f39a0d0c7fb1a3b2f3; cross_origin_proto=SSL; TC-V5-G0=28bf4f11899208be3dc10225cf7ad3c6; WBStorage=201905021112|undefined; wb_view_log=1366*7681; _s_tentry=passport.weibo.com; Apache=5013257162154.1875.1556766743058; SINAGLOBAL=5013257162154.1875.1556766743058; ULV=1556766743067:1:1:1:5013257162154.1875.1556766743058:; SSOLoginState=1556766751; SCF=Al9EEOavAhTWMw9n2hiE4KM_wvzt7X5tFmMVglhMHNP8sysHDbNbEW9fnCszzE0GAaafTEKC_TMT_EcWekgQCOY.; SUB=_2A25xzhBwDeRhGeFP61EU8i3JyDyIHXVSuga4rDV8PUNbmtAKLRbdkW9NQS42409xe69Nx9KKQF2BNAHtgeS3YcMB; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WF8puQJrYeKg6Y-pvo8b5.r5JpX5K2hUgL.FoMpehefeoefe052dJLoI0qLxKMLB.-L12-LxKnL1hzLBK2LxKnLBK2L12eLxKqL1heL1h-LxKqL12-LBKnLxK.L1h5L1h2t; SUHB=08JLrvZzE1QGZQ; ALF=1588302751; un=tioframework@gmail.com; wvr=6; TC-Page-G0=1e758cd0025b6b0d876f76c087f85f2c|1556766757|1556766757; wb_view_log_7103523530=1366*7681; WBtopGlobal_register_version=84a7c082648185f6; wb_timefeed_7103523530=1; webim_unReadCount=%7B%22time%22%3A1556766772563%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A0%2C%22allcountNum%22%3A0%2C%22msgbox%22%3A0%7D");

    }


    public void sendWeiBoMessage(HttpMessage httpMessage){
        WeiBoRequest request = new WeiBoRequest(httpMessage.getContent());
        if ("delete".equals(httpMessage.getOption()) || "update".equals(httpMessage.getOption())) {
            deleteWeiBo(messageMap.get(httpMessage.getId()));
            if ("delete".equals(httpMessage.getOption())) {
                return;
            }
        }
        //发微博去重
        checkMessage(httpMessage);

        HttpHeaders headers = httpHeaders;
        String formData = null;
        try {
            formData = "location=v6_content_home&text=" + URLEncoder.encode(httpMessage.getContent(), "UTF-8") + "&appkey=&style_type=1&pic_id=&tid=&pdetail=&mid=&isReEdit=false&rank=0&rankid=&module=stissue&pub_source=main_&pub_type=dialog&isPri=0&_t=0";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ResponseEntity<String> responseEntity = new RestTemplate().exchange(SEND_URL + System.currentTimeMillis(), HttpMethod.POST,
                new HttpEntity<>(formData, headers), String.class);

        if (responseEntity.getStatusCode() == HttpStatus.FOUND) {
            logger.error("client not login : {}", responseEntity);
            return;
        }
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            logger.error("send weibo error : {}", responseEntity);
            return;
        }

        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        if (!"100000".equals(jsonObject.getString("code"))) {
            logger.error("add mblog statusCode:{} , responseEntity:{}", responseEntity.getStatusCode(), responseEntity.getBody());
            return;
        }
        logger.info("add mblog statusCode:{} , content:{}", responseEntity.getStatusCode(), httpMessage.getContent());

        String data = jsonObject.getString("data");
        String weiBoId = data.substring(data.indexOf("mid") + 4, data.indexOf("action-type")).replaceAll("\\\\", "").replaceAll("\"", "");

        RevokeRequst revokeRequst = new RevokeRequst();
        revokeRequst.setContent(httpMessage.getContent());
        revokeRequst.setClientMsgId(weiBoId.trim());
        revokeRequst.setSvrMsgId(httpMessage.getId().toString());
        revokeRequst.setDate(new Date());
        messageMap.put(httpMessage.getId(), revokeRequst);
    }

    private void deleteWeiBo(RevokeRequst revokeRequst) {
        if (revokeRequst == null) {
            return;
        }
        HttpHeaders headers = httpHeaders;
        ResponseEntity<String> responseEntity = new RestTemplate().exchange(DELETE_URL, HttpMethod.POST, new HttpEntity<>("mid=" + revokeRequst.getClientMsgId(), headers), String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            logger.error("delete weibo error : {}", responseEntity);
            return;
        }
        logger.info("delete mblog responseEntity:{}", responseEntity.getBody());
    }


    private Map<Long, RevokeRequst> messageMap = new HashMap<>();

    private void checkMessage(HttpMessage httpMessage) {
        /**
         * 判断相似度
         */
        Levenshtein levenshtein = new Levenshtein();
        Date now = new Date();

        Iterator<Long> iterable = messageMap.keySet().iterator();
        while (iterable.hasNext()) {
            RevokeRequst revokeRequst = messageMap.get(iterable.next());

            /**
             * 已经超时
             */
            if (now.getTime() - revokeRequst.getDate().getTime() > 2000 * 1000L) {
                iterable.remove();
                continue;
            }
            /**
             * 文本相似度
             */
            if (levenshtein.getSimilarityRatio(revokeRequst.getContent(), httpMessage.getContent()) > 0.5F || httpMessage.getId().equals(revokeRequst.getSvrMsgId())) {
                iterable.remove();
                deleteWeiBo(revokeRequst);
            }
        }
    }


}
