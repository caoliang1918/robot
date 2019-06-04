package com.zhongweixian.service;

import com.alibaba.fastjson.JSONObject;
import com.zhongweixian.domain.HttpMessage;
import com.zhongweixian.domain.request.RevokeRequst;
import com.zhongweixian.domain.weibo.WeiBoUser;
import com.zhongweixian.exception.RobotException;
import com.zhongweixian.utils.Levenshtein;
import com.zhongweixian.utils.MessageUtils;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
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

    private ScheduledExecutorService taskExecutor = new ScheduledThreadPoolExecutor(500, new BasicThreadFactory.Builder().namingPattern("weibo-schedule-pool--%d").daemon(true).build());

    private Queue<Long> blackUserIds = new LinkedBlockingDeque();
    private Queue<Long> fans = new LinkedBlockingDeque();

    /**
     * 获取首页,同时限制登录频次 {} 分钟
     */
    private static final Integer HONE_RATE = 10;
    /**
     * 粉丝队列长度限制
     */
    private static final Integer FANS_LIMIT = 500;
    /**
     * 获取僵尸用户的粉丝频率 {} 分钟
     */
    private static final Integer FANS_RATE = 2;
    /**
     * 拉黑僵尸用户频率 {} 秒
     */
    private static final Integer BLACK_USER_RATE = 5;


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
    private static final String FOLLOW_URL = "https://weibo.com/p/100505%s/follow?page=%s";

    /**
     * 粉丝
     */
    private static final String FANS_URL = "https://weibo.com/p/100505%s/follow?relate=fans&page=%s";


    private String[] USER_AGENT = new String[]{
            "Mozilla/4.0(compatible;MSIE7.0;WindowsNT5.1;Trident/4.0;SE2.XMetaSr1.0;SE2.XMetaSr1.0;.NETCLR2.0.50727;SE2.XMetaSr1.0)",
            "Mozilla/4.0(compatible;MSIE7.0;WindowsNT5.1;360SE)",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 SE 2.X MetaSr 1.0",
    };

    private int i = 0;


    private HttpHeaders httpHeaders;

    private RestTemplate restTemplate;

    private boolean isLogin = false;
    private boolean nextLogin = true;
    private Integer loginTime = 0;

    @PostConstruct
    public void init() {
        isLogin = login();

        /**
         * 定时任务1:打开我的主页
         */
        taskExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isLogin) {
                        return;
                    }
                    ResponseEntity<String> responseEntity = new RestTemplate().exchange(HOME, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
                    logger.info("get weibo base home ,status:{}", responseEntity.getStatusCode());
                } catch (Exception e) {
                    logger.error("{}", e);
                }
                nextLogin = true;
            }
        }, 5, HONE_RATE, TimeUnit.MINUTES);


        /**
         * 定时任务2:获取黑粉的粉丝
         */
        taskExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Long userId = fans.poll();
                    if (userId == null) {
                        return;
                    }
                    List<WeiBoUser> weiBoUserList = fans(userId, 1);
                    if (CollectionUtils.isEmpty(weiBoUserList)) {
                        return;
                    }
                    for (WeiBoUser weiBoUser : weiBoUserList) {
                        blackUserIds.add(weiBoUser.getId());
                    }
                } catch (Exception e) {
                    logger.error("{}", e);
                }
            }
        }, 5, FANS_RATE, TimeUnit.MINUTES);

        /**
         * 定时任务3:拉黑队列中的僵尸用户
         */
        taskExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Long userId = blackUserIds.poll();
                if (userId == null) {
                    return;
                }
                black(userId);
            }
        }, 5, BLACK_USER_RATE, TimeUnit.SECONDS);
    }


    /**
     * 先用邮箱、密码登录，根据返回的URL再去拿cookie，这个URL是一次性的
     */
    private boolean login() {
        if (!nextLogin) {
            logger.warn("登录次数过多 , loginTime:{}", loginTime);
            return false;
        }
        nextLogin = false;
        loginTime++;
        String formData = null;
        try {
            formData = String.format(
                    "entry=sso&gateway=1&from=null&savestate=30&useticket=0&pagerefer=&vsnf=1&su=%s&service=sso&sp=%s&sr=1280*800&encoding=UTF-8&cdult=3&domain=sina.com.cn&prelt=0&returntype=TEXT",
                    URLEncoder.encode(Base64.encodeBase64String(username.replace("@", "%40").getBytes()), "UTF-8"), password);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        httpHeaders = new HttpHeaders();
        httpHeaders.add("origin", "https://www.weibo.com");
        httpHeaders.add("Referer", referer);
        httpHeaders.add("User-Agent", getUserAgent());
        httpHeaders.add("X-Requested-With", "XMLHttpRequest");
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Referer", "http://login.sina.com.cn/signup/signin.php?entry=sso");
        headers.add("User-Agent", getUserAgent());
        restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(LOFIN_URL, HttpMethod.POST, new HttpEntity<>(formData, httpHeaders), String.class);
        } catch (Exception e) {
            logger.error("{}", e);
        }
        logger.info("login responseEntity :{}", responseEntity.getBody());
        String text = responseEntity.getBody();
        String token = null;
        try {
            token = text.substring(text.indexOf("https:"), text.indexOf(",\"https:") - 1).replace("\\", "");
        } catch (Exception e) {
            logger.error("{}", e);
        }
        if (token == null) {
            return false;
        }
        //https://passport.weibo.com/wbsso/login?ticket=ST-NzEwMzUyMzUzMA%3D%3D-1556522528-gz-C427A34DD45B0991800DA3F6DC59EB1F-1&ssosavestate=1588058528
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
        httpHeaders.add("Cookie", cookies.toString().substring(0, cookies.length() - 1));
        return true;
    }


    /**
     * 发送微博
     *
     * @param httpMessage
     * @throws RobotException
     */
    public void sendWeiBoMessage(HttpMessage httpMessage) throws RobotException {
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

        HttpHeaders headers = httpHeaders;
        headers.add(HttpHeaders.USER_AGENT, getUserAgent());
        String formData = null;
        ResponseEntity<String> responseEntity = null;
        try {
            formData = "location=v6_content_home&text=" + URLEncoder.encode(httpMessage.getContent(), "UTF-8") + "&appkey=&style_type=1&pic_id=&tid=&pdetail=&mid=&isReEdit=false&rank=0&rankid=&module=stissue&pub_source=main_&pub_type=dialog&isPri=0&_t=0";
            responseEntity = new RestTemplate().exchange(SEND_URL + System.currentTimeMillis(), HttpMethod.POST,
                    new HttpEntity<>(formData, headers), String.class);
        } catch (Exception e) {
            logger.error("{}", e);
            login();
            return;
        }
        if (responseEntity.getStatusCode() == HttpStatus.FOUND) {
            logger.error("client not login : {}", responseEntity);
            login();
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

    /**
     * 删除微博
     *
     * @param revokeRequst
     */
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

    /**
     * 轮训获取userAgent
     *
     * @return
     */
    private String getUserAgent() {
        int index = i % USER_AGENT.length;
        i++;
        return USER_AGENT[index];
    }


    /**
     * 关注的用户
     *
     * @param userId
     * @return
     */
    public List<WeiBoUser> follow(Long userId, Integer page) {
        if (userId == null || page > 5) {
            return null;
        }
        HttpHeaders headers = httpHeaders;
        headers.add(HttpHeaders.USER_AGENT, getUserAgent());
        ResponseEntity<String> responseEntity = new RestTemplate().exchange(String.format(FOLLOW_URL, userId, page), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        logger.info("follow {}", responseEntity.getStatusCode());
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return null;
        }
        List<WeiBoUser> pageList = getUserList(responseEntity.getBody());
        logger.info("get {} follow of page:{} , follow size:{}", userId, page, pageList.size());
        return pageList;
    }

    /**
     * 粉丝用户
     *
     * @param userId
     * @return
     */
    public List<WeiBoUser> fans(Long userId, Integer page) {
        if (userId == null || page > 5) {
            return null;
        }
        HttpHeaders headers = httpHeaders;
        headers.add(HttpHeaders.USER_AGENT, getUserAgent());
        try {
            ResponseEntity<String> responseEntity = new RestTemplate().exchange(String.format(FANS_URL, userId, page), HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                logger.warn("get fans error , statusCode:{}", responseEntity.getStatusCode());
                fans.add(userId);
                return null;
            }
            List<WeiBoUser> pageList = getUserList(responseEntity.getBody());
            logger.info("get {} fans of page:{} , fans size:{}", userId, page, pageList.size());
            return pageList;
        } catch (Exception e) {
            logger.error("{}", e);
        }
        return null;
    }

    /**
     * 用户列表(fans 和 follow公用这块代码)
     *
     * @param body
     * @return
     */
    private List<WeiBoUser> getUserList(String body) {
        List<WeiBoUser> userList = new ArrayList<>();

        Document document = Jsoup.parse(body);
        List<Node> nodeList = document.childNode(1).childNode(2).childNodes();
        if (nodeList.size() >= 40 && nodeList.get(nodeList.size() - 2).outerHtml().contains("followTab")) {
            Node node = nodeList.get(nodeList.size() - 2);
            String nodeString = node.toString();
            nodeString = nodeString.substring(nodeString.indexOf("<div"), nodeString.lastIndexOf("/div>") + 5);

            nodeString = nodeString.replaceAll("\\\\r\\\\n", "");

            nodeString = nodeString.replaceAll("\\\\t", "");

            nodeString = nodeString.replaceAll("\\\\", "");


            Document div = Jsoup.parse(nodeString);
            Elements elements = div.getElementsByClass("follow_item S_line2");
            if (elements == null || elements.size() == 0) {
                return userList;
            }
            WeiBoUser weiBoUser = null;
            try {
                for (Element e : elements) {
                    weiBoUser = parse(e);
                    if (weiBoUser != null) {
                        userList.add(weiBoUser);
                    }
                }
            } catch (Exception e) {
                logger.error("{}", e);
            }
        }
        return userList;
    }

    /**
     * HTML解析
     *
     * @param element
     * @return
     * @throws Exception
     */
    private WeiBoUser parse(Element element) throws Exception {
        WeiBoUser user = new WeiBoUser();
        user.setNikename(element.getElementsByClass("mod_pic").get(0).children().get(0).attr("title"));
        Elements elements = element.getElementsByClass("info_connect").get(0).children();
        //话题或者其他
        if (elements.size() == 0) {
            return null;
        }
        String userId = elements.get(0).getElementsByTag("a").attr("href");
        user.setId(Long.parseLong(userId.substring(1, userId.indexOf("/follow"))));
        String usercard = elements.get(2).getElementsByTag("a").attr("href");
        if (usercard.contains("u")) {
            user.setUsercard(usercard.substring(3, usercard.length()));
        } else {
            user.setUsercard(usercard.substring(1, usercard.length()));
        }
        user.setAddress(element.getElementsByClass("info_add").get(0).child(1).html());
        user.setFollow(Long.parseLong(elements.get(0).getElementsByTag("a").html()));
        user.setFans(Long.parseLong(elements.get(1).getElementsByTag("a").html()));
        user.setWeibo(Long.parseLong(elements.get(2).getElementsByTag("a").html()));

        /**
         * 粉丝大于1000或者发微博数大于300的用户，应该不是僵尸用户吧
         */
        if (user.getFans() > 1000 || user.getWeibo() > 300 || !user.getUsercard().equals(user.getId().toString())) {
            return null;
        }

        /**
         * 贵州人民爱玩僵尸，僵尸用户的昵称一般包含中文和字母
         */
        if ((user.getAddress().contains("贵州") || user.getWeibo() < 15L) && MessageUtils.checkLan(user.getNikename())) {
            logger.info("{}", user.toString());
            if (fans.size() < FANS_LIMIT) {
                fans.add(user.getId());
            }
            return user;
        }
        return null;
    }

    /**
     * 执行拉黑动作
     *
     * @param userId
     */
    private void black(Long userId) {
        if (userId == null) {
            return;
        }
        HttpHeaders headers = httpHeaders;
        headers.add(HttpHeaders.USER_AGENT, getUserAgent());
        String formData = "uid=%s&f=1";
        formData = String.format(formData, userId);

        try {
            ResponseEntity<String> responseEntity = new RestTemplate().exchange(FEED_USER_URL, HttpMethod.POST,
                    new HttpEntity<>(formData, headers), String.class);
            logger.info("add blackUser:{} response:{} , queue size:{}", userId, responseEntity.getBody(), blackUserIds.size());
            logger.debug("reponse cookie:{}", responseEntity.getHeaders().get("Set-Cookie"));
        } catch (Exception e) {
            logger.error("black user error:{}", e.getMessage());
            if (e.getMessage().equals("400 Bad Request")) {
                if (!login()) {
                    blackUserIds.add(userId);
                }
            }
        }
    }

    /**
     * 添加黑户
     *
     * @param id
     */
    public void addBlackUser(Long id) {
        blackUserIds.add(id);
    }
}
