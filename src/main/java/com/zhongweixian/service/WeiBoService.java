package com.zhongweixian.service;

import com.alibaba.fastjson.JSONObject;
import com.zhongweixian.domain.HttpMessage;
import com.zhongweixian.domain.request.RevokeRequst;
import com.zhongweixian.utils.Levenshtein;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    @Value("${weibo.username}")
    private String username;

    @Value("${weibo.password}")
    private String password;

    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("schedule-pool--%d").daemon(true).build());

    private static final String HOME = "https://weibo.com/u/7103523530/home?topnav=1&wvr=6";
    private static final String SEND_URL = "https://www.weibo.com/aj/mblog/add?ajwvr=6&__rnd=";
    private static final String DELETE_URL = "https://www.weibo.com/aj/mblog/del?ajwvr=6";
    private static final String LOFIN_URL = "https://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.15)";

    /**
     * 添加黑名单
     */
    private static final String FEED_USER_URL = "https://weibo.com/aj/f/addblack?ajwvr=6";
    /**
     * 关注
     */
    private static final String FOLLOW_URL = "https://weibo.com/p/100505%s/follow?page=1";

    /**
     * 粉丝
     */
    private static final String FANS_URL = "https://weibo.com/p/100505%s/follow?relate=fans&page=1";


    private String[] USER_AGENT = new String[]{
            "Mozilla/4.0(compatible;MSIE7.0;WindowsNT5.1;Trident/4.0;SE2.XMetaSr1.0;SE2.XMetaSr1.0;.NETCLR2.0.50727;SE2.XMetaSr1.0)",
            "Mozilla/4.0(compatible;MSIE7.0;WindowsNT5.1;360SE)",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 SE 2.X MetaSr 1.0",
    };

    private int i = 0;


    private HttpHeaders httpHeaders;

    private RestTemplate restTemplate;

    private Long time = 0L;

    @PostConstruct
    public void init() {
        httpHeaders = new HttpHeaders();
        httpHeaders.add("origin", "https://www.weibo.com");
        httpHeaders.add("Referer", referer);
        httpHeaders.add("User-Agent", getUserAgent());
        httpHeaders.add("X-Requested-With", "XMLHttpRequest");
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);


        try {
            login();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                ResponseEntity<String> responseEntity = new RestTemplate().exchange(HOME, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
                logger.debug("home page :{}", responseEntity.getBody());
                time = time <= 0 ? 0L : (time - 600L);
            }
        }, 5, 1, TimeUnit.MINUTES);


    }


    /**
     * 先用邮箱、密码登录，根据返回的URL再去拿cookie，这个URL是一次性的
     */
    private void login() throws UnsupportedEncodingException, URISyntaxException {
        if (time > 600L) {
            return;
        }
        time = time + 600L;
        String formData = String.format(
                "entry=sso&gateway=1&from=null&savestate=30&useticket=0&pagerefer=&vsnf=1&su=%s&service=sso&sp=%s&sr=1280*800&encoding=UTF-8&cdult=3&domain=sina.com.cn&prelt=0&returntype=TEXT",
                URLEncoder.encode(Base64.encodeBase64String(username.replace("@", "%40").getBytes()), "UTF-8"), password);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Referer", "http://login.sina.com.cn/signup/signin.php?entry=sso");
        headers.add("User-Agent", getUserAgent());
        restTemplate = new RestTemplate();
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
        List<String> list = responseHeaders.get("Set-Cookie");
        list.forEach(cookie -> {
            logger.info("{}", cookie);
            cookies.append(cookie);
            cookies.append(cookie.split(";")[0]).append(";");
        });
        httpHeaders.add("Cookie", cookies.toString().substring(0, cookies.length() - 1));
    }


    public void sendWeiBoMessage(HttpMessage httpMessage) {
        if ("delete".equals(httpMessage.getOption()) || "update".equals(httpMessage.getOption())) {
            deleteWeiBo(messageMap.get(httpMessage.getId()));
            if ("delete".equals(httpMessage.getOption())) {
                return;
            }
        }
        //发微博去重
        checkMessage(httpMessage);

        HttpHeaders headers = httpHeaders;
        headers.add(HttpHeaders.USER_AGENT, getUserAgent());
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
            try {
                login();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
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

    private final String checkContent = "微信";

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


    private String getUserAgent() {
        int index = i % USER_AGENT.length;
        i++;
        return USER_AGENT[index];
    }

    /**
     * 添加屏蔽用户
     *
     * @param userId
     */
    public void addBlackUser(String userId) {
        HttpHeaders headers = httpHeaders;
        headers.add(HttpHeaders.USER_AGENT, getUserAgent());
        String formData = "uid=%s&f=1";
        formData = String.format(formData, userId);

        ResponseEntity<String> responseEntity = new RestTemplate().exchange(FEED_USER_URL, HttpMethod.POST,
                new HttpEntity<>(formData, headers), String.class);
        logger.info("add blackUser:{} response:{}", userId, responseEntity.getBody());
    }

    /**
     * 关注
     *
     * @param userId
     * @return
     */
    public List<String> follow(String userId) {
        HttpHeaders headers = httpHeaders;
        headers.add(HttpHeaders.USER_AGENT, getUserAgent());
        ResponseEntity<String> responseEntity = new RestTemplate().exchange(String.format(FOLLOW_URL, userId), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        logger.info("follow {}", responseEntity.getBody());
        return null;
    }

    /**
     * 粉丝
     *
     * @param userId
     * @return
     */
    public List<String> fans(String userId) {
        HttpHeaders headers = httpHeaders;
        headers.add(HttpHeaders.USER_AGENT, getUserAgent());
        ResponseEntity<String> responseEntity = new RestTemplate().exchange(String.format(FANS_URL, userId), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return null;
        }

        Document document = Jsoup.parse(responseEntity.getBody());
        logger.debug("document:{}", document);
        List<Node> nodeList = document.childNode(1).childNode(2).childNodes();
        if (nodeList.size() == 42 && nodeList.get(40).outerHtml().contains("followTab")) {
            Node node = nodeList.get(40);
            logger.info("{}", node);
            String nodeString = node.toString();
            nodeString = nodeString.substring(nodeString.indexOf("<div"), nodeString.lastIndexOf("/div>") + 5);
            logger.info("div:{}", nodeString);

            nodeString = nodeString.replaceAll("\\\\r\\\\n", "");

            nodeString = nodeString.replaceAll("\\\\t", "");

            nodeString = nodeString.replaceAll("\\\\", "");
            logger.debug(" :{}", nodeString);


            Document div = Jsoup.parse(nodeString);
            Elements elements = div.getElementsByClass("follow_item S_line2");
            for (Element e: elements){
                logger.info("e:{}" , e);
            }

        }
        return null;
    }

}
