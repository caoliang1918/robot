package com.zhongweixian.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.zhongweixian.domain.BaseUserCache;
import com.zhongweixian.domain.shared.*;
import com.zhongweixian.utils.MessageUtils;
import com.zhongweixian.service.MessageHandler;
import com.zhongweixian.service.WechatMessageService;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private String imageUrl = "%s/cgi-bin/mmwebwx-bin/webwxgetmsgimg?&MsgID=%s&skey=%s";


    public MessageHandlerImpl(WechatMessageService wechatHttpService, BaseUserCache baseUserCache) {
        this.wechatHttpService = wechatHttpService;
        this.baseUserCache = baseUserCache;
    }

    @Override
    public void onReceivingChatRoomTextMessage(BaseUserCache userCache, Message message) {
        Contact chatRoom = baseUserCache.getChatRoomMembers().get(message.getFromUserName());
        String content = MessageUtils.getChatRoomTextMessageContent(message.getContent());
        if (chatRoom == null) {
            chatRoom = baseUserCache.getChatRoomMembers().get(message.getToUserName());
            logger.info("roomName : {} ,from person: me ", chatRoom.getNickName());
        } else {
            logger.info("roomName :{} ,from person: {} ", chatRoom.getNickName(), MessageUtils.getSenderOfChatRoomTextMessage(message.getContent()));
        }
        logger.info("content:{}", content);
    }

    @Override
    public void onReceivingChatRoomImageMessage(BaseUserCache userCache, Message message) {
        logger.info("from :{} fullImageUrl:{}", String.format(imageUrl, userCache.getWxHost(), message.getMsgId(), userCache.getsKey()));
        wechatHttpService.downloadImage(userCache, message.getMsgId());
    }

    @Override
    public void onReceivingPrivateTextMessage(BaseUserCache userCache, Message message) {
        logger.info("content:{}", message.getContent());
        try {
            logger.info("from:{} ", userCache.getChatContants().get(message.getFromUserName()).getNickName());
        } catch (Exception e) {
            logger.error("from:{} , to:{} , error:{}", message.getFromUserName(), message.getToUserName(), e);
        }
        if ("进群".equals(message.getContent())) {
            String roomName = null;
            userCache.getChatRoomMembers().values().forEach(x -> {
                if ("免费分享".equals(x.getNickName())) {
                    logger.info("拉用户:{} 进 {} 群", message.getFromUserName(), x.getUserName());
                    try {
                        wechatHttpService.addChatRoomMember(userCache, x.getUserName(), message.getFromUserName());
                    } catch (Exception e) {
                        logger.error("chatRoom add member error:{} ", e);
                    }
                }
            });
        }
    }

    @Override
    public void onReceivingPrivateImageMessage(BaseUserCache userCache, Message message) {
        logger.info("from :{}  fullImageUrl:{}", String.format(imageUrl, userCache.getWxHost(), message.getMsgId(), userCache.getsKey()));
        wechatHttpService.downloadImage(userCache, message.getMsgId());
    }

    @Override
    public boolean onReceivingFriendInvitation(RecommendInfo info) {
        logger.info("接受新加好友信息:{}", info);
        return true;
    }

    @Override
    public void onAppMessage(BaseUserCache userCache, Message message) {
        if (message.getFromUserName().startsWith("@@")) {
            Contact chatRoom = baseUserCache.getChatRoomMembers().get(message.getFromUserName());
            logger.info("roomName :{} ,from person: {} ", chatRoom.getNickName(), MessageUtils.getSenderOfChatRoomTextMessage(message.getContent()));
            if (chatRoom.getNickName().startsWith("老九群") || chatRoom.getNickName().startsWith("免费")) {
                String content = MessageUtils.getChatRoomTextMessageContent(message.getContent());
                logger.info("AppMsgType:{} , content:{} ", message.getAppMsgType(), content);
                if (message.getAppMsgType() == 5) {
                    /**
                     * 非法的外面连接，必须给予警告
                     */
                }
            }
        }
    }

    @Override
    public void postAcceptFriendInvitation(BaseUserCache userCache, Message message) {
        String content = StringEscapeUtils.unescapeXml(message.getContent());
        ObjectMapper xmlMapper = new XmlMapper();
        try {
            /**
             * 备注新的好友信息
             */
            FriendInvitationContent friendInvitationContent = xmlMapper.readValue(content, FriendInvitationContent.class);
            logger.info("备注好友信息 username:{} , content:{} , Fromusername:{} ", message.getRecommendInfo().getUserName(), friendInvitationContent.getContent(), friendInvitationContent.getFromusername());
            wechatHttpService.setAlias(userCache, message.getRecommendInfo().getUserName(), friendInvitationContent.getFromusername());
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    @Override
    public void onChatRoomMembersChanged(Contact chatRoom, Set<ChatRoomMember> membersJoined, Set<ChatRoomMember> membersLeft) {
        if (membersJoined != null && membersJoined.size() > 0) {
            logger.info("chatRoom:{} ,  membersJoined:{}", chatRoom.getNickName(), String.join(",", membersJoined.stream().map(ChatRoomMember::getNickName).collect(Collectors.toList())));
        }
        if (membersLeft != null && membersLeft.size() > 0) {
            logger.info("chatRoom:{} , membersLeft:{}", chatRoom.getNickName(), String.join(",", membersLeft.stream().map(ChatRoomMember::getNickName).collect(Collectors.toList())));
        }
    }

    @Override
    public void onNewChatRoomsFound(Set<Contact> chatRooms) {
        chatRooms.forEach(x -> {
            logger.info("onNewChatRoomsFound username :{}  , roomName :{} ", x.getUserName(), x.getNickName());
        });
    }

    @Override
    public void onChatRoomsDeleted(Set<Contact> chatRooms) {
        chatRooms.forEach(x -> {
            logger.info("onChatRoomsDeleted username :{}  , roomName :{} ", x.getUserName(), x.getNickName());
        });
    }

    @Override
    public void onNewFriendsFound(Set<Contact> contacts) {
        contacts.forEach(x -> {
            logger.info("onNewFriendsFound username:{} , nikename:{}", x.getUserName(), x.getNickName());
        });
    }

    @Override
    public void onFriendsDeleted(Set<Contact> contacts) {
        contacts.forEach(x -> {
            logger.info("onFriendsDeleted username:{} , nikename:{}", x.getUserName(), x.getNickName());
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
        if (contact != null) {
            logger.info("the red packet is from :{}", contact.getNickName());
        }
    }

    private void replyMessage(BaseUserCache userCache, Message message) throws IOException {
        wechatHttpService.sendText(userCache, message.getFromUserName(), message.getContent());
    }
}
