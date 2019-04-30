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


    @Autowired
    private RestTemplate restTemplate;


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
                    ResponseEntity<String> responseEntity = restTemplate.exchange(HOME, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
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
        /**
         *  "YF-V5-G0=694581d81c495bd4b6d62b3ba4f9f1c8;
         *  Ugrow-G0=9642b0b34b4c0d569ed7a372f8823a8e;
         *  wb_view_log=1680*10502;
         *  login_sid_t=c7a568ed6c1f8dd7f7c70f5acf742767;
         *  cross_origin_proto=SSL;
         *  _s_tentry=passport.weibo.com;
         *  Apache=3769409341707.43.1556510129444;
         *  SINAGLOBAL=3769409341707.43.1556510129444;
         *  ULV=1556510130373:1:1:1:3769409341707.43.1556510129444:;
         *  SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WF8puQJrYeKg6Y-pvo8b5.r5JpX5K2hUgL.FoMpehefeoefe052dJLoIf2LxK-L12BL12-LxK-LBo5L1KBLxKnLBK2L1KMLxKnLBKML1h2LxK.L1KMLB.zLxK-LB--L1--LxKqL1KMLBoqLxKqL1KzLB-BLxKqL122LBK-t;
         *  SSOLoginState=1556512757;
         *  SCF=AjjdiB4MPjhAfrkVYUT_ITO_SSnWahXWtdYwkp7weN9k_C9TSwUQ8HzDhitIQqhF7ukr9tXwaWuWDQP-5e_kWzk.;
         *  SUB=_2A25xwucfDeRhGeFP61EU8i3JyDyIHXVStl_XrDV8PUNbmtBeLWz8kW9NQS4246BCHB95e4DspM6hOM-6jBM7H9xJ;
         *  SUHB=079ziRqS86CByg;
         *  ALF=1588054734;
         *  wvr=6;
         *  wb_timefeed_7103523530=1;
         *  wb_view_log_7103523530=1680*10502;
         *  WBtopGlobal_register_version=84a7c082648185f6;
         *  webim_unReadCount=%7B%22time%22%3A1556519265440%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A0%2C%22allcountNum%22%3A6%2C%22msgbox%22%3A0%7D;
         *  YF-Page-G0=2583080cfb7221db1341f7a137b6762e|1556519268|1556519013
         */


        httpHeaders.add("Cookie", "YF-V5-G0=a5a6106293f9aeef5e34a2e71f04fae4; Ugrow-G0=57484c7c1ded49566c905773d5d00f82; WBtopGlobal_register_version=84a7c082648185f6; wb_view_log_7103523530=1680*10502; _s_tentry=passport.weibo.com; Apache=4612214530500.273.1556531494616; SINAGLOBAL=4612214530500.273.1556531494616; ULV=1556531494667:1:1:1:4612214530500.273.1556531494616:; login_sid_t=617f551f7bbbeabf398da7cca902fac3; cross_origin_proto=SSL; WBStorage=201904291923|undefined; wb_view_log=1680*10502; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WF8puQJrYeKg6Y-pvo8b5.r5JpX5K2hUgL.FoMpehefeoefe052dJLoIf2LxK-L12BL12-LxK-LBo5L1KBLxKnLBK2L1KMLxKnLBKML1h2LxK.L1KMLB.zLxK-LB--L1--LxKqL1KMLBoqLxKqL1KzLB-BLxKqL122LBK-t; ALF=1588073003; SSOLoginState=1556537004; SCF=AkN14rwFVgCw7WapsPHnrq_rIGj3XXtr5f44Qe6Is4pF4VET1X2Sit6s63waoVC8kPCBt0QyGAQuenIVvqBfsaQ.; SUB=_2A25xwq78DeRhGeFP61EU8i3JyDyIHXVSuYc0rDV8PUNbmtBeLVbckW9NQS424yxzkfqAaq4NiuHfKkiykzW6ezDS; SUHB=0102WrEyzcPYnx; un=tioframework@gmail.com; wvr=6; wb_timefeed_7103523530=1; YF-Page-G0=0f25bf37128de43a8f69dd8388468211|1556537153|1556537005; webim_unReadCount=%7B%22time%22%3A1556537154222%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A0%2C%22allcountNum%22%3A1%2C%22msgbox%22%3A0%7D");

    }


    public void sendWeiBoMessage(HttpMessage httpMessage) throws UnsupportedEncodingException {
        WeiBoRequest request = new WeiBoRequest(httpMessage.getContent());

        if ("delete".equals(httpMessage.getOption()) || "update".equals(httpMessage.getOption())) {
            deleteWeiBo(messageMap.get(httpMessage.getId()));
            if ("delete".equals(httpMessage.getOption())) {
                return;
            }
        }
        //发微博去重
        checkMessage(httpMessage.getContent());

        String formData = "location=v6_content_home&text=" + URLEncoder.encode(httpMessage.getContent(), "UTF-8") + "&appkey=&style_type=1&pic_id=&tid=&pdetail=&mid=&isReEdit=false&rank=0&rankid=&module=stissue&pub_source=main_&pub_type=dialog&isPri=0&_t=0\n";
        ResponseEntity<String> responseEntity = restTemplate.exchange(SEND_URL + System.currentTimeMillis(), HttpMethod.POST,
                new HttpEntity<>(formData, httpHeaders), String.class);

        if (responseEntity.getStatusCode() == HttpStatus.FOUND) {
            logger.error("client not login : {}", responseEntity);

        }
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            logger.error("send weibo error : {}", responseEntity);
            return;
        }

        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        logger.info("add mblog responseEntity:{}", jsonObject);

        if (!"100000".equals(jsonObject.getString("code"))) {
            return;
        }

        String data = jsonObject.getString("data");
        String weiBoId = data.substring(data.indexOf("mid") + 4, data.indexOf("action-type")).replaceAll("\\\\", "").replaceAll("\"", "");

        RevokeRequst revokeRequst = new RevokeRequst();
        revokeRequst.setContent(httpMessage.getContent());
        revokeRequst.setClientMsgId(weiBoId.trim());
        revokeRequst.setDate(new Date());
        messageMap.put(httpMessage.getId(), revokeRequst);
    }

    private void deleteWeiBo(RevokeRequst revokeRequst) {
        if (revokeRequst == null) {
            return;
        }
        ResponseEntity<String> responseEntity = restTemplate.exchange(DELETE_URL, HttpMethod.POST,
                new HttpEntity<>("mid=" + revokeRequst.getClientMsgId(), httpHeaders), String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            logger.error("delete weibo error : {}", responseEntity);
            return;
        }
        logger.info("delete mblog responseEntity:{}", responseEntity.getBody());
    }


    private Map<Long, RevokeRequst> messageMap = new HashMap<>();

    private void checkMessage(String content) {
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
            Boolean check = false;
            if (levenshtein.getSimilarityRatio(revokeRequst.getContent(), content) > 0.5F) {
                check = true;
                deleteWeiBo(revokeRequst);
            }
            if (check) {
                iterable.remove();
            }
        }
    }


}
