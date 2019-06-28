package com.zhongweixian.service;

import com.zhongweixian.domain.weibo.WeiBoUser;
import com.zhongweixian.utils.MessageUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caoliang on 2019-06-25
 */
@Component
public class WbBlockUser {
    private Logger logger = LoggerFactory.getLogger(WbBlockUser.class);


    private HttpHeaders headers;

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

    /**
     * 循环线程池
     */
    private ScheduledExecutorService taskExecutor = new ScheduledThreadPoolExecutor(100, new BasicThreadFactory.Builder().namingPattern("weibo-block-pool--%d").daemon(true).build());

    private Queue<Long> blackUserIds = new LinkedBlockingDeque();
    private Queue<Long> fans = new LinkedBlockingDeque();

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


    public void startTask() {
        if (headers == null) {
            return;
        }
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
     * 关注的用户
     *
     * @param userId
     * @return
     */
    public List<WeiBoUser> follow(Long userId, Integer page) {
        if (userId == null || page > 5) {
            return null;
        }

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

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }
}
