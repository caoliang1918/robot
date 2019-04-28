package com.zhongweixian.domain.response.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class WechatHttpResponseBase {
    @JsonProperty
    private BaseResponse BaseResponse;

    public BaseResponse getBaseResponse() {
        return BaseResponse;
    }

    public void setBaseResponse(BaseResponse baseResponse) {
        BaseResponse = baseResponse;
    }

    @Override
    public String toString() {
        return "WechatHttpResponseBase{" +
                "BaseResponse=" + BaseResponse +
                '}';
    }
}
