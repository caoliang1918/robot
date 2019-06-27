package com.zhongweixian.domain.request;

import com.zhongweixian.domain.request.component.BaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpLogRequest {
    @JsonProperty
    private com.zhongweixian.domain.request.component.BaseRequest BaseRequest;
    @JsonProperty
    private int CmdId;
    @JsonProperty
    private String RemarkName;
    @JsonProperty
    private String UserName;

    public BaseRequest getBaseRequest() {
        return BaseRequest;
    }

    public void setBaseRequest(BaseRequest baseRequest) {
        BaseRequest = baseRequest;
    }

    public int getCmdId() {
        return CmdId;
    }

    public void setCmdId(int cmdId) {
        CmdId = cmdId;
    }

    public String getRemarkName() {
        return RemarkName;
    }

    public void setRemarkName(String remarkName) {
        RemarkName = remarkName;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }
}