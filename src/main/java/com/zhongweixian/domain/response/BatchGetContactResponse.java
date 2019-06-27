package com.zhongweixian.domain.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhongweixian.domain.response.component.WechatHttpResponseBase;
import com.zhongweixian.domain.shared.Contact;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchGetContactResponse extends WechatHttpResponseBase {
    @JsonProperty
    private Set<Contact> ContactList;
    @JsonProperty
    private int Count;

    public Set<Contact> getContactList() {
        return ContactList;
    }

    public void setContactList(Set<Contact> contactList) {
        ContactList = contactList;
    }

    public int getCount() {
        return Count;
    }

    public void setCount(int count) {
        Count = count;
    }
}
