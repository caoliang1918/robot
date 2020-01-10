package com.zhongweixian.login;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.zhongweixian.domain.weibo.WeiBoUser;
import com.zhongweixian.service.OssService;
import com.zhongweixian.service.WbService;
import com.zhongweixian.service.WeiBoHttpService;
import com.zhongweixian.web.entity.BotVideo;
import com.zhongweixian.web.entity.enums.Channel;
import com.zhongweixian.web.service.BotVideoService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 微博IM同步消息类
 */
@Component
public class WbSyncMessage {
    private Logger logger = LoggerFactory.getLogger(WbSyncMessage.class);


    private final static String VIDEO_URL = "https://upload.api.weibo.com/2/mss/msget?source=209678993&fid=%s";
    private final static Integer MEDIA_TYPE = 10;

    @Autowired
    private WbService wbService;

    private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(100,
            new BasicThreadFactory.Builder().namingPattern("video-pool-%d").daemon(true).build());


    @Autowired
    private WeiBoHttpService weiBoHttpService;


    @Autowired
    private BotVideoService botVideoService;

    @Autowired
    private OssService ossService;

    String JD_HOST = "https://wb-video.s3.cn-south-1.jdcloud-oss.com/";
    String CONTENT_TYPE = "application/octet-stream";

    private WeiBoUser weiBoUser;

    @Value("${weibo.username}")
    private String username;

    @Value("${weibo.password}")
    private String password;


