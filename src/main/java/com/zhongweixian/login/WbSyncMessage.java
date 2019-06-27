package com.zhongweixian.login;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhongweixian.service.WbService;
import jdk.nashorn.internal.ir.IfNode;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 微博IM同步消息类
 */
@Component
public class WbSyncMessage {
    private Logger logger = LoggerFactory.getLogger(WbSyncMessage.class);

    private final static String CHAT_URL = "https://api.weibo.com/chat/#/chat?source_from=2";
    /**
     * 备注好友
     */
    private final static String CHAT_REMARK = "https://api.weibo.com/webim/query_remark.json?source=209678993&t=1561447841245";
    /**
     * 联系人列表
     */
    private final static String CHAT_CONTENT = "https://api.weibo.com/webim/2/direct_messages/contacts.json?add_virtual_user=3&is_include_group=0&need_back=0,0&count=30&source=209678993&t=1561451504153";
    /**
     * channel
     */
    private final static String CHAT_CHANNEL = "https://api.weibo.com/webim/webim_nas.json?source=209678993&returntype=json&v=1.1&callback=__jp1";
    /**
     * handshake
     */
    private final static String CHAT_HANDSHAKE = "https://web.im.weibo.com/im/handshake";

    /**
     * 订阅
     */
    private final static String CHAT_SUBSCRIBE = "https://web.im.weibo.com/im";

    /**
     * 轮训
     */
    private final static String CHAT_CONNECT = "https://web.im.weibo.com/im/connect";


    private final static String CHAT_REFERER = "https://api.weibo.com/chat/";
    private final static String CHAT_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36";


    @Autowired
    private WbService wbService;

    @Autowired
    private ScheduledExecutorService wbExecutor;

    @Autowired
    private RestTemplate restTemplate;


    @PostConstruct
    public void init() throws URISyntaxException, MalformedURLException {
        logger.info("PostConstruct {} , {}", wbService, wbExecutor);

        if (!wbService.login()) {
            return;
        }

        String uid = wbService.getUid();

        String cookie = wbService.getCookie();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.COOKIE, cookie);
        String referer = String.format("https://weibo.com/%s/home?wvr=5&sudaref=login.sina.com.cn", uid);
        httpHeaders.add(HttpHeaders.REFERER, referer);
        httpHeaders.add(HttpHeaders.USER_AGENT, CHAT_UA);

