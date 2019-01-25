package com.zhongweixian.wechat.domain;

import com.zhongweixian.wechat.domain.request.component.BaseRequest;
import com.zhongweixian.wechat.domain.shared.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by caoliang on 2019/1/24
 */
public class BaseUserCache implements Serializable {

    /**
     * qrcode uuid
     */
    private String uuid;
    private Boolean alive;
    private String passTicket;
    private BaseRequest baseRequest;
    private Owner owner;
    private SyncKey syncKey;
    private SyncCheckKey syncCheckKey;
    private String sKey;
    private String uin;
    private String sid;

    /**
     * 聊天群组
     */
    private Map<String, ChatRoomDescription> chatRooms = new HashMap<>();
    /**
     * 联系人
     */
    private Map<String, Contact> chatContants = new HashMap<>();


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getAlive() {
        return alive;
    }

    public void setAlive(Boolean alive) {
        this.alive = alive;
    }

    public String getPassTicket() {
        return passTicket;
    }

    public void setPassTicket(String passTicket) {
        this.passTicket = passTicket;
    }

    public BaseRequest getBaseRequest() {
        return baseRequest;
    }

    public void setBaseRequest(BaseRequest baseRequest) {
        this.baseRequest = baseRequest;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public SyncKey getSyncKey() {
        return syncKey;
    }

    public void setSyncKey(SyncKey syncKey) {
        this.syncKey = syncKey;
    }

    public SyncCheckKey getSyncCheckKey() {
        return syncCheckKey;
    }

    public void setSyncCheckKey(SyncCheckKey syncCheckKey) {
        this.syncCheckKey = syncCheckKey;
    }

    public String getsKey() {
        return sKey;
    }

    public void setsKey(String sKey) {
        this.sKey = sKey;
    }

    public String getUin() {
        return uin;
    }

    public void setUin(String uin) {
        this.uin = uin;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public Map<String, ChatRoomDescription> getChatRooms() {
        return chatRooms;
    }

    public void setChatRooms(Map<String, ChatRoomDescription> chatRooms) {
        this.chatRooms = chatRooms;
    }

    public Map<String, Contact> getChatContants() {
        return chatContants;
    }

    public void setChatContants(Map<String, Contact> chatContants) {
        this.chatContants = chatContants;
    }
}
