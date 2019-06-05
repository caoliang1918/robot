package com.zhongweixian.service;

import com.zhongweixian.domain.BaseUserCache;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CacheService {

    private Map<String, BaseUserCache> wxUserCache = new HashMap<>();

    public BaseUserCache getUserCache(String uid) {
        return wxUserCache.get(uid);
    }

    public void cacheUser(BaseUserCache baseUserCache) {
        wxUserCache.put(baseUserCache.getUin(), baseUserCache);
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