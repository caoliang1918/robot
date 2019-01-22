package com.zhongweixian.wechat.domain.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhongweixian.wechat.domain.request.component.BaseRequest;

/**
 * Created by caoliang on 2019/1/14
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevokeRequst {

    @JsonProperty
    private BaseRequest BaseRequest;
    @JsonProperty
    private String SvrMsgId;
    @JsonProperty
    private String ToUserName;
    @JsonProperty
    private String ClientMsgId;

    public RevokeRequst() {
    }

    public RevokeRequst(String toUserName, String clientMsgId) {
        ToUserName = toUserName;
        ClientMsgId = clientMsgId;
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
}
