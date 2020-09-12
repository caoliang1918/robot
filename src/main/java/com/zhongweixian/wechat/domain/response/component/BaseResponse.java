package com.zhongweixian.wechat.domain.response.component;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BaseResponse {
    @JsonProperty
    private int Ret;
    @JsonProperty
    private String ErrMsg;

    public int getRet() {
        return Ret;
    }

    public void setRet(int ret) {
        Ret = ret;
    }

    public String getErrMsg() {
        return ErrMsg;
    }

    public void setErrMsg(String errMsg) {
        ErrMsg = errMsg;
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "Ret=" + Ret +
                ", ErrMsg='" + ErrMsg + '\'' +
                '}';
    }
}
