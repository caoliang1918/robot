package com.zhongweixian.domain;

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
     * client 发送时间
     */
    private Date date;

    /**
     * 微信发送时间
     */
    private Date sendTime;
    /**
     * 消息体
     */
    private String content;

    /**
     * create/update/delete
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

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    @Override
    public String toString() {
        return "HttpMessage{" +
                "id=" + id +
                ", date=" + date +
                ", sendTime=" + sendTime +
                ", content='" + content + '\'' +
                ", option='" + option + '\'' +
                '}';
    }
}
