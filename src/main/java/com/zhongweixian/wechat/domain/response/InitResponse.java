package com.zhongweixian.wechat.domain.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhongweixian.wechat.domain.response.component.WechatHttpResponseBase;
import com.zhongweixian.wechat.domain.shared.Contact;
import com.zhongweixian.wechat.domain.shared.MPSubscription;
import com.zhongweixian.wechat.domain.shared.Owner;
import com.zhongweixian.wechat.domain.shared.SyncKey;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InitResponse extends WechatHttpResponseBase {
    @JsonProperty
    private int Count;
    @JsonProperty
    private Set<Contact> ContactList;
    @JsonProperty
    private com.zhongweixian.wechat.domain.shared.SyncKey SyncKey;
    @JsonProperty
    private Owner User;
    @JsonProperty
    private String ChatSet;
    @JsonProperty
    private String SKey;
    @JsonProperty
    private String ClientVersion;
    @JsonProperty
    private long SystemTime;
    @JsonProperty
    private int GrayScale;
    @JsonProperty
    private int InviteStartCount;
    @JsonProperty
    private int MPSubscribeMsgCount;
    @JsonProperty
    private MPSubscription[] MPSubscribeMsgList;
    @JsonProperty
    private long ClickReportInterval;

    public int getCount() {
        return Count;
    }

    public void setCount(int count) {
        Count = count;
    }

    public Set<Contact> getContactList() {
        return ContactList;
    }

    public void setContactList(Set<Contact> contactList) {
        ContactList = contactList;
    }

    public SyncKey getSyncKey() {
        return SyncKey;
    }

    public void setSyncKey(SyncKey syncKey) {
        SyncKey = syncKey;
    }

    public Owner getUser() {
        return User;
    }

    public void setUser(Owner user) {
        User = user;
    }

    public String getChatSet() {
        return ChatSet;
    }

    public void setChatSet(String chatSet) {
        ChatSet = chatSet;
    }

    public String getSKey() {
        return SKey;
    }

    public void setSKey(String SKey) {
        this.SKey = SKey;
    }

    public String getClientVersion() {
        return ClientVersion;
    }

    public void setClientVersion(String clientVersion) {
        ClientVersion = clientVersion;
    }

    public long getSystemTime() {
        return SystemTime;
    }

    public void setSystemTime(long systemTime) {
        SystemTime = systemTime;
    }

    public int getGrayScale() {
        return GrayScale;
    }

    public void setGrayScale(int grayScale) {
        GrayScale = grayScale;
    }

    public int getInviteStartCount() {
        return InviteStartCount;
    }

    public void setInviteStartCount(int inviteStartCount) {
        InviteStartCount = inviteStartCount;
    }

    public int getMPSubscribeMsgCount() {
        return MPSubscribeMsgCount;
    }

    public void setMPSubscribeMsgCount(int MPSubscribeMsgCount) {
        this.MPSubscribeMsgCount = MPSubscribeMsgCount;
    }

    public MPSubscription[] getMPSubscribeMsgList() {
        return MPSubscribeMsgList;
    }

    public void setMPSubscribeMsgList(MPSubscription[] MPSubscribeMsgList) {
        this.MPSubscribeMsgList = MPSubscribeMsgList;
    }

    public long getClickReportInterval() {
        return ClickReportInterval;
    }

    public void setClickReportInterval(long clickReportInterval) {
        ClickReportInterval = clickReportInterval;
    }
}
