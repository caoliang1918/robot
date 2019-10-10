package com.zhongweixian.login;

import com.alibaba.fastjson.JSON;
import com.zhongweixian.domain.WxUserCache;
import com.zhongweixian.domain.response.SyncCheckResponse;
import com.zhongweixian.domain.response.SyncResponse;
import com.zhongweixian.domain.response.VerifyUserResponse;
import com.zhongweixian.domain.shared.*;
import com.zhongweixian.enums.MessageType;
import com.zhongweixian.enums.RetCode;
import com.zhongweixian.enums.Selector;
import com.zhongweixian.service.WxHttpService;
import com.zhongweixian.service.WxMessageHandler;
import com.zhongweixian.utils.WechatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 每个登录用户一个线程来处理消息同步
 */
public class WxSyncMessage {
    private static final Logger logger = LoggerFactory.getLogger(WxSyncMessage.class);

    private WxUserCache wxUserCache;
    private WxHttpService wxHttpService;
    private WxMessageHandler wxMessageHandler;

    private final static String RED_PACKET_CONTENT = "收到红包，请在手机上查看";


    public WxSyncMessage(WxUserCache wxUserCache, WxHttpService wxHttpService, WxMessageHandler wxMessageHandler) {
        this.wxUserCache = wxUserCache;
        this.wxHttpService = wxHttpService;
        this.wxMessageHandler = wxMessageHandler;
    }

    public Integer listen() {
        SyncCheckResponse syncCheckResponse = null;
        try {
            syncCheckResponse = wxHttpService.syncCheck(wxUserCache);
        } catch (Exception e) {
            logger.error("syncCheckResponse error:{}", e);
        }
        /**
         * 可能存在为空
         */
        if (syncCheckResponse == null) {
            logger.error("syncCheckResponse is null");
            return RetCode.NORMAL.getCode();
        }
        int retCode = syncCheckResponse.getRetcode();
        int selector = syncCheckResponse.getSelector();
        logger.info("user  [{}]  sync result retcode : {}, selector : {}", wxUserCache.getOwner().getNickName(), retCode, selector);
        if (retCode == RetCode.NORMAL.getCode()) {
            //有新消息
            if (selector == Selector.NEW_MESSAGE.getCode()) {
                onNewMessage();
            } else if (selector != Selector.NORMAL.getCode()) {
                sync();
            }
        } else {
            logger.error("sync error:{}", syncCheckResponse.toString());
            return syncCheckResponse.getRetcode();
        }
        return RetCode.NORMAL.getCode();
    }

    private SyncResponse sync() {
        SyncResponse syncResponse = wxHttpService.sync(wxUserCache);
        WechatUtils.checkBaseResponse(syncResponse);
        wxUserCache.setSyncKey(syncResponse.getSyncKey());
        wxUserCache.setSyncCheckKey(syncResponse.getSyncCheckKey());

        //mod包含新增和修改
        if (syncResponse.getModContactCount() > 0) {
            logger.info("add member");
            onContactsModified(syncResponse.getModContactList());
        }
        //del->联系人移除
        if (syncResponse.getDelContactCount() > 0) {
            logger.info("delete member");
            onContactsDeleted(syncResponse.getDelContactList());
        }
        return syncResponse;
    }

    private void acceptFriendInvitation(RecommendInfo info) {
        VerifyUser user = new VerifyUser();
        user.setValue(info.getUserName());
        user.setVerifyUserTicket(info.getTicket());
        VerifyUserResponse verifyUserResponse = null;
        try {
            verifyUserResponse = wxHttpService.acceptFriend(wxUserCache, new VerifyUser[]{user});
            WechatUtils.checkBaseResponse(verifyUserResponse);
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    private boolean isMessageFromIndividual(Message message) {
        return message.getFromUserName() != null
                && !message.getFromUserName().startsWith("@@")
                && !message.getToUserName().startsWith("@@");
    }

    private boolean isMessageFromChatRoom(Message message) {
        return message.getFromUserName() != null && (message.getToUserName().startsWith("@@") || message.getFromUserName().startsWith("@@"));
    }

    private void onNewMessage() {
        SyncResponse syncResponse = sync();
        if (wxMessageHandler == null) {
            return;
        }

        for (Message message : syncResponse.getAddMsgList()) {
            logger.debug("onNewMessage : {}", JSON.toJSONString(message));
            //文本消息
            if (message.getMsgType() == MessageType.TEXT.getCode()) {
                //个人
                if (isMessageFromIndividual(message)) {
                    wxMessageHandler.onReceivingPrivateTextMessage(wxUserCache, message);
                    continue;
                }
                //群
                else if (isMessageFromChatRoom(message)) {
                    wxMessageHandler.onReceivingChatRoomTextMessage(wxUserCache, message);
                    continue;
                }
                logger.warn("what is this message:{} , {} , {}", message.getContent(), message.getFromUserName(), message.getToUserName());
                //图片
            } else if (message.getMsgType() == MessageType.IMAGE.getCode()) {
                //个人
                if (isMessageFromIndividual(message)) {
                    wxMessageHandler.onReceivingPrivateImageMessage(wxUserCache, message);
                }
                //群
                else if (isMessageFromChatRoom(message)) {
                    wxMessageHandler.onReceivingChatRoomImageMessage(wxUserCache, message);
                }
            } else if (message.getMsgType() == MessageType.APP.getCode()) {
                wxMessageHandler.onAppMessage(wxUserCache, message);
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
                        logger.info("{}", message.getContent());
                    }
                    //群
                    else if (isMessageFromChatRoom(message)) {
                        logger.info("{}", message.getContent());
                    }
                    if (contacts != null) {
                        Contact contact = contacts.stream().filter(x -> Objects.equals(x.getUserName(), from)).findAny().orElse(null);
                        wxMessageHandler.onRedPacketReceived(contact);
                    }
                }
            }
            //好友邀请
            else if (message.getMsgType() == MessageType.VERIFYMSG.getCode() && wxUserCache.getOwner().getUserName().equals(message.getToUserName())) {
                if (wxMessageHandler.onReceivingFriendInvitation(message.getRecommendInfo())) {
                    logger.info("[*] you've accepted the invitation");
                    acceptFriendInvitation(message.getRecommendInfo());
                    wxMessageHandler.postAcceptFriendInvitation(wxUserCache, message);

                    /**
                     * 给新加的好友发送一段话
                     */
                    if (wxUserCache.getUin().equals("5275953")) {
                        String welcome = "你好，我是搬运工，输入:进群，我将拉你进入指定微信群。\n";
                        wxHttpService.sendText(wxUserCache, welcome, message.getRecommendInfo().getUserName());
                    }
                } else {
                    logger.info("[*] you've declined the invitation");
                    //TODO decline invitation
                }
            }
        }
    }

