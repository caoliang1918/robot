package com.zhongweixian.service;

import com.zhongweixian.domain.BaseUserCache;
import com.zhongweixian.domain.response.SyncCheckResponse;
import com.zhongweixian.domain.response.SyncResponse;
import com.zhongweixian.domain.response.VerifyUserResponse;
import com.zhongweixian.domain.shared.*;
import com.zhongweixian.enums.MessageType;
import com.zhongweixian.enums.RetCode;
import com.zhongweixian.enums.Selector;
import com.zhongweixian.exception.WechatException;
import com.zhongweixian.utils.WechatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 每个登录用户一个线程来处理消息同步
 */
public class SyncServie {
    private static final Logger logger = LoggerFactory.getLogger(SyncServie.class);

    private BaseUserCache baseUserCache;
    private WechatHttpService wechatHttpService;
    private MessageHandler messageHandler;

    private String WECHAT_GET_IMG_URL = "https://wx.qq.com//cgi-bin/mmwebwx-bin/webwxgetmsgimg?&MsgID=%s&skey=%s";

    private final static String RED_PACKET_CONTENT = "收到红包，请在手机上查看";


    public SyncServie(BaseUserCache baseUserCache, WechatHttpService wechatHttpService, MessageHandler messageHandler) {
        this.baseUserCache = baseUserCache;
        this.wechatHttpService = wechatHttpService;
        this.messageHandler = messageHandler;
    }

    public Integer listen() throws IOException, URISyntaxException {
        SyncCheckResponse syncCheckResponse = wechatHttpService.syncCheck(baseUserCache);
        /**
         * 可能存在为空
         */
        if (syncCheckResponse == null) {
            return RetCode.NORMAL.getCode();
        }
        int retCode = syncCheckResponse.getRetcode();
        int selector = syncCheckResponse.getSelector();
        logger.info("user:[{}] sync result retcode : {}, selector = {}", baseUserCache.getOwner().getNickName(), retCode, selector);
        if (retCode == RetCode.NORMAL.getCode()) {
            //有新消息
            if (selector == Selector.NEW_MESSAGE.getCode()) {
                onNewMessage();
            } else if (selector == Selector.ENTER_LEAVE_CHAT.getCode()) {
                sync();
            } else if (selector == Selector.CONTACT_UPDATED.getCode()) {
                sync();
            } else if (selector == Selector.UNKNOWN1.getCode()) {
                sync();
            } else if (selector == Selector.UNKNOWN6.getCode()) {
                sync();
            } else if (selector == Selector.UNKNOWN3.getCode()) {
                sync();
            } else if (selector != Selector.NORMAL.getCode()) {
                throw new WechatException("syncCheckResponse ret = " + retCode);
            }
        } else {
            logger.error("syncCheckResponse:{}" , syncCheckResponse);
            return RetCode.LOGOUT2.getCode();
        }
        return RetCode.NORMAL.getCode();
    }

    private SyncResponse sync() throws IOException {
        SyncResponse syncResponse = wechatHttpService.sync(baseUserCache);
        WechatUtils.checkBaseResponse(syncResponse);
        baseUserCache.setSyncKey(syncResponse.getSyncKey());
        baseUserCache.setSyncCheckKey(syncResponse.getSyncCheckKey());
        //mod包含新增和修改
        if (syncResponse.getModContactCount() > 0) {
            //onContactsModified(syncResponse.getModContactList());
        }
        //del->联系人移除
        if (syncResponse.getDelContactCount() > 0) {
            //onContactsDeleted(syncResponse.getDelContactList());
        }
        return syncResponse;
    }

    private void acceptFriendInvitation(RecommendInfo info) throws IOException, URISyntaxException {
        VerifyUser user = new VerifyUser();
        user.setValue(info.getUserName());
        user.setVerifyUserTicket(info.getTicket());
        VerifyUserResponse verifyUserResponse = wechatHttpService.acceptFriend(
                baseUserCache,
                new VerifyUser[]{user}
        );
        WechatUtils.checkBaseResponse(verifyUserResponse);
    }

    private boolean isMessageFromIndividual(Message message) {
        return message.getFromUserName() != null
                && message.getFromUserName().startsWith("@")
                && !message.getFromUserName().startsWith("@@");
    }

    private boolean isMessageFromChatRoom(Message message) {
        return message.getFromUserName() != null && message.getFromUserName().startsWith("@@");
    }

    private void onNewMessage() throws IOException, URISyntaxException {
        SyncResponse syncResponse = sync();
        if (messageHandler == null) {
            return;
        }
        for (Message message : syncResponse.getAddMsgList()) {
            //文本消息
            if (message.getMsgType() == MessageType.TEXT.getCode()) {
                //个人
                if (isMessageFromIndividual(message)) {
                    messageHandler.onReceivingPrivateTextMessage(message);
                }
                //群
                else if (isMessageFromChatRoom(message)) {
                    messageHandler.onReceivingChatRoomTextMessage(message);
                }
                //图片
            } else if (message.getMsgType() == MessageType.IMAGE.getCode()) {
                String fullImageUrl = String.format(WECHAT_GET_IMG_URL, baseUserCache.getWxHost(), message.getMsgId(), baseUserCache.getsKey());
                String thumbImageUrl = fullImageUrl + "&type=slave";
                //个人
                if (isMessageFromIndividual(message)) {
                    messageHandler.onReceivingPrivateImageMessage(baseUserCache,message, thumbImageUrl, fullImageUrl);
                }
                //群
                else if (isMessageFromChatRoom(message)) {
                    messageHandler.onReceivingChatRoomImageMessage(message, thumbImageUrl, fullImageUrl);
                }
            }
            //系统消息
            else if (message.getMsgType() == MessageType.SYS.getCode()) {
                //红包
                if (RED_PACKET_CONTENT.equals(message.getContent())) {
                    logger.info("[*] you've received a red packet");
                    String from = message.getFromUserName();
                    Set<Contact> contacts = null;
                    //个人
                    if (isMessageFromIndividual(message)) {

                    }
                    //群
                    else if (isMessageFromChatRoom(message)) {

                    }
                    if (contacts != null) {
                        Contact contact = contacts.stream().filter(x -> Objects.equals(x.getUserName(), from)).findAny().orElse(null);
                        messageHandler.onRedPacketReceived(contact);
                    }
                }
            }
            //好友邀请
            else if (message.getMsgType() == MessageType.VERIFYMSG.getCode() && baseUserCache.getOwner().getUserName().equals(message.getToUserName())) {
                if (messageHandler.onReceivingFriendInvitation(message.getRecommendInfo())) {
                    acceptFriendInvitation(message.getRecommendInfo());
                    logger.info("[*] you've accepted the invitation");
                    messageHandler.postAcceptFriendInvitation(baseUserCache , message);
                } else {
                    logger.info("[*] you've declined the invitation");
                    //TODO decline invitation
                }
            }

        }
    }

