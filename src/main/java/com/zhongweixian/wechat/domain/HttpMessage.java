package com.zhongweixian.wechat.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by caoliang on 2019/1/16
 */
public class HttpMessage implements Serializable {

    /**
     * 消息id
     */
    private Long id;
    /**
     * 发送时间
     */
    private Date date;
    /**
     * 消息体
     */
    private String content;

    /**
     * create/update
     */
    private String option;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    @Override
    public String toString() {
        return "HttpMessage{" +
                "id=" + id +
                ", date=" + date +
                ", content='" + content + '\'' +
                ", option='" + option + '\'' +
                '}';
    }
}
