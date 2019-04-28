package com.zhongweixian.domain.request;

import org.apache.catalina.util.URLEncoder;
import org.apache.commons.codec.binary.Base64;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * Created by caoliang on 2019-04-28
 */
public class WeiBoRequest implements Serializable {

    String location;
    String text;
    String appkey;
    String style_type;
    private String pic_id;
    private String tid;
    private String pdetail;
    private String mid;
    private boolean isReEdit;
    private String rank;
    private String rankid;
    private String module;
    private String pub_source;
    private String pub_type;
    private String isPri;
    private String _t;

    public WeiBoRequest(String text) {
        this.location = "v6_content_home";
        this.text = new URLEncoder().encode(text , Charset.defaultCharset());
        this.appkey = appkey;
        this.style_type = "1";
        this.pic_id = pic_id;
        this.tid = tid;
        this.pdetail = pdetail;
        this.mid = mid;
        this.isReEdit = false;
        this.rank = "0";
        this.rankid = rankid;
        this.module = "stissue";
        this.pub_source = "main_";
        this.pub_type = "dialog";
        this.isPri = "0";
        this._t = "0";
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public String getStyle_type() {
        return style_type;
    }

    public void setStyle_type(String style_type) {
        this.style_type = style_type;
    }

    public String getPic_id() {
        return pic_id;
    }

    public void setPic_id(String pic_id) {
        this.pic_id = pic_id;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getPdetail() {
        return pdetail;
    }

    public void setPdetail(String pdetail) {
        this.pdetail = pdetail;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public boolean isReEdit() {
        return isReEdit;
    }

    public void setReEdit(boolean reEdit) {
        isReEdit = reEdit;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getRankid() {
        return rankid;
    }

    public void setRankid(String rankid) {
        this.rankid = rankid;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getPub_source() {
        return pub_source;
    }

    public void setPub_source(String pub_source) {
        this.pub_source = pub_source;
    }

    public String getPub_type() {
        return pub_type;
    }

    public void setPub_type(String pub_type) {
        this.pub_type = pub_type;
    }

    public String getIsPri() {
        return isPri;
    }

    public void setIsPri(String isPri) {
        this.isPri = isPri;
    }

    public String get_t() {
        return _t;
    }

    public void set_t(String _t) {
        this._t = _t;
    }
}
