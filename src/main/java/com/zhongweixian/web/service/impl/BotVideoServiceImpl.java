package com.zhongweixian.web.service.impl;

import com.zhongweixian.web.entity.BotVideo;
import com.zhongweixian.web.mapper.BaseMapper;
import com.zhongweixian.web.mapper.BotVideoMapper;
import com.zhongweixian.web.service.BotVideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BotVideoServiceImpl extends BaseServiceImpl<BotVideo> implements BotVideoService {
    private Logger logger = LoggerFactory.getLogger(BotVideoServiceImpl.class);

    @Autowired
    private BotVideoMapper botVideoMapper;

    @Override
    BaseMapper<BotVideo> baseMapper() {
        return botVideoMapper;
    }
}
