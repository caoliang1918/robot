package com.zhongweixian.login;

import com.zhongweixian.cache.CacheService;
import com.zhongweixian.domain.WxUserCache;
import com.zhongweixian.domain.request.component.BaseRequest;
import com.zhongweixian.domain.response.ContactResponse;
import com.zhongweixian.domain.response.InitResponse;
import com.zhongweixian.domain.response.LoginResult;
import com.zhongweixian.domain.response.StatusNotifyResponse;
import com.zhongweixian.domain.shared.Contact;
import com.zhongweixian.domain.shared.Token;
import com.zhongweixian.enums.LoginCode;
import com.zhongweixian.enums.RetCode;
import com.zhongweixian.enums.StatusNotifyCode;
import com.zhongweixian.exception.RobotException;
import com.zhongweixian.service.WxHttpService;
import com.zhongweixian.service.WxMessageHandler;
import com.zhongweixian.utils.QRCodeUtils;
import com.zhongweixian.utils.WechatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 每个登录用一个线程来维护
 */
public class WxIMThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(WxIMThread.class);

    private CacheService cacheService;
    private WxHttpService wxHttpService;
    private WxMessageHandler wxMessageHandler;


    public WxIMThread(CacheService cacheService, WxHttpService wxHttpService, WxMessageHandler wxMessageHandler) {
        this.cacheService = cacheService;
        this.wxHttpService = wxHttpService;
        this.wxMessageHandler = wxMessageHandler;
    }


    private String qrUrl;

    public String showQrcode() {
        int a = 0;
        while (qrUrl == null && a < 100) {
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            a++;
        }
        return qrUrl;
    }

    public void login() throws Exception {
        /**
         *
         */
        wxHttpService.start();
        logger.info("[0] entry completed");
        /**
         * 获取uuid
         */
        String uuid = wxHttpService.getUUID();
        logger.info("uuid completed: uuid{}", uuid);
        //2 qr
        byte[] qrData = wxHttpService.getQR(uuid);
        ByteArrayInputStream stream = new ByteArrayInputStream(qrData);
        qrUrl = QRCodeUtils.decode(stream);
        stream.close();
        String qr = QRCodeUtils.generateQR(qrUrl, 40, 40);
        logger.info("\r\n" + qr);
        logger.info("[2] qrcode completed");

        //4 login
        LoginResult loginResult;

        WxUserCache userCache = new WxUserCache();
        while (true) {
            loginResult = wxHttpService.login(uuid);
            logger.info("loginResult:{}", loginResult.toString());
            if (LoginCode.SUCCESS.getCode().equals(loginResult.getCode())) {
                if (loginResult.getHostUrl() == null) {
                    throw new RobotException("hostUrl can't be found");
                }
                if (loginResult.getRedirectUrl() == null) {
                    throw new RobotException("redirectUrl can't be found");
                }
                cacheService.setHostUrl(loginResult.getHostUrl());
                break;
            } else if (LoginCode.AWAIT_CONFIRMATION.getCode().equals(loginResult.getCode())) {
                logger.info("[*] login status = AWAIT_CONFIRMATION");
            } else if (LoginCode.AWAIT_SCANNING.getCode().equals(loginResult.getCode())) {
                logger.info("[*] login status = AWAIT_SCANNING");
            } else if (LoginCode.EXPIRED.getCode().equals(loginResult.getCode())) {
                logger.error("[*] login status = EXPIRED");
            } else {
                logger.info("[*] login status = " + loginResult.getCode());
            }
        }

        logger.info("[4] login completed");
        //5 redirect login
        Token token = wxHttpService.openNewloginpage(loginResult.getRedirectUrl(), userCache);
        if (token.getRet() == 0) {
            userCache.setUuid(uuid);
            userCache.setUin(token.getWxuin());
            userCache.setSid(token.getWxsid());
            userCache.setsKey(token.getSkey());
            userCache.setPassTicket(token.getPass_ticket());

            BaseRequest baseRequest = new BaseRequest();
            baseRequest.setUin(token.getWxuin());
            baseRequest.setSid(token.getWxsid());
            baseRequest.setSkey(token.getSkey());
            userCache.setToken(token);
            userCache.setBaseRequest(baseRequest);
            userCache.setWxHost(loginResult.getHostUrl());
            userCache.setReferer(loginResult.getHostUrl());
            userCache.setOrigin(loginResult.getHostUrl());
        } else {
            throw new RobotException("token ret = " + token.getRet());
        }

        wxHttpService.statReport(userCache);

        logger.info("[5] redirect login completed , wxUin:{}", token.getWxuin());
        //6 redirect
        wxHttpService.redirect(loginResult.getHostUrl(), userCache);

        logger.info("[6] redirect completed");
        //7 init
        InitResponse initResponse = wxHttpService.init(loginResult.getHostUrl(), userCache);
        WechatUtils.checkBaseResponse(initResponse);
        userCache.setSyncKey(initResponse.getSyncKey());
        userCache.setOwner(initResponse.getUser());
        logger.info("[7] init completed");
        //8 status notify
        StatusNotifyResponse statusNotifyResponse =
                wxHttpService.statusNotify(loginResult.getHostUrl(),
                        userCache.getBaseRequest(),
                        initResponse.getUser().getUserName(), StatusNotifyCode.INITED.getCode());
        WechatUtils.checkBaseResponse(statusNotifyResponse);
        //9 get contact
        long seq = 0;
        //好友
        List<Contact> chatRooms = new ArrayList<>();
        //群组(这里包含已经保存的技能组和最近聊天的技能组)
        do {
            ContactResponse contactResponse = wxHttpService.getContact(userCache);
            WechatUtils.checkBaseResponse(contactResponse);
            logger.info("[*] getContactResponse seq = " + contactResponse.getSeq());
            logger.info("[*] getContactResponse memberCount = " + contactResponse.getMemberCount());
            AtomicInteger count = new AtomicInteger(1);
            contactResponse.getMemberList().forEach(contact -> {
                if (WechatUtils.isChatRoom(contact.getUserName())) {
                    userCache.getChatRoomMembers().put(contact.getUserName(), contact);
                    logger.info("chatRoom name :{} ,memberSize:{} ,  id :{} ", contact.getNickName(), contact.getMemberList().size(), contact.getUserName());
                    chatRooms.add(contact);

                    contact.getMemberList().forEach(chatRoomMember -> {
                        contact.getMemberMap().put(chatRoomMember.getUserName(), chatRoomMember);
                    });

                } else {
                    logger.info("{} nickName:{} , remarkName:{} , userName:{}", count.get(), contact.getNickName(), contact.getRemarkName(), contact.getUserName());
                    count.decrementAndGet();
                    userCache.getChatContants().put(contact.getUserName(), contact);
                }
            });
            seq = contactResponse.getSeq();

            //cacheService.getIndividuals().addAll(contactResponse.getMemberList().stream().filter(WechatUtils::isIndividual).collect(Collectors.toSet()));
            //cacheService.getMediaPlatforms().addAll(contactResponse.getMemberList().stream().filter(WechatUtils::isMediaPlatform).collect(Collectors.toSet()));
        } while (seq > 0);


        initResponse.getContactList().stream()
                .filter(x -> WechatUtils.isChatRoom(x.getUserName())).forEach(x -> {
            userCache.getChatRoomMembers().put(x.getUserName(), x);
            logger.info("chatRoom name :{} ,memberSize:{} ,  id :{} ", x.getNickName(), x.getMemberList().size(), x.getUserName());
            chatRooms.add(x);
        });


        logger.info("chatRoomDescriptions size : {}", chatRooms.size());

        userCache.setAlive(true);
        cacheService.cacheUser(userCache);

        WxSyncMessage wxSyncMessage = new WxSyncMessage(userCache, wxHttpService, wxMessageHandler);
        while (true) {
            try {
                if (wxSyncMessage.listen() != RetCode.NORMAL.getCode()) {
                    logger.warn("logout user:{}", userCache.getUin());
                    userCache.setAlive(false);
                    break;
                }
            } catch (Exception e) {
                logger.error("synccheck error:{}", e);
            }
        }
    }

    @Override
    public void run() {
        try {
            login();
        } catch (Exception e) {
            run();
            logger.error("{}", e);
        }
    }
}