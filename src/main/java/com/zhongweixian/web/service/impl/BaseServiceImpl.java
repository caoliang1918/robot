package com.zhongweixian.web.service.impl;

import com.zhongweixian.web.entity.page.Page;
import com.zhongweixian.web.mapper.BaseMapper;
import com.zhongweixian.web.service.BaseService;

import java.util.List;
import java.util.Map;

/**
 * Created by caoliang on 2019-06-03
 */
public abstract class BaseServiceImpl<T> implements BaseService<T> {

    abstract BaseMapper<T> baseMapper();

    @Override
    public int add(T record) {
        return baseMapper().insertSelective(record);
    }

    @Override
    public int deleteById(Integer id) {
        return baseMapper().deleteByPrimaryKey(id);
    }

    @Override
    public int editById(T record) {
        return baseMapper().updateByPrimaryKeySelective(record);
    }

    @Override
    public T findById(Integer id) {
        return baseMapper().selectByPrimaryKey(id);
    }

    @Override
    public Page<T> findByPageParams(Map params) {
        int pageNum = (int) params.get("pageNum");
        int limit = (int) params.get("limit");
        params.put("offset", pageNum * limit);
        Integer total = baseMapper().selectCountByMap(params);
        List<T> list = baseMapper().selectListByMap(params);
        return new Page<T>(limit, pageNum * limit, total, list);
    }
}
