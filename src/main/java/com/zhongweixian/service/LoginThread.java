package com.zhongweixian.service;

import com.zhongweixian.MessageHandlerImpl;
import com.zhongweixian.domain.BaseUserCache;
import com.zhongweixian.domain.request.component.BaseRequest;
import com.zhongweixian.domain.response.ContactResponse;
import com.zhongweixian.domain.response.InitResponse;
import com.zhongweixian.domain.response.LoginResult;
import com.zhongweixian.domain.response.StatusNotifyResponse;
import com.zhongweixian.domain.shared.ChatRoomDescription;
import com.zhongweixian.domain.shared.Contact;
import com.zhongweixian.domain.shared.Token;
import com.zhongweixian.enums.LoginCode;
import com.zhongweixian.enums.RetCode;
import com.zhongweixian.enums.StatusNotifyCode;
import com.zhongweixian.exception.WechatException;
import com.zhongweixian.exception.WechatQRExpiredException;
import com.zhongweixian.utils.QRCodeUtils;
import com.zhongweixian.utils.WechatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 每个登录用一个线程来维护
 */
public class LoginThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LoginThread.class);

    private CacheService cacheService;
    private WechatHttpService wechatHttpService;
    private WechatMessageService wechatMessageService;

    public LoginThread(CacheService cacheService, WechatHttpService wechatHttpService, WechatMessageService wechatMessageService) {
        this.cacheService = cacheService;
        this.wechatHttpService = wechatHttpService;
        this.wechatMessageService = wechatMessageService;
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
        wechatHttpService.start();
        logger.info("[0] entry completed");
        /**
         * 获取uuid
         */
        String uuid = wechatHttpService.getUUID();
        logger.info("uuid completed: uuid{}", uuid);
        //2 qr
        byte[] qrData = wechatHttpService.getQR(uuid);
        ByteArrayInputStream stream = new ByteArrayInputStream(qrData);
        qrUrl = QRCodeUtils.decode(stream);
        stream.close();
        String qr = QRCodeUtils.generateQR(qrUrl, 40, 40);
        logger.info("\r\n" + qr);
        logger.info("[2] qrcode completed");

        //4 login
        LoginResult loginResult;

        BaseUserCache userCache = new BaseUserCache();
        while (true) {
            loginResult = wechatHttpService.login(uuid);
            logger.info("loginResult:{}", loginResult.toString());
            if (LoginCode.SUCCESS.getCode().equals(loginResult.getCode())) {
                if (loginResult.getHostUrl() == null) {
                    throw new WechatException("hostUrl can't be found");
                }
                if (loginResult.getRedirectUrl() == null) {
                    throw new WechatException("redirectUrl can't be found");
                }
                cacheService.setHostUrl(loginResult.getHostUrl());
                break;
            } else if (LoginCode.AWAIT_CONFIRMATION.getCode().equals(loginResult.getCode())) {
                logger.info("[*] login status = AWAIT_CONFIRMATION");
            } else if (LoginCode.AWAIT_SCANNING.getCode().equals(loginResult.getCode())) {
                logger.info("[*] login status = AWAIT_SCANNING");
            } else if (LoginCode.EXPIRED.getCode().equals(loginResult.getCode())) {
                logger.info("[*] login status = EXPIRED");
                throw new WechatQRExpiredException();
            } else {
                logger.info("[*] login status = " + loginResult.getCode());
            }
        }

        logger.info("[4] login completed");
        //5 redirect login
        Token token = wechatHttpService.openNewloginpage(loginResult.getRedirectUrl());
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
            throw new WechatException("token ret = " + token.getRet());
        }

        wechatHttpService.statReport(userCache);

        logger.info("[5] redirect login completed , wxUin:{}", token.getWxuin());
        //6 redirect
        wechatHttpService.redirect(loginResult.getHostUrl());

        logger.info("[6] redirect completed");
        //7 init
        InitResponse initResponse = wechatHttpService.init(loginResult.getHostUrl(), userCache.getBaseRequest());
        WechatUtils.checkBaseResponse(initResponse);
        userCache.setSyncKey(initResponse.getSyncKey());
        userCache.setOwner(initResponse.getUser());
        logger.info("[7] init completed");
        //8 status notify
        StatusNotifyResponse statusNotifyResponse =
                wechatHttpService.statusNotify(loginResult.getHostUrl(),
                        userCache.getBaseRequest(),
                        initResponse.getUser().getUserName(), StatusNotifyCode.INITED.getCode());
        WechatUtils.checkBaseResponse(statusNotifyResponse);
        //9 get contact
        long seq = 0;
        //好友
        List<ChatRoomDescription> chatRooms = new ArrayList<>();
        //群组(这里包含已经保存的技能组和最近聊天的技能组)
        do {
            ContactResponse contactResponse = wechatHttpService.getContact(loginResult.getHostUrl(), userCache.getBaseRequest().getSkey(), seq);
            WechatUtils.checkBaseResponse(contactResponse);
            for (Contact contact : contactResponse.getMemberList()) {


            }
            logger.info("[*] getContactResponse seq = " + contactResponse.getSeq());
            logger.info("[*] getContactResponse memberCount = " + contactResponse.getMemberCount());
            AtomicInteger count = new AtomicInteger(1);
            contactResponse.getMemberList().forEach(contact -> {
                if (WechatUtils.isChatRoom(contact)) {
                    ChatRoomDescription chatRoomDescription = new ChatRoomDescription();
                    chatRoomDescription.setChatRoomId(contact.getUserName());
                    chatRoomDescription.setUserName(contact.getNickName());
                    userCache.getChatRooms().put(chatRoomDescription.getChatRoomId(), chatRoomDescription);
                    logger.info("chatRoom name :{} , id :{} ", chatRoomDescription.getUserName(), chatRoomDescription.getChatRoomId());
                    chatRooms.add(chatRoomDescription);
                } else {

                    logger.info("{} nickName:{} , remarkName:{} , userName:{}",count.get(), contact.getNickName(), contact.getRemarkName(), contact.getUserName());
                    count.decrementAndGet();
                    userCache.getChatContants().put(contact.getUserName(), contact);
                }
            });
            seq = contactResponse.getSeq();

            cacheService.getIndividuals().addAll(contactResponse.getMemberList().stream().filter(WechatUtils::isIndividual).collect(Collectors.toSet()));
            cacheService.getMediaPlatforms().addAll(contactResponse.getMemberList().stream().filter(WechatUtils::isMediaPlatform).collect(Collectors.toSet()));
        } while (seq > 0);


        initResponse.getContactList().stream()
                .filter(x -> WechatUtils.isChatRoom(x)).forEach(x -> {
            ChatRoomDescription chatRoomDescription = new ChatRoomDescription();
            chatRoomDescription.setUserName(x.getNickName());
            chatRoomDescription.setChatRoomId(x.getUserName());
            userCache.getChatRooms().put(chatRoomDescription.getChatRoomId(), chatRoomDescription);
            logger.info("chatRoom name :{} , id :{} ", chatRoomDescription.getUserName(), chatRoomDescription.getChatRoomId());
            chatRooms.add(chatRoomDescription);
        });


        logger.info("chatRoomDescriptions size : {}", chatRooms.size());

        userCache.setAlive(true);
        cacheService.cacheUser(userCache);

        MessageHandler messageHandler = new MessageHandlerImpl(wechatMessageService, userCache);
        SyncServie syncServie = new SyncServie(userCache, wechatHttpService, messageHandler);
        while (true) {
            if (syncServie.listen() != RetCode.NORMAL.getCode()) {
                logger.warn("logout user:{}", userCache.getUin());
                userCache.setAlive(false);
                break;
            }
        }

    }

    @Override
    public void run() {
        try {
            login();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
