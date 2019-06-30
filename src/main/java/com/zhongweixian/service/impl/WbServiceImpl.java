package com.zhongweixian.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhongweixian.domain.HttpMessage;
import com.zhongweixian.domain.request.RevokeRequst;
import com.zhongweixian.service.WbBlockUser;
import com.zhongweixian.service.WbService;
import com.zhongweixian.utils.Levenshtein;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caoliang on 2019-06-25
 */

@Component
public class WbServiceImpl implements WbService {
    private Logger logger = LoggerFactory.getLogger(WbServiceImpl.class);

    @Value("${weibo.username}")
    private String username;

    @Value("${weibo.password}")
    private String password;

    @Autowired
    private ScheduledExecutorService wbExecutor;

    @Autowired
    private WbBlockUser wbBlockUser;

    private String cookie;

    private Integer loginTime = 0;

    private Boolean nextLogin = true;

    /**
     * 获取首页,同时限制登录频次 {} 分钟
     */
    private static final Integer HONE_RATE = 10;


    private static final String HOME = "https://weibo.com/u/%s/home?topnav=1&wvr=6";
    private static final String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 SE 2.X MetaSr 1.0";
    private static final String LOFIN_URL = "https://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.15)";
    private static final String SEND_URL = "https://www.weibo.com/aj/mblog/add?ajwvr=6&__rnd=";
    private static final String DELETE_URL = "https://www.weibo.com/aj/mblog/del?ajwvr=6";

    private HttpHeaders defaultHeader;

    private String homeReferer = "https://www.weibo.com/u/%s/home?topnav=1&wvr=6";

    private String uid;


