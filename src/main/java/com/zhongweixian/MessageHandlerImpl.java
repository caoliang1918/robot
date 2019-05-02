package com.zhongweixian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.zhongweixian.domain.BaseUserCache;
import com.zhongweixian.domain.shared.*;
import com.zhongweixian.utils.MessageUtils;
import com.zhongweixian.exception.WechatException;
import com.zhongweixian.service.MessageHandler;
import com.zhongweixian.service.WechatMessageService;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 接受消息类
 */
public class MessageHandlerImpl implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandlerImpl.class);

    private WechatMessageService wechatHttpService;

    private BaseUserCache baseUserCache;


    public MessageHandlerImpl(WechatMessageService wechatHttpService, BaseUserCache baseUserCache) {
        this.wechatHttpService = wechatHttpService;
        this.baseUserCache = baseUserCache;
     }

    @Override
    public void onReceivingChatRoomTextMessage(Message message) {
        ChatRoomDescription chatRoom = baseUserCache.getChatRooms().get(message.getFromUserName());
        if (chatRoom != null) {
            logger.info("roomName:{}",chatRoom.getUserName());
        }
        logger.info("from person: {} ", MessageUtils.getSenderOfChatRoomTextMessage(message.getContent()));
        logger.info("content:{}", MessageUtils.getChatRoomTextMessageContent(message.getContent()));
    }

    @Override
    public void onReceivingChatRoomImageMessage(Message message, String thumbImageUrl, String fullImageUrl) {
        logger.info("onReceivingChatRoomImageMessage");
        logger.info("thumbImageUrl:{}", thumbImageUrl);
        logger.info("fullImageUrl:{}", fullImageUrl);
    }

    @Override
    public void onReceivingPrivateTextMessage(Message message) throws WechatException {
        //logger.info("from my message to userName:{}" , cacheService.getUserCache(uid).getChatContants().get(message.getToUserName()).getRemarkName());
        logger.info("from:{} ", message.getFromUserName());
        logger.info("to:{}", message.getToUserName());
        logger.info("content:{}", message.getContent());
    }

    @Override
    public void onReceivingPrivateImageMessage(BaseUserCache userCache , Message message, String thumbImageUrl, String fullImageUrl) throws IOException {
        logger.info("onReceivingPrivateImageMessage");
        logger.info("thumbImageUrl:{}", thumbImageUrl);
        logger.info("fullImageUrl:{}", fullImageUrl);
//        将图片保存在本地
        byte[] data = wechatHttpService.downloadImage(userCache,thumbImageUrl);
        FileOutputStream fos = new FileOutputStream(System.currentTimeMillis() + ".jpg");
        fos.write(data);
        fos.close();
    }

    @Override
    public boolean onReceivingFriendInvitation(RecommendInfo info) {
        logger.info("onReceivingFriendInvitation");
        logger.info("recommendinfo content:{}", info.getContent());
//        默认接收所有的邀请
        return true;
    }

    @Override
    public void postAcceptFriendInvitation(BaseUserCache userCache , Message message) throws IOException {
        logger.info("postAcceptFriendInvitation");
//        将该用户的微信号设置成他的昵称
        String content = StringEscapeUtils.unescapeXml(message.getContent());
        ObjectMapper xmlMapper = new XmlMapper();
        FriendInvitationContent friendInvitationContent = xmlMapper.readValue(content, FriendInvitationContent.class);
        wechatHttpService.setAlias(userCache,message.getRecommendInfo().getUserName(), friendInvitationContent.getFromusername());
    }

    @Override
    public void onChatRoomMembersChanged(Contact chatRoom, Set<ChatRoomMember> membersJoined, Set<ChatRoomMember> membersLeft) {
        logger.info("onChatRoomMembersChanged");
        logger.info("chatRoom:{}", chatRoom.getUserName());
        if (membersJoined != null && membersJoined.size() > 0) {
            logger.info("membersJoined:{}", String.join(",", membersJoined.stream().map(ChatRoomMember::getNickName).collect(Collectors.toList())));
        }
        if (membersLeft != null && membersLeft.size() > 0) {
            logger.info("membersLeft:{}", String.join(",", membersLeft.stream().map(ChatRoomMember::getNickName).collect(Collectors.toList())));
        }
    }

    @Override
    public void onNewChatRoomsFound(Set<Contact> chatRooms) {
        logger.info("onNewChatRoomsFound");
        chatRooms.forEach(x -> logger.info(x.getUserName()));
    }

    @Override
    public void onChatRoomsDeleted(Set<Contact> chatRooms) {
        logger.info("onChatRoomsDeleted");
        chatRooms.forEach(x -> logger.info(x.getUserName()));
    }

    @Override
    public void onNewFriendsFound(Set<Contact> contacts) {
        logger.info("onNewFriendsFound");
        contacts.forEach(x -> {
            logger.info(x.getUserName());
            logger.info(x.getNickName());
        });
    }

    @Override
    public void onFriendsDeleted(Set<Contact> contacts) {
        logger.info("onFriendsDeleted");
        contacts.forEach(x -> {
            logger.info(x.getUserName());
            logger.info(x.getNickName());
        });
    }

    @Override
    public void onNewMediaPlatformsFound(Set<Contact> mps) {
        logger.info("onNewMediaPlatformsFound");
    }

    @Override
    public void onMediaPlatformsDeleted(Set<Contact> mps) {
        logger.info("onMediaPlatformsDeleted");
    }

    @Override
    public void onRedPacketReceived(Contact contact) {
        logger.info("onRedPacketReceived");
        if (contact != null) {
            logger.info("the red packet is from :{}", contact.getNickName());
        }
    }

    private void replyMessage(BaseUserCache userCache , Message message) throws IOException {
        wechatHttpService.sendText(userCache ,message.getFromUserName(), message.getContent());
    }
}
