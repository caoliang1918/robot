package com.zhongweixian.service;


import com.zhongweixian.domain.BaseUserCache;
import com.zhongweixian.domain.response.*;
import com.zhongweixian.domain.shared.ChatRoomDescription;
import com.zhongweixian.domain.shared.Contact;
import com.zhongweixian.utils.WechatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class WechatMessageService {
    private Logger logger = LoggerFactory.getLogger(WechatMessageService.class);

    @Autowired
    private WechatHttpService wechatHttpService;

    /**
     * Log out
     *
     * @throws IOException if logout fails
     */
    public void logout(BaseUserCache userCache) throws IOException {
        wechatHttpService.logout(userCache);
    }

    /**
     * Get all the contacts
     *
     * @param userCache
     * @return
     * @throws IOException
     */
    public Set<Contact> getContact(BaseUserCache userCache) throws IOException {
        Set<Contact> contacts = new HashSet<>();
        long seq = 0;
        do {
            ContactResponse response = wechatHttpService.getContact(userCache);
            WechatUtils.checkBaseResponse(response);
            seq = response.getSeq();
            contacts.addAll(response.getMemberList());
        }
        while (seq > 0);
        return contacts;
    }

    /**
     * 发送文本消息
     *
     * @param userCache
     * @param toUserName
     * @param content
     * @return
     * @throws IOException
     */
    public SendMsgResponse sendText(BaseUserCache userCache, String toUserName, String content) throws IOException {
        //notifyNecessary(userName);
        SendMsgResponse response = wechatHttpService.sendText(userCache, content, toUserName);
        logger.info("sendMsgResponse:{}", response.toString());
        WechatUtils.checkBaseResponse(response);
        return response;
    }

    public void revoke(BaseUserCache userCache, String wxMessageId, String toUserName) throws IOException {
        wechatHttpService.revoke(userCache, toUserName, wxMessageId);
    }

    /**
     * Set the alias of a contact
     *
     * @param userName the username of the contact
     * @param newAlias alias
     * @throws IOException if setAlias fails
     */
    public void setAlias(BaseUserCache userCache, String userName, String newAlias) throws IOException {
        OpLogResponse response = wechatHttpService.setAlias(userCache, newAlias, userName);
        WechatUtils.checkBaseResponse(response);
    }

    /**
     * Get contacts in chatrooms
     *
     * @param list chatroom usernames
     * @return chatroom list
     * @throws IOException if batchGetContact fails
     */
    public Set<Contact> batchGetContact(BaseUserCache userCache, Set<String> list) throws IOException {
        ChatRoomDescription[] descriptions =
                list.stream().map(x -> {
                    ChatRoomDescription description = new ChatRoomDescription();
                    description.setUserName(x);
                    return description;
                }).toArray(ChatRoomDescription[]::new);
        BatchGetContactResponse response = wechatHttpService.batchGetContact(userCache, descriptions);
        WechatUtils.checkBaseResponse(response);
        return response.getContactList();
    }

    /**
     * Create a chatroom with a topic.
     * In fact, a topic is usually not provided when creating the chatroom.
     *
     * @param userNames the usernames of the contacts who are invited to the chatroom.
     * @param topic     the topic(or nickname)
     * @throws IOException
     */
    public void createChatRoom(BaseUserCache userCache, String[] userNames, String topic) throws IOException {
        CreateChatRoomResponse response = wechatHttpService.createChatRoom(userCache, userNames, topic);
        WechatUtils.checkBaseResponse(response);
        //invoke BatchGetContact after CreateChatRoom
        ChatRoomDescription description = new ChatRoomDescription();
        description.setUserName(response.getChatRoomName());
        ChatRoomDescription[] descriptions = new ChatRoomDescription[]{description};
        BatchGetContactResponse batchGetContactResponse = wechatHttpService.batchGetContact(userCache, descriptions);
        WechatUtils.checkBaseResponse(batchGetContactResponse);
        //userCache.getChatRooms().addAll(batchGetContactResponse.getContactList());
    }

    /**
     * Delete a contact from a certain chatroom (if you're the owner!)
     *
     * @param chatRoomUserName chatroom username
     * @param userName         contact username
     * @throws IOException if remove chatroom member fails
     */
    public void deleteChatRoomMember(BaseUserCache userCache, String chatRoomUserName, String userName) throws IOException {
        DeleteChatRoomMemberResponse response = wechatHttpService.deleteChatRoomMember(userCache, chatRoomUserName, userName);
        WechatUtils.checkBaseResponse(response);
    }

    /**
     * 群添加人
     *
     * @param userCache
     * @param chatRoomUserName
     * @param userName
     * @throws IOException
     */
    public void addChatRoomMember(BaseUserCache userCache, String chatRoomUserName, String userName) throws IOException {
        AddChatRoomMemberResponse response = wechatHttpService.addChatRoomMember(userCache, chatRoomUserName, userName);
        WechatUtils.checkBaseResponse(response);
        userCache.getChatRoomMembers().get(chatRoomUserName).getMemberList().addAll(response.getMemberList());
    }

    /**
     * download images in the conversation. Note that it's better not to download image directly. This method has included cookies in the request.
     *
     * @param url image url
     * @return image data
     */
    public byte[] downloadImage(BaseUserCache userCache, String url) {
        return wechatHttpService.downloadImage(userCache, url);
    }


}
