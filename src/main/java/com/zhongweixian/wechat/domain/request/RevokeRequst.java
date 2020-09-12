package com.zhongweixian.wechat.domain.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhongweixian.wechat.domain.request.component.BaseRequest;

import java.util.Date;

/**
 * Created by caoliang on 2019/1/14
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevokeRequst {

    @JsonProperty
    private com.zhongweixian.wechat.domain.request.component.BaseRequest BaseRequest;
    @JsonProperty
    private String SvrMsgId;
    @JsonProperty
    private String ToUserName;
    @JsonProperty
    private String ClientMsgId;

    private String content;

    private Date date;

    private Long messageId;


    public RevokeRequst() {
    }

    public RevokeRequst(String toUserName, String clientMsgId , String content , Long httpMessageId) {
        ToUserName = toUserName;
        ClientMsgId = clientMsgId;
        this.content = content;
        this.date = new Date();
    }

    public BaseRequest getBaseRequest() {
        return BaseRequest;
    }

    public void setBaseRequest(BaseRequest baseRequest) {
        BaseRequest = baseRequest;
    }

    public String getSvrMsgId() {
        return SvrMsgId;
    }

    public void setSvrMsgId(String svrMsgId) {
        SvrMsgId = svrMsgId;
    }

    public String getToUserName() {
        return ToUserName;
    }

    public void setToUserName(String toUserName) {
        ToUserName = toUserName;
    }

    public String getClientMsgId() {
        return ClientMsgId;
    }

    public void setClientMsgId(String clientMsgId) {
        ClientMsgId = clientMsgId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
