package com.zhongweixian.web.entity.page;

import java.util.List;
import java.util.Map;


public class Page<T> {

    /**
     * 每页显示数
     */
    private int pageSize;

    /**
     * 当前第几页
     */
    private int pageNum;
    /**
     * 总的记录数
     */
    private int total;
    /**
     * 总页数
     */
    private int totleSize;


    /**
     * 分页参数
     */
    private String orderField;

    private String orderDirection;
    /**
     * 单页数据
     */
    private List<T> list;

    /**
     * @param pageSize 页面大小
     * @param offset   起始页
     * @param total    总条数
     * @param list     单页数据
     */
    public Page(int pageSize, int offset, int total, List<T> list) {
        this.pageSize = pageSize;
        this.pageNum = offset / pageSize + 1;
        this.total = total;
        this.totleSize = totalPage(total, pageSize);
        this.list = list;
    }

    public Page() {
    }

    public Page(Map<String, Object> params, int count, List<T> list) {
        this.list = list;
        this.total = count;
        if (params.containsKey("limit") && params.containsKey("offset")) {
            int limit = (int) params.get("limit");
            int offset = (int) params.get("offset");
            this.pageNum = offset / limit + 1;
            this.pageSize = limit;
            this.totleSize = totalPage(count, limit);
        }
    }


    //计算总页数
    private static int totalPage(int count, int limit) {
        if (count % limit == 0) {
            return count / limit;
        } else {
            return count / limit + 1;
        }
    }

    // 计算统一认证的第几页
    public static int solvePageNum(int limit, int offset) {
        return offset / limit + 1;
    }


    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotleSize() {
        return totleSize;
    }

    public void setTotleSize(int totleSize) {
        this.totleSize = totleSize;
    }

    public String getOrderField() {
        return orderField;
    }

    public void setOrderField(String orderField) {
        this.orderField = orderField;
    }

    public String getOrderDirection() {
        return orderDirection;
    }

    public void setOrderDirection(String orderDirection) {
        this.orderDirection = orderDirection;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
