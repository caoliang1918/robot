package com.zhongweixian.enums;

public enum SpecialContact {

    FILE_HELPER("filehelper"),
    NEWSAPP("newsapp"),
    RECOMMEND_HELP("fmessage");

    private final String code;

    public String getCode() {
        return code;
    }

    SpecialContact(String code) {
        this.code = code;
    }
}
