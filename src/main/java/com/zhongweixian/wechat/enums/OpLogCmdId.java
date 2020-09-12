package com.zhongweixian.wechat.enums;

public enum OpLogCmdId {
    TOPCONTACT(3),
    MODREMARKNAME(2);

    private final int code;

    OpLogCmdId(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
