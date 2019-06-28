package com.zhongweixian.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhongweixian.domain.weibo.WeiBoUser;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

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
        httpHeaders.add(HttpHeaders.COOKIE, "_s_tentry=login.sina.com.cn; UOR=login.sina.com.cn,weibo.com,login.sina.com.cn; Apache=9456802003620.64.1561629285945; SINAGLOBAL=9456802003620.64.1561629285945; ULV=1561629286071:1:1:1:9456802003620.64.1561629285945:; BAYEUX_BROWSER=f80f-1qs8ysmrs5nhojxehriqn1czz; login_sid_t=7ef39c96c39b4c611ff3ab52ab13f681; cross_origin_proto=SSL; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whu6d1rS1RzpY.o0E_MyWMw5JpX5K2hUgL.FoMp1KBRShq0e0.2dJLoIf2LxKqL122LBKBLxK.LB.-L1K.LxK-LBo2LBo2LxK-LB.2L1hBLxK-LBKBLBK.LxK-LB-BL1KMLxKMLB-eLB-eLxKBLBonL1h5LxK-L12qLBoMt; ALF=1593165375; SSOLoginState=1561629375; SCF=Aqqh9eM0QX_5TJHvIT1Bp_DVlqjNkTXae_JopKR-sMiuT-7t3hhhu1-_txV34Ed3yGyBBe3pYFNaOydNZdKpP1M.; SUB=_2A25wEOKQDeRhGeFP4lYZ9CjPyDWIHXVTZFNYrDV8PUNbmtBeLUzykW9NQO_rykPBZjlLxPMsdDujy8O6LC3bU0g7; SUHB=0vJYo4Mu_LBsWG; un=1923531384@qq.com; wvr=6; webim_unReadCount=%7B%22time%22%3A1561629481545%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A5452%2C%22allcountNum%22%3A5452%2C%22msgbox%22%3A0%7D");

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
            httpHeaders.add(HttpHeaders.COOKIE, "_s_tentry=login.sina.com.cn; UOR=login.sina.com.cn,weibo.com,login.sina.com.cn; Apache=9456802003620.64.1561629285945; SINAGLOBAL=9456802003620.64.1561629285945; ULV=1561629286071:1:1:1:9456802003620.64.1561629285945:; BAYEUX_BROWSER=f80f-1qs8ysmrs5nhojxehriqn1czz; login_sid_t=7ef39c96c39b4c611ff3ab52ab13f681; cross_origin_proto=SSL; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whu6d1rS1RzpY.o0E_MyWMw5JpX5K2hUgL.FoMp1KBRShq0e0.2dJLoIf2LxKqL122LBKBLxK.LB.-L1K.LxK-LBo2LBo2LxK-LB.2L1hBLxK-LBKBLBK.LxK-LB-BL1KMLxKMLB-eLB-eLxKBLBonL1h5LxK-L12qLBoMt; ALF=1593165375; SSOLoginState=1561629375; SCF=Aqqh9eM0QX_5TJHvIT1Bp_DVlqjNkTXae_JopKR-sMiuT-7t3hhhu1-_txV34Ed3yGyBBe3pYFNaOydNZdKpP1M.; SUB=_2A25wEOKQDeRhGeFP4lYZ9CjPyDWIHXVTZFNYrDV8PUNbmtBeLUzykW9NQO_rykPBZjlLxPMsdDujy8O6LC3bU0g7; SUHB=0vJYo4Mu_LBsWG; un=1923531384@qq.com; wvr=6; webim_unReadCount=%7B%22time%22%3A1561629481545%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A5452%2C%22allcountNum%22%3A5452%2C%22msgbox%22%3A0%7D");
            httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
            URIBuilder builder = new URIBuilder(CHAT_CONNECT);
            builder.addParameter("jsonp", "jQuery112406869571085748343_1561447840951");
            builder.addParameter("message", "[{\"channel\":\"/meta/connect\",\"connectionType\":\"callback-polling\",\"id\":\"89\",\"clientId\":\"1jyfxv176zc4vr3hu4ls2pjw6elitrq\"}]");
            URI uri = builder.build().toURL().toURI();
            ResponseEntity connectEntity = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
            logger.debug("status:{} , connectEntity:{}", connectEntity.getStatusCode(), connectEntity.getBody());
            return connectEntity;
        } catch (Exception e) {
            logger.error("{}", e);
        }
        return null;
    }

    private static RestTemplate createRest() {

        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(2000);
        simpleClientHttpRequestFactory.setReadTimeout(50000);
        RestTemplate restTemplate = new RestTemplate(simpleClientHttpRequestFactory);
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
        RestTemplate restTemplate = createRest();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.REFERER , "https://api.weibo.com/chat/");
        httpHeaders.add(HttpHeaders.COOKIE, "JSESSIONID=2389A3740D82E4F72190EF2E1C9F1AAC; _s_tentry=login.sina.com.cn; Apache=351887198397.7934.1561655226197; SINAGLOBAL=351887198397.7934.1561655226197; ULV=1561655226225:1:1:1:351887198397.7934.1561655226197:; login_sid_t=dacbff5a360e932f01cd451ea2971302; cross_origin_proto=SSL; UOR=,,login.sina.com.cn; SUB=_2A25wEkwKDeRhGeFP4lYZ9CjPyDWIHXVTZjrCrDV8PUNbmtBeLVTmkW9NQO_rylqUaPxKm--fXfnTtiqhvxqoXgSO; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whu6d1rS1RzpY.o0E_MyWMw5JpX5KzhUgL.FoMp1KBRShq0e0.2dJLoIEXLxK-L12qL12BLxKML1hnLB-eLxKqL1-eLB.2LxK-L1K.LBKnLxKBLB.2LB.2t; SUHB=03oVW_nyuoplSt; ALF=1593274329; SSOLoginState=1561738330; wvr=6; webim_unReadCount=%7B%22time%22%3A1561741653808%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A1002%2C%22allcountNum%22%3A1002%2C%22msgbox%22%3A0%7D");
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
        String url = "https://upload.api.weibo.com/2/mss/msget?source=209678993&fid=4388327261988989";
        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(url ,HttpMethod.GET, new HttpEntity<byte[]>(httpHeaders),  byte[].class );
        byte[] result = responseEntity.getBody();
        System.out.println(result.length);
        System.out.println(result.hashCode());
        InputStream inputStream = new ByteArrayInputStream(result);
        File file = new File("logs/"+System.currentTimeMillis()+".mp4");
        FileUtils.copyInputStreamToFile(inputStream , file);
        inputStream.close();
    }
}