    //@PostConstruct
    public void init() {
        weiBoUser = wbService.login(username , password);

        if (weiBoUser == null){
            return;
        }


        /**
         *
         */
        ResponseEntity openChatEntity = weiBoHttpService.openChat(weiBoUser);
        if (openChatEntity.getStatusCode() != HttpStatus.OK) {
            return;
        }
        /**
         * 获取qrcode  这一步可能用不上
         * https://login.sina.com.cn/sso/qrcode/image?entry=weibo&size=180&source=209678993&callback=__jp0
         */
        weiBoHttpService.getQrcode();

        /**
         * 获取备注好友列表
         * GET  https://api.weibo.com/webim/query_remark.json?source=209678993&t=1561447841245
         */
        ResponseEntity remarkEntity = weiBoHttpService.getRemark(weiBoUser);

        /**
         * 获取消息列表
         * GET https://api.weibo.com/webim/2/direct_messages/contacts.json?add_virtual_user=3&is_include_group=0&need_back=0,0&count=30&source=209678993&t=1561451504153
         *
         */
        ResponseEntity contentEntity = weiBoHttpService.getContent(weiBoUser);


        /**
         * 获取IM channel
         * https://api.weibo.com/webim/webim_nas.json?source=209678993&returntype=json&v=1.1&callback=__jp1
         * response : try{__jp1({"server":"https://web.im.weibo.com/","channel":"/im/7194846339"});}catch(e){}
         */
        ResponseEntity channelEntity = weiBoHttpService.getChannel(weiBoUser);


        /**
         * handshake 握手
         * https://web.im.weibo.com/im/handshake?jsonp=jQuery112406869571085748343_1561447840951&message=
         * [{"version":"1.0","minimumVersion":"1.0","channel":"/meta/handshake","supportedConnectionTypes":["callback-polling"],"advice":{"timeout":60000,"interval":0},"id":"2"}]&_=1561447840952
         */
        JSONObject jsonObject = weiBoHttpService.handshake(weiBoUser);
        if (jsonObject == null || !jsonObject.containsKey("clientId")) {
            logger.warn("clientId is null");
            return;
        }
        weiBoUser.setClientId(jsonObject.getString("clientId"));


        /**
         * IM 订阅消息
         * https://web.im.weibo.com/im/?jsonp=jQuery112406869571085748343_1561447840951&message=
         * [{"channel":"/meta/subscribe","subscription":"/im/2672447121","id":"3","clientId":"1jl3zr1vvsca5hspfp6ww6vxgc6ne2s"}]&_=1561447840953
         */
        ResponseEntity<String> subscribeEntity = weiBoHttpService.subscribe(weiBoUser);
        /**
         * 发送report
         * POST https://api.weibo.com/webim/report.json
         */

        /**
         * 轮训新消息
         * https://web.im.weibo.com/im/connect?jsonp=jQuery112405320978791403412_1561451503942&message=
         * [{"channel":"/meta/connect","connectionType":"callback-polling","advice":{"timeout":0},"id":"4","clientId":"1jlveqbjc1xat687b71kh97b7s7ssjl"}]&_=1561451503943
         */


        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                ResponseEntity<String> connectEntity = weiBoHttpService.connect(weiBoUser);
                if (connectEntity == null) {
                    return;
                }
                logger.debug("status:{} , connectEntity:{}", connectEntity.getStatusCode(), connectEntity.getBody());
                String body = connectEntity.getBody();
                JSONArray jsonArray = JSONObject.parseArray(body.substring(body.indexOf("([") + +1, body.indexOf("])") + 1));
                for (Object object : jsonArray) {
                    JSONObject json = JSONObject.parseObject(object.toString());
                    JSONObject data = json.getJSONObject("data");
                    if (data == null) {
                        continue;
                    }
                    JSONObject info = data.getJSONObject("info");
                    if (info == null || info.size() < 8) {
                        continue;
                    }
                    downloadVideo(weiBoUser, info);
                }
            } catch (Exception e) {
                logger.error("{}", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void setCookie(String cookie) {
        weiBoUser.setCookie(cookie);
    }

    private void downloadVideo(WeiBoUser weiBoUser, JSONObject info) {
        BotVideo video = new BotVideo();
        video.setChannel(Channel.WB.name());
        video.setCts(new Date());
        video.setChatName(info.getString("group_name"));
        video.setChatType("groupchat");
        video.setStatus(1);
        video.setFromUid(info.getString("from_uid"));
        JSONObject fromUser = info.getJSONObject("from_user");
        if (fromUser == null && "322".equals(info.getString("type"))) {
            logger.warn("{}   :{} ", info.getString("content"), info.getString("group_name"));
            return;
        }
        video.setFromUser(fromUser.getString("screen_name"));
        logger.info("group_name:{} , from_user:{} , content:{}", video.getChatName(), video.getFromUser(), info.getString("content"));
        if (!info.containsKey("media_type") || !info.getInteger("media_type").equals(MEDIA_TYPE)) {
            return;
        }
        JSONArray fids = info.getJSONArray("fids");
        if (fids == null || fids.size() == 0) {
            return;
        }
        logger.info("fids:{}", fids);
        String videoId = fids.get(0).toString();
        video.setFromUrl(String.format(VIDEO_URL, videoId));

        scheduledExecutorService.execute(() -> {
            try {
                ResponseEntity<byte[]> fileEntity = weiBoHttpService.download(video.getFromUrl(), weiBoUser.getCookie());
                if (fileEntity == null || fileEntity.getStatusCode() != HttpStatus.OK) {
                    return;
                }

                String hashCode = DigestUtils.md5Hex(fileEntity.getBody());
                Integer size = fileEntity.getBody().length;
                logger.info("groupName:{}, fromUser:{}, size:{} KB, hashCode:{}, video:{}", video.getChatName(), video.getFromUser(), size / 1024, hashCode, video.getFromUrl());
                BotVideo exist = botVideoService.findByHashCode(size, hashCode);
                if (exist != null) {
                    logger.warn("file is exist:{}", JSONObject.toJSONString(exist));
                    return;
                }
                logger.info("new video:{}", videoId);

                /**
                 * 上传云服务器
                 */
                InputStream inputStream = new ByteArrayInputStream(fileEntity.getBody());
                String bucket = "weibo-video/" + DateUtils.formatDate(new Date(), "yyyy-MM") + "/" + DateUtils.formatDate(new Date(), "yyyy-MM-dd");
                String videoId_ = videoId + ".mp4";
                PutObjectResult result = ossService.uploadJdcloud(inputStream, size, CONTENT_TYPE, bucket, videoId_);
                if (result == null) {
                    logger.warn("upload oss error");
                    return;
                }
                video.setVideoCloud("jdcloud");
                video.setVideoUrl(JD_HOST + bucket + "/" + videoId_);
                video.setHashCode(hashCode);
                video.setVideoSize(size);
                video.setStatus(2);
            } catch (Exception e) {
                logger.error("{}", e);
            }
            botVideoService.add(video);
        });
    }
}