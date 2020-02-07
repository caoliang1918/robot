package com.zhongweixian.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.zhongweixian.domain.WxUserCache;
import com.zhongweixian.domain.response.SendMsgResponse;
import com.zhongweixian.domain.shared.*;
import com.zhongweixian.service.WxHttpService;
import com.zhongweixian.service.WxMessageHandler;
import com.zhongweixian.utils.MessageUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 接受消息类
 */
@Service
public class WxMessageHandlerImpl implements WxMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(WxMessageHandlerImpl.class);

    @Autowired
    private WxHttpService wxHttpService;


    private String imageUrl = "%s/cgi-bin/mmwebwx-bin/webwxgetmsgimg?&MsgID=%s&skey=%s";

    private String imageDir = "../logs/images";


    @Override
    public void onReceivingChatRoomTextMessage(WxUserCache userCache, Message message) {
        Contact chatRoom = userCache.getChatRoomMembers().get(message.getFromUserName());
        String content = MessageUtils.getChatRoomTextMessageContent(message.getContent());
        String fromUser = null;
        if (chatRoom == null) {
            chatRoom = userCache.getChatRoomMembers().get(message.getToUserName());
            fromUser = message.getFromUserName();
        } else {
            fromUser = MessageUtils.getSenderOfChatRoomTextMessage(message.getContent());
        }

        if (chatRoom.getMemberMap().size() > 0) {
            logger.info("roomName : {} ,from person:{} ,content:{}", chatRoom.getNickName(), chatRoom.getMemberMap().get(fromUser).getNickName(), content);
        } else {
            logger.info("roomName : {} ,from person:{} ,content:{}", chatRoom.getNickName(), fromUser, content);
        }
    }

    @Override
    public void onReceivingChatRoomImageMessage(WxUserCache userCache, Message message) {
        logger.info("group image from :{} fullImageUrl:{}", String.format(imageUrl, userCache.getWxHost(), message.getMsgId(), userCache.getsKey()));
        downloadImage(userCache, message.getMsgId());
    }

    @Override
    public void onReceivingPrivateImageMessage(WxUserCache userCache, Message message) {
        logger.info("private image from :{}  fullImageUrl:{}", String.format(imageUrl, userCache.getWxHost(), message.getMsgId(), userCache.getsKey()));
        downloadImage(userCache, message.getMsgId());
    }

    @Override
    public void onReceivingPrivateTextMessage(WxUserCache userCache, Message message) {
        try {
            if (userCache.getOwner().getUserName().equals(message.getFromUserName())) {
                logger.info("from me content:{}", message.getContent());
                return;
            } else {
                logger.info("from:{} , content:{} ", userCache.getChatContants().get(message.getFromUserName()).getNickName(), message.getContent());
            }
        } catch (Exception e) {
            logger.error("from:{} , to:{} , error:{}", message.getFromUserName(), message.getToUserName(), e);
        }

        switch (userCache.getUin()) {
            case "5275953":
                if (message.getContent().contains("行情")) {
                    userCache.getChatRoomMembers().values().forEach(x -> {
                        if (x.getNickName().startsWith("美股新闻")) {
                            try {
                                wxHttpService.addChatRoomMember(userCache, x.getUserName(), message.getFromUserName());
                                logger.info("拉用户:{} 进 {} 群", message.getFromUserName(), x.getNickName());
                            } catch (Exception e) {
                                logger.error("chatRoom add member error:{} ", e);
                            }
                        }
                    });
                } else if (message.getContent().contains("进群")) {
                    userCache.getChatRoomMembers().values().forEach(x -> {
                        if (x.getNickName().startsWith("天南地北")) {
                            try {
                                wxHttpService.addChatRoomMember(userCache, x.getUserName(), message.getFromUserName());
                                logger.info("拉用户:{} 进 {} 群", message.getFromUserName(), x.getNickName());
                            } catch (Exception e) {
                                logger.error("chatRoom add member error:{} ", e);
                            }
                        }
                    });
                }
                break;
        }
    }

    @Override
    public boolean onReceivingFriendInvitation(RecommendInfo info) {
        logger.info("接受新加好友信息:{}", info);
        return true;
    }

    @Override
    public void onAppMessage(WxUserCache userCache, Message message) {
        if (message.getFromUserName().startsWith("@@")) {
            Contact chatRoom = userCache.getChatRoomMembers().get(message.getFromUserName());
            logger.info("roomName :{} ,from person: {} ", chatRoom.getNickName(), MessageUtils.getSenderOfChatRoomTextMessage(message.getContent()));
            if (chatRoom.getNickName().contains("天南地北")) {
                String content = MessageUtils.getChatRoomTextMessageContent(message.getContent());
                logger.info("AppMsgType:{} , content:{} ", message.getAppMsgType(), content);
                if (message.getAppMsgType() == 5 || message.getAppMsgType() == 36) {
                    /**
                     * 非法的外面连接，必须给予警告
                     */
                    sendText(userCache, "请不要发送广告链接，谢谢合作！", chatRoom.getUserName());
                }
            }
        }
    }

    @Override
    public void postAcceptFriendInvitation(WxUserCache userCache, Message message) {
        String content = StringEscapeUtils.unescapeXml(message.getContent());
        ObjectMapper xmlMapper = new XmlMapper();
        try {
            /**
             * 备注新的好友信息
             */
            FriendInvitationContent friendInvitationContent = xmlMapper.readValue(content, FriendInvitationContent.class);
            logger.info("备注好友信息 username:{} , content:{} , Fromusername:{} ", message.getRecommendInfo().getUserName(), friendInvitationContent.getContent(), friendInvitationContent.getFromusername());
            wxHttpService.setAlias(userCache, message.getRecommendInfo().getUserName(), friendInvitationContent.getFromusername());
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
            logger.info("onNewFriendsFound username:{} , nickname:{}", x.getUserName(), x.getNickName());
        });
    }

    @Override
    public void onFriendsDeleted(Set<Contact> contacts) {
        contacts.forEach(x -> {
            logger.info("onFriendsDeleted username:{} , nickname:{}", x.getUserName(), x.getNickName());
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

    @Override
    public SendMsgResponse sendText(WxUserCache userCache, String content, String toUserName) {
        return wxHttpService.sendText(userCache, content, toUserName);
    }

    @Override
    public void revoke(WxUserCache userCache, String clientMsgId, String toUserName) {
        wxHttpService.revoke(userCache, toUserName, clientMsgId);
    }

    private void replyMessage(WxUserCache userCache, Message message) throws IOException {
        wxHttpService.sendText(userCache, message.getFromUserName(), message.getContent());
    }

    /**
     * 下载图片到本地
     *
     * @param userCache
     * @param imageUrl
     */
    private void downloadImage(WxUserCache userCache, String imageUrl) {
        try {
            byte[] data = wxHttpService.downloadImage(userCache, imageUrl);
            FileUtils.writeByteArrayToFile(new File(imageDir + System.currentTimeMillis() + ".jpg"), data, true);
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }
}