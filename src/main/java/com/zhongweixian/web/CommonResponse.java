package com.zhongweixian.web;

/**
 * @author: caoliang
 */
public class CommonResponse<T> {
    int code;
    String message = "success";
    T data;

    public CommonResponse() {
    }

    public CommonResponse(T data) {
        this.data = data;
    }

    public CommonResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.data = null;
    }

    public CommonResponse(int code, String message, T data) {
        this.data = data;
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "CommonResponse{code=" + this.code + ", message=\'" + this.message + '\'' + ", data=" + this.data + '}';
    }
}
