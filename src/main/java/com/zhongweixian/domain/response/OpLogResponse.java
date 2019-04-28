package com.zhongweixian.domain.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zhongweixian.domain.response.component.WechatHttpResponseBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpLogResponse extends WechatHttpResponseBase {
}