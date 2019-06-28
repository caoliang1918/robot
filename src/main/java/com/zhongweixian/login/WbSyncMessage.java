package com.zhongweixian.login;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zhongweixian.domain.weibo.WeiBoUser;
import com.zhongweixian.service.WbService;
import com.zhongweixian.service.WeiBoHttpService;
import com.zhongweixian.web.entity.BotVideo;
import com.zhongweixian.web.entity.enums.Channel;
import com.zhongweixian.web.service.BotVideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;

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

    @Autowired
    private ScheduledExecutorService wbExecutor;

    @Autowired
    private WeiBoHttpService weiBoHttpService;
    ;

    @Autowired
    private BotVideoService botVideoService;


    @PostConstruct
    public void init() throws URISyntaxException, MalformedURLException {
        logger.info("PostConstruct {} , {}", wbService, wbExecutor);

        if (!wbService.login()) {
            return;
        }

        String uid = wbService.getUid();
        String cookie = wbService.getCookie();

        WeiBoUser weiBoUser = new WeiBoUser();
        weiBoUser.setId(Long.parseLong(uid));
        weiBoUser.setCookie(cookie);

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
        weiBoUser.setCookie("_s_tentry=login.sina.com.cn; UOR=login.sina.com.cn,weibo.com,login.sina.com.cn; Apache=9456802003620.64.1561629285945; SINAGLOBAL=9456802003620.64.1561629285945; ULV=1561629286071:1:1:1:9456802003620.64.1561629285945:; BAYEUX_BROWSER=f80f-1qs8ysmrs5nhojxehriqn1czz; login_sid_t=7ef39c96c39b4c611ff3ab52ab13f681; cross_origin_proto=SSL; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whu6d1rS1RzpY.o0E_MyWMw5JpX5K2hUgL.FoMp1KBRShq0e0.2dJLoIf2LxKqL122LBKBLxK.LB.-L1K.LxK-LBo2LBo2LxK-LB.2L1hBLxK-LBKBLBK.LxK-LB-BL1KMLxKMLB-eLB-eLxKBLBonL1h5LxK-L12qLBoMt; ALF=1593165375; SSOLoginState=1561629375; SCF=Aqqh9eM0QX_5TJHvIT1Bp_DVlqjNkTXae_JopKR-sMiuT-7t3hhhu1-_txV34Ed3yGyBBe3pYFNaOydNZdKpP1M.; SUB=_2A25wEOKQDeRhGeFP4lYZ9CjPyDWIHXVTZFNYrDV8PUNbmtBeLUzykW9NQO_rykPBZjlLxPMsdDujy8O6LC3bU0g7; SUHB=0vJYo4Mu_LBsWG; un=1923531384@qq.com; wvr=6; webim_unReadCount=%7B%22time%22%3A1561629481545%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A5452%2C%22allcountNum%22%3A5452%2C%22msgbox%22%3A0%7D");
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
        wbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        ResponseEntity<String> connectEntity = weiBoHttpService.connect(weiBoUser);
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
                            BotVideo video = new BotVideo();
                            video.setChannel(Channel.WB.name());
                            video.setCts(new Date());
                            video.setChatName(info.getString("group_name"));
                            video.setChatType(data.getString("type"));
                            video.setFromUid(info.getString("from_uid"));
                            JSONObject fromUser = info.getJSONObject("from_user");
                            video.setFromUser(fromUser.getString("screen_name"));
                            video.setStatus(1);
                            logger.info("group_name:{} , from_user:{} , content:{}", video.getChatName(), video.getFromUser(), info.getString("content"));
                            if (!info.containsKey("media_type") || !info.getInteger("media_type").equals(MEDIA_TYPE)) {
                                continue;
                            }
                            JSONArray fids = info.getJSONArray("fids");
                            if (fids == null || fids.size() == 0) {
                                continue;
                            }
                            logger.info("fids:{}", fids);
                            Long videoId = Long.parseLong(fids.get(0).toString());
                            video.setVideoUrl(String.format(VIDEO_URL, videoId));
                           // botVideoService.add(video);
                        }
                    } catch (Exception e) {
                        logger.error("{}", e.getMessage());
                    }
                }
            }
        });
    }
}
