package com.zhongweixian.login;

import com.zhongweixian.service.WbService;
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
    //备注好友
    private final static String CHAT_REMARK = "https://api.weibo.com/webim/query_remark.json?source=209678993&t=1561447841245";
    //联系人列表
    private final static String CHAT_CONTENT = "https://api.weibo.com/webim/2/direct_messages/contacts.json?add_virtual_user=3&is_include_group=0&need_back=0,0&count=30&source=209678993&t=1561451504153";
    //channel
    private final static String CHAT_CHANNEL = "https://api.weibo.com/webim/webim_nas.json?source=209678993&returntype=json&v=1.1&callback=__jp1";
    //handshake
    private final static String CHAT_HANDSHAKE = "https://web.im.weibo.com/im/handshake?jsonp=jQuery112406869571085748343_1561447840951&message=%s&_=1561447840952";


    private final static String CHAT_REFERER = "https://api.weibo.com/chat/";
    private final static String CHAT_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36";


    @Autowired
    private WbService wbService;

    @Autowired
    private ScheduledExecutorService wbExecutor;

    @Autowired
    private RestTemplate restTemplate;


    @PostConstruct
    public void init() throws UnsupportedEncodingException {
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
        logger.info("status:{} , body:{}", openChatEntity.getStatusCode(), openChatEntity.getBody());
        /**
         * 获取qrcode  这一步可能用不上
         * https://login.sina.com.cn/sso/qrcode/image?entry=weibo&size=180&source=209678993&callback=__jp0
         */
        String qrcodeResponse = restTemplate.getForObject("https://login.sina.com.cn/sso/qrcode/image?entry=weibo&size=180&source=209678993&callback=__jp0", String.class);
        logger.info("qrcodeResponse:{}", qrcodeResponse);

        /**
         * 获取备注好友列表
         * GET  https://api.weibo.com/webim/query_remark.json?source=209678993&t=1561447841245
         */
        httpHeaders.add(HttpHeaders.REFERER, CHAT_REFERER);
        ResponseEntity remarkEntity = restTemplate.exchange(CHAT_REMARK, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("remarkEntity:{}", remarkEntity.getBody());

        /**
         * 获取消息列表
         * GET https://api.weibo.com/webim/2/direct_messages/contacts.json?add_virtual_user=3&is_include_group=0&need_back=0,0&count=30&source=209678993&t=1561451504153
         *
         */
        ResponseEntity contentEntity = restTemplate.exchange(CHAT_CONTENT, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("contentEntity:{}", contentEntity.getBody());


        /**
         * 获取IM channel
         * https://api.weibo.com/webim/webim_nas.json?source=209678993&returntype=json&v=1.1&callback=__jp1
         * response : try{__jp1({"server":"https://web.im.weibo.com/","channel":"/im/7194846339"});}catch(e){}
         */
        ResponseEntity channelEntity = restTemplate.exchange(CHAT_CHANNEL, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("channelEntity:{}", channelEntity.getBody());


        /**
         * handshake 握手
         * https://web.im.weibo.com/im/handshake?jsonp=jQuery112406869571085748343_1561447840951&message=
         * [{"version":"1.0","minimumVersion":"1.0","channel":"/meta/handshake","supportedConnectionTypes":["callback-polling"],"advice":{"timeout":60000,"interval":0},"id":"2"}]&_=1561447840952
         */
        httpHeaders = new HttpHeaders();
        httpHeaders.add("Host" , "web.im.weibo.com");
        httpHeaders.add("Upgrade-Insecure-Requests" , "1");
        httpHeaders.add("Cookie" , "SINAGLOBAL=5080674429158.669.1556553357725; _ga=GA1.2.224659666.1557758545; un=tioframework@gmail.com; _s_tentry=-; Apache=8143274685276.611.1561289720108; ULV=1561289720126:8:5:1:8143274685276.611.1561289720108:1560444029950; BAYEUX_BROWSER=a55b-1n0j9jmtvwg2pjx8wd02ywnv; login_sid_t=fa7f984943e807fcfecfa47bdb2e2768; cross_origin_proto=SSL; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WF8puQJrYeKg6Y-pvo8b5.r5JpX5K2hUgL.FoMpehefeoefe052dJLoI0qLxKMLB.-L12-LxKnL1hzLBK2LxKnLBK2L12eLxKqL1heL1h-LxKqL12-LBKnLxK.L1h5L1h2t; ALF=1593007050; SSOLoginState=1561471051; SCF=AvfocslHFfRL3A3QdY693A_dLQL-XjeG7C_R53ve7tvCzy9L_61UKxKVATrxlgQoK6kX0iWBNFQs6AqgXzoYWJM.; SUB=_2A25wFlgbDeRhGeFP61EU8i3JyDyIHXVTYs7TrDV8PUNbmtBeLU3wkW9NQS4245U-d5N1jRmVORbQVkw5HZL-6W63; SUHB=0KABy07tRXVlwV; wvr=6; UOR=,,cn.club.vmall.com; webim_unReadCount=%7B%22time%22%3A1561476432863%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A0%2C%22allcountNum%22%3A0%2C%22msgbox%22%3A0%7D");
         ResponseEntity handshakeEntity = restTemplate.exchange("https://web.im.weibo.com/im/handshake?jsonp=jQuery112408767360835790106_1561476271482&message=%5B%7B%22version%22%3A%221.0%22%2C%22minimumVersion%22%3A%221.0%22%2C%22channel%22%3A%22%2Fmeta%2Fhandshake%22%2C%22supportedConnectionTypes%22%3A%5B%22callback-polling%22%5D%2C%22advice%22%3A%7B%22timeout%22%3A60000%2C%22interval%22%3A0%7D%2C%22id%22%3A%222%22%7D%5D&_=1561476271483", HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("handshakeEntity:{}", handshakeEntity.getBody());

        /**
         * IM 订阅消息
         * https://web.im.weibo.com/im/?jsonp=jQuery112406869571085748343_1561447840951&message=
         * [{"channel":"/meta/subscribe","subscription":"/im/2672447121","id":"3","clientId":"1jl3zr1vvsca5hspfp6ww6vxgc6ne2s"}]&_=1561447840953
         */

        /**
         * 发送report
         * POST https://api.weibo.com/webim/report.json
         */

        /**
         * 轮训新消息
         * https://web.im.weibo.com/im/connect?jsonp=jQuery112405320978791403412_1561451503942&message=
         * [{"channel":"/meta/connect","connectionType":"callback-polling","advice":{"timeout":0},"id":"4","clientId":"1jlveqbjc1xat687b71kh97b7s7ssjl"}]&_=1561451503943
         */
    }


    private RestTemplate createRest() {

        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(2000);
        simpleClientHttpRequestFactory.setReadTimeout(50000);
        RestTemplate restTemplate = new RestTemplate(simpleClientHttpRequestFactory);
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
