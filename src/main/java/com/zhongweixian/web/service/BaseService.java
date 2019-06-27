package com.zhongweixian.web.service;

import com.zhongweixian.web.entity.page.Page;

import java.util.Map;

/**
 * Created by caoliang on 2019-06-03
 */
public interface BaseService<T> {

    /**
     * 根据记录新增
     *
     * @param record
     * @return
     */
    int add(T record);

    /**
     * 根据id删除
     *
     * @param id
     * @return
     */
    int deleteById(Integer id);

    /**
     * 根据id修改（只修改不为空的字段）
     *
     * @param record
     * @return
     */
    int editById(T record);

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    T findById(Integer id);


    /**
     * 分页查询
     *
     * @param params
     * @return
     */
    Page<T> findByPageParams(Map params);


}
