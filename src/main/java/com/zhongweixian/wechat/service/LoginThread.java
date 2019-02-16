package com.zhongweixian.wechat.service;

import com.zhongweixian.wechat.domain.BaseUserCache;
import com.zhongweixian.wechat.domain.request.component.BaseRequest;
import com.zhongweixian.wechat.domain.response.*;
import com.zhongweixian.wechat.domain.shared.ChatRoomDescription;
import com.zhongweixian.wechat.domain.shared.Contact;
import com.zhongweixian.wechat.domain.shared.Token;
import com.zhongweixian.wechat.enums.LoginCode;
import com.zhongweixian.wechat.enums.StatusNotifyCode;
import com.zhongweixian.wechat.exception.WechatException;
import com.zhongweixian.wechat.exception.WechatQRExpiredException;
import com.zhongweixian.wechat.utils.QRCodeUtils;
import com.zhongweixian.wechat.utils.WechatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 每个登录用一个线程来维护
 */
public class LoginThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LoginThread.class);

    private CacheService cacheService;
    private SyncServie syncServie;
    private WechatHttpServiceInternal wechatHttpServiceInternal;

    public LoginThread(CacheService cacheService, SyncServie syncServie, WechatHttpServiceInternal wechatHttpServiceInternal) {
        this.cacheService = cacheService;
        this.syncServie = syncServie;
        this.wechatHttpServiceInternal = wechatHttpServiceInternal;
    }

    private int qrRefreshTimes = 0;

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

    public void login() {
        try {
            /**
             *
             */
            wechatHttpServiceInternal.open(qrRefreshTimes);
            logger.info("[0] entry completed");
            /**
             * 获取uuid
             */
            String uuid = wechatHttpServiceInternal.getUUID();
            cacheService.setUuid(uuid);
            logger.info("uuid completed: uuid{}", uuid);
            //2 qr
            byte[] qrData = wechatHttpServiceInternal.getQR(uuid);
            ByteArrayInputStream stream = new ByteArrayInputStream(qrData);
            qrUrl = QRCodeUtils.decode(stream);
            stream.close();
            String qr = QRCodeUtils.generateQR(qrUrl, 40, 40);
            logger.info("\r\n" + qr);
            logger.info("[2] qrcode completed");
            //3 statreport
            wechatHttpServiceInternal.statReport();
            logger.info("[3] statReport completed");
            //4 login
            LoginResult loginResponse;

            BaseUserCache userCache = new BaseUserCache();
            while (true) {
                loginResponse = wechatHttpServiceInternal.login(uuid);
                logger.info("loginResponse:{}", loginResponse.toString());
                if (LoginCode.SUCCESS.getCode().equals(loginResponse.getCode())) {
                    if (loginResponse.getHostUrl() == null) {
                        throw new WechatException("hostUrl can't be found");
                    }
                    if (loginResponse.getRedirectUrl() == null) {
                        throw new WechatException("redirectUrl can't be found");
                    }
                    cacheService.setHostUrl(loginResponse.getHostUrl());
                    if (loginResponse.getHostUrl().equals("https://wechat.com")) {
                        cacheService.setSyncUrl("https://webpush.web.wechat.com");
                        cacheService.setFileUrl("https://file.web.wechat.com");
                    } else {
                        cacheService.setSyncUrl(loginResponse.getHostUrl().replaceFirst("^https://", "https://webpush."));
                        cacheService.setFileUrl(loginResponse.getHostUrl().replaceFirst("^https://", "https://file."));
                    }
                    break;
                } else if (LoginCode.AWAIT_CONFIRMATION.getCode().equals(loginResponse.getCode())) {
                    logger.info("[*] login status = AWAIT_CONFIRMATION");
                } else if (LoginCode.AWAIT_SCANNING.getCode().equals(loginResponse.getCode())) {
                    logger.info("[*] login status = AWAIT_SCANNING");
                } else if (LoginCode.EXPIRED.getCode().equals(loginResponse.getCode())) {
                    logger.info("[*] login status = EXPIRED");
                    throw new WechatQRExpiredException();
                } else {
                    logger.info("[*] login status = " + loginResponse.getCode());
                }
            }

            logger.info("[4] login completed");
            //5 redirect login
            Token token = wechatHttpServiceInternal.openNewloginpage(loginResponse.getRedirectUrl());
            if (token.getRet() == 0) {
                cacheService.setPassTicket(token.getPass_ticket());
                cacheService.setsKey(token.getSkey());
                cacheService.setSid(token.getWxsid());
                cacheService.setUin(token.getWxuin());
                userCache.setUuid(uuid);
                userCache.setUin(token.getWxuin());
                userCache.setSid(token.getWxsid());
                userCache.setsKey(token.getSkey());
                userCache.setPassTicket(token.getPass_ticket());

                BaseRequest baseRequest = new BaseRequest();
                baseRequest.setUin(cacheService.getUin());
                baseRequest.setSid(cacheService.getSid());
                baseRequest.setSkey(cacheService.getsKey());
                cacheService.setBaseRequest(baseRequest);
            } else {
                throw new WechatException("token ret = " + token.getRet());
            }
            logger.info("[5] redirect login completed");
            //6 redirect
            wechatHttpServiceInternal.redirect(cacheService.getHostUrl());
            logger.info("[6] redirect completed");
            //7 init
            InitResponse initResponse = wechatHttpServiceInternal.init(cacheService.getHostUrl(), cacheService.getBaseRequest());
            WechatUtils.checkBaseResponse(initResponse);
            cacheService.setSyncKey(initResponse.getSyncKey());
            cacheService.setOwner(initResponse.getUser());
            logger.info("[7] init completed");
            //8 status notify
            StatusNotifyResponse statusNotifyResponse =
                    wechatHttpServiceInternal.statusNotify(cacheService.getHostUrl(),
                            cacheService.getBaseRequest(),
                            cacheService.getOwner().getUserName(), StatusNotifyCode.INITED.getCode());
            WechatUtils.checkBaseResponse(statusNotifyResponse);
            //9 get contact
            long seq = 0;
            //好友
            List<ChatRoomDescription> chatRooms = new ArrayList<>();
            //群组(这里包含已经保存的技能组和最近聊天的技能组)
            List<Contact> chatContact = new ArrayList<>();
            do {
                ContactResponse contactResponse = wechatHttpServiceInternal.getContact(cacheService.getHostUrl(), cacheService.getBaseRequest().getSkey(), seq);
                WechatUtils.checkBaseResponse(contactResponse);
                for (Contact contact : contactResponse.getMemberList()) {


                }
                logger.info("[*] getContactResponse seq = " + contactResponse.getSeq());
                logger.info("[*] getContactResponse memberCount = " + contactResponse.getMemberCount());
                contactResponse.getMemberList().forEach(contact -> {
                    if (WechatUtils.isChatRoom(contact)) {
                        ChatRoomDescription chatRoomDescription = new ChatRoomDescription();
                        chatRoomDescription.setChatRoomId(contact.getUserName());
                        chatRoomDescription.setUserName(contact.getNickName());
                        userCache.getChatRooms().put(chatRoomDescription.getChatRoomId(), chatRoomDescription);
                        logger.info("chatRoom name :{} , id :{} ", chatRoomDescription.getUserName(), chatRoomDescription.getChatRoomId());
                        chatRooms.add(chatRoomDescription);
                    } else {
                        logger.debug("nickName:{} , remarkName:{} , userName:{}", contact.getNickName(), contact.getRemarkName(), contact.getUserName());
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
           /* if (chatRoomDescriptions.length > 0) {
                BatchGetContactResponse batchGetContactResponse = wechatHttpServiceInternal.batchGetContact(
                        cacheService.getHostUrl(),
                        cacheService.getBaseRequest(),
                        chatRoomDescriptions);
                WechatUtils.checkBaseResponse(batchGetContactResponse);
                cacheService.getChatRooms().addAll(batchGetContactResponse.getContactList());
                batchGetContactResponse.getContactList().forEach(room->{
                    logger.info("22222 chatRoom name :{} , id :{} ", room.getNickName(), room.getChatRoomId());
                });

            }*/
            cacheService.setAlive(true);
            userCache.setAlive(true);
            cacheService.cacheUser(userCache);
            while (true) {
                syncServie.listen();
            }
        } catch (Exception ex) {
            logger.error("{}", ex);
        }
    }

    @Override
    public void run() {
        login();
    }
}
