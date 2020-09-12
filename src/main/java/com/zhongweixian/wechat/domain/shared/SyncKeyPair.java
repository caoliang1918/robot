package com.zhongweixian.wechat.domain.shared;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncKeyPair {
    @JsonProperty
    private int Key;
    @JsonProperty
    private String Val;

    public int getKey() {
        return Key;
    }

    public void setKey(int key) {
        Key = key;
    }

    public String getVal() {
        return Val;
    }

    public void setVal(String val) {
        Val = val;
    }
}