    /**
     * 群成员变更
     *
     * @param contacts
     */
    private void onContactsModified(Set<Contact> contacts) {
        Set<Contact> individuals = new HashSet<>();
        Set<Contact> chatRooms = new HashSet<>();

        for (Contact contact : contacts) {
            logger.info("onContactsModified:{}", contact.getNickName());
            if (WechatUtils.isChatRoom(contact.getUserName()) || WechatUtils.isChatRoom(contact.getFromUserName())) {
                chatRooms.add(contact);
            } else if (WechatUtils.isIndividual(contact)) {
                individuals.add(contact);
            } else if (WechatUtils.isMediaPlatform(contact)) {

            }
        }

        //个人
        if (individuals.size() > 0) {
            Set<String> existingIndividuals = new HashSet<>(wxUserCache.getChatContants().keySet());
            Set<Contact> newIndividuals = new HashSet<>();
            individuals.forEach(x -> {
                if (!existingIndividuals.contains(x)) {
                    newIndividuals.add(x);
                }
                wxUserCache.getChatContants().put(x.getUserName(), x);
            });
            if (newIndividuals.size() > 0) {
                wxMessageHandler.onNewFriendsFound(newIndividuals);
            }
        }
        //群组
        if (chatRooms.size() > 0) {
            Set<String> existingChatRooms = new HashSet<>(wxUserCache.getChatRoomMembers().keySet());
            Set<Contact> newChatRooms = new HashSet<>();
            Set<Contact> modifiedChatRooms = new HashSet<>();
            for (Contact chatRoom : chatRooms) {
                if (existingChatRooms.contains(chatRoom.getUserName())) {
                    /**
                     * 有变更的群组
                     */
                    modifiedChatRooms.add(chatRoom);
                    chatRoom.getMemberList().forEach(chatRoomMember -> {
                        chatRoom.getMemberMap().put(chatRoomMember.getUserName(), chatRoomMember);
                    });

                } else {
                    /**
                     * 新的群组
                     */
                    newChatRooms.add(chatRoom);
                    chatRoom.getMemberList().forEach(chatRoomMember -> {
                        chatRoom.getMemberMap().put(chatRoomMember.getUserName(), chatRoomMember);
                    });
                }
                wxUserCache.getChatRoomMembers().put(chatRoom.getUserName(), chatRoom);
            }
            if (wxMessageHandler != null && newChatRooms.size() > 0) {
                wxMessageHandler.onNewChatRoomsFound(newChatRooms);
            }
            for (Contact chatRoom : modifiedChatRooms) {
                Contact existingChatRoom = wxUserCache.getChatRoomMembers().get(chatRoom.getUserName());
                if (existingChatRoom == null) {
                    continue;
                }
                existingChatRooms.remove(existingChatRoom.getUserName());
                existingChatRooms.add(chatRoom.getUserName());
                Set<ChatRoomMember> oldMembers = existingChatRoom.getMemberList();
                Set<ChatRoomMember> newMembers = chatRoom.getMemberList();
                Set<ChatRoomMember> joined = newMembers.stream().filter(x -> !oldMembers.contains(x)).collect(Collectors.toSet());
                Set<ChatRoomMember> left = oldMembers.stream().filter(x -> !newMembers.contains(x)).collect(Collectors.toSet());
                if (joined.size() > 0 || left.size() > 0) {
                    wxMessageHandler.onChatRoomMembersChanged(chatRoom, joined, left);
                }
            }
        }

    }

    private void onContactsDeleted(Set<Contact> contacts) {
        Set<Contact> individuals = new HashSet<>();
        Set<Contact> chatRooms = new HashSet<>();
        for (Contact contact : contacts) {
            if (WechatUtils.isIndividual(contact)) {
                individuals.add(contact);
                wxUserCache.getChatContants().remove(contact.getUserName());
            } else if (WechatUtils.isChatRoom(contact.getUserName()) || WechatUtils.isChatRoom(contact.getFromUserName())) {
                chatRooms.add(contact);
                wxUserCache.getChatRoomMembers().remove(contact.getUserName());
            }
        }
        if (wxMessageHandler != null) {
            if (individuals.size() > 0) {
                wxMessageHandler.onFriendsDeleted(individuals);
            }
            if (chatRooms.size() > 0) {
                wxMessageHandler.onChatRoomsDeleted(chatRooms);
            }
        }
    }


}