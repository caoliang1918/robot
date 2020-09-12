package com.zhongweixian.cache;

import com.zhongweixian.wechat.domain.WxUserCache;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CacheService {

    private Map<String, WxUserCache> wxUserCache = new HashMap<>();

    public WxUserCache getUserCache(String uid) {
        return wxUserCache.get(uid);
    }

    public void cacheUser(WxUserCache wxUserCache) {
        this.wxUserCache.put(wxUserCache.getUin(), wxUserCache);
    }

    public void deleteCacheUser(String uid) {
        wxUserCache.remove(uid);
    }


    private String hostUrl;


    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

}