    @PostConstruct
    private void task() {
        /**
         * 定时任务1:打开我的主页
         */
        wbExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (uid == null || cookie == null) {
                        return;
                    }
                    ResponseEntity<String> responseEntity = new RestTemplate().exchange(String.format(HOME, uid), HttpMethod.GET, new HttpEntity<>(defaultHeader), String.class);
                    logger.info("get weibo base home ,status:{}", responseEntity.getStatusCode());
                } catch (Exception e) {
                    logger.error("{}", e);
                }
                nextLogin = true;
            }
        }, 5, HONE_RATE, TimeUnit.MINUTES);

    }


    @Override
    public Boolean login() {
        if (!nextLogin) {
            logger.warn("登录次数过多 , loginTime:{}", loginTime);
            return false;
        }
        loginTime++;
        nextLogin = false;
        String formData = null;
        try {
            formData = String.format(
                    "entry=sso&gateway=1&from=null&savestate=30&useticket=0&pagerefer=&vsnf=1&su=%s&service=sso&sp=%s&sr=1280*800&encoding=UTF-8&cdult=3&domain=sina.com.cn&prelt=0&returntype=TEXT",
                    URLEncoder.encode(Base64.encodeBase64String(username.replace("@", "%40").getBytes()), "UTF-8"), password);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("origin", "https://www.weibo.com");
        httpHeaders.add("Referer", "https://www.weibo.com");
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 SE 2.X MetaSr 1.0");
        httpHeaders.add("X-Requested-With", "XMLHttpRequest");
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(LOFIN_URL, HttpMethod.POST, new HttpEntity<>(formData, httpHeaders), String.class);
        } catch (Exception e) {
            logger.error("{}", e);
            return false;
        }
        logger.info("login responseEntity :{}", responseEntity.getBody());
        String text = responseEntity.getBody();
        JSONObject jsonObject = JSON.parseObject(text);
        if (!"0".equals(jsonObject.getString("retcode"))) {
            logger.error("login error , username:{} , retcode:{}", username, jsonObject.getString("retcode"));
            return false;
        }
        uid = jsonObject.getString("uid");
        String token = null;
        try {
            token = text.substring(text.indexOf("https:"), text.indexOf(",\"https:") - 1).replace("\\", "");
        } catch (Exception e) {
            logger.error("{}", e);
        }
        if (token == null) {
            return false;
        }
        logger.info("token:{}", token);
        ResponseEntity<String> cookieResponse = null;
        try {
            cookieResponse = restTemplate.getForEntity(new URI(token), String.class);
        } catch (Exception e) {
            logger.error("{}", e);
        }
        HttpHeaders responseHeaders = cookieResponse.getHeaders();
        if (!responseHeaders.containsKey("Set-Cookie")) {
            logger.error("can not find Cookies");
            return false;
        }
        StringBuilder cookies = new StringBuilder();
        List<String> list = responseHeaders.get("Set-Cookie");
        list.forEach(cookie -> {
            logger.info("{}", cookie);
            cookies.append(cookie);
            cookies.append(cookie.split(";")[0]).append(";");
        });
        cookie = cookies.toString().substring(0, cookies.length() - 1);

        defaultHeader = new HttpHeaders();
        defaultHeader.add("origin", "https://www.weibo.com");
        defaultHeader.add("Referer", String.format(homeReferer, uid));
        defaultHeader.add("User-Agent", userAgent);
        defaultHeader.add("X-Requested-With", "XMLHttpRequest");
        defaultHeader.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        defaultHeader.add("Cookie", cookie);
        wbBlockUser.setHeaders(defaultHeader);
        return true;
    }

    @Override
    public String getCookie() {
        return cookie;
    }

    @Override
    public void sendWbBlog(HttpMessage httpMessage) {
        if ("delete".equals(httpMessage.getOption()) || "update".equals(httpMessage.getOption())) {
            deleteWeiBo(messageMap.get(httpMessage.getId()));
            if ("delete".equals(httpMessage.getOption())) {
                return;
            }
        }
        /**
         * 发微博去重
         */
        checkMessage(httpMessage);


        String formData = null;
        ResponseEntity<String> responseEntity = null;
        try {
            formData = "location=v6_content_home&text=" + URLEncoder.encode(httpMessage.getContent(), "UTF-8") + "&appkey=&style_type=1&pic_id=&tid=&pdetail=&mid=&isReEdit=false&rank=0&rankid=&module=stissue&pub_source=main_&pub_type=dialog&isPri=0&_t=0";
            responseEntity = new RestTemplate().exchange(SEND_URL + System.currentTimeMillis(), HttpMethod.POST,
                    new HttpEntity<>(formData, defaultHeader), String.class);
        } catch (Exception e) {
            logger.error("{}", e);
            return;
        }
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

        String data = jsonObject.getString("data");
        String weiBoId = data.substring(data.indexOf("mid") + 4, data.indexOf("action-type")).replaceAll("\\\\", "").replaceAll("\"", "");
        logger.info("add mblog statusCode:{} , content:{} , weiboId:{}", responseEntity.getStatusCode(), httpMessage.getContent(), weiBoId);

        /**
         * 用于记录重复微博，如果在2000秒内有重复，则删除之前的微博
         */
        RevokeRequst revokeRequst = new RevokeRequst();
        revokeRequst.setContent(httpMessage.getContent());
        revokeRequst.setClientMsgId(weiBoId.trim());
        revokeRequst.setSvrMsgId(httpMessage.getId().toString());
        revokeRequst.setDate(new Date());
        messageMap.put(httpMessage.getId(), revokeRequst);
    }

    @Override
    public void deleteWeiBo(RevokeRequst revokeRequst) {
        if (revokeRequst == null) {
            return;
        }
        ResponseEntity<String> responseEntity = new RestTemplate().exchange(DELETE_URL, HttpMethod.POST, new HttpEntity<>("mid=" + revokeRequst.getClientMsgId(), defaultHeader), String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            logger.error("delete weibo error : {}", responseEntity);
            return;
        }
        logger.info("delete mblog responseEntity:{}", responseEntity.getBody());
    }

    @Override
    public void sendWbMessage(String username, String message) {

    }


    /**
     * 把已经发送的微博存在hashMap中
     */
    private Map<Long, RevokeRequst> messageMap = new HashMap<>();

    private final String checkContent = "微信";

    /**
     * 检查重复微博
     *
     * @param httpMessage
     */
    private void checkMessage(HttpMessage httpMessage) {
        if (httpMessage.getContent().contains(checkContent)) {
            String content = httpMessage.getContent();
            httpMessage.setContent(content.substring(0, content.indexOf(checkContent)));
        }

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

    @Override
    public String getUid() {
        return uid;
    }

}
