package com.zhongweixian.enums;

public enum ChatRoomNotify {
    OPEN(1),
    CLOSE(0);

    private final int code;

    ChatRoomNotify(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