        ResponseEntity openChatEntity = restTemplate.exchange(CHAT_URL, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        if (openChatEntity.getStatusCode() != HttpStatus.OK) {
            return;
        }
        logger.info("status:{} ,  openChatEntity:{}", openChatEntity.getStatusCode(), openChatEntity.getBody());
        /**
         * 获取qrcode  这一步可能用不上
         * https://login.sina.com.cn/sso/qrcode/image?entry=weibo&size=180&source=209678993&callback=__jp0
         */
        String qrcodeResponse = restTemplate.getForObject("https://login.sina.com.cn/sso/qrcode/image?entry=weibo&size=180&source=209678993&callback=__jp0", String.class);
        logger.info("status:{} , qrcodeResponse:{}", qrcodeResponse);

        /**
         * 获取备注好友列表
         * GET  https://api.weibo.com/webim/query_remark.json?source=209678993&t=1561447841245
         */
        httpHeaders.add(HttpHeaders.REFERER, CHAT_REFERER);
        ResponseEntity remarkEntity = restTemplate.exchange(CHAT_REMARK, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("status:{} , remarkEntity:{}", remarkEntity.getStatusCode(), remarkEntity.getBody());

        /**
         * 获取消息列表
         * GET https://api.weibo.com/webim/2/direct_messages/contacts.json?add_virtual_user=3&is_include_group=0&need_back=0,0&count=30&source=209678993&t=1561451504153
         *
         */
        ResponseEntity contentEntity = restTemplate.exchange(CHAT_CONTENT, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("status:{} , contentEntity:{}", contentEntity.getStatusCode(), contentEntity.getBody());


        /**
         * 获取IM channel
         * https://api.weibo.com/webim/webim_nas.json?source=209678993&returntype=json&v=1.1&callback=__jp1
         * response : try{__jp1({"server":"https://web.im.weibo.com/","channel":"/im/7194846339"});}catch(e){}
         */
        ResponseEntity channelEntity = restTemplate.exchange(CHAT_CHANNEL, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("status:{} , channelEntity:{}", channelEntity.getStatusCode(), channelEntity.getBody());


        /**
         * handshake 握手
         * https://web.im.weibo.com/im/handshake?jsonp=jQuery112406869571085748343_1561447840951&message=
         * [{"version":"1.0","minimumVersion":"1.0","channel":"/meta/handshake","supportedConnectionTypes":["callback-polling"],"advice":{"timeout":60000,"interval":0},"id":"2"}]&_=1561447840952
         */
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
        URIBuilder builder = new URIBuilder(CHAT_HANDSHAKE);
        builder.addParameter("jsonp", "jQuery112406869571085748343_1561447840951");
        builder.addParameter("message", "[{\"version\":\"1.0\",\"minimumVersion\":\"1.0\",\"channel\":\"/meta/handshake\",\"supportedConnectionTypes\":[\"callback-polling\"],\"advice\":{\"timeout\":60000,\"interval\":0},\"id\":\"2\"}]");
        builder.addParameter("_", String.valueOf(System.currentTimeMillis()));
        URI handUri = builder.build().toURL().toURI();
        ResponseEntity<String> handshakeEntity = restTemplate.exchange(handUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("status:{} , handshakeEntity:{}", handshakeEntity.getStatusCode(), handshakeEntity.getBody());
        logger.info("handshake Cookie:{}", handshakeEntity.getHeaders().get(HttpHeaders.SET_COOKIE));

        String handshakeResult = handshakeEntity.getBody();
        JSONObject jsonObject = JSON.parseObject(handshakeResult.substring(handshakeResult.indexOf("([") + 2, handshakeResult.indexOf("])")));
        if (!jsonObject.containsKey("clientId")) {
            logger.warn("clientId is null");
            return;
        }


        /**
         * IM 订阅消息
         * https://web.im.weibo.com/im/?jsonp=jQuery112406869571085748343_1561447840951&message=
         * [{"channel":"/meta/subscribe","subscription":"/im/2672447121","id":"3","clientId":"1jl3zr1vvsca5hspfp6ww6vxgc6ne2s"}]&_=1561447840953
         */


        httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.COOKIE, "_s_tentry=login.sina.com.cn; UOR=login.sina.com.cn,weibo.com,login.sina.com.cn; Apache=9456802003620.64.1561629285945; SINAGLOBAL=9456802003620.64.1561629285945; ULV=1561629286071:1:1:1:9456802003620.64.1561629285945:; BAYEUX_BROWSER=f80f-1qs8ysmrs5nhojxehriqn1czz; login_sid_t=7ef39c96c39b4c611ff3ab52ab13f681; cross_origin_proto=SSL; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whu6d1rS1RzpY.o0E_MyWMw5JpX5K2hUgL.FoMp1KBRShq0e0.2dJLoIf2LxKqL122LBKBLxK.LB.-L1K.LxK-LBo2LBo2LxK-LB.2L1hBLxK-LBKBLBK.LxK-LB-BL1KMLxKMLB-eLB-eLxKBLBonL1h5LxK-L12qLBoMt; ALF=1593165375; SSOLoginState=1561629375; SCF=Aqqh9eM0QX_5TJHvIT1Bp_DVlqjNkTXae_JopKR-sMiuT-7t3hhhu1-_txV34Ed3yGyBBe3pYFNaOydNZdKpP1M.; SUB=_2A25wEOKQDeRhGeFP4lYZ9CjPyDWIHXVTZFNYrDV8PUNbmtBeLUzykW9NQO_rykPBZjlLxPMsdDujy8O6LC3bU0g7; SUHB=0vJYo4Mu_LBsWG; un=1923531384@qq.com; wvr=6; webim_unReadCount=%7B%22time%22%3A1561629481545%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A5452%2C%22allcountNum%22%3A5452%2C%22msgbox%22%3A0%7D");
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
        builder = new URIBuilder(CHAT_SUBSCRIBE);
        builder.addParameter("jsonp", "jQuery112406869571085748343_1561447840951");
        builder.addParameter("message", "[{\"channel\":\"/meta/subscribe\",\"subscription\":\"/im/" + uid + "\",\"id\":\"3\",\"clientId\":\"" + jsonObject.getString("clientId") + "\"}]");
        URI uri = builder.build().toURL().toURI();
        ResponseEntity<String> subscribeEntity = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("status:{} , subscribeEntity:{}", subscribeEntity.getStatusCode(), subscribeEntity.getBody());
        logger.info("subscribe Cookie:{}", subscribeEntity.getHeaders().get(HttpHeaders.SET_COOKIE));
        /**
         * 发送report
         * POST https://api.weibo.com/webim/report.json
         */

        /**
         * 轮训新消息
         * https://web.im.weibo.com/im/connect?jsonp=jQuery112405320978791403412_1561451503942&message=
         * [{"channel":"/meta/connect","connectionType":"callback-polling","advice":{"timeout":0},"id":"4","clientId":"1jlveqbjc1xat687b71kh97b7s7ssjl"}]&_=1561451503943
         */
        builder = new URIBuilder(CHAT_CONNECT);
        builder.addParameter("_", String.valueOf(System.currentTimeMillis()));
        builder.addParameter("jsonp", "jQuery112406869571085748343_1561447840951");
        builder.addParameter("message", "[{\"channel\":\"/meta/connect\",\"connectionType\":\"callback-polling\",\"id\":\"89\",\"clientId\":\"1jyfxv176zc4vr3hu4ls2pjw6elitrq\"}]");
        URI connecteUri = builder.build().toURL().toURI();

        HttpHeaders finalHttpHeaders = httpHeaders;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        ResponseEntity connectEntity = restTemplate.exchange(connecteUri, HttpMethod.GET, new HttpEntity<>(finalHttpHeaders), String.class);
                        logger.info("status:{} , subscribeEntity:{}", connectEntity.getStatusCode(), connectEntity.getBody());
                    } catch (Exception e) {
                        logger.error("{}", e.getMessage());
                    }
                }
            }
        }).start();
    }


    private static RestTemplate createRest() {

        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(2000);
        simpleClientHttpRequestFactory.setReadTimeout(50000);
        RestTemplate restTemplate = new RestTemplate(simpleClientHttpRequestFactory);
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    public static void main(String[] args) throws URISyntaxException, MalformedURLException {
        System.setProperty("javax.net.ssl.trustStore", "src/main/resources/cert/weibo.cer");
        RestTemplate restTemplate = createRest();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.COOKIE, "_s_tentry=login.sina.com.cn; UOR=login.sina.com.cn,weibo.com,login.sina.com.cn; Apache=9456802003620.64.1561629285945; SINAGLOBAL=9456802003620.64.1561629285945; ULV=1561629286071:1:1:1:9456802003620.64.1561629285945:; BAYEUX_BROWSER=f80f-1qs8ysmrs5nhojxehriqn1czz; login_sid_t=7ef39c96c39b4c611ff3ab52ab13f681; cross_origin_proto=SSL; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whu6d1rS1RzpY.o0E_MyWMw5JpX5K2hUgL.FoMp1KBRShq0e0.2dJLoIf2LxKqL122LBKBLxK.LB.-L1K.LxK-LBo2LBo2LxK-LB.2L1hBLxK-LBKBLBK.LxK-LB-BL1KMLxKMLB-eLB-eLxKBLBonL1h5LxK-L12qLBoMt; ALF=1593165375; SSOLoginState=1561629375; SCF=Aqqh9eM0QX_5TJHvIT1Bp_DVlqjNkTXae_JopKR-sMiuT-7t3hhhu1-_txV34Ed3yGyBBe3pYFNaOydNZdKpP1M.; SUB=_2A25wEOKQDeRhGeFP4lYZ9CjPyDWIHXVTZFNYrDV8PUNbmtBeLUzykW9NQO_rykPBZjlLxPMsdDujy8O6LC3bU0g7; SUHB=0vJYo4Mu_LBsWG; un=1923531384@qq.com; wvr=6; webim_unReadCount=%7B%22time%22%3A1561629481545%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A5452%2C%22allcountNum%22%3A5452%2C%22msgbox%22%3A0%7D");
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
        String url = "https://web.im.weibo.com/im/connect";
        URIBuilder builder = new URIBuilder(url);
        builder.addParameter("jsonp", "jQuery112406869571085748343_1561447840951");
        builder.addParameter("message", "[{\"channel\":\"/meta/connect\",\"connectionType\":\"callback-polling\",\"id\":\"89\",\"clientId\":\"1jyfxv176zc4vr3hu4ls2pjw6elitrq\"}]");
        URI uri = builder.build().toURL().toURI();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        ResponseEntity responseEntity = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
                        System.out.println(responseEntity.getBody());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }
}
