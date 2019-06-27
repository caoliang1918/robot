package com.zhongweixian.domain.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhongweixian.domain.response.component.WechatHttpResponseBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusNotifyResponse extends WechatHttpResponseBase {
    @JsonProperty
    private String MsgID;

    public String getMsgID() {
        return MsgID;
    }

    public void setMsgID(String msgID) {
        MsgID = msgID;
    }
}
