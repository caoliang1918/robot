package com.zhongweixian.web.service;

import com.zhongweixian.web.entity.BotVideo;

public interface BotVideoService extends BaseService<BotVideo> {


    /**
     * 通过文件大小和hashcode过滤重复文件
     *
     * @param size
     * @param hashCode
     * @return
     */
    BotVideo findByHashCode(Integer size, String hashCode);


}
