package com.zhongweixian.wechat.domain.request;

import com.zhongweixian.wechat.domain.request.component.BaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteChatRoomMemberRequest {

    @JsonProperty
    private BaseRequest BaseRequest;
    @JsonProperty
    private String ChatRoomName;
    @JsonProperty
    private String DelMemberList;

    public BaseRequest getBaseRequest() {
        return BaseRequest;
    }

    public void setBaseRequest(BaseRequest baseRequest) {
        BaseRequest = baseRequest;
    }

    public String getChatRoomName() {
        return ChatRoomName;
    }

    public void setChatRoomName(String chatRoomName) {
        ChatRoomName = chatRoomName;
    }

    public String getDelMemberList() {
        return DelMemberList;
    }

    public void setDelMemberList(String delMemberList) {
        DelMemberList = delMemberList;
    }
}
