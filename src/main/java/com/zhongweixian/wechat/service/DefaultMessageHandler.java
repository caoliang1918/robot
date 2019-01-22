package com.zhongweixian.wechat.service;

import com.zhongweixian.wechat.domain.shared.ChatRoomMember;
import com.zhongweixian.wechat.domain.shared.Contact;
import com.zhongweixian.wechat.domain.shared.Message;
import com.zhongweixian.wechat.domain.shared.RecommendInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class DefaultMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageHandler.class);

    @Override
    public void onReceivingChatRoomTextMessage(Message message) {
        logger.info("onReceivingChatRoomTextMessage");
    }

    @Override
    public void onReceivingChatRoomImageMessage(Message message, String thumbImageUrl, String fullImageUrl) {
        logger.info("onReceivingChatRoomImageMessage");
    }

    @Override
    public void onReceivingPrivateTextMessage(Message message) throws IOException {
        logger.info("onReceivingPrivateTextMessage");
    }

    @Override
    public void onReceivingPrivateImageMessage(Message message, String thumbImageUrl, String fullImageUrl) throws IOException {
        logger.info("onReceivingPrivateImageMessage");
    }

    @Override
    public boolean onReceivingFriendInvitation(RecommendInfo info) {
        logger.info("onReceivingFriendInvitation");
        return false;
    }

    @Override
    public void postAcceptFriendInvitation(Message message) throws IOException {
        logger.info("postAcceptFriendInvitation");
    }

    @Override
    public void onChatRoomMembersChanged(Contact chatRoom, Set<ChatRoomMember> membersJoined, Set<ChatRoomMember> membersLeft) {
        logger.info("onChatRoomMembersChanged");
    }

    @Override
    public void onNewChatRoomsFound(Set<Contact> chatRooms) {
        logger.info("onNewChatRoomsFound");
    }

    @Override
    public void onChatRoomsDeleted(Set<Contact> chatRooms) {
        logger.info("onChatRoomsDeleted");
    }

    @Override
    public void onNewFriendsFound(Set<Contact> contacts) {
        logger.info("onNewFriendsFound");
    }

    @Override
    public void onFriendsDeleted(Set<Contact> contacts) {
        logger.info("onFriendsDeleted");
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
    }
}
