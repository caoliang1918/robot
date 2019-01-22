package com.zhongweixian.wechat.domain.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zhongweixian.wechat.domain.response.component.WechatHttpResponseBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteChatRoomMemberResponse extends WechatHttpResponseBase {
}