    private void onContactsModified(Set<Contact> contacts) {
        Set<Contact> individuals = new HashSet<>();
        Set<Contact> chatRooms = new HashSet<>();
        Set<Contact> mediaPlatforms = new HashSet<>();

        for (Contact contact : contacts) {
            if (WechatUtils.isIndividual(contact)) {
                individuals.add(contact);
            } else if (WechatUtils.isMediaPlatform(contact)) {
                mediaPlatforms.add(contact);
            } else if (WechatUtils.isChatRoom(contact)) {
                chatRooms.add(contact);
            }
        }

        //individual
        if (individuals.size() > 0) {
            Set<Contact> existingIndividuals = null;//cacheService.getIndividuals();
            Set<Contact> newIndividuals = individuals.stream().filter(x -> !existingIndividuals.contains(x)).collect(Collectors.toSet());
            individuals.forEach(x -> {
                existingIndividuals.remove(x);
                existingIndividuals.add(x);
            });
            if (messageHandler != null && newIndividuals.size() > 0) {
                messageHandler.onNewFriendsFound(newIndividuals);
            }
        }
        //chatroom
        if (chatRooms.size() > 0) {
            Set<Contact> existingChatRooms = null;// cacheService.getChatRooms();
            Set<Contact> newChatRooms = new HashSet<>();
            Set<Contact> modifiedChatRooms = new HashSet<>();
            for (Contact chatRoom : chatRooms) {
                if (existingChatRooms.contains(chatRoom)) {
                    modifiedChatRooms.add(chatRoom);
                } else {
                    newChatRooms.add(chatRoom);
                }
            }
            existingChatRooms.addAll(newChatRooms);
            if (messageHandler != null && newChatRooms.size() > 0) {
                messageHandler.onNewChatRoomsFound(newChatRooms);
            }
            for (Contact chatRoom : modifiedChatRooms) {
                Contact existingChatRoom = existingChatRooms.stream().filter(x -> x.getUserName().equals(chatRoom.getUserName())).findFirst().orElse(null);
                if (existingChatRoom == null) {
                    continue;
                }
                existingChatRooms.remove(existingChatRoom);
                existingChatRooms.add(chatRoom);
                if (messageHandler != null) {
                    Set<ChatRoomMember> oldMembers = existingChatRoom.getMemberList();
                    Set<ChatRoomMember> newMembers = chatRoom.getMemberList();
                    Set<ChatRoomMember> joined = newMembers.stream().filter(x -> !oldMembers.contains(x)).collect(Collectors.toSet());
                    Set<ChatRoomMember> left = oldMembers.stream().filter(x -> !newMembers.contains(x)).collect(Collectors.toSet());
                    if (joined.size() > 0 || left.size() > 0) {
                        messageHandler.onChatRoomMembersChanged(chatRoom, joined, left);
                    }
                }
            }
        }
        if (mediaPlatforms.size() > 0) {
            //media platform
            Set<Contact> existingPlatforms = null;//cacheService.getMediaPlatforms();
            Set<Contact> newMediaPlatforms = existingPlatforms.stream().filter(x -> !existingPlatforms.contains(x)).collect(Collectors.toSet());
            mediaPlatforms.forEach(x -> {
                existingPlatforms.remove(x);
                existingPlatforms.add(x);
            });
            if (messageHandler != null && newMediaPlatforms.size() > 0) {
                messageHandler.onNewMediaPlatformsFound(newMediaPlatforms);
            }
        }
    }

    private void onContactsDeleted(Set<Contact> contacts) {
        Set<Contact> individuals = new HashSet<>();
        Set<Contact> chatRooms = new HashSet<>();
        Set<Contact> mediaPlatforms = new HashSet<>();
        for (Contact contact : contacts) {
            if (WechatUtils.isIndividual(contact)) {
                individuals.add(contact);
                //cacheService.getIndividuals().remove(contact);
            } else if (WechatUtils.isChatRoom(contact)) {
                chatRooms.add(contact);
                //cacheService.getChatRooms().remove(contact);
            } else if (WechatUtils.isMediaPlatform(contact)) {
                mediaPlatforms.add(contact);
                //cacheService.getMediaPlatforms().remove(contact);
            }
        }
        if (messageHandler != null) {
            if (individuals.size() > 0) {
                messageHandler.onFriendsDeleted(individuals);
            }
            if (chatRooms.size() > 0) {
                messageHandler.onChatRoomsDeleted(chatRooms);
            }
            if (mediaPlatforms.size() > 0) {
                messageHandler.onMediaPlatformsDeleted(mediaPlatforms);
            }
        }
    }


}
