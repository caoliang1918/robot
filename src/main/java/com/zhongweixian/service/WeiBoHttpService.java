package com.zhongweixian.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhongweixian.domain.weibo.WeiBoUser;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class WeiBoHttpService {
    private Logger logger = LoggerFactory.getLogger(WeiBoHttpService.class);

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


    @Value("${User-Agent}")
    private String USER_AGENT;

    @Autowired
    private RestTemplate restTemplate;


    /**
     * @param weiBoUser
     * @return
     */
    public ResponseEntity<String> openChat(WeiBoUser weiBoUser) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.COOKIE, weiBoUser.getCookie());
        httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
        String referer = String.format("https://weibo.com/%s/home?wvr=5&sudaref=login.sina.com.cn", weiBoUser.getId());
        httpHeaders.add(HttpHeaders.REFERER, referer);
        httpHeaders.add(HttpHeaders.USER_AGENT, CHAT_UA);

        ResponseEntity openChatEntity = restTemplate.exchange(CHAT_URL, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        if (openChatEntity.getStatusCode() != HttpStatus.OK) {
            return null;
        }
        httpHeaders.remove(HttpHeaders.REFERER);
        logger.info("status:{} ,  openChatEntity:{}", openChatEntity.getStatusCode(), openChatEntity.getBody());
        return openChatEntity;
    }

    /**
     * 不一定需要这个请求
     *
     * @return
     */
    public ResponseEntity<String> getQrcode() {
        ResponseEntity<String> qrcodeResponse = restTemplate.getForEntity("https://login.sina.com.cn/sso/qrcode/image?entry=weibo&size=180&source=209678993&callback=__jp0", String.class);
        logger.info("status:{} , qrcodeResponse:{}", qrcodeResponse);
        return qrcodeResponse;
    }

    /**
     * 被主好友
     *
     * @param weiBoUser
     * @return
     */
    public ResponseEntity<String> getRemark(WeiBoUser weiBoUser) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.REFERER, CHAT_REFERER);
        httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
        httpHeaders.add(HttpHeaders.COOKIE, weiBoUser.getCookie());
        ResponseEntity remarkEntity = restTemplate.exchange(CHAT_REMARK, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("status:{} , remarkEntity:{}", remarkEntity.getStatusCode(), remarkEntity.getBody());
        return remarkEntity;
    }

    /**
     * 获取会话信息
     *
     * @param weiBoUser
     * @return
     */
    public ResponseEntity<String> getContent(WeiBoUser weiBoUser) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.REFERER, CHAT_REFERER);
        httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
        httpHeaders.add(HttpHeaders.COOKIE, weiBoUser.getCookie());
        ResponseEntity contentEntity = restTemplate.exchange(CHAT_CONTENT, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("status:{} , contentEntity:{}", contentEntity.getStatusCode(), contentEntity.getBody());
        return contentEntity;
    }

    public ResponseEntity<String> getChannel(WeiBoUser weiBoUser) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.REFERER, "https://api.weibo.com");
        httpHeaders.add(HttpHeaders.COOKIE, weiBoUser.getCookie());
        ResponseEntity channelEntity = restTemplate.exchange(CHAT_CHANNEL, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        logger.info("status:{} , channelEntity:{}", channelEntity.getStatusCode(), channelEntity.getBody());
        return channelEntity;
    }

    /**
     * 握手
     *
     * @param weiBoUser
     * @return
     */
    public JSONObject handshake(WeiBoUser weiBoUser) {
        try {
            URIBuilder builder = new URIBuilder(CHAT_HANDSHAKE);
            builder.addParameter("jsonp", "jQuery112406869571085748343_1561447840951");
            builder.addParameter("message", "[{\"version\":\"1.0\",\"minimumVersion\":\"1.0\",\"channel\":\"/meta/handshake\",\"supportedConnectionTypes\":[\"callback-polling\"],\"advice\":{\"timeout\":60000,\"interval\":0},\"id\":\"2\"}]");
            builder.addParameter("_", String.valueOf(System.currentTimeMillis()));
            URI handUri = builder.build().toURL().toURI();

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
            httpHeaders.add(HttpHeaders.REFERER, "https://web.im.weibo.com");
            httpHeaders.add(HttpHeaders.COOKIE, weiBoUser.getCookie());
            ResponseEntity<String> handshakeEntity = restTemplate.exchange(handUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
            logger.info("status:{} , handshakeEntity:{}", handshakeEntity.getStatusCode(), handshakeEntity.getBody());
            logger.info("handshake Cookie:{}", handshakeEntity.getHeaders().get(HttpHeaders.SET_COOKIE));

            String handshakeResult = handshakeEntity.getBody();
            JSONObject jsonObject = JSON.parseObject(handshakeResult.substring(handshakeResult.indexOf("([") + 2, handshakeResult.indexOf("])")));
            if (!jsonObject.containsKey("clientId")) {
                logger.warn("clientId is null");
                return null;
            }
            return jsonObject;
        } catch (Exception e) {
            logger.error("{}", e);
        }
        return null;
    }


    /**
     * @param weiBoUser
     * @return
     */
    public ResponseEntity<String> subscribe(WeiBoUser weiBoUser) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
        httpHeaders.add(HttpHeaders.COOKIE, weiBoUser.getCookie());

        try {
            URIBuilder builder = new URIBuilder(CHAT_SUBSCRIBE);
            builder.addParameter("jsonp", "jQuery112406869571085748343_1561447840951");
            builder.addParameter("message", "[{\"channel\":\"/meta/subscribe\",\"subscription\":\"/im/" + weiBoUser.getId() + "\",\"id\":\"3\",\"clientId\":\"" + weiBoUser.getClientId() + "\"}]");
            URI uri = builder.build().toURL().toURI();
            ResponseEntity<String> subscribeEntity = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
            logger.info("status:{} , subscribeEntity:{}", subscribeEntity.getStatusCode(), subscribeEntity.getBody());
            logger.info("subscribe Cookie:{}", subscribeEntity.getHeaders().get(HttpHeaders.SET_COOKIE));
            return subscribeEntity;
        } catch (Exception e) {
            logger.error("{}", e);
        }
        return null;
    }


    /**
     * 轮训
     *
     * @param weiBoUser
     * @return
     */
    public ResponseEntity<String> connect(WeiBoUser weiBoUser) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.COOKIE, weiBoUser.getCookie());
            httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
            URIBuilder builder = new URIBuilder(CHAT_CONNECT);
            builder.addParameter("jsonp", "jQuery112406869571085748343_1561447840951");
            builder.addParameter("message", "[{\"channel\":\"/meta/connect\",\"connectionType\":\"callback-polling\",\"id\":\"89\",\"clientId\":\"1jyfxv176zc4vr3hu4ls2pjw6elitrq\"}]");
            URI uri = builder.build().toURL().toURI();
            ResponseEntity connectEntity = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
            logger.debug("status:{} , connectEntity:{}", connectEntity.getStatusCode(), connectEntity.getBody());
            return connectEntity;
        } catch (Exception e) {
            //日志太多了
            logger.debug("{}", e);
        }
        return null;
    }

    /**
     * 下载视频文件
     *
     * @param url
     * @param cookie
     * @return
     */
    public ResponseEntity<byte[]> download(String url, String cookie) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.REFERER, CHAT_REFERER);
        httpHeaders.add(HttpHeaders.COOKIE, cookie);
        httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<byte[]>(httpHeaders), byte[].class);
        return responseEntity;

    }
}